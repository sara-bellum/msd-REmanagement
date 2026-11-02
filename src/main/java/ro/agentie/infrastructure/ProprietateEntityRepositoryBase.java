package ro.agentie.infrastructure;

import org.springframework.stereotype.Repository;
import ro.agentie.domain.listing.Proprietate;

@Repository
public class ProprietateEntityRepositoryBase extends InMemoryRepository<Proprietate>
        implements IProprietateEntityRepository {}
