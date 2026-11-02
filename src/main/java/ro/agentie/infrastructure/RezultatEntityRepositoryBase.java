package ro.agentie.infrastructure;

import org.springframework.stereotype.Repository;
import ro.agentie.domain.transaction.Rezultat;

@Repository
public class RezultatEntityRepositoryBase extends InMemoryRepository<Rezultat>
        implements IRezultatEntityRepository {}
