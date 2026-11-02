package ro.agentie;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.test.annotation.DirtiesContext;
import ro.agentie.application.*;
import ro.agentie.application.events.*;
import ro.agentie.domain.client.*;
import ro.agentie.domain.listing.*;
import ro.agentie.domain.shared.*;
import ro.agentie.domain.transaction.*;
import ro.agentie.infrastructure.*;
import ro.agentie.services.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WorkflowIntegrationTest {
    @Autowired ListingApplicationService listings;
    @Autowired ClientApplicationService clients;
    @Autowired TranzactieApplicationService transactions;
    @Autowired IPersoanaEntityRepository persoane;
    @Autowired IZonaEntityRepository zone;
    @Autowired IAgentEntityRepository agenti;
    @Autowired IOfertaEntityRepository oferte;
    @Autowired IRezultatEntityRepository rezultate;
    @Autowired IValidatingDomainService validator;
    @Autowired AuditingService audit;
    @Autowired SummarizingDomainService summaries;
    @Autowired ApplicationEventPublisher events;
    @Autowired Responses responses;
    @Autowired IRealEstateEntityFactory factory;
    Integer ownerId, clientId, zoneId, agentId, listingId;
    LocalDate businessDate;

    @TestConfiguration
    static class Config {
        @Bean
        Responses responses() {
            return new Responses();
        }
    }

    static class Responses {
        final List<WorkflowResponseEvent> received = new ArrayList<>();

        @EventListener
        public void receive(WorkflowResponseEvent e) {
            received.add(e);
        }
    }

    @BeforeEach
    void data() {
        // The @PostConstruct factory has already initialized this context's repositories.
        SampleData sample = factory.getSampleData();
        ownerId = sample.proprietarId();
        clientId = sample.cumparatorId();
        zoneId = sample.zonaCopouId();
        agentId = sample.agentVanzareId();
        listingId = sample.ofertaVanzareId();
        businessDate = sample.dataReferinta();
    }

    Integer viewedInterest() {
        listings.alocaAutomat(listingId);
        listings.activeazaAnunt(listingId);
        Integer i =
                clients.inregistreazaInteres(
                        clientId, listingId, factory.getSampleData().cerereCumparareId(), businessDate);
        Integer v = clients.programeazaVizionare(i, agentId, businessDate.atTime(17, 0));
        clients.confirmaVizionare(i, v);
        clients.inregistreazaRezultatVizionare(i, v, "Doreste sa continue", 5, DecizieClient.CONTINUA);
        return i;
    }

    @Autowired IProprietateEntityRepository properties;

    @Test
    void createListingWithPropertyPersistsBothOnlyAfterValidation() {
        CreareAnuntCommand cmd =
                new CreareAnuntCommand(
                        ownerId,
                        zoneId,
                        TipProprietate.APARTAMENT,
                        "Adresa noua",
                        new BigDecimal("78"),
                        3,
                        2,
                        "balcon",
                        "",
                        TipOferta.VANZARE,
                        "Anunt nou",
                        new BigDecimal("85000"),
                        businessDate);
        int p = properties.size(), o = oferte.size();
        Integer id = listings.creeazaAnuntCuProprietate(cmd);
        assertEquals(p + 1, properties.size());
        assertEquals(o + 1, oferte.size());
        assertEquals(StareOferta.NOUA, listings.getOferta(id).getStare());
    }

    @Test
    void invalidListingPriceDoesNotLeaveAnOrphanProperty() {
        CreareAnuntCommand cmd =
                new CreareAnuntCommand(
                        ownerId,
                        zoneId,
                        TipProprietate.APARTAMENT,
                        "Adresa noua",
                        new BigDecimal("78"),
                        3,
                        2,
                        "balcon",
                        "",
                        TipOferta.VANZARE,
                        "Anunt nou",
                        BigDecimal.ZERO,
                        businessDate);
        int p = properties.size(), o = oferte.size();
        assertThrows(RegulaBusinessException.class, () -> listings.creeazaAnuntCuProprietate(cmd));
        assertEquals(p, properties.size());
        assertEquals(o, oferte.size());
    }

    @Test
    @DisplayName("Sale: assign, view, negotiate, sign, pay and complete with 40% agent commission")
    void fullSaleWorkflowThroughInjectedServices() {
        Integer i = viewedInterest();
        Integer id = transactions.initiazaTranzactie(i, agentId, businessDate);
        transactions.adaugaPropunere(
                id, new BigDecimal("78000"), ParteNegociere.CUMPARATOR, businessDate, "");
        transactions.adaugaPropunere(
                id, new BigDecimal("83000"), ParteNegociere.PROPRIETAR, businessDate, "");
        transactions.acceptaPropunerea(id);
        transactions.pregatesteContract(id);
        transactions.modificaConditiiContract(
                id, MetodaPlata.TRANSFER_BANCAR, new BigDecimal("8300"), "", null, null, "");
        transactions.convineContract(id);
        transactions.semneazaContract(id, businessDate);
        transactions.stabilesteComisioane(id, new BigDecimal("1500"), new BigDecimal("1500"));
        transactions.inregistreazaPlataAvans(id);
        transactions.inregistreazaPlataComisionCumparator(id);
        transactions.inregistreazaPlataComisionVanzator(id);
        transactions.finalizeazaTranzactie(id, businessDate);
        assertEquals(StareOferta.VANDUTA, listings.getOferta(listingId).getStare());
        assertEquals(StareInteres.FINALIZAT, clients.getInteres(i).getStare());
        assertEquals(StareRezultat.FINALIZAT, transactions.getRezultat(id).getStare());
        assertEquals(StareCerere.SATISFACUTA, clients.getInteres(i).getCerere().getStare());
        assertEquals(
                new BigDecimal("1200.00"),
                transactions.getRezultat(id).getComisionAgent(agenti.getById(agentId)));
        assertEquals(2, summaries.getTranzactieSummary(transactions.getRezultat(id)).numarPropuneri());
        assertTrue(
                audit.getEvenimente().stream()
                        .anyMatch(
                                e -> e.actiune().equals("FINALIZARE_TRANZACTIE") && e.entitateId().equals(id)));
    }

    @Test
    void failedInitiationDoesNotSaveTransactionOrReserveListing() {
        listings.alocaAutomat(listingId);
        listings.activeazaAnunt(listingId);
        Integer i =
                clients.inregistreazaInteres(
                        clientId, listingId, factory.getSampleData().cerereCumparareId(), businessDate);
        int count = rezultate.size();
        assertThrows(
                RegulaBusinessException.class,
                () -> transactions.initiazaTranzactie(i, agentId, businessDate));
        assertEquals(count, rezultate.size());
        assertTrue(listings.getOferta(listingId).esteDisponibila());
        assertEquals(StareInteres.ACTIV, clients.getInteres(i).getStare());
    }

    @Test
    void beanValidationRejectsMalformedEmailBeforeSaving() {
        int size = persoane.size();
        assertThrows(
                RegulaBusinessException.class,
                () -> clients.inregistreazaClient("Nume", "Prenume", "0700", "invalid-email", "Iasi"));
        assertEquals(size, persoane.size());
        assertFalse(
                validator
                        .validate(
                                new Persoana(
                                        999999, "Nume", "Prenume", "0700", "invalid", "Iasi", TipPersoana.CLIENT))
                        .isEmpty());
    }

    @Test
    void missingOwnerOrZoneIsRejected() {
        assertThrows(
                RegulaBusinessException.class,
                () ->
                        listings.inregistreazaProprietate(
                                -1, zoneId, TipProprietate.CASA, "Adresa", BigDecimal.TEN, 1, 0, "", ""));
        assertThrows(
                RegulaBusinessException.class,
                () ->
                        listings.inregistreazaProprietate(
                                ownerId, -1, TipProprietate.CASA, "Adresa", BigDecimal.TEN, 1, 0, "", ""));
    }

    @Test
    void springEventsInvokeWorkflowAndReturnCorrelatedResponse() {
        UUID correlation = UUID.randomUUID();
        events.publishEvent(
                new WorkflowCallEvent(correlation, WorkflowAction.ALOCARE_AUTOMATA, listingId, null));
        assertEquals(StareOferta.ALOCATA, listings.getOferta(listingId).getStare());
        assertTrue(
                responses.received.stream()
                        .anyMatch(r -> r.correlationId().equals(correlation) && r.id().equals(listingId)));
    }

    @Test
    void rejectsDuplicateOpenInterest() {
        listings.alocaAutomat(listingId);
        listings.activeazaAnunt(listingId);
        clients.inregistreazaInteres(
                clientId, listingId, factory.getSampleData().cerereCumparareId(), businessDate);
        assertThrows(
                RegulaBusinessException.class,
                () -> clients.inregistreazaInteres(clientId, listingId, null, businessDate));
    }

    @Test
    void rejectsAgentDoubleBookingAtSameTime() {
        Integer i =
                viewedInterest(); // First appointment is completed, so a second time slot is allowed.
        clients.programeazaVizionare(i, agentId, businessDate.plusDays(1).atTime(18, 0));
        Integer other =
                clients.inregistreazaClient("Alt", "Client", "0700000009", "other@example.test", "Iasi");
        Integer j = clients.inregistreazaInteres(other, listingId, null, businessDate);
        assertThrows(
                RegulaBusinessException.class,
                () -> clients.programeazaVizionare(j, agentId, businessDate.plusDays(1).atTime(18, 0)));
        assertTrue(clients.getInteres(j).getVizionari().isEmpty());
    }

    @Test
    @DisplayName(
            "Rental: use factory viewing, agree rent, sign and complete; contract remains active")
    void rentalWorkflowFromFactorySampleData() {
        SampleData sample = factory.getSampleData();
        Integer interest = sample.interesInchiriereId();
        Integer viewing = sample.vizionareInchiriereId();
        clients.confirmaVizionare(interest, viewing);
        clients.inregistreazaRezultatVizionare(
                interest, viewing, "Doreste sa inchirieze", 5, DecizieClient.CONTINUA);
        LocalDate dataTranzactie = businessDate.plusDays(1);
        Integer id =
                transactions.initiazaTranzactie(interest, sample.agentInchiriereId(), dataTranzactie);
        transactions.adaugaPropunere(
                id, new BigDecimal("400"), ParteNegociere.CUMPARATOR, dataTranzactie, "Chirie lunara");
        transactions.acceptaPropunerea(id);
        transactions.pregatesteContract(id);
        transactions.modificaConditiiContract(
                id,
                MetodaPlata.TRANSFER_BANCAR,
                BigDecimal.ZERO,
                "Lunar",
                dataTranzactie,
                dataTranzactie.plusYears(1),
                "");
        transactions.convineContract(id);
        transactions.semneazaContract(id, dataTranzactie);
        transactions.stabilesteComisioane(id, new BigDecimal("400"), BigDecimal.ZERO);
        transactions.inregistreazaPlataComisionCumparator(id);
        transactions.finalizeazaTranzactie(id, dataTranzactie);

        assertEquals(StareRezultat.FINALIZAT, transactions.getRezultat(id).getStare());
        assertEquals(
                StareOferta.INCHIRIATA, listings.getOferta(sample.ofertaInchiriereId()).getStare());
        assertEquals(StareContract.ACTIV, transactions.getRezultat(id).getContract().getStare());
        assertEquals(StareInteres.FINALIZAT, clients.getInteres(interest).getStare());
        assertNull(clients.getInteres(interest).getCerere());
        assertEquals(
                new BigDecimal("160.00"),
                transactions.getRezultat(id).getComisionAgent(agenti.getById(sample.agentInchiriereId())));
    }
}
