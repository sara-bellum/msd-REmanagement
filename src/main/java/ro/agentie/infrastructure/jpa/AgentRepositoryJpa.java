package ro.agentie.infrastructure.jpa;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ro.agentie.domain.shared.*;
import ro.agentie.infrastructure.*;

// Reuse person storage for agents instead of inserting duplicate person records.
@Repository
@Profile("!memory")
@Transactional
public class AgentRepositoryJpa implements IAgentEntityRepository {
    private final IPersoanaEntityRepository persoane;

    public AgentRepositoryJpa(IPersoanaEntityRepository persoane) {
        this.persoane = persoane;
    }

    public Agent add(Agent agent) {
        return (Agent) persoane.add(agent);
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