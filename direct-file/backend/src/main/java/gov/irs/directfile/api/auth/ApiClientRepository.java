package gov.irs.directfile.api.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import gov.irs.directfile.api.auth.model.ApiClient;

@Repository
public interface ApiClientRepository extends JpaRepository<ApiClient, UUID> {
    Optional<ApiClient> findByApiKeyHash(String apiKeyHash);

    Optional<ApiClient> findByClientName(String clientName);

    List<ApiClient> findByUserId(UUID userId);

    List<ApiClient> findByEnabledTrue();
}
