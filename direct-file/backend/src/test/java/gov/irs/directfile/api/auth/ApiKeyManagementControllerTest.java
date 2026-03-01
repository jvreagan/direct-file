package gov.irs.directfile.api.auth;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import gov.irs.directfile.api.auth.model.ApiClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyManagementControllerTest {
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ApiClientRepository apiClientRepository;

    @BeforeEach
    void setUp() {
        ApiKeyManagementController controller = new ApiKeyManagementController(apiClientRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private static ApiClient createClientWithId(String name, String keyHash, String secretHash) {
        ApiClient client = new ApiClient(name, keyHash, secretHash);
        try {
            Field idField = ApiClient.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(client, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return client;
    }

    @Test
    void givenValidRequest_whenCreateApiKey_thenReturnsCreated() throws Exception {
        when(apiClientRepository.findByClientName("new-client")).thenReturn(Optional.empty());
        when(apiClientRepository.save(any(ApiClient.class))).thenAnswer(invocation -> {
            ApiClient saved = invocation.getArgument(0);
            try {
                Field idField = ApiClient.class.getDeclaredField("id");
                idField.setAccessible(true);
                if (idField.get(saved) == null) {
                    idField.set(saved, UUID.randomUUID());
                }
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            return saved;
        });

        mockMvc.perform(post("/v1/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("client_name", "new-client"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.client_name").value("new-client"))
                .andExpect(jsonPath("$.api_key").exists())
                .andExpect(jsonPath("$.client_secret").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void givenMissingClientName_whenCreateApiKey_thenReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/v1/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("client_name is required"));
    }

    @Test
    void givenDuplicateClientName_whenCreateApiKey_thenReturnsConflict() throws Exception {
        ApiClient existing = new ApiClient("existing-client", "hash", "secret-hash");
        when(apiClientRepository.findByClientName("existing-client")).thenReturn(Optional.of(existing));

        mockMvc.perform(post("/v1/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("client_name", "existing-client"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Client name already exists"));
    }

    @Test
    void givenEnabledClients_whenListApiKeys_thenReturnsAll() throws Exception {
        ApiClient client1 = createClientWithId("client-1", "hash1", "secret1");
        ApiClient client2 = createClientWithId("client-2", "hash2", "secret2");
        when(apiClientRepository.findByEnabledTrue()).thenReturn(List.of(client1, client2));

        mockMvc.perform(get("/v1/api-keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].client_name").value("client-1"))
                .andExpect(jsonPath("$[1].client_name").value("client-2"));
    }

    @Test
    void givenExistingClient_whenDeleteApiKey_thenReturnsNoContent() throws Exception {
        UUID clientId = UUID.randomUUID();
        ApiClient client = new ApiClient("test-client", "hash", "secret");
        when(apiClientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(apiClientRepository.save(any(ApiClient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(delete("/v1/api-keys/{id}", clientId)).andExpect(status().isNoContent());
    }

    @Test
    void givenExistingClient_whenRotateApiKey_thenReturnsNewCredentials() throws Exception {
        ApiClient client = createClientWithId("test-client", "old-hash", "old-secret");
        UUID clientId = client.getId();
        when(apiClientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(apiClientRepository.save(any(ApiClient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/v1/api-keys/{id}/rotate", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.client_name").value("test-client"))
                .andExpect(jsonPath("$.api_key").exists())
                .andExpect(jsonPath("$.client_secret").exists())
                .andExpect(jsonPath("$.message").exists());
    }
}
