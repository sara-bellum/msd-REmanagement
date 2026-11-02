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

class ContractCompletionTest {
    DomainFixture f;
    Rezultat r;

    @BeforeEach
    void setup() {
        f = new DomainFixture();
        r = f.rezultat();
    }

    @Test
    void paidSaleFinalizesAllStatesAndComputesCommissions() {
        r.adaugaAgentRezultat(f.agent2);
        f.semnat(r, new BigDecimal("8300"));
        r.stabilesteComisioane(new BigDecimal("1500"), new BigDecimal("1500"));
        r.inregistreazaPlataAvans();
        r.inregistreazaPlataComisionCumparator();
        r.inregistreazaPlataComisionVanzator();
        r.finalizeaza(DomainFixture.DATE);
        assertEquals(StareRezultat.FINALIZAT, r.getStare());
        assertEquals(StareContract.FINALIZAT, r.getContract().getStare());
        assertEquals(StareOferta.VANDUTA, f.oferta.getStare());
        assertEquals(StareInteres.FINALIZAT, r.getInteres().getStare());
        assertEquals(new BigDecimal("600.00"), r.getComisionAgent(f.agent));
        assertEquals(new BigDecimal("600.00"), r.getComisionAgent(f.agent2));
    }

    @Test
    void optionalRequestIsSatisfied() {
        DomainFixture g = new DomainFixture();
        g.activeaza();
        Cerere c = g.cerere();
        Rezultat t = new Rezultat(1, g.vizionat(c), g.agent, DomainFixture.DATE);
        g.semnat(t, BigDecimal.ZERO);
        t.finalizeaza(DomainFixture.DATE);
        assertEquals(StareCerere.SATISFACUTA, c.getStare());
    }

    @ParameterizedTest
    @CsvSource({"false,true,true", "true,false,true", "true,true,false"})
    void unpaidObligationsDoNotPartiallyFinalize(boolean advance, boolean buyer, boolean seller) {
        f.semnat(r, BigDecimal.TEN);
        r.stabilesteComisioane(BigDecimal.TEN, BigDecimal.TEN);
        if (advance) r.inregistreazaPlataAvans();
        if (buyer) r.inregistreazaPlataComisionCumparator();
        if (seller) r.inregistreazaPlataComisionVanzator();
        assertThrows(RegulaBusinessException.class, () -> r.finalizeaza(DomainFixture.DATE));
        assertEquals(StareRezultat.SEMNAT, r.getStare());
        assertEquals(StareOferta.REZERVATA, f.oferta.getStare());
        assertEquals(StareInteres.IN_TRANZACTIE, r.getInteres().getStare());
    }

    @Test
    void cannotFinalizeUnsignedTransaction() {
        f.convenit(r);
        assertThrows(RegulaBusinessException.class, () -> r.finalizeaza(DomainFixture.DATE));
    }

    @Test
    void completionDateCannotPrecedeSigning() {
        f.semnat(r, BigDecimal.ZERO);
        assertThrows(
                RegulaBusinessException.class, () -> r.finalizeaza(DomainFixture.DATE.minusDays(1)));
    }

    @Test
    void commissionsCannotBeNegative() {
        assertThrows(
                RegulaBusinessException.class,
                () -> r.stabilesteComisioane(BigDecimal.ONE.negate(), BigDecimal.ZERO));
    }

    @Test
    void paidCommissionsCannotBeChanged() {
        f.semnat(r, BigDecimal.ZERO);
        r.stabilesteComisioane(BigDecimal.TEN, BigDecimal.ZERO);
        r.inregistreazaPlataComisionCumparator();
        assertThrows(
                RegulaBusinessException.class,
                () -> r.stabilesteComisioane(BigDecimal.ONE, BigDecimal.ZERO));
    }

    @Test
    void cannotCancelAfterSigning() {
        f.semnat(r, BigDecimal.ZERO);
        assertThrows(RegulaBusinessException.class, () -> r.anuleaza("Nu", DomainFixture.DATE));
    }

    @Test
    void draftContractCannotBeSigned() {
        f.convenit(r);
        r.pregatesteContract();
        assertThrows(RegulaBusinessException.class, () -> r.semneazaContract(DomainFixture.DATE));
    }

