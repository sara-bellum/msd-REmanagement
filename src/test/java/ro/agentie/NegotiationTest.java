package ro.agentie;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.provider.*;
import ro.agentie.domain.client.*;
import ro.agentie.domain.listing.*;
import ro.agentie.domain.shared.*;
import ro.agentie.domain.transaction.*;

class NegotiationTest {
    DomainFixture f;
    Rezultat r;

    @BeforeEach
    void setup() {
        f = new DomainFixture();
        r = f.rezultat();
    }

    @Test
    void initiationReservesListingAndInterest() {
        assertEquals(StareOferta.REZERVATA, f.oferta.getStare());
        assertEquals(StareInteres.IN_TRANZACTIE, r.getInteres().getStare());
    }

    @Test
    void ownerCannotMakeFirstProposal() {
        assertThrows(
                RegulaBusinessException.class,
                () -> r.adaugaPropunere(BigDecimal.TEN, ParteNegociere.PROPRIETAR, DomainFixture.DATE, ""));
        assertTrue(r.getPropuneri().isEmpty());
    }

    @Test
    void counterOfferAndAcceptance() {
        PropunerePret first =
                r.adaugaPropunere(
                        new BigDecimal("78000"), ParteNegociere.CUMPARATOR, DomainFixture.DATE, "");
        PropunerePret second =
                r.adaugaPropunere(
                        new BigDecimal("83000"), ParteNegociere.PROPRIETAR, DomainFixture.DATE, "");
        assertEquals(StarePropunere.CONTRAPROPUSA, first.getStare());
        r.acceptaPropunerea();
        assertEquals(StarePropunere.ACCEPTATA, second.getStare());
        assertEquals(new BigDecimal("83000"), r.getPretConvenit());
        assertEquals(StareRezultat.CONVENIT, r.getStare());
    }

    @Test
    void samePartyCannotReplacePendingProposal() {
        PropunerePret p =
                r.adaugaPropunere(BigDecimal.TEN, ParteNegociere.CUMPARATOR, DomainFixture.DATE, "");
        assertThrows(
                RegulaBusinessException.class,
                () -> r.adaugaPropunere(BigDecimal.ONE, ParteNegociere.CUMPARATOR, DomainFixture.DATE, ""));
        assertEquals(StarePropunere.IN_ASTEPTARE, p.getStare());
    }

    @Test
    void invalidCounterofferDoesNotConsumeOldProposal() {
        PropunerePret p =
                r.adaugaPropunere(BigDecimal.TEN, ParteNegociere.CUMPARATOR, DomainFixture.DATE, "");
        assertThrows(
                RegulaBusinessException.class,
                () ->
                        r.adaugaPropunere(BigDecimal.ZERO, ParteNegociere.PROPRIETAR, DomainFixture.DATE, ""));
        assertEquals(StarePropunere.IN_ASTEPTARE, p.getStare());
    }

    @Test
    void rejectionKeepsNegotiationOpen() {
        r.adaugaPropunere(BigDecimal.TEN, ParteNegociere.CUMPARATOR, DomainFixture.DATE, "");
        r.respingePropunerea();
        assertEquals(StareRezultat.NEGOCIERE, r.getStare());
        assertNull(r.getPropunereInAsteptare());
    }

    @Test
    void cannotAcceptTwice() {
        f.convenit(r);
        assertThrows(RegulaBusinessException.class, r::acceptaPropunerea);
    }

    @Test
    void contractRequiresPriceAgreement() {
        assertThrows(RegulaBusinessException.class, r::pregatesteContract);
    }

    @Test
    void contractCopiesNegotiatedPrice() {
        f.convenit(r);
        assertEquals(r.getPretConvenit(), r.pregatesteContract().getPretNegociat());
    }

    @Test
    void duplicateParticipantRejected() {
        assertThrows(RegulaBusinessException.class, () -> r.adaugaAgentRezultat(f.agent));
    }

    @Test
    void maximumFourParticipantsAndEqualShares() {
        r.adaugaAgentRezultat(f.agent2);
        r.adaugaAgentRezultat(f.agent(5));
        r.adaugaAgentRezultat(f.agent(6));
        assertTrue(r.getParticipanti().stream().allMatch(a -> a.getDescrierePondere().equals("1/4")));
        assertThrows(RegulaBusinessException.class, () -> r.adaugaAgentRezultat(f.agent(7)));
    }

    @Test
    void cannotCreateSecondTransactionForSameListing() {
        assertThrows(
                RegulaBusinessException.class,
                () -> new Rezultat(2, r.getInteres(), f.agent2, DomainFixture.DATE));
    }

    @Test
    void noViewingLeavesBothObjectsUnchanged() {
        DomainFixture other = new DomainFixture();
        other.activeaza();
        Interes i = other.interes(null);
        assertThrows(
                RegulaBusinessException.class, () -> new Rezultat(1, i, other.agent, DomainFixture.DATE));
        assertEquals(StareInteres.ACTIV, i.getStare());
        assertEquals(StareOferta.ACTIVA, other.oferta.getStare());
    }

    @Test
    void conflictingResponsibleAgentLeavesObjectsUnchanged() {
        DomainFixture other = new DomainFixture();
        other.activeaza();
        Proprietate own =
                new Proprietate(
                        3,
                        other.agent2,
                        other.zona,
                        TipProprietate.CASA,
                        "Adresa",
                        BigDecimal.TEN,
                        1,
                        0,
                        "",
                        "");
        Oferta o = new Oferta(3, own, TipOferta.VANZARE, "Casa", BigDecimal.TEN, DomainFixture.DATE);
        o.alocaAgent(other.agent);
        o.activeaza();
        Interes i = new Interes(3, other.client, o, null, DomainFixture.DATE, "");
        Vizionare v = i.programeazaVizionare(other.agent, DomainFixture.DATE.atTime(12, 0));
        v.confirma();
        v.inregistreazaRezultat("Ok", 5, DecizieClient.CONTINUA);
        assertThrows(
                RegulaBusinessException.class, () -> new Rezultat(3, i, other.agent2, DomainFixture.DATE));
        assertTrue(o.esteDisponibila());
        assertEquals(StareInteres.ACTIV, i.getStare());
    }

    @Test
    void cancelRestoresAvailabilityAndClosesPendingProposal() {
        PropunerePret p =
                r.adaugaPropunere(BigDecimal.TEN, ParteNegociere.CUMPARATOR, DomainFixture.DATE, "");
        r.anuleaza("Fara acord", DomainFixture.DATE);
        assertTrue(f.oferta.esteDisponibila());
        assertEquals(StareInteres.ACTIV, r.getInteres().getStare());
        assertEquals(StarePropunere.RESPINSA, p.getStare());
    }
}
