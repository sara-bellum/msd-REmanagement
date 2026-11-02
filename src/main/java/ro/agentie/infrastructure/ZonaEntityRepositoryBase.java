package ro.agentie.infrastructure;

import org.springframework.stereotype.Repository;
import ro.agentie.domain.shared.Zona;

@Repository
public class ZonaEntityRepositoryBase extends InMemoryRepository<Zona>
        implements IZonaEntityRepository {}
