package ro.agentie.services;

import java.util.*;
import org.springframework.stereotype.Service;
import ro.agentie.domain.listing.Oferta;
import ro.agentie.domain.shared.*;

@Service
public class AgentSelectionService implements IAgentSelectionService {
    public Agent alegeAgentPentruOferta(Collection<Agent> agenti, Oferta oferta) {
        return agenti.stream()
                .filter(
                        a -> a.acoperaZona(oferta.getProprietate().getZona()) && !a.esteProprietarul(oferta))
                .min(Comparator.comparingLong(Agent::getNumarOferteInLucru).thenComparing(Agent::getId))
                .orElseThrow(
                        () ->
                                new RegulaBusinessException(
                                        "Niciun agent nu acopera zona "
                                                + oferta.getProprietate().getZona().getNumeZona()
                                                + "!"));
    }
}
