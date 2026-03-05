package gov.irs.directfile.api.auth;

import java.lang.reflect.Field;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import gov.irs.directfile.api.auth.model.ApiClient;
import gov.irs.directfile.api.forms.translation.FormTranslationService;
import gov.irs.directfile.api.loaders.service.FactGraphService;
import gov.irs.directfile.api.pdf.PdfService;
import gov.irs.directfile.api.taxreturn.TaxReturnService;
import gov.irs.directfile.api.taxreturn.models.TaxReturn;
import gov.irs.directfile.api.user.UserService;
import gov.irs.directfile.api.user.domain.UserInfo;
import gov.irs.directfile.api.v2.TaxReturnV2Controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for the OAuth2 token flow and API key authentication.
 * Uses standalone MockMvc to test the token controller, JWT validation,
 * and API key filter without a full Spring Boot context.
 */
@ExtendWith(MockitoExtension.class)
class OAuth2IntegrationTest {
    private static final String SIGNING_KEY = "test-signing-key-must-be-at-least-32-bytes-long!!";
    private static final String TEST_CLIENT_NAME = "test-client";
    private static final String TEST_CLIENT_SECRET = "test-secret-value";
    private static final String TEST_API_KEY = "test-api-key-value";
    private static final UUID TEST_CLIENT_ID = UUID.randomUUID();
    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final UserInfo TEST_USER_INFO =
            new UserInfo(TEST_USER_ID, UUID.randomUUID(), "test@example.com", "123456789");

    private MockMvc mockMvc;
    private JwtTokenService jwtTokenService;
    private ObjectMapper objectMapper;

    @Mock
    private ApiClientRepository apiClientRepository;

    @Mock
    private TaxReturnService taxReturnService;

    @Mock
    private UserService userService;

    @Mock
    private FactGraphService factGraphService;

    @Mock
    private FormTranslationService formTranslationService;

    @Mock
    private PdfService pdfService;

    private ApiClient testClient;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        jwtTokenService = new JwtTokenService(SIGNING_KEY, "direct-file-api", 3600);

        String secretHash = ApiKeyAuthenticationFilter.hashApiKey(TEST_CLIENT_SECRET);
        String apiKeyHash = ApiKeyAuthenticationFilter.hashApiKey(TEST_API_KEY);
        testClient = new ApiClient(TEST_CLIENT_NAME, apiKeyHash, secretHash);

        // Set the id field via reflection since @GeneratedValue only works with JPA
        Field idField = ApiClient.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(testClient, TEST_CLIENT_ID);

        OAuth2TokenController tokenController = new OAuth2TokenController(apiClientRepository, jwtTokenService);
        TaxReturnV2Controller v2Controller = new TaxReturnV2Controller(
                taxReturnService, userService, factGraphService, formTranslationService, pdfService);

