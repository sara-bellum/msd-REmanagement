package ro.agentie.services;

import java.util.Collection;
import ro.agentie.domain.listing.Oferta;
import ro.agentie.domain.shared.Agent;

public interface IAgentSelectionService {
    Agent alegeAgentPentruOferta(Collection<Agent> agenti, Oferta oferta);
}
