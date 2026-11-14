package ro.agentie.infrastructure.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ro.agentie.domain.shared.Zona;
import ro.agentie.infrastructure.IZonaEntityRepository;

// Register this adapter only for PostgreSQL; legacy tests use the memory implementation.
@Repository
@Profile("!memory")
@Transactional
public class ZonaRepositoryJpa extends JpaRepositoryAdapter<Zona> implements IZonaEntityRepository {
    public ZonaRepositoryJpa(ZonaJpaRepository repository, EntityManager em) {
        super(repository, em);
    }
}