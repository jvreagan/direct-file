package gov.irs.directfile.api.auth;

import java.time.Instant;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import gov.irs.directfile.api.auth.model.ApiClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationFilterTest {
    private ApiKeyAuthenticationFilter filter;

    @Mock
    private ApiClientRepository apiClientRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new ApiKeyAuthenticationFilter(apiClientRepository);
    }

    @Test
    void givenValidApiKey_whenDoFilter_thenAuthenticationIsSet() throws Exception {
        String apiKey = "valid-api-key";
        String hashedKey = ApiKeyAuthenticationFilter.hashApiKey(apiKey);
        ApiClient client = new ApiClient("test-client", hashedKey, "secret-hash");

        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiClientRepository.findByApiKeyHash(hashedKey)).thenReturn(Optional.of(client));

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertInstanceOf(
                ApiKeyAuthenticationToken.class,
                SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void givenNoApiKeyHeader_whenDoFilter_thenNoAuthenticationSet() throws Exception {
        when(request.getHeader("X-API-Key")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(apiClientRepository, never()).findByApiKeyHash(anyString());
    }

    @Test
    void givenUnknownApiKey_whenDoFilter_thenNoAuthenticationSet() throws Exception {
        String apiKey = "unknown-key";
        String hashedKey = ApiKeyAuthenticationFilter.hashApiKey(apiKey);

        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiClientRepository.findByApiKeyHash(hashedKey)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void givenDisabledClient_whenDoFilter_thenNoAuthenticationSet() throws Exception {
        String apiKey = "disabled-key";
        String hashedKey = ApiKeyAuthenticationFilter.hashApiKey(apiKey);
        ApiClient client = new ApiClient("disabled-client", hashedKey, "secret-hash");
        client.setEnabled(false);

        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiClientRepository.findByApiKeyHash(hashedKey)).thenReturn(Optional.of(client));

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void givenExpiredClient_whenDoFilter_thenNoAuthenticationSet() throws Exception {
        String apiKey = "expired-key";
        String hashedKey = ApiKeyAuthenticationFilter.hashApiKey(apiKey);
        ApiClient client = new ApiClient("expired-client", hashedKey, "secret-hash");
        client.setExpiresAt(Instant.now().minusSeconds(3600));

        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiClientRepository.findByApiKeyHash(hashedKey)).thenReturn(Optional.of(client));

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void givenSameInput_whenHashApiKey_thenProducesSameOutput() {
        String key = "test-api-key-12345";

        String hash1 = ApiKeyAuthenticationFilter.hashApiKey(key);
        String hash2 = ApiKeyAuthenticationFilter.hashApiKey(key);

        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length()); // SHA-256 produces 64 hex chars
    }
}
