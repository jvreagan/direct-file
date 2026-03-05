package gov.irs.directfile.api.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import gov.irs.directfile.api.auth.model.ApiClient;

@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    private static final String API_KEY_HEADER = "X-API-Key";
    private final ApiClientRepository apiClientRepository;

    public ApiKeyAuthenticationFilter(ApiClientRepository apiClientRepository) {
        this.apiClientRepository = apiClientRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey != null
                && !apiKey.isBlank()
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String hashedKey = hashApiKey(apiKey);
            Optional<ApiClient> clientOpt = apiClientRepository.findByApiKeyHash(hashedKey);

            if (clientOpt.isPresent()) {
                ApiClient client = clientOpt.get();
                if (client.isEnabled() && !isExpired(client)) {
                    var authorities = client.getScopeSet().stream()
                            .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                            .collect(Collectors.toList());
                    ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(client, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authenticated API client: {}", client.getClientName());
                } else {
                    log.warn("API key for disabled/expired client: {}", client.getClientName());
                }
            } else {
                log.warn("Invalid API key presented");
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExpired(ApiClient client) {
        return client.getExpiresAt() != null && client.getExpiresAt().isBefore(java.time.Instant.now());
    }

    static String hashApiKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
