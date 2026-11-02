package ro.agentie;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import ro.agentie.domain.client.*;
import ro.agentie.domain.listing.*;
import ro.agentie.domain.shared.*;
import ro.agentie.domain.transaction.*;

class ClientViewingTest {
    DomainFixture f;

    @BeforeEach
    void setup() {
        f = new DomainFixture();
        f.activeaza();
    }

    @Test
    void ownerCannotExpressInterest() {
        assertThrows(
                RegulaBusinessException.class,
                () -> new Interes(2, f.proprietar, f.oferta, null, DomainFixture.DATE, ""));
    }

    @Test
    void requestMatchesActiveSuitableListing() {
        assertTrue(f.cerere().estePotrivitaCu(f.oferta));
    }

    @Test
    void requestDoesNotMatchReservedListing() {
        Cerere c = f.cerere();
        f.oferta.rezerva(1);
        assertFalse(c.estePotrivitaCu(f.oferta));
    }

    @Test
    void requestMustBelongToInterestedClient() {
        assertThrows(
                RegulaBusinessException.class,
                () ->
                        new Interes(
                                2, f.persoana(9, "Alt", "Client"), f.oferta, f.cerere(), DomainFixture.DATE, ""));
    }

    @Test
    void requestNeedsZoneBeforeActivation() {
        Cerere c =
                new Cerere(
                        2,
                        f.client,
                        TipCerere.CUMPARARE,
                        TipProprietate.CASA,
                        BigDecimal.ZERO,
                        BigDecimal.TEN,
                        BigDecimal.ZERO,
                        0,
                        DomainFixture.DATE);
        assertThrows(RegulaBusinessException.class, c::activeaza);
    }

    @Test
    void duplicatePreferredZoneRejected() {
        Cerere c = f.cerere();
        assertThrows(RegulaBusinessException.class, () -> c.adaugaZonaPreferata(f.zona, 2));
    }

    @Test
    void invalidBudgetRejected() {
        assertThrows(
                RegulaBusinessException.class,
                () ->
                        new Cerere(
                                2,
                                f.client,
                                TipCerere.CUMPARARE,
                                TipProprietate.CASA,
                                BigDecimal.TEN,
                                BigDecimal.ONE,
                                BigDecimal.ZERO,
                                0,
                                DomainFixture.DATE));
    }

    @Test
    void scheduleConfirmAndReschedule() {
        Interes i = f.interes(null);
        Vizionare v = i.programeazaVizionare(f.agent, DomainFixture.DATE.atTime(17, 0));
        assertEquals(StareVizionare.SOLICITATA, v.getStare());
        v.confirma();
        v.reprogrameaza(DomainFixture.DATE.plusDays(1).atTime(18, 0));
        assertEquals(StareVizionare.SOLICITATA, v.getStare());
        assertEquals(18, v.getDataOra().getHour());
    }

    @Test
    void onlyOneScheduledViewingPerInterest() {
        Interes i = f.interes(null);
        i.programeazaVizionare(f.agent, DomainFixture.DATE.atTime(17, 0));
        assertThrows(
                RegulaBusinessException.class,
                () -> i.programeazaVizionare(f.agent, DomainFixture.DATE.atTime(18, 0)));
        assertEquals(1, i.getVizionari().size());
    }

    @Test
    void reservedListingRejectsViewing() {
        Interes i = f.interes(null);
        f.oferta.rezerva(2);
        assertThrows(
                RegulaBusinessException.class,
                () -> i.programeazaVizionare(f.agent, DomainFixture.DATE.atTime(17, 0)));
    }

    @ParameterizedTest
    @EnumSource(DecizieClient.class)
    void feedbackUpdatesInterestAccordingToDecision(DecizieClient decizie) {
        Interes i = f.interes(null);
        Vizionare v = i.programeazaVizionare(f.agent, DomainFixture.DATE.atTime(17, 0));
        v.confirma();
        v.inregistreazaRezultat("Feedback", 3, decizie);
        assertEquals(StareVizionare.EFECTUATA, v.getStare());
        assertEquals(
                decizie == DecizieClient.RESPINGE ? StareInteres.RESPINS : StareInteres.ACTIV,
                i.getStare());
        assertEquals("Feedback", v.getFeedback());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6, -1})
    void invalidInterestLevelDoesNotCompleteViewing(int level) {
        Vizionare v = f.interes(null).programeazaVizionare(f.agent, DomainFixture.DATE.atTime(17, 0));
        v.confirma();
        assertThrows(
                RegulaBusinessException.class,
                () -> v.inregistreazaRezultat("Feedback", level, DecizieClient.CONTINUA));
        assertEquals(StareVizionare.CONFIRMATA, v.getStare());
        assertNull(v.getFeedback());
    }

    @Test
    void cannotRecordOutcomeBeforeConfirmation() {
        Vizionare v = f.interes(null).programeazaVizionare(f.agent, DomainFixture.DATE.atTime(17, 0));
        assertThrows(
                RegulaBusinessException.class,
                () -> v.inregistreazaRezultat("Feedback", 5, DecizieClient.CONTINUA));
    }

    @Test
    void noShowAllowsAnotherViewing() {
        Interes i = f.interes(null);
        Vizionare v = i.programeazaVizionare(f.agent, DomainFixture.DATE.atTime(17, 0));
        v.confirma();
        v.marcheazaNeprezentare();
        assertEquals(StareInteres.ACTIV, i.getStare());
        assertNotNull(i.programeazaVizionare(f.agent, DomainFixture.DATE.plusDays(1).atTime(17, 0)));
    }

    @Test
    void cancellationIsTerminal() {
        Vizionare v = f.interes(null).programeazaVizionare(f.agent, DomainFixture.DATE.atTime(17, 0));
        v.anuleaza("Client indisponibil");
        assertThrows(RegulaBusinessException.class, v::confirma);
    }

    @Test
    void cancelledRequestCannotBeSatisfied() {
        Cerere c = f.cerere();
        c.anuleaza();
        assertThrows(RegulaBusinessException.class, c::marcheazaSatisfacuta);
    }
}
