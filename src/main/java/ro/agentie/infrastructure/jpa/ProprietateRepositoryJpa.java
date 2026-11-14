package ro.agentie.infrastructure.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ro.agentie.domain.listing.Proprietate;
import ro.agentie.infrastructure.IProprietateEntityRepository;

// Register this adapter only for PostgreSQL; legacy tests use the memory implementation.
@Repository
@Profile("!memory")
@Transactional
public class ProprietateRepositoryJpa extends JpaRepositoryAdapter<Proprietate>
        implements IProprietateEntityRepository {
    public ProprietateRepositoryJpa(ProprietateJpaRepository repository, EntityManager em) {
        super(repository, em);
    }
}