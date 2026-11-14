package ro.agentie.domain.shared;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.Hibernate;

/** Domain IDs remain stable; persistenceId is the database-generated technical primary key. */
// Shared JPA identity and audit fields are inherited by every mapped entity.
@MappedSuperclass
@EntityListeners(PersistenceAuditListener.class)
public abstract class Entitate {
    // Technical database identity, independent of the ID exposed by domain services.
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "entity_pk")
    @SequenceGenerator(name = "entity_pk", sequenceName = "entity_pk_seq", allocationSize = 1)
    @Column(name = "persistence_id")
    private Long persistenceId;

    @Column(name = "domain_id", nullable = false, updatable = false)
    // Keep the existing getId() contract used throughout the application.
    private Integer domainId;
    // Detect stale updates; business rules still belong to domain methods.
    @Version
    private Long version;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    // JPA hydration bypasses normal construction; application code uses validated constructors.
    protected Entitate() {
    } // JPA hydration only

    protected Entitate(Integer id) {
        Reguli.cere(id != null && id > 0, "Identificator invalid!");
        domainId = id;
    }
    public Integer getId() {
        return domainId;
    }
    public Long getPersistenceId() {
        return persistenceId;
    }
    public Long getVersion() {
        return version;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    void auditInsert() {
        createdAt = updatedAt = Instant.now();
    }

    void auditUpdate() {
        updatedAt = Instant.now();
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Entitate e) || Hibernate.getClass(this) != Hibernate.getClass(e))
            return false;
        if (getPersistenceId() != null || e.getPersistenceId() != null) {
            return getPersistenceId() != null && getPersistenceId().equals(e.getPersistenceId());
        }
        return getId() != null && getId().equals(e.getId());
    }
    // Must not change when the generated technical PK is assigned during persist().
    @Override
    public final int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
