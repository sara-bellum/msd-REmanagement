package ro.agentie.domain.transaction;

import jakarta.persistence.*;
import java.math.*;
import ro.agentie.domain.shared.*;

// Agent participation belongs to a transaction; the agent itself remains an independent entity.
@Entity
@Table(name = "agenti_rezultate")
public class AgentRezultat extends Entitate {
    // Required for JPA hydration; normal creation uses the validated domain constructor.
    protected AgentRezultat() {}

    // Reference an existing entity without cascading its deletion from this object.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    // Reference an existing entity without cascading its deletion from this object.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rezultat_id", nullable = false)
    private Rezultat rezultat;

    AgentRezultat(Integer id, Agent agent, Rezultat rezultat) {
        super(id);
        this.agent = agent;
        this.rezultat = rezultat;
    }

    public String getDescrierePondere() {
        return "1/" + rezultat.getParticipanti().size();
    }

    public BigDecimal getPondereParticipare() {
        return BigDecimal.ONE.divide(
                BigDecimal.valueOf(rezultat.getParticipanti().size()), 8, RoundingMode.HALF_UP);
    }

    public BigDecimal calculeazaComisionAgentSimplu() {
        return rezultat.getComisionAgent(agent);
    }

    public Agent getAgent() {
        return agent;
    }

    public Rezultat getRezultat() {
        return rezultat;
    }
}