package ro.agentie.infrastructure;

import org.springframework.stereotype.Repository;
import ro.agentie.domain.listing.Oferta;
// Only the legacy memory profile registers this implementation.
// PostgreSQL uses a separate adapter for the same application-facing interface.
@org.springframework.context.annotation.Profile("memory")
@Repository
public class OfertaEntityRepositoryBase extends InMemoryRepository<Oferta>
        implements IOfertaEntityRepository {}
