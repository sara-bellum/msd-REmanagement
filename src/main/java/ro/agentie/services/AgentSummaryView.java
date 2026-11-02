package ro.agentie.services;

import java.util.Map;
import ro.agentie.domain.listing.StareOferta;

public record AgentSummaryView(
        Integer codAgent, String nume, long oferteInLucru, Map<StareOferta, Long> peStari) {}
