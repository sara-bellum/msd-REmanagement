package ro.agentie.application;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ro.agentie.application.events.WorkflowEvent;
import ro.agentie.domain.client.*;
import ro.agentie.domain.listing.*;
import ro.agentie.domain.shared.*;
import ro.agentie.infrastructure.*;
import ro.agentie.services.IValidatingDomainService;

@Service
public class ClientApplicationService implements IClientWorkflowService {
    private final IPersoanaEntityRepository persoane;
    private final IAgentEntityRepository agenti;
    private final IOfertaEntityRepository oferte;
    private final ICerereEntityRepository cereri;
    private final IInteresEntityRepository interese;
    private final ApplicationEventPublisher events;
    private final IValidatingDomainService validator;

    public ClientApplicationService(
            IPersoanaEntityRepository p,
            IAgentEntityRepository a,
            IOfertaEntityRepository o,
            ICerereEntityRepository c,
            IInteresEntityRepository i,
            ApplicationEventPublisher e,
            IValidatingDomainService v) {
        persoane = p;
        agenti = a;
        oferte = o;
        cereri = c;
        interese = i;
        events = e;
        validator = v;
    }

    public Integer inregistreazaClient(
            String nume, String prenume, String telefon, String email, String adresa) {
        Persoana p =
                new Persoana(
                        persoane.getNextID(), nume, prenume, telefon, email, adresa, TipPersoana.CLIENT);
        validator.validateWithException(p);
        persoane.add(p);
        return p.getId();
    }

    public Integer inregistreazaCerere(
            Integer clientId,
            TipCerere tip,
            TipProprietate proprietate,
            BigDecimal min,
            BigDecimal max,
            BigDecimal suprafata,
            int camere,
            LocalDate data) {
        Cerere c =
                new Cerere(
                        cereri.getNextID(),
                        persoane.getById(clientId),
                        tip,
                        proprietate,
                        min,
                        max,
                        suprafata,
                        camere,
                        data);
        cereri.add(c);
        return c.getId();
    }

    public void adaugaZonaPreferata(Integer id, ro.agentie.domain.shared.Zona zona, int prioritate) {
        Cerere c = cereri.getById(id);
        c.adaugaZonaPreferata(zona, prioritate);
        cereri.save(c);
    }

    public void activeazaCerere(Integer id) {
        Cerere c = cereri.getById(id);
        c.activeaza();
        cereri.save(c);
    }

    public void anuleazaCerere(Integer id) {
        Reguli.cere(
                interese.toCollection().stream()
                        .noneMatch(
                                i ->
                                        i.getCerere() != null
                                                && i.getCerere().getId().equals(id)
                                                && i.getStare() == StareInteres.IN_TRANZACTIE),
                "Cererea are o tranzactie in curs!");
        Cerere c = cereri.getById(id);
        c.anuleaza();
        cereri.save(c);
    }

    public List<Oferta> cautaOferte(Integer cerereId) {
        Cerere c = cereri.getById(cerereId);
        return oferte.toCollection().stream().filter(c::estePotrivitaCu).toList();
    }

    public Integer inregistreazaInteres(
            Integer clientId, Integer ofertaId, Integer cerereId, LocalDate data) {
        Reguli.cere(
                interese.toCollection().stream()
                        .noneMatch(
                                i ->
                                        i.getClient().getId().equals(clientId)
                                                && i.getOferta().getId().equals(ofertaId)
                                                && i.getStare() != StareInteres.RESPINS
                                                && i.getStare() != StareInteres.FINALIZAT),
                "Exista deja un interes deschis pentru aceasta oferta!");
        Interes i =
                new Interes(
                        interese.getNextID(),
                        persoane.getById(clientId),
                        oferte.getById(ofertaId),
                        cerereId == null ? null : cereri.getById(cerereId),
                        data,
                        "");
        interese.add(i);
        return i.getId();
    }

    public Integer programeazaVizionare(Integer interesId, Integer agentId, LocalDateTime dataOra) {
        // In this deliverable appointments are points in time; no duration was specified.
        verificaOraAgent(agentId, dataOra, null);
        Interes i = interese.getById(interesId);
        Vizionare v = i.programeazaVizionare(agenti.getById(agentId), dataOra);
        interese.save(i);
        events.publishEvent(new WorkflowEvent("PROGRAMARE_VIZIONARE", interesId));
        return v.getId();
    }

    private void verificaOraAgent(Integer id, LocalDateTime data, Vizionare exceptie) {
        Reguli.cere(
                interese.toCollection().stream()
                        .flatMap(i -> i.getVizionari().stream())
                        .noneMatch(
                                v ->
                                        v != exceptie
                                                && v.esteProgramata()
                                                && v.getAgent().getId().equals(id)
                                                && v.getDataOra().equals(data)),
                "Agentul are deja o vizionare la aceasta ora!");
    }

    public void confirmaVizionare(Integer id, Integer vId) {
        Interes i = interese.getById(id);
        i.getVizionare(vId).confirma();
        interese.save(i);
    }

    public void reprogrameazaVizionare(Integer id, Integer vId, LocalDateTime data) {
        Interes i = interese.getById(id);
        Vizionare v = i.getVizionare(vId);
        verificaOraAgent(v.getAgent().getId(), data, v);
        v.reprogrameaza(data);
        interese.save(i);
    }

    public void anuleazaVizionare(Integer id, Integer vId, String motiv) {
        Interes i = interese.getById(id);
        i.getVizionare(vId).anuleaza(motiv);
        interese.save(i);
    }

    public void marcheazaNeprezentare(Integer id, Integer vId) {
        Interes i = interese.getById(id);
        i.getVizionare(vId).marcheazaNeprezentare();
        interese.save(i);
    }

    public void inregistreazaRezultatVizionare(
            Integer id, Integer vId, String feedback, int nivel, DecizieClient decizie) {
        Interes i = interese.getById(id);
        i.getVizionare(vId).inregistreazaRezultat(feedback, nivel, decizie);
        interese.save(i);
        events.publishEvent(new WorkflowEvent("REZULTAT_VIZIONARE", id));
    }

    public Interes getInteres(Integer id) {
        return interese.getById(id);
    }
}
