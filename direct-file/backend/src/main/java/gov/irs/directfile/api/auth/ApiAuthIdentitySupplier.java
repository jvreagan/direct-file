package gov.irs.directfile.api.auth;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import gov.irs.directfile.api.auth.model.ApiClient;
import gov.irs.directfile.api.config.BeanProfiles;
import gov.irs.directfile.api.config.identity.IdentityAttributes;
import gov.irs.directfile.api.config.identity.IdentitySupplier;

@Slf4j
@Component
@Profile("!" + BeanProfiles.ENABLE_DEVELOPMENT_IDENTITY_SUPPLIER)
public class ApiAuthIdentitySupplier implements IdentitySupplier {

    @Override
    public IdentityAttributes get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("No authentication found in security context");
        }

        if (authentication instanceof ApiKeyAuthenticationToken apiKeyAuth) {
            ApiClient client = apiKeyAuth.getApiClient();
            UUID userId = client.getUser() != null ? client.getUser().getId() : client.getId();
            UUID externalId = client.getUser() != null ? client.getUser().getExternalId() : client.getId();
            return new IdentityAttributes(userId, externalId, client.getClientName(), null);
        }

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            UUID clientId = UUID.fromString(jwt.getSubject());
            String clientName = jwt.getClaimAsString("client_name");
            return new IdentityAttributes(clientId, clientId, clientName, null);
        }

        throw new IllegalStateException(
                "Unsupported authentication type: " + authentication.getClass().getName());
    }
}
