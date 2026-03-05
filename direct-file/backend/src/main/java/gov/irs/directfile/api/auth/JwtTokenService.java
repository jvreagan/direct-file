package gov.irs.directfile.api.auth;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JwtTokenService {
    private final byte[] signingKey;
    private final String issuer;
    private final long tokenTtlSeconds;

    public JwtTokenService(
            @Value("${direct-file.auth.jwt.signing-key:default-dev-signing-key-must-be-at-least-32-bytes-long!!}")
                    String signingKey,
            @Value("${direct-file.auth.jwt.issuer:direct-file-api}") String issuer,
            @Value("${direct-file.auth.jwt.ttl-seconds:3600}") long tokenTtlSeconds) {
        this.signingKey = signingKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        this.issuer = issuer;
        this.tokenTtlSeconds = tokenTtlSeconds;
    }

    public String generateToken(UUID clientId, String clientName, Set<String> scopes) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(clientId.toString())
                    .issuer(issuer)
                    .claim("client_id", clientId.toString())
                    .claim("client_name", clientName)
                    .claim("scopes", String.join(",", scopes))
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(tokenTtlSeconds)))
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                    .type(JOSEObjectType.JWT)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(new MACSigner(signingKey));

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to generate JWT", e);
        }
    }

    public JWTClaimsSet validateToken(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWSVerifier verifier = new MACVerifier(signingKey);

        if (!signedJWT.verify(verifier)) {
            throw new JOSEException("JWT signature verification failed");
        }

        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        if (claims.getExpirationTime() != null && claims.getExpirationTime().before(new Date())) {
            throw new JOSEException("JWT has expired");
        }

        return claims;
    }
}
