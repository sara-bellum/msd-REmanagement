package ro.agentie;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import ro.agentie.application.*;
import ro.agentie.domain.client.*;
import ro.agentie.domain.listing.*;
import ro.agentie.domain.shared.*;
import ro.agentie.domain.transaction.*;
import ro.agentie.infrastructure.*;
import ro.agentie.infrastructure.jpa.*;

/** Real PostgreSQL, actual commits, and a new persistence context for every reload. */
// Use real commits and fresh persistence contexts to distinguish database state from Java memory.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
// The explicit test profile overrides the temporary default memory profile before L11.
@ActiveProfiles("test")
class PostgresPersistenceTest {
    @Autowired EntityManager em;
    @Autowired TransactionTemplate transaction;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource datasource;
    @Autowired IRealEstateEntityFactory factory;
    @Autowired IPersoanaEntityRepository persoane;
    @Autowired IAgentEntityRepository agenti;
    @Autowired IZonaEntityRepository zone;
    @Autowired IProprietateEntityRepository proprietati;
    @Autowired IOfertaEntityRepository oferte;
    @Autowired ICerereEntityRepository cereri;
    @Autowired IInteresEntityRepository interese;
    @Autowired IRezultatEntityRepository rezultate;
    @Autowired ListingApplicationService listings;
    @Autowired ClientApplicationService clienti;
    @Autowired ITranzactieWorkflowService tranzactii;
    @Autowired OfertaJpaRepository ofertaQueries;

    @BeforeEach
    void isolatedDatabase() {
        // This suite truncates tables; refuse cleanup unless the actual database has the test suffix.
        String database = jdbc.queryForObject("select current_database()", String.class);
        assertTrue(
                database.endsWith("_test"), "Refusing cleanup: test database name must end in _test");
        // Reset application rows only; retain Flyway history and do not use a shared development database.
        jdbc.execute(
                "TRUNCATE demo_dataset, agenti_rezultate, rezultate, vizionari, interese, cerere_zone,"
                        + " cereri, imagini_oferte, oferte, proprietati, agent_zone, agenti, persoane, zone"
                        + " CASCADE");
        // Rebuild the same fictional fixtures after cleanup through the real persistent factory.
        factory.initDomainServiceEntities();
    }

    // Each helper invocation has a transaction boundary; assertions reload managed data afresh.
    private <T> T tx(Supplier<T> operation) {
        return transaction.execute(
                status -> {
                    // Avoid passing an assertion merely because an entity is still cached in the persistence context.
                    em.clear();
                    T result = operation.get();
                    // Send pending SQL before this transaction commits so database failures are observable.
                    em.flush();
                    return result;
                });
    }

    private record Scenario(SampleData sample, Integer interesId, Integer vizionareId) {}

    // Prepare a valid listing/client/viewing path using application services rather than raw SQL fixtures.
    private Scenario scenario(boolean viewed) {
        SampleData s = factory.getSampleData();
        listings.alocaAutomat(s.ofertaVanzareId());
        listings.activeazaAnunt(s.ofertaVanzareId());
        Integer interest =
                clienti.inregistreazaInteres(
                        s.cumparatorId(), s.ofertaVanzareId(), s.cerereCumparareId(), s.dataReferinta());
        Integer viewing =
                clienti.programeazaVizionare(
                        interest, s.agentVanzareId(), s.dataReferinta().plusDays(2).atTime(15, 0));
        if (viewed) {
            clienti.confirmaVizionare(interest, viewing);
            clienti.inregistreazaRezultatVizionare(
                    interest,
                    viewing,
                    "Compartimentare potrivita; clientul doreste negociere.",
                    4,
                    DecizieClient.CONTINUA);
        }
        return new Scenario(s, interest, viewing);
    }

    @Test
        // Verify the migration and that JOINED agent/person lookups share one managed identity.
    void flywaySchemaAndJoinedPersonAgentIdentity() {
        assertEquals(
                1,
                jdbc.queryForObject(
                        "select count(*) from flyway_schema_history where success", Integer.class));
        SampleData s = factory.getSampleData();
        tx(
                () -> {
                    assertSame(persoane.getById(s.agentVanzareId()), agenti.getById(s.agentVanzareId()));
                    Agent a = agenti.getById(s.agentVanzareId());
                    assertEquals(2, a.getZone().size());
                    assertTrue(a.acoperaZona(zone.getById(s.zonaCopouId())));
                    assertEquals(1, a.getPortofoliu().size());
                    return null;
                });
    }

