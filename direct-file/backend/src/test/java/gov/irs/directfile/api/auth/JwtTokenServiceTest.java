package gov.irs.directfile.api.auth;

import java.util.Set;
import java.util.UUID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {
    private JwtTokenService jwtTokenService;

    private static final String SIGNING_KEY = "test-signing-key-that-is-at-least-32-bytes-long!!";
    private static final String ISSUER = "test-issuer";
    private static final long TTL_SECONDS = 3600;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(SIGNING_KEY, ISSUER, TTL_SECONDS);
    }

    @Test
    void givenValidInputs_whenGenerateAndValidate_thenClaimsMatch() throws Exception {
        UUID clientId = UUID.randomUUID();
        String clientName = "test-client";
        Set<String> scopes = Set.of("read", "write");

        String token = jwtTokenService.generateToken(clientId, clientName, scopes);
        JWTClaimsSet claims = jwtTokenService.validateToken(token);

        assertEquals(clientId.toString(), claims.getSubject());
        assertEquals(ISSUER, claims.getIssuer());
        assertEquals(clientId.toString(), claims.getStringClaim("client_id"));
        assertEquals(clientName, claims.getStringClaim("client_name"));
        assertNotNull(claims.getExpirationTime());
        assertNotNull(claims.getIssueTime());
        assertNotNull(claims.getJWTID());
    }

    @Test
    void givenValidToken_whenValidate_thenScopesPreserved() throws Exception {
        UUID clientId = UUID.randomUUID();
        Set<String> scopes = Set.of("read", "write");

        String token = jwtTokenService.generateToken(clientId, "client", scopes);
        JWTClaimsSet claims = jwtTokenService.validateToken(token);

        String scopesClaim = claims.getStringClaim("scopes");
        assertNotNull(scopesClaim);
        assertTrue(scopesClaim.contains("read"));
        assertTrue(scopesClaim.contains("write"));
    }

    @Test
    void givenExpiredToken_whenValidate_thenThrowsJOSEException() throws Exception {
        JwtTokenService shortLivedService = new JwtTokenService(SIGNING_KEY, ISSUER, 1);
        String token = shortLivedService.generateToken(UUID.randomUUID(), "client", Set.of("read"));

        Thread.sleep(1100);

        assertThrows(JOSEException.class, () -> shortLivedService.validateToken(token));
    }

    @Test
    void givenWrongSigningKey_whenValidate_thenThrowsJOSEException() {
        UUID clientId = UUID.randomUUID();
        String token = jwtTokenService.generateToken(clientId, "client", Set.of("read"));

        JwtTokenService otherService =
                new JwtTokenService("different-signing-key-that-is-at-least-32-bytes!!", ISSUER, TTL_SECONDS);

        assertThrows(JOSEException.class, () -> otherService.validateToken(token));
    }

    @Test
    void givenTwoTokens_whenGenerate_thenHaveDifferentJwtIds() throws Exception {
        UUID clientId = UUID.randomUUID();
        String token1 = jwtTokenService.generateToken(clientId, "client", Set.of("read"));
        String token2 = jwtTokenService.generateToken(clientId, "client", Set.of("read"));

        JWTClaimsSet claims1 = jwtTokenService.validateToken(token1);
        JWTClaimsSet claims2 = jwtTokenService.validateToken(token2);

        assertNotEquals(claims1.getJWTID(), claims2.getJWTID());
    }
}
