package ro.agentie.infrastructure;

import org.springframework.stereotype.Repository;
import ro.agentie.domain.listing.Oferta;

@Repository
public class OfertaEntityRepositoryBase extends InMemoryRepository<Oferta>
        implements IOfertaEntityRepository {}