    @Test
        // A fresh Spring context must reuse the durable demo marker instead of duplicating records.
    void factoryIsIdempotentAcrossNewApplicationContext() throws Exception {
        SampleData before = factory.getSampleData();
        factory.initDomainServiceEntities();
        String url;
        try (var connection = datasource.getConnection()) {
            url = connection.getMetaData().getURL();
        }
        try (var restarted =
                     new SpringApplicationBuilder(RealEstateApplication.class)
                             .web(WebApplicationType.NONE)
                             .profiles("test", "demo")
                             .run(
                                     "--spring.datasource.url=" + url,
                                     "--spring.datasource.username="
                                             + System.getenv().getOrDefault("TEST_DB_USERNAME", "test"),
                                     "--spring.datasource.password="
                                             + System.getenv().getOrDefault("TEST_DB_PASSWORD", "test"))) {
            var newFactory = restarted.getBean(IRealEstateEntityFactory.class);
            assertEquals(before, newFactory.getSampleData());
            newFactory.initDomainServiceEntities();
            assertEquals(6, restarted.getBean(IPersoanaEntityRepository.class).size());
            assertEquals(3, restarted.getBean(IOfertaEntityRepository.class).size());
        }
    }

    @Test
        // Create through the service, then reload committed property/listing/agent relationships.
    void listingCreationAndAgentAssignmentSurviveReload() {
        SampleData s = factory.getSampleData();
        Integer property =
                listings.inregistreazaProprietate(
                        s.proprietarId(),
                        s.zonaCopouId(),
                        TipProprietate.APARTAMENT,
                        "Str. Sararie 72, Iasi (exemplu fictiv)",
                        new BigDecimal("54.50"),
                        2,
                        3,
                        "balcon",
                        "Apartament demonstrativ");
        Integer id =
                listings.creeazaAnunt(
                        property,
                        TipOferta.VANZARE,
                        "Apartament 2 camere Copou",
                        new BigDecimal("112000.00"),
                        s.dataReferinta());
        listings.alocaAutomat(id);
        listings.activeazaAnunt(id);
        tx(
                () -> {
                    Oferta o = oferte.getById(id);
                    assertEquals(StareOferta.ACTIVA, o.getStare());
                    assertEquals(new BigDecimal("112000.00"), o.getPretListare());
                    assertEquals(s.agentVanzareId(), o.getAgent().getId());
                    assertTrue(o.getAgent().getPortofoliu().contains(o));
                    assertEquals(2, ofertaQueries.findByStare(StareOferta.ACTIVA).size());
                    return null;
                });
    }

    @Test
        // Reassignment must persist both the listing owner link and the agents' portfolio views.
    void reassignmentKeepsPortfoliosConsistentAfterReload() {
        Scenario s = scenario(false);
        Integer other =
                tx(
                        () -> {
                            Agent a =
                                    new Agent(
                                            persoane.getNextID(),
                                            "Dobre",
                                            "Mihai",
                                            "0700000002",
                                            "mihai@example.test",
                                            LocalDate.now().minusYears(2),
                                            new BigDecimal("4000"));
                            a.adaugaZona(
                                    zone.getById(s.sample().zonaCopouId()), LocalDate.now().minusYears(1), null);
                            return agenti.add(a).getId();
                        });
        listings.alocaAnunt(s.sample().ofertaVanzareId(), other);
        tx(
                () -> {
                    Oferta o = oferte.getById(s.sample().ofertaVanzareId());
                    assertEquals(StareOferta.ACTIVA, o.getStare());
                    assertEquals(other, o.getAgent().getId());
                    assertFalse(agenti.getById(s.sample().agentVanzareId()).getPortofoliu().contains(o));
                    assertTrue(agenti.getById(other).getPortofoliu().contains(o));
                    return null;
                });
    }

    @Test
        // Deleting an owned image must keep the independent property and owner records.
    void imagesCascadeAndOrphanRemovalDoNotDeletePropertyOrOwner() {
        SampleData s = factory.getSampleData();
        Integer image =
                tx(
                        () ->
                                oferte
                                        .getById(s.ofertaVanzareId())
                                        .adaugaImagine(
                                                "https://example.test/copou/dormitor.jpg", "Dormitor", LocalDate.now())
                                        .getId());
        tx(
                () -> {
                    Oferta o = oferte.getById(s.ofertaVanzareId());
                    assertEquals(2, o.getImagini().size());
                    o.stergeImagine(image);
                    return null;
                });
        tx(
                () -> {
                    assertEquals(1, oferte.getById(s.ofertaVanzareId()).getImagini().size());
                    return null;
                });
        assertEquals(1, jdbc.queryForObject("select count(*) from imagini_oferte", Integer.class));
        assertEquals(3, proprietati.size());
        assertEquals(6, persoane.size());
    }

