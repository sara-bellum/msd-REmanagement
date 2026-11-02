package ro.agentie;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ro.agentie.domain.client.*;
import ro.agentie.domain.listing.*;
import ro.agentie.infrastructure.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class FactorySampleDataTest {
    @Autowired IRealEstateEntityFactory factory;
    @Autowired IPersoanaEntityRepository persoane;
    @Autowired IAgentEntityRepository agenti;
    @Autowired IZonaEntityRepository zone;
    @Autowired IProprietateEntityRepository proprietati;
    @Autowired IOfertaEntityRepository oferte;
    @Autowired ICerereEntityRepository cereri;
    @Autowired IInteresEntityRepository interese;
    @Autowired IRezultatEntityRepository rezultate;

    @Test
    @DisplayName("Factory @PostConstruct initializes connected sample entities before tests")
    void sampleDataIsInitializedBySpring() {
        SampleData sample = factory.getSampleData();
        assertEquals(6, persoane.size());
        assertEquals(2, agenti.toCollection().size());
        assertEquals(3, zone.size());
        assertEquals(3, proprietati.size());
        assertEquals(3, oferte.size());
        assertEquals(1, cereri.size());
        assertEquals(1, interese.size());
        assertEquals(StareOferta.NOUA, oferte.getById(sample.ofertaVanzareId()).getStare());
        assertEquals(StareOferta.ACTIVA, oferte.getById(sample.ofertaInchiriereId()).getStare());
        assertEquals(StareCerere.ACTIVA, cereri.getById(sample.cerereCumparareId()).getStare());
        assertEquals(
                StareVizionare.SOLICITATA,
                interese
                        .getById(sample.interesInchiriereId())
                        .getVizionare(sample.vizionareInchiriereId())
                        .getStare());
        assertEquals(0, rezultate.size(), "The factory must not execute a completed workflow");
    }

    @Test
    @DisplayName("Calling sample initialization again does not duplicate or replace data")
    void initializationIsIdempotent() {
        SampleData before = factory.getSampleData();
        Object listing = oferte.getById(before.ofertaVanzareId());
        factory.initDomainServiceEntities();
        assertSame(before, factory.getSampleData());
        assertSame(listing, oferte.getById(before.ofertaVanzareId()));
        assertEquals(6, persoane.size());
        assertEquals(3, oferte.size());
        assertEquals(1, interese.size());
    }

    @Test
    @DisplayName("Agents share person identities and references connect the same repository objects")
    void sampleReferencesAreConsistent() {
        SampleData sample = factory.getSampleData();
        assertSame(persoane.getById(sample.agentVanzareId()), agenti.getById(sample.agentVanzareId()));
        Oferta sale = oferte.getById(sample.ofertaVanzareId());
        assertSame(persoane.getById(sample.proprietarId()), sale.getProprietate().getProprietar());
        assertSame(zone.getById(sample.zonaCopouId()), sale.getProprietate().getZona());
        assertSame(
                oferte.getById(sample.ofertaInchiriereId()),
                interese.getById(sample.interesInchiriereId()).getOferta());
        assertTrue(
                agenti
                        .getById(sample.agentInchiriereId())
                        .getPortofoliu()
                        .contains(oferte.getById(sample.ofertaInchiriereId())));
    }

}
