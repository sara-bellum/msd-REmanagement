package ro.agentie.services;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import ro.agentie.domain.listing.Oferta;
import ro.agentie.domain.shared.Agent;
import ro.agentie.domain.transaction.*;

@Service
public class SummarizingDomainService {
    public OfertaSummaryView getOfertaSummary(Oferta o) {
        return new OfertaSummaryView(
                o.getId(),
                o.getProprietate().getAdresa(),
                o.getStare(),
                o.getAgent() == null ? "Nealocata" : o.getAgent().getNumeComplet(),
                o.getImagini().size());
    }

    public AgentSummaryView getAgentSummary(Agent a) {
        return new AgentSummaryView(
                a.getId(),
                a.getNumeComplet(),
                a.getNumarOferteInLucru(),
                Map.copyOf(a.getStatisticiPortofoliu()));
    }

    public TranzactieSummaryView getTranzactieSummary(Rezultat r) {
        return new TranzactieSummaryView(
                r.getId(),
                r.getStare(),
                r.getPretConvenit(),
                r.getComisionAgentieTotal(),
                r.getParticipanti().size(),
                r.getPropuneri().size());
    }

    public BigDecimal totalComisioaneFinalizate(Collection<Rezultat> rezultate) {
        return rezultate.stream()
                .filter(r -> r.getStare() == StareRezultat.FINALIZAT)
                .map(Rezultat::getComisionAgentieTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
