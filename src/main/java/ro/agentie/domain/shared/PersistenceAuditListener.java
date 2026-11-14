package ro.agentie.domain.shared;
import jakarta.persistence.*;

/** JPA callbacks run for actual inserts/updates, independently of workflow events. */
// Technical persistence timestamps are separate from business workflow events.
public class PersistenceAuditListener {
    @PrePersist
    public void insert(Entitate entity) {
        entity.auditInsert();
    }

    @PreUpdate
    public void update(Entitate entity) {
        entity.auditUpdate();
    }
}