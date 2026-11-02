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

class OfertaTest {
    DomainFixture f;

    @BeforeEach
    void setup() {
        f = new DomainFixture();
    }

    @Test
    void initialStateIsNew() {
        assertEquals(StareOferta.NOUA, f.oferta.getStare());
        assertNull(f.oferta.getAgent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void rejectInvalidPrice(String p) {
        assertThrows(
                RegulaBusinessException.class,
                () ->
                        new Oferta(
                                2,
                                f.proprietate,
                                TipOferta.VANZARE,
                                "test",
                                new BigDecimal(p),
                                DomainFixture.DATE));
    }

    @Test
    void activationRequiresAssignment() {
        assertThrows(RegulaBusinessException.class, f.oferta::activeaza);
        assertEquals(StareOferta.NOUA, f.oferta.getStare());
    }

    @Test
    void assignThenActivate() {
        f.activeaza();
        assertEquals(StareOferta.ACTIVA, f.oferta.getStare());
        assertTrue(f.agent.getPortofoliu().contains(f.oferta));
    }

    @Test
    void reassignmentPreservesActiveStateAndRemovesOldPortfolio() {
        f.activeaza();
        f.oferta.alocaAgent(f.agent2);
        assertEquals(StareOferta.ACTIVA, f.oferta.getStare());
        assertFalse(f.agent.getPortofoliu().contains(f.oferta));
        assertTrue(f.agent2.getPortofoliu().contains(f.oferta));
    }

    @Test
    void sameAgentDoesNotDuplicatePortfolio() {
        f.oferta.alocaAgent(f.agent);
        f.oferta.alocaAgent(f.agent);
        assertEquals(1, f.agent.getPortofoliu().size());
    }

    @Test
    void wrongZoneLeavesListingUnassigned() {
        Agent a = new Agent(8, "A", "B", "0700", "a@example.test", DomainFixture.DATE, BigDecimal.ZERO);
        a.adaugaZona(f.altaZona, LocalDate.of(2000, 1, 1), null);
        assertThrows(RegulaBusinessException.class, () -> f.oferta.alocaAgent(a));
        assertNull(f.oferta.getAgent());
        assertTrue(a.getPortofoliu().isEmpty());
    }

    @Test
    void ownerCannotBeResponsibleAgent() {
        Proprietate p =
                new Proprietate(
                        9, f.agent, f.zona, TipProprietate.CASA, "Adresa", BigDecimal.TEN, 1, 0, "", "");
        Oferta o = new Oferta(9, p, TipOferta.VANZARE, "Casa", BigDecimal.TEN, DomainFixture.DATE);
        assertThrows(RegulaBusinessException.class, () -> o.alocaAgent(f.agent));
    }

    @Test
    void reservationBelongsToTransaction() {
        f.activeaza();
        f.oferta.rezerva(10);
        assertThrows(RegulaBusinessException.class, () -> f.oferta.elibereaza(11));
        assertEquals(StareOferta.REZERVATA, f.oferta.getStare());
        f.oferta.elibereaza(10);
        assertTrue(f.oferta.esteDisponibila());
    }

    @Test
    void cannotReassignReservedListing() {
        f.activeaza();
        f.oferta.rezerva(1);
        assertThrows(RegulaBusinessException.class, () -> f.oferta.alocaAgent(f.agent2));
    }

    @Test
    void cannotArchiveReservedListing() {
        f.activeaza();
        f.oferta.rezerva(1);
        assertThrows(RegulaBusinessException.class, f.oferta::arhiveaza);
    }

    @Test
    void archivedListingCannotBeEdited() {
        f.oferta.arhiveaza();
        assertThrows(RegulaBusinessException.class, () -> f.oferta.modificaPret(BigDecimal.TEN));
    }

    @Test
    void negativePriceUpdateDoesNotChangePrice() {
        assertThrows(RegulaBusinessException.class, () -> f.oferta.modificaPret(BigDecimal.ZERO));
        assertEquals(new BigDecimal("85000"), f.oferta.getPretListare());
    }

    @Test
    void photoCollectionCannotBeChangedExternally() {
        assertThrows(UnsupportedOperationException.class, () -> f.oferta.getImagini().clear());
    }

    @Test
    void mainPhotoAndOrderStayConsistentAfterRemoval() {
        ImgOferta a = f.oferta.adaugaImagine("https://example.test/1", "living", DomainFixture.DATE);
        ImgOferta b = f.oferta.adaugaImagine("https://example.test/2", "camera", DomainFixture.DATE);
        f.oferta.seteazaImaginePrincipala(b.getId());
        assertFalse(a.isEstePrincipala());
        assertEquals(b, f.oferta.getImaginePrincipala());
        f.oferta.stergeImagine(b.getId());
        assertEquals(a, f.oferta.getImaginePrincipala());
        assertEquals(0, a.getOrdineAfisare());
    }

    @Test
    void photoIdsAreNotReusedAfterDeletion() {
        ImgOferta a = f.oferta.adaugaImagine("a", "", DomainFixture.DATE);
        f.oferta.stergeImagine(a.getId());
        assertNotEquals(a.getId(), f.oferta.adaugaImagine("b", "", DomainFixture.DATE).getId());
    }

    @Test
    void zoneAssignmentRespectsDates() {
        AgentZona z = new AgentZona(10, f.zona, DomainFixture.DATE, DomainFixture.DATE.plusDays(2));
        assertFalse(z.esteActivaLa(DomainFixture.DATE.minusDays(1)));
        assertTrue(z.esteActivaLa(DomainFixture.DATE));
        assertFalse(z.esteActivaLa(DomainFixture.DATE.plusDays(3)));
        z.dezactiveaza();
        assertFalse(z.esteActivaLa(DomainFixture.DATE));
    }
}
