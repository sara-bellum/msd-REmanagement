package ro.agentie.infrastructure;

import org.springframework.stereotype.Repository;
import ro.agentie.domain.client.Interes;

@Repository
public class InteresEntityRepositoryBase extends InMemoryRepository<Interes>
        implements IInteresEntityRepository {}
