package ro.agentie.infrastructure;

import org.springframework.stereotype.Repository;
import ro.agentie.domain.client.Cerere;

@Repository
public class CerereEntityRepositoryBase extends InMemoryRepository<Cerere>
        implements ICerereEntityRepository {}
