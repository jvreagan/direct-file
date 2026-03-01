package gov.irs.directfile.api.auth;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import gov.irs.directfile.api.auth.model.ApiClient;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyAuthenticationTokenTest {

    @Test
    void givenApiClient_whenGetPrincipal_thenReturnsClientId() {
        ApiClient client = new ApiClient("test-client", "key-hash", "secret-hash");
        var authorities = List.of(new SimpleGrantedAuthority("SCOPE_read"));

        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(client, authorities);

        assertEquals(client.getId(), token.getPrincipal());
    }

    @Test
    void givenApiClient_whenGetCredentials_thenReturnsNull() {
        ApiClient client = new ApiClient("test-client", "key-hash", "secret-hash");
        var authorities = List.of(new SimpleGrantedAuthority("SCOPE_read"));

        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(client, authorities);

        assertNull(token.getCredentials());
    }

    @Test
    void givenApiClient_whenCreated_thenIsAuthenticated() {
        ApiClient client = new ApiClient("test-client", "key-hash", "secret-hash");
        var authorities = List.of(new SimpleGrantedAuthority("SCOPE_read"));

        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(client, authorities);

        assertTrue(token.isAuthenticated());
    }

    @Test
    void givenApiClient_whenGetApiClient_thenReturnsSameInstance() {
        ApiClient client = new ApiClient("test-client", "key-hash", "secret-hash");
        var authorities = List.of(new SimpleGrantedAuthority("SCOPE_read"));

        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(client, authorities);

        assertSame(client, token.getApiClient());
    }
}
