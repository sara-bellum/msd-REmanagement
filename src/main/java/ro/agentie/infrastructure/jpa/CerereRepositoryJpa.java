package ro.agentie.infrastructure.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ro.agentie.domain.client.Cerere;
import ro.agentie.infrastructure.ICerereEntityRepository;

// This adapter implements the existing application interface using shared JPA CRUD methods.
@Repository
// The memory implementation is registered separately; only one adapter is active per profile.
@Profile("!memory")
@Transactional
public class CerereRepositoryJpa extends JpaRepositoryAdapter<Cerere>
        implements ICerereEntityRepository {
    public CerereRepositoryJpa(CerereJpaRepository repository, EntityManager em) {
        // Delegate CRUD and root domain-ID allocation to the shared adapter from L07.
        super(repository, em);
    }
}