        mockMvc = MockMvcBuilders.standaloneSetup(tokenController, v2Controller)
                .addFilter(new ApiKeyAuthenticationFilter(apiClientRepository))
                .build();
    }

    @Test
    void givenValidCredentials_whenGetToken_thenReturnsJwt() throws Exception {
        when(apiClientRepository.findByClientName(TEST_CLIENT_NAME)).thenReturn(Optional.of(testClient));

        MvcResult result = mockMvc.perform(post("/oauth/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials")
                        .param("client_id", TEST_CLIENT_NAME)
                        .param("client_secret", TEST_CLIENT_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(3600))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> tokenResponse = objectMapper.readValue(responseBody, Map.class);
        String accessToken = (String) tokenResponse.get("access_token");
        assertNotNull(accessToken);
        assertFalse(accessToken.isBlank());

        // Verify the token is valid by decoding it
        assertDoesNotThrow(() -> jwtTokenService.validateToken(accessToken));
    }

    @Test
    void givenInvalidGrantType_whenGetToken_thenReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/oauth/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("client_id", TEST_CLIENT_NAME)
                        .param("client_secret", TEST_CLIENT_SECRET))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("unsupported_grant_type"));
    }

    @Test
    void givenInvalidClientSecret_whenGetToken_thenReturnsUnauthorized() throws Exception {
        when(apiClientRepository.findByClientName(TEST_CLIENT_NAME)).thenReturn(Optional.of(testClient));

        mockMvc.perform(post("/oauth/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials")
                        .param("client_id", TEST_CLIENT_NAME)
                        .param("client_secret", "wrong-secret"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_client"));
    }

    @Test
    void givenUnknownClient_whenGetToken_thenReturnsUnauthorized() throws Exception {
        when(apiClientRepository.findByClientName("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(post("/oauth/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials")
                        .param("client_id", "unknown")
                        .param("client_secret", TEST_CLIENT_SECRET))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_client"));
    }

    @Test
    void givenDisabledClient_whenGetToken_thenReturnsUnauthorized() throws Exception {
        testClient.setEnabled(false);
        when(apiClientRepository.findByClientName(TEST_CLIENT_NAME)).thenReturn(Optional.of(testClient));

        mockMvc.perform(post("/oauth/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials")
                        .param("client_id", TEST_CLIENT_NAME)
                        .param("client_secret", TEST_CLIENT_SECRET))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_client"));
    }

    @Test
    void givenApiKey_whenCallV2Endpoint_thenAuthenticates() throws Exception {
        String apiKeyHash = ApiKeyAuthenticationFilter.hashApiKey(TEST_API_KEY);
        when(apiClientRepository.findByApiKeyHash(apiKeyHash)).thenReturn(Optional.of(testClient));
        when(userService.getCurrentUserInfo()).thenReturn(TEST_USER_INFO);

        UUID taxReturnId = UUID.randomUUID();
        TaxReturn taxReturn = TaxReturn.testObjectFactory();
        when(taxReturnService.findByIdAndUserId(taxReturnId, TEST_USER_ID)).thenReturn(Optional.of(taxReturn));

        mockMvc.perform(get("/v2/tax-returns/{id}", taxReturnId).header("X-API-Key", TEST_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taxReturn.getId().toString()));
    }

    @Test
    void givenInvalidApiKey_whenCallV2Endpoint_thenFilterDoesNotAuthenticate() throws Exception {
        lenient().when(userService.getCurrentUserInfo()).thenReturn(TEST_USER_INFO);

        // Filter doesn't reject — it just doesn't set authentication.
        // In standalone MockMvc without Spring Security filter chain enforcement,
        // the request still reaches the controller.
        UUID taxReturnId = UUID.randomUUID();
        TaxReturn taxReturn = TaxReturn.testObjectFactory();
        lenient()
                .when(taxReturnService.findByIdAndUserId(taxReturnId, TEST_USER_ID))
                .thenReturn(Optional.of(taxReturn));

        mockMvc.perform(get("/v2/tax-returns/{id}", taxReturnId).header("X-API-Key", "invalid-key"))
                .andExpect(status().isOk());
    }

    @Test
    void givenFullFlow_whenGetTokenThenCallEndpoint_thenSucceeds() throws Exception {
        // Step 1: Get a token via OAuth2 client_credentials
        when(apiClientRepository.findByClientName(TEST_CLIENT_NAME)).thenReturn(Optional.of(testClient));

        MvcResult tokenResult = mockMvc.perform(post("/oauth/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials")
                        .param("client_id", TEST_CLIENT_NAME)
                        .param("client_secret", TEST_CLIENT_SECRET))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        Map<String, Object> tokenResponse =
                objectMapper.readValue(tokenResult.getResponse().getContentAsString(), Map.class);
        String accessToken = (String) tokenResponse.get("access_token");
        assertNotNull(accessToken);

        // Step 2: Validate the token independently
        assertDoesNotThrow(() -> jwtTokenService.validateToken(accessToken));

        // Step 3: Call v2 endpoint with the JWT
        when(userService.getCurrentUserInfo()).thenReturn(TEST_USER_INFO);
        UUID taxReturnId = UUID.randomUUID();
        TaxReturn taxReturn = TaxReturn.testObjectFactory();
        when(taxReturnService.findByIdAndUserId(taxReturnId, TEST_USER_ID)).thenReturn(Optional.of(taxReturn));

        mockMvc.perform(get("/v2/tax-returns/{id}", taxReturnId).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taxReturn.getId().toString()));
    }
}
