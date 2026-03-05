package gov.irs.directfile.api.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import gov.irs.directfile.api.auth.model.ApiClient;

@Slf4j
@RestController
@RequestMapping("/v1/api-keys")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class ApiKeyManagementController {
    private final ApiClientRepository apiClientRepository;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public ApiKeyManagementController(ApiClientRepository apiClientRepository) {
        this.apiClientRepository = apiClientRepository;
    }

    @PostMapping
    public ResponseEntity<?> createApiKey(@RequestBody Map<String, String> request) {
        String clientName = request.get("client_name");
        if (clientName == null || clientName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "client_name is required"));
        }

        if (apiClientRepository.findByClientName(clientName).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Client name already exists"));
        }

        String apiKey = generateSecureToken();
        String clientSecret = generateSecureToken();
        String apiKeyHash = ApiKeyAuthenticationFilter.hashApiKey(apiKey);
        String clientSecretHash = ApiKeyAuthenticationFilter.hashApiKey(clientSecret);

        String scopesStr = request.getOrDefault("scopes", "read,write");

        ApiClient client = new ApiClient(clientName, apiKeyHash, clientSecretHash);
        client.setScopes(scopesStr);

        String expiresIn = request.get("expires_in_days");
        if (expiresIn != null) {
            client.setExpiresAt(Instant.now().plus(Long.parseLong(expiresIn), ChronoUnit.DAYS));
        }

        apiClientRepository.save(client);
        log.info("Created API client: {}", clientName);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "id", client.getId().toString(),
                        "client_name", clientName,
                        "api_key", apiKey,
                        "client_secret", clientSecret,
                        "scopes", scopesStr,
                        "message", "Store these credentials securely. They cannot be retrieved again."));
    }

    @GetMapping
    public ResponseEntity<?> listApiKeys() {
        List<ApiClient> clients = apiClientRepository.findByEnabledTrue();
        List<Map<String, Object>> result = clients.stream()
                .map(c -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", c.getId().toString());
                    map.put("client_name", c.getClientName());
                    map.put("scopes", c.getScopes());
                    map.put("enabled", c.isEnabled());
                    map.put(
                            "created_at",
                            c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
                    map.put(
                            "expires_at",
                            c.getExpiresAt() != null ? c.getExpiresAt().toString() : null);
                    return map;
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteApiKey(@PathVariable UUID id) {
        Optional<ApiClient> clientOpt = apiClientRepository.findById(id);
        if (clientOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ApiClient client = clientOpt.get();
        client.setEnabled(false);
        apiClientRepository.save(client);
        log.info("Disabled API client: {}", client.getClientName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/rotate")
    public ResponseEntity<?> rotateApiKey(@PathVariable UUID id) {
        Optional<ApiClient> clientOpt = apiClientRepository.findById(id);
        if (clientOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ApiClient client = clientOpt.get();
        String newApiKey = generateSecureToken();
        String newClientSecret = generateSecureToken();
        client.setApiKeyHash(ApiKeyAuthenticationFilter.hashApiKey(newApiKey));
        client.setClientSecretHash(ApiKeyAuthenticationFilter.hashApiKey(newClientSecret));
        apiClientRepository.save(client);
        log.info("Rotated credentials for API client: {}", client.getClientName());

        return ResponseEntity.ok(Map.of(
                "id",
                client.getId().toString(),
                "client_name",
                client.getClientName(),
                "api_key",
                newApiKey,
                "client_secret",
                newClientSecret,
                "message",
                "Store these new credentials securely. Previous credentials are now invalid."));
    }

    private static String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