    @Test
    void missingPaymentMethodPreventsAgreement() {
        f.convenit(r);
        r.pregatesteContract();
        assertThrows(RegulaBusinessException.class, r::convineContract);
    }

    @Test
    void cancelledDraftContractIsTerminated() {
        f.convenit(r);
        r.pregatesteContract();
        r.anuleaza("Retragere", DomainFixture.DATE);
        assertEquals(StareContract.REZILIAT, r.getContract().getStare());
    }

    @Test
    void participantChangesFrozenAfterSigning() {
        f.semnat(r, BigDecimal.ZERO);
        assertThrows(RegulaBusinessException.class, () -> r.adaugaAgentRezultat(f.agent2));
    }

    @Test
    void noCommissionCalculationBeforeCompletion() {
        assertThrows(RegulaBusinessException.class, () -> r.getComisionAgent(f.agent));
    }

    @Test
    void fractionalCommissionRemainderIsNotLost() {
        r.adaugaAgentRezultat(f.agent2);
        r.adaugaAgentRezultat(f.agent(5));
        f.semnat(r, BigDecimal.ZERO);
        r.stabilesteComisioane(BigDecimal.ONE, BigDecimal.ZERO);
        r.inregistreazaPlataComisionCumparator();
        r.finalizeaza(DomainFixture.DATE);
        BigDecimal sum =
                r.getParticipanti().stream()
                        .map(AgentRezultat::calculeazaComisionAgentSimplu)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("0.40"), sum);
        assertEquals(new BigDecimal("0.14"), r.getComisionAgent(f.agent));
    }

    @Test
    void completingRentalLeavesContractActiveAndZeroLandlordCommissionIsPaid() {
        DomainFixture g = new DomainFixture();
        Oferta o =
                new Oferta(
                        2,
                        g.proprietate,
                        TipOferta.INCHIRIERE,
                        "Chirie",
                        new BigDecimal("400"),
                        DomainFixture.DATE);
        o.alocaAgent(g.agent);
        o.activeaza();
        Interes i = new Interes(2, g.client, o, null, DomainFixture.DATE, "");
        Vizionare v = i.programeazaVizionare(g.agent, DomainFixture.DATE.atTime(17, 0));
        v.confirma();
        v.inregistreazaRezultat("Ok", 5, DecizieClient.CONTINUA);
        Rezultat t = new Rezultat(2, i, g.agent, DomainFixture.DATE);
        g.semnat(t, BigDecimal.ZERO);
        t.stabilesteComisioane(new BigDecimal("400"), BigDecimal.ZERO);
        t.inregistreazaPlataComisionCumparator();
        t.finalizeaza(DomainFixture.DATE);
        assertEquals(StareOferta.INCHIRIATA, o.getStare());
        assertEquals(StareContract.ACTIV, t.getContract().getStare());
        t.incheieInchiriere();
        assertEquals(StareContract.FINALIZAT, t.getContract().getStare());
    }

    @Test
    void rentalRequiresValidPeriod() {
        DomainFixture g = new DomainFixture();
        Oferta o =
                new Oferta(
                        2, g.proprietate, TipOferta.INCHIRIERE, "Chirie", BigDecimal.TEN, DomainFixture.DATE);
        o.alocaAgent(g.agent);
        o.activeaza();
        Interes i = new Interes(2, g.client, o, null, DomainFixture.DATE, "");
        Vizionare v = i.programeazaVizionare(g.agent, DomainFixture.DATE.atTime(17, 0));
        v.confirma();
        v.inregistreazaRezultat("Ok", 5, DecizieClient.CONTINUA);
        Rezultat t = new Rezultat(2, i, g.agent, DomainFixture.DATE);
        g.convenit(t);
        t.pregatesteContract();
        assertThrows(
                RegulaBusinessException.class,
                () -> t.modificaConditiiContract(MetodaPlata.NUMERAR, BigDecimal.ZERO, "", null, null, ""));
        assertEquals(StareContract.CIORNA, t.getContract().getStare());
    }

    @Test
    void repeatedCompletionRejectedWithoutDuplicateCommissionState() {
        f.semnat(r, BigDecimal.ZERO);
        r.finalizeaza(DomainFixture.DATE);
        assertThrows(RegulaBusinessException.class, () -> r.finalizeaza(DomainFixture.DATE));
    }
}
