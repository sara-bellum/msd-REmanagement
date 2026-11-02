package ro.agentie.services;

import java.math.BigDecimal;
import ro.agentie.domain.transaction.StareRezultat;

public record TranzactieSummaryView(
        Integer nrRezultat,
        StareRezultat stare,
        BigDecimal pretConvenit,
        BigDecimal comisionAgentie,
        int numarParticipanti,
        int numarPropuneri) {}
