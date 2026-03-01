package gov.irs.directfile.api.auth;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import gov.irs.directfile.api.auth.model.ApiClient;
import gov.irs.directfile.api.util.base.BaseRepositoryTest;

import static org.junit.jupiter.api.Assertions.*;

class ApiClientRepositoryTest extends BaseRepositoryTest {
    @Autowired
    private ApiClientRepository apiClientRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void givenSavedClient_whenFindByApiKeyHash_thenReturnsClient() {
        ApiClient client = new ApiClient("test-client", "hash-abc123", "secret-hash");
        entityManager.persistAndFlush(client);

        Optional<ApiClient> found = apiClientRepository.findByApiKeyHash("hash-abc123");

        assertTrue(found.isPresent());
        assertEquals("test-client", found.get().getClientName());
    }

    @Test
    void givenSavedClient_whenFindByClientName_thenReturnsClient() {
        ApiClient client = new ApiClient("unique-client", "hash-xyz", "secret-hash");
        entityManager.persistAndFlush(client);

        Optional<ApiClient> found = apiClientRepository.findByClientName("unique-client");

        assertTrue(found.isPresent());
        assertEquals("hash-xyz", found.get().getApiKeyHash());
    }

    @Test
    void givenMixedEnabledClients_whenFindByEnabledTrue_thenFiltersDisabled() {
        ApiClient enabled = new ApiClient("enabled-client", "hash-enabled", "secret1");
        ApiClient disabled = new ApiClient("disabled-client", "hash-disabled", "secret2");
        disabled.setEnabled(false);
        entityManager.persistAndFlush(enabled);
        entityManager.persistAndFlush(disabled);

        List<ApiClient> result = apiClientRepository.findByEnabledTrue();

        assertTrue(result.stream().anyMatch(c -> c.getClientName().equals("enabled-client")));
        assertTrue(result.stream().noneMatch(c -> c.getClientName().equals("disabled-client")));
    }

    @Test
    void givenNonexistentHash_whenFindByApiKeyHash_thenReturnsEmpty() {
        Optional<ApiClient> found = apiClientRepository.findByApiKeyHash("nonexistent-hash");

        assertTrue(found.isEmpty());
    }
}