    @Test
        // Verify matching, feedback and distinct technical keys for equal child domain IDs.
    void requestsMatchAndViewingFeedbackPersistWithLocalChildIds() {
        Scenario s = scenario(true);
        assertTrue(
                clienti.cautaOferte(s.sample().cerereCumparareId()).stream()
                        .anyMatch(o -> o.getId().equals(s.sample().ofertaVanzareId())));
        tx(
                () -> {
                    Interes i = interese.getById(s.interesId());
                    assertEquals(s.sample().cerereCumparareId(), i.getCerere().getId());
                    assertEquals(1, i.getCerere().getZonePreferate().size());
                    Vizionare v = i.getVizionare(s.vizionareId());
                    assertEquals(StareVizionare.EFECTUATA, v.getStare());
                    assertEquals(DecizieClient.CONTINUA, v.getDecizie());
                    assertEquals(4, v.getNivelInteres());
                    assertSame(i, v.getInteres());
                    Vizionare other =
                            interese
                                    .getById(s.sample().interesInchiriereId())
                                    .getVizionare(s.sample().vizionareInchiriereId());
                    assertNotEquals(
                            v, other, "Child domain IDs are local; persisted identity must distinguish them");
                    return null;
                });
        // Both interests have viewing number 1, but database surrogate PKs are distinct.
        assertEquals(
                2, jdbc.queryForObject("select count(*) from vizionari where domain_id=1", Integer.class));
        assertEquals(
                2,
                jdbc.queryForObject(
                        "select count(distinct persistence_id) from vizionari where domain_id=1",
                        Integer.class));
    }

    @Test
        // Rejected operations must not alter the previously committed viewing data.
    void duplicateAppointmentAndInvalidFeedbackLeaveStoredStateUnchanged() {
        Scenario s = scenario(false);
        assertThrows(
                RegulaBusinessException.class,
                () ->
                        clienti.inregistreazaInteres(
                                s.sample().cumparatorId(), s.sample().ofertaVanzareId(), null, LocalDate.now()));
        Integer otherClient =
                clienti.inregistreazaClient("Marin", "Ana", "0700000003", "ana@example.test", "Iasi");
        Integer otherInterest =
                clienti.inregistreazaInteres(
                        otherClient, s.sample().ofertaVanzareId(), null, s.sample().dataReferinta());
        assertThrows(
                RegulaBusinessException.class,
                () ->
                        clienti.programeazaVizionare(
                                otherInterest,
                                s.sample().agentVanzareId(),
                                s.sample().dataReferinta().plusDays(2).atTime(15, 0)));
        clienti.confirmaVizionare(s.interesId(), s.vizionareId());
        assertThrows(
                RegulaBusinessException.class,
                () ->
                        clienti.inregistreazaRezultatVizionare(
                                s.interesId(), s.vizionareId(), "feedback", 6, DecizieClient.CONTINUA));
        tx(
                () -> {
                    assertEquals(
                            StareVizionare.CONFIRMATA,
                            interese.getById(s.interesId()).getVizionare(s.vizionareId()).getStare());
                    return null;
                });
    }

    @Test
        // Reservation and release must survive separate commits and reloads.
    void initiatesAndCancelsTransactionAcrossCommittedContexts() {
        Scenario s = scenario(true);
        Integer result =
                tranzactii.initiazaTranzactie(
                        s.interesId(), s.sample().agentVanzareId(), s.sample().dataReferinta().plusDays(2));
        tx(
                () -> {
                    Rezultat r = rezultate.getById(result);
                    assertEquals(StareRezultat.INITIAT, r.getStare());
                    assertEquals(StareOferta.REZERVATA, r.getOferta().getStare());
                    r.getOferta().verificaRezervare(result);
                    assertEquals(StareInteres.IN_TRANZACTIE, r.getInteres().getStare());
                    assertEquals(1, r.getParticipanti().size());
                    assertSame(r, r.getParticipanti().getFirst().getRezultat());
                    return null;
                });
        tranzactii.anuleazaTranzactie(
                result, "Clientul renunta", s.sample().dataReferinta().plusDays(3));
        tx(
                () -> {
                    Rezultat r = rezultate.getById(result);
                    assertEquals(StareRezultat.ANULAT, r.getStare());
                    assertEquals(StareOferta.ACTIVA, r.getOferta().getStare());
                    assertEquals(StareInteres.ACTIV, r.getInteres().getStare());
                    assertEquals("Clientul renunta", r.getMotivAnulare());
                    return null;
                });
    }

