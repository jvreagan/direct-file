package gov.irs.directfile.api.auth;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import gov.irs.directfile.api.auth.model.ApiClient;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OAuth2TokenControllerTest {
    private MockMvc mockMvc;

    @Mock
    private ApiClientRepository apiClientRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        OAuth2TokenController controller = new OAuth2TokenController(apiClientRepository, jwtTokenService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void givenValidCredentials_whenToken_thenReturnsJwt() throws Exception {
        String clientName = "test-client";
        String clientSecret = "my-secret";
        String secretHash = ApiKeyAuthenticationFilter.hashApiKey(clientSecret);
        ApiClient client = new ApiClient(clientName, "key-hash", secretHash);
        client.setScopes("read,write");

        when(apiClientRepository.findByClientName(clientName)).thenReturn(Optional.of(client));
        when(jwtTokenService.generateToken(any(), eq(clientName), anySet())).thenReturn("jwt-token-value");

        mockMvc.perform(post("/oauth/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials")
                        .param("client_id", clientName)
                        .param("client_secret", clientSecret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("jwt-token-value"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(3600));
    }

    @Test
    void givenWrongGrantType_whenToken_thenReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/oauth/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("client_id", "client")
                        .param("client_secret", "secret"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("unsupported_grant_type"));
    }

    @Test
    void givenUnknownClient_whenToken_thenReturnsUnauthorized() throws Exception {
        when(apiClientRepository.findByClientName("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(post("/oauth/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials")
                        .param("client_id", "unknown")
                        .param("client_secret", "secret"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_client"));
    }

    @Test
    void givenWrongSecret_whenToken_thenReturnsUnauthorized() throws Exception {
        String clientName = "test-client";
        ApiClient client = new ApiClient(clientName, "key-hash", "correct-secret-hash");

        when(apiClientRepository.findByClientName(clientName)).thenReturn(Optional.of(client));

        mockMvc.perform(post("/oauth/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials")
                        .param("client_id", clientName)
                        .param("client_secret", "wrong-secret"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_description").value("Invalid client secret"));
    }

    @Test
    void givenDisabledClient_whenToken_thenReturnsUnauthorized() throws Exception {
        String clientName = "disabled-client";
        String clientSecret = "my-secret";
        String secretHash = ApiKeyAuthenticationFilter.hashApiKey(clientSecret);
        ApiClient client = new ApiClient(clientName, "key-hash", secretHash);
        client.setEnabled(false);

        when(apiClientRepository.findByClientName(clientName)).thenReturn(Optional.of(client));

        mockMvc.perform(post("/oauth/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials")
                        .param("client_id", clientName)
                        .param("client_secret", clientSecret))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_description").value("Client is disabled"));
    }
}
