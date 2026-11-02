package ro.agentie.domain.transaction;

import java.math.*;
import ro.agentie.domain.shared.*;

public final class AgentRezultat extends Entitate {
    private final Agent agent;
    private final Rezultat rezultat;

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
