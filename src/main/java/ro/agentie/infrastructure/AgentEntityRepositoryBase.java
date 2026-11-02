package ro.agentie.infrastructure;

import java.util.List;
import org.springframework.stereotype.Repository;
import ro.agentie.domain.shared.*;

@Repository
public class AgentEntityRepositoryBase implements IAgentEntityRepository {
    private final IPersoanaEntityRepository persoane;

    public AgentEntityRepositoryBase(IPersoanaEntityRepository persoane) {
        this.persoane = persoane;
    }

    public Agent add(Agent agent) {
        persoane.add(agent);
        return agent;
    }

    public Agent getById(Integer id) {
        Persoana p = persoane.getById(id);
        Reguli.cere(p instanceof Agent, "Persoana nu este agent!");
        return (Agent) p;
    }

    public List<Agent> toCollection() {
        return persoane.toCollection().stream()
                .filter(Agent.class::isInstance)
                .map(Agent.class::cast)
                .toList();
    }

    public List<Agent> findByZona(Zona zona) {
        return toCollection().stream().filter(a -> a.acoperaZona(zona)).toList();
    }
}
