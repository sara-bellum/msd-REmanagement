package ro.agentie.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import ro.agentie.domain.listing.TipOferta;
import ro.agentie.domain.shared.Agent;

public interface IListingWorkflowService {
    Integer creeazaAnunt(
            Integer proprietateId, TipOferta tip, String descriere, BigDecimal pret, LocalDate data);

    Agent propuneAgent(Integer ofertaId);

    void alocaAnunt(Integer ofertaId, Integer agentId);

    void alocaAutomat(Integer ofertaId);

    void activeazaAnunt(Integer ofertaId);

    void arhiveazaAnunt(Integer ofertaId);
}
