package gov.irs.directfile.api.auth.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import gov.irs.directfile.api.user.models.User;

@Getter
@Entity
@Table(name = "api_clients")
public class ApiClient {
    @Id
    @GeneratedValue(generator = "UUID4")
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Setter
    @Column(name = "api_key_hash", nullable = false, unique = true)
    private String apiKeyHash;

    @Setter
    @Column(name = "client_name", nullable = false, unique = true)
    private String clientName;

    @Setter
    @Column(name = "client_secret_hash", nullable = false)
    private String clientSecretHash;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "api_clients_user_id_fkey"))
    private User user;

    @Setter
    @Column(nullable = false)
    private String scopes = "read,write";

    @Setter
    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Setter
    @Column(name = "expires_at")
    private Instant expiresAt;

    protected ApiClient() {}

    public ApiClient(String clientName, String apiKeyHash, String clientSecretHash) {
        this.clientName = clientName;
        this.apiKeyHash = apiKeyHash;
        this.clientSecretHash = clientSecretHash;
    }

    public Set<String> getScopeSet() {
        if (scopes == null || scopes.isBlank()) return Set.of();
        return Set.of(scopes.split(","));
    }
}
