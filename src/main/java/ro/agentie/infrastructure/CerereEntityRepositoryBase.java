package ro.agentie.infrastructure;

import org.springframework.stereotype.Repository;
import ro.agentie.domain.client.Cerere;
// Only the legacy memory profile registers this implementation.
// PostgreSQL uses a separate adapter for the same application-facing interface.
@org.springframework.context.annotation.Profile("memory")
@Repository
public class CerereEntityRepositoryBase extends InMemoryRepository<Cerere>
        implements ICerereEntityRepository {}
