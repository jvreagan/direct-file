package gov.irs.directfile.api.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import gov.irs.directfile.api.auth.model.ApiClient;

@Slf4j
@RestController
@RequestMapping("/oauth")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class OAuth2TokenController {
    private final ApiClientRepository apiClientRepository;
    private final JwtTokenService jwtTokenService;

    public OAuth2TokenController(ApiClientRepository apiClientRepository, JwtTokenService jwtTokenService) {
        this.apiClientRepository = apiClientRepository;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam("client_id") String clientName,
            @RequestParam("client_secret") String clientSecret) {

        if (!"client_credentials".equals(grantType)) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "unsupported_grant_type",
                            "error_description", "Only client_credentials grant type is supported"));
        }

        Optional<ApiClient> clientOpt = apiClientRepository.findByClientName(clientName);
        if (clientOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "invalid_client",
                            "error_description", "Client not found"));
        }

        ApiClient client = clientOpt.get();
        String hashedSecret = ApiKeyAuthenticationFilter.hashApiKey(clientSecret);
        if (!MessageDigest.isEqual(
                hashedSecret.getBytes(StandardCharsets.UTF_8),
                client.getClientSecretHash().getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "invalid_client",
                            "error_description", "Invalid client secret"));
        }

        if (!client.isEnabled()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "invalid_client",
                            "error_description", "Client is disabled"));
        }

        String token = jwtTokenService.generateToken(client.getId(), client.getClientName(), client.getScopeSet());

        return ResponseEntity.ok(
                Map.of("access_token", token, "token_type", "Bearer", "expires_in", 3600, "scope", client.getScopes()));
    }
}
