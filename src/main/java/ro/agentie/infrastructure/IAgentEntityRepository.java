package ro.agentie.infrastructure;

import java.util.List;
import ro.agentie.domain.shared.*;

public interface IAgentEntityRepository {
    Agent add(Agent agent);

    Agent getById(Integer id);

    List<Agent> toCollection();

    List<Agent> findByZona(Zona zona);
}
