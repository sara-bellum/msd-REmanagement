package ro.agentie.infrastructure.jpa;

import jakarta.persistence.EntityManager;
import java.util.*;
import ro.agentie.domain.shared.Entitate;
import ro.agentie.infrastructure.IEntityRepository;

// Reuse CRUD implementation while preserving the existing domain-facing repository API.
public abstract class JpaRepositoryAdapter<T extends Entitate> implements IEntityRepository<T> {
    protected final DomainJpaRepository<T> repository;
    private final EntityManager em;

    protected JpaRepositoryAdapter(DomainJpaRepository<T> repository, EntityManager em) {
        this.repository = repository;
        this.em = em;
    }

    // Database sequences coordinate IDs across instances; rollback may leave harmless gaps.
    public Integer getNextID() {
        return ((Number)
                em.createNativeQuery("select nextval('domain_id_seq')", Long.class).getSingleResult())
                .intValue();
    }

    public T add(T entity) {
        // Flush sends SQL now; the surrounding transaction still controls commit or rollback.
        return repository.saveAndFlush(entity);
    }

    public Optional<T> findById(Integer id) {
        // The incoming Integer is a domain ID, not the technical Long primary key.
        return repository.findByDomainId(id);
    }

    public List<T> toCollection() {
        return repository.findAll(org.springframework.data.domain.Sort.by("domainId"));
    }

    public boolean remove(T entity) {
        Optional<T> stored = repository.findByDomainId(entity.getId());
        if (stored.isEmpty()) return false;
        repository.delete(stored.get());
        repository.flush();
        return true;
    }

    public int size() {
        return Math.toIntExact(repository.count());
    }
}