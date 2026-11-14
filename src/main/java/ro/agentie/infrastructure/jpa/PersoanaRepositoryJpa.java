package ro.agentie.infrastructure.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ro.agentie.domain.shared.Persoana;
import ro.agentie.infrastructure.IPersoanaEntityRepository;

// Register this adapter only for PostgreSQL; legacy tests use the memory implementation.
@Repository
@Profile("!memory")
@Transactional
public class PersoanaRepositoryJpa extends JpaRepositoryAdapter<Persoana>
        implements IPersoanaEntityRepository {
    public PersoanaRepositoryJpa(PersoanaJpaRepository repository, EntityManager em) {
        super(repository, em);
    }
}