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
import ro.agentie.infrastructure.*;
import ro.agentie.services.*;

class RepositoryServicesTest {
    DomainFixture f = new DomainFixture();

    @Test
    void idGeneratorDoesNotOverwriteExplicitIds() {
        IPersoanaEntityRepository repo = new PersoanaEntityRepositoryBase();
        repo.add(f.persoana(100, "A", "B"));
        assertEquals(101, repo.getNextID());
    }

    @Test
    void missingEntityThrowsClearError() {
        assertThrows(
                RegulaBusinessException.class, () -> new OfertaEntityRepositoryBase().getById(999));
    }

    @Test
    void repositorySupportsSaveUpdateAndRemove() {
        IOfertaEntityRepository repo = new OfertaEntityRepositoryBase();
        repo.add(f.oferta);
        f.oferta.modificaPret(BigDecimal.TEN);
        repo.save(f.oferta);
        assertEquals(1, repo.size());
        assertEquals(BigDecimal.TEN, repo.getById(1).getPretListare());
        assertTrue(repo.remove(f.oferta));
        assertEquals(0, repo.size());
    }

    @Test
    void agentRepositorySharesPersonIdentity() {
        IPersoanaEntityRepository p = new PersoanaEntityRepositoryBase();
        IAgentEntityRepository a = new AgentEntityRepositoryBase(p);
        a.add(f.agent);
        assertSame(f.agent, p.getById(f.agent.getId()));
    }

    @Test
    void choosesLeastLoadedAgentThenLowestId() {
        f.oferta.alocaAgent(f.agent);
        AgentSelectionService s = new AgentSelectionService();
        assertEquals(f.agent2, s.alegeAgentPentruOferta(List.of(f.agent2, f.agent), f.oferta));
    }

    @Test
    void noEligibleAgentRejected() {
        assertThrows(
                RegulaBusinessException.class,
                () -> new AgentSelectionService().alegeAgentPentruOferta(List.of(), f.oferta));
    }

    @Test
    void summaryContainsRealCounts() {
        f.oferta.adaugaImagine("image", "", DomainFixture.DATE);
        SummarizingDomainService s = new SummarizingDomainService();
        assertEquals(1, s.getOfertaSummary(f.oferta).numarImagini());
        assertEquals("Nealocata", s.getOfertaSummary(f.oferta).agent());
    }

    @Test
    void onlyCompletedTransactionsContributeToRevenue() {
        Rezultat r = f.rezultat();
        r.stabilesteComisioane(BigDecimal.TEN, BigDecimal.ZERO);
        SummarizingDomainService s = new SummarizingDomainService();
        assertEquals(BigDecimal.ZERO, s.totalComisioaneFinalizate(List.of(r)));
        f.semnat(r, BigDecimal.ZERO);
        r.inregistreazaPlataComisionCumparator();
        r.finalizeaza(DomainFixture.DATE);
        assertEquals(BigDecimal.TEN, s.totalComisioaneFinalizate(List.of(r)));
    }
}
