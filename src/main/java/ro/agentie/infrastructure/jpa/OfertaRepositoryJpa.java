package ro.agentie.infrastructure.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ro.agentie.domain.listing.Oferta;
import ro.agentie.infrastructure.IOfertaEntityRepository;

// Register this adapter only for PostgreSQL; legacy tests use the memory implementation.
@Repository
@Profile("!memory")
@Transactional
public class OfertaRepositoryJpa extends JpaRepositoryAdapter<Oferta>
        implements IOfertaEntityRepository {
    public OfertaRepositoryJpa(OfertaJpaRepository repository, EntityManager em) {
        super(repository, em);
    }
}