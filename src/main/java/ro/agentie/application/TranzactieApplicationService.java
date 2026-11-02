package ro.agentie.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Consumer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ro.agentie.application.events.WorkflowEvent;
import ro.agentie.domain.transaction.*;
import ro.agentie.infrastructure.*;

@Service
public class TranzactieApplicationService implements ITranzactieWorkflowService {
    private final IRezultatEntityRepository rezultate;
    private final IInteresEntityRepository interese;
    private final IAgentEntityRepository agenti;
    private final IRealEstateEntityFactory factory;
    private final ApplicationEventPublisher events;

    public TranzactieApplicationService(
            IRezultatEntityRepository r,
            IInteresEntityRepository i,
            IAgentEntityRepository a,
            IRealEstateEntityFactory f,
            ApplicationEventPublisher e) {
        rezultate = r;
        interese = i;
        agenti = a;
        factory = f;
        events = e;
    }

    public Integer initiazaTranzactie(Integer interesId, Integer agentId, LocalDate data) {
        Rezultat r =
                factory.buildRezultat(
                        rezultate.getNextID(), interese.getById(interesId), agenti.getById(agentId), data);
        rezultate.add(r);
        events.publishEvent(new WorkflowEvent("INITIERE_TRANZACTIE", r.getId()));
        return r.getId();
    }

    private void modifica(Integer id, String actiune, Consumer<Rezultat> operation) {
        Rezultat r = rezultate.getById(id);
        operation.accept(r);
        rezultate.save(r);
        events.publishEvent(new WorkflowEvent(actiune, id));
    }

    public Integer adaugaPropunere(
            Integer id, BigDecimal suma, ParteNegociere parte, LocalDate data, String observatii) {
        Rezultat r = rezultate.getById(id);
        PropunerePret p = r.adaugaPropunere(suma, parte, data, observatii);
        rezultate.save(r);
        events.publishEvent(new WorkflowEvent("PROPUNERE_PRET", id));
        return p.getId();
    }

    public void acceptaPropunerea(Integer id) {
        modifica(id, "ACCEPTARE_PRET", Rezultat::acceptaPropunerea);
    }

    public void respingePropunerea(Integer id) {
        modifica(id, "RESPINGERE_PRET", Rezultat::respingePropunerea);
    }

    public void adaugaAgent(Integer id, Integer agentId) {
        modifica(id, "ADAUGARE_AGENT", r -> r.adaugaAgentRezultat(agenti.getById(agentId)));
    }

    public Contract pregatesteContract(Integer id) {
        Rezultat rezultat = rezultate.getById(id);
        Contract contract = rezultat.pregatesteContract();
        rezultate.save(rezultat);
        events.publishEvent(new WorkflowEvent("PREGATIRE_CONTRACT", id));
        return contract;
    }

    public void modificaConditiiContract(
            Integer id,
            MetodaPlata metoda,
            BigDecimal avans,
            String grafic,
            LocalDate inceput,
            LocalDate sfarsit,
            String clauze) {
        modifica(
                id,
                "CONDITII_CONTRACT",
                r -> r.modificaConditiiContract(metoda, avans, grafic, inceput, sfarsit, clauze));
    }

    public void trimiteContractLaNegociere(Integer id) {
        modifica(id, "NEGOCIERE_CONTRACT", Rezultat::trimiteContractLaNegociere);
    }

    public void convineContract(Integer id) {
        modifica(id, "ACORD_CONTRACT", Rezultat::convineContract);
    }

    public void semneazaContract(Integer id, LocalDate data) {
        modifica(id, "SEMNARE_CONTRACT", r -> r.semneazaContract(data));
    }

    public void stabilesteComisioane(Integer id, BigDecimal cumparator, BigDecimal vanzator) {
        modifica(id, "COMISIOANE", r -> r.stabilesteComisioane(cumparator, vanzator));
    }

    public void inregistreazaPlataAvans(Integer id) {
        modifica(id, "PLATA_AVANS", Rezultat::inregistreazaPlataAvans);
    }

    public void inregistreazaPlataComisionCumparator(Integer id) {
        modifica(id, "PLATA_COMISION_CUMPARATOR", Rezultat::inregistreazaPlataComisionCumparator);
    }

    public void inregistreazaPlataComisionVanzator(Integer id) {
        modifica(id, "PLATA_COMISION_VANZATOR", Rezultat::inregistreazaPlataComisionVanzator);
    }

    public void finalizeazaTranzactie(Integer id, LocalDate data) {
        modifica(id, "FINALIZARE_TRANZACTIE", r -> r.finalizeaza(data));
    }

    public void anuleazaTranzactie(Integer id, String motiv, LocalDate data) {
        modifica(id, "ANULARE_TRANZACTIE", r -> r.anuleaza(motiv, data));
    }

    public Rezultat getRezultat(Integer id) {
        return rezultate.getById(id);
    }
}