    @Test
        // Reject initiation before a completed viewing without reserving the listing.
    void missingViewingCannotReserveListing() {
        Scenario s = scenario(false);
        assertThrows(
                RegulaBusinessException.class,
                () ->
                        tranzactii.initiazaTranzactie(
                                s.interesId(), s.sample().agentVanzareId(), s.sample().dataReferinta()));
        assertEquals(0, rezultate.size());
        tx(
                () -> {
                    assertEquals(StareOferta.ACTIVA, oferte.getById(s.sample().ofertaVanzareId()).getStare());
                    return null;
                });
    }

    @Test
        // Force an SQL failure and prove that listing, interest and result changes roll back together.
    void insertFailureRollsBackReservationAndInterest() {
        Scenario s = scenario(true);
        jdbc.execute("ALTER TABLE rezultate ADD CONSTRAINT simulated_failure CHECK (false)");
        try {
            assertThrows(
                    RuntimeException.class,
                    () ->
                            tranzactii.initiazaTranzactie(
                                    s.interesId(),
                                    s.sample().agentVanzareId(),
                                    s.sample().dataReferinta().plusDays(2)));
        } finally {
            jdbc.execute("ALTER TABLE rezultate DROP CONSTRAINT simulated_failure");
        }
        assertEquals(0, rezultate.size());
        tx(
                () -> {
                    assertEquals(StareOferta.ACTIVA, oferte.getById(s.sample().ofertaVanzareId()).getStare());
                    assertEquals(StareInteres.ACTIV, interese.getById(s.interesId()).getStare());
                    return null;
                });
    }

    @Test
        // Both service and reloaded domain objects must reject unsupported persistent workflows.
    void unimplementedOperationsAreRejectedAtServiceAndDomainBoundary() {
        Scenario s = scenario(true);
        Integer result =
                tranzactii.initiazaTranzactie(
                        s.interesId(), s.sample().agentVanzareId(), s.sample().dataReferinta().plusDays(2));
        assertThrows(
                RegulaBusinessException.class,
                () ->
                        tranzactii.adaugaPropunere(
                                result, new BigDecimal("80000"), ParteNegociere.CUMPARATOR, LocalDate.now(), ""));
        assertThrows(
                RegulaBusinessException.class,
                () ->
                        tx(
                                () ->
                                        rezultate
                                                .getById(result)
                                                .adaugaPropunere(
                                                        new BigDecimal("80000"),
                                                        ParteNegociere.CUMPARATOR,
                                                        LocalDate.now(),
                                                        "")));
        tx(
                () -> {
                    assertEquals(StareRezultat.INITIAT, rezultate.getById(result).getStare());
                    return null;
                });
    }

    @Test
        // A new advanced in-memory result must not be saved while silently losing transient data.
    void entityCallbackRejectsAdvancedStateBuiltBeforeFirstSave() {
        Scenario s = scenario(true);
        assertThrows(
                RuntimeException.class,
                () ->
                        tx(
                                () -> {
                                    Rezultat r =
                                            factory.buildRezultat(
                                                    rezultate.getNextID(),
                                                    interese.getById(s.interesId()),
                                                    agenti.getById(s.sample().agentVanzareId()),
                                                    s.sample().dataReferinta().plusDays(2));
                                    r.adaugaPropunere(
                                            new BigDecimal("80000"),
                                            ParteNegociere.CUMPARATOR,
                                            s.sample().dataReferinta().plusDays(2),
                                            "");
                                    rezultate.add(r);
                                    return null;
                                }));
        assertEquals(0, rezultate.size());
        tx(
                () -> {
                    assertEquals(StareOferta.ACTIVA, oferte.getById(s.sample().ofertaVanzareId()).getStare());
                    return null;
                });
    }

    @Test
        // Verify stored timestamps and optimistic-lock version after an actual update.
    void auditCallbacksAndVersionAdvanceOnUpdate() {
        SampleData s = factory.getSampleData();
        var before =
                tx(
                        () -> {
                            Oferta o = oferte.getById(s.ofertaVanzareId());
                            assertNotNull(o.getCreatedAt());
                            assertNotNull(o.getUpdatedAt());
                            return new Object[] {o.getCreatedAt(), o.getVersion()};
                        });
        tx(
                () -> {
                    oferte.getById(s.ofertaVanzareId()).modificaPret(new BigDecimal("86000"));
                    return null;
                });
        tx(
                () -> {
                    Oferta o = oferte.getById(s.ofertaVanzareId());
                    assertEquals(before[0], o.getCreatedAt());
                    assertTrue(o.getVersion() > (Long) before[1]);
                    assertFalse(o.getUpdatedAt().isBefore(o.getCreatedAt()));
                    return null;
                });
    }
}