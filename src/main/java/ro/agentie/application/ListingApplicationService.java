package ro.agentie.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ro.agentie.application.events.WorkflowEvent;
import ro.agentie.domain.listing.*;
import ro.agentie.domain.shared.*;
import ro.agentie.infrastructure.*;
import ro.agentie.services.*;

@Service
public class ListingApplicationService implements IListingWorkflowService {
    private final IOfertaEntityRepository oferte;
    private final IProprietateEntityRepository proprietati;
    private final IPersoanaEntityRepository persoane;
    private final IZonaEntityRepository zone;
    private final IAgentEntityRepository agenti;
    private final IRealEstateEntityFactory factory;
    private final IValidatingDomainService validator;
    private final IAgentSelectionService selectie;
    private final ApplicationEventPublisher events;

    public ListingApplicationService(
            IOfertaEntityRepository o,
            IProprietateEntityRepository p,
            IPersoanaEntityRepository persoane,
            IZonaEntityRepository zone,
            IAgentEntityRepository a,
            IRealEstateEntityFactory f,
            IValidatingDomainService v,
            IAgentSelectionService selectie,
            ApplicationEventPublisher events) {
        oferte = o;
        proprietati = p;
        this.persoane = persoane;
        this.zone = zone;
        agenti = a;
        factory = f;
        validator = v;
        this.selectie = selectie;
        this.events = events;
    }

    /** CU1: build and validate both objects before adding either to its repository. */
    public Integer creeazaAnuntCuProprietate(CreareAnuntCommand command) {
        Proprietate p =
                new Proprietate(
                        proprietati.getNextID(),
                        persoane.getById(command.proprietarId()),
                        zone.getById(command.zonaId()),
                        command.tipProprietate(),
                        command.adresa(),
                        command.suprafata(),
                        command.numarCamere(),
                        command.etaj(),
                        command.facilitati(),
                        command.descriereProprietate());
        Oferta o =
                factory.buildOferta(
                        oferte.getNextID(),
                        p,
                        command.tipOferta(),
                        command.descriereAnunt(),
                        command.pret(),
                        command.data());
        validator.validateWithException(p);
        validator.validateWithException(o);
        proprietati.add(p);
        oferte.add(o);
        events.publishEvent(new WorkflowEvent("CREARE_ANUNT", o.getId()));
        return o.getId();
    }

    public Integer inregistreazaProprietate(
            Integer proprietarId,
            Integer zonaId,
            TipProprietate tip,
            String adresa,
            BigDecimal suprafata,
            int camere,
            int etaj,
            String facilitati,
            String descriere) {
        Proprietate p =
                new Proprietate(
                        proprietati.getNextID(),
                        persoane.getById(proprietarId),
                        zone.getById(zonaId),
                        tip,
                        adresa,
                        suprafata,
                        camere,
                        etaj,
                        facilitati,
                        descriere);
        validator.validateWithException(p);
        proprietati.add(p);
        return p.getId();
    }

    public Integer creeazaAnunt(
            Integer proprietateId, TipOferta tip, String descriere, BigDecimal pret, LocalDate data) {
        Oferta o =
                factory.buildOferta(
                        oferte.getNextID(), proprietati.getById(proprietateId), tip, descriere, pret, data);
        validator.validateWithException(o);
        oferte.add(o);
        events.publishEvent(new WorkflowEvent("CREARE_ANUNT", o.getId()));
        return o.getId();
    }

    public Agent propuneAgent(Integer id) {
        Oferta o = oferte.getById(id);
        return selectie.alegeAgentPentruOferta(agenti.findByZona(o.getProprietate().getZona()), o);
    }

    public void alocaAnunt(Integer id, Integer agentId) {
        Oferta o = oferte.getById(id);
        o.alocaAgent(agenti.getById(agentId));
        oferte.save(o);
        events.publishEvent(new WorkflowEvent("ALOCARE_ANUNT", id));
    }

    public void alocaAutomat(Integer id) {
        alocaAnunt(id, propuneAgent(id).getId());
    }

    public void activeazaAnunt(Integer id) {
        Oferta o = oferte.getById(id);
        o.activeaza();
        oferte.save(o);
        events.publishEvent(new WorkflowEvent("ACTIVARE_ANUNT", id));
    }

    public void arhiveazaAnunt(Integer id) {
        Oferta o = oferte.getById(id);
        o.arhiveaza();
        oferte.save(o);
    }

    public Oferta getOferta(Integer id) {
        return oferte.getById(id);
    }
}
