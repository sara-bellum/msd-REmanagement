package ro.agentie.infrastructure;

import org.springframework.stereotype.Repository;
import ro.agentie.domain.shared.Persoana;

@Repository
public class PersoanaEntityRepositoryBase extends InMemoryRepository<Persoana>
        implements IPersoanaEntityRepository {}
