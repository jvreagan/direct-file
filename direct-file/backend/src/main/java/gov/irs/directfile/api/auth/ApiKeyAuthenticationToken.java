package gov.irs.directfile.api.auth;

import java.util.Collection;
import java.util.UUID;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import gov.irs.directfile.api.auth.model.ApiClient;

public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {
    private static final long serialVersionUID = 1L;

    private final UUID clientId;
    private final transient ApiClient apiClient;

    public ApiKeyAuthenticationToken(ApiClient apiClient, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.clientId = apiClient.getId();
        this.apiClient = apiClient;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return clientId;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }
}
