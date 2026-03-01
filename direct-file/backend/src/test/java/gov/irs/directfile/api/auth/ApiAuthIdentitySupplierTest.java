package gov.irs.directfile.api.auth;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import gov.irs.directfile.api.auth.model.ApiClient;
import gov.irs.directfile.api.config.identity.IdentityAttributes;

import static org.junit.jupiter.api.Assertions.*;

class ApiAuthIdentitySupplierTest {
    private ApiAuthIdentitySupplier identitySupplier;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        identitySupplier = new ApiAuthIdentitySupplier();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenApiKeyAuthentication_whenGet_thenReturnsCorrectIdentity() {
        ApiClient client = new ApiClient("test-client", "key-hash", "secret-hash");
        var authorities = List.of(new SimpleGrantedAuthority("SCOPE_read"));
        ApiKeyAuthenticationToken auth = new ApiKeyAuthenticationToken(client, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        IdentityAttributes identity = identitySupplier.get();

        assertEquals(client.getId(), identity.id());
        assertEquals("test-client", identity.email());
    }

    @Test
    void givenJwtAuthentication_whenGet_thenReturnsCorrectIdentity() {
        UUID clientId = UUID.randomUUID();
        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", clientId.toString(), "client_name", "jwt-client"));
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);

        IdentityAttributes identity = identitySupplier.get();

        assertEquals(clientId, identity.id());
        assertEquals(clientId, identity.externalId());
        assertEquals("jwt-client", identity.email());
    }

    @Test
    void givenUnsupportedAuthentication_whenGet_thenThrowsIllegalStateException() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("user", "pass");
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(IllegalStateException.class, () -> identitySupplier.get());
    }
}
