package ro.agentie.infrastructure;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
import ro.agentie.domain.client.*;
import ro.agentie.domain.listing.*;
import ro.agentie.domain.shared.*;
import ro.agentie.domain.transaction.Rezultat;

/** Builds domain objects and initializes sample entities, as in the course example. */
@Component
public class RealEstateEntityFactory implements IRealEstateEntityFactory {
    private final IPersoanaEntityRepository persoane;
    private final IAgentEntityRepository agenti;
    private final IZonaEntityRepository zone;
    private final IProprietateEntityRepository proprietati;
    private final IOfertaEntityRepository oferte;
    private final ICerereEntityRepository cereri;
    private final IInteresEntityRepository interese;
    private SampleData sampleData;

    public RealEstateEntityFactory(
            IPersoanaEntityRepository persoane,
            IAgentEntityRepository agenti,
            IZonaEntityRepository zone,
            IProprietateEntityRepository proprietati,
            IOfertaEntityRepository oferte,
            ICerereEntityRepository cereri,
            IInteresEntityRepository interese) {
        this.persoane = persoane;
        this.agenti = agenti;
        this.zone = zone;
        this.proprietati = proprietati;
        this.oferte = oferte;
        this.cereri = cereri;
        this.interese = interese;
    }

    @Override
    public Oferta buildOferta(
            Integer id, Proprietate p, TipOferta tip, String descriere, BigDecimal pret, LocalDate data) {
        return new Oferta(id, p, tip, descriere, pret, data);
    }

    @Override
    public Rezultat buildRezultat(Integer id, Interes i, Agent a, LocalDate data) {
        return new Rezultat(id, i, a, data);
    }

    /**
     * Spring calls this once after injecting the repositories. Repeating it does not duplicate data.
     */
    @PostConstruct
    @Override
    public void initDomainServiceEntities() {
        if (sampleData != null) return;
        LocalDate data = LocalDate.now();

        Zona copou = zone.add(new Zona(zone.getNextID(), "Copou", "Iasi", "Iasi"));
        Zona centru = zone.add(new Zona(zone.getNextID(), "Centru", "Iasi", "Iasi"));
        Zona canta = zone.add(new Zona(zone.getNextID(), "Canta", "Iasi", "Iasi"));

        Persoana proprietar = client("Dumitru", "Ion", "owner@example.test");
        Persoana proprietara = client("Ionescu", "Elena", "landlord@example.test");
        Persoana cumparator = client("Moraru", "Teodor", "buyer@example.test");
        Persoana chiriasa = client("Stan", "Maria", "tenant@example.test");

        Agent alex = agent("Popescu", "Alex", "alex@example.test", data);
        alex.adaugaZona(copou, data.minusYears(1), null);
        alex.adaugaZona(canta, data.minusYears(1), null);
        Agent maria = agent("Ionescu", "Maria", "maria@example.test", data);
        maria.adaugaZona(centru, data.minusYears(1), null);

        Proprietate apartament =
                proprietate(proprietar, copou, TipProprietate.APARTAMENT, "Bd. Carol I 40", "78", 3, 2);
        Proprietate garsoniera =
                proprietate(
                        proprietara, centru, TipProprietate.GARSONIERA, "Str. Universitatii 10", "32", 1, 1);
        Proprietate casa =
                proprietate(proprietar, canta, TipProprietate.CASA, "Str. Canta 20", "120", 4, 0);

        // Starting states only: no negotiation, signed contract or completed sale is created here.
        Oferta vanzare =
                oferte.add(
                        buildOferta(
                                oferte.getNextID(),
                                apartament,
                                TipOferta.VANZARE,
                                "Apartament Copou",
                                new BigDecimal("85000"),
                                data));
        vanzare.adaugaImagine("https://example.test/copou/living.jpg", "Living", data);

        Oferta inchiriere =
                buildOferta(
                        oferte.getNextID(),
                        garsoniera,
                        TipOferta.INCHIRIERE,
                        "Garsoniera Centru",
                        new BigDecimal("400"),
                        data);
        inchiriere.alocaAgent(maria);
        inchiriere.activeaza();
        oferte.add(inchiriere);

        Oferta casaAlocata =
                buildOferta(
                        oferte.getNextID(),
                        casa,
                        TipOferta.VANZARE,
                        "Casa Canta",
                        new BigDecimal("130000"),
                        data);
        casaAlocata.alocaAgent(alex);
        oferte.add(casaAlocata);

        Cerere cerereCumparare =
                new Cerere(
                        cereri.getNextID(),
                        cumparator,
                        TipCerere.CUMPARARE,
                        TipProprietate.APARTAMENT,
                        new BigDecimal("70000"),
                        new BigDecimal("90000"),
                        new BigDecimal("60"),
                        2,
                        data);
        cerereCumparare.adaugaZonaPreferata(copou, 1);
        cerereCumparare.activeaza();
        cereri.add(cerereCumparare);

        Interes interesChirie = new Interes(interese.getNextID(), chiriasa, inchiriere, null, data, "");
        Vizionare vizionare = interesChirie.programeazaVizionare(maria, data.plusDays(1).atTime(17, 0));
        interese.add(interesChirie);

        sampleData =
                new SampleData(
                        data,
                        proprietar.getId(),
                        cumparator.getId(),
                        copou.getId(),
                        alex.getId(),
                        vanzare.getId(),
                        cerereCumparare.getId(),
                        inchiriere.getId(),
                        interesChirie.getId(),
                        vizionare.getId(),
                        maria.getId());
    }

    @Override
    public SampleData getSampleData() {
        Reguli.cere(sampleData != null, "Datele de exemplu nu au fost initializate!");
        return sampleData;
    }

    private Persoana client(String nume, String prenume, String email) {
        return persoane.add(
                new Persoana(
                        persoane.getNextID(), nume, prenume, "0700000000", email, "Iasi", TipPersoana.CLIENT));
    }

    private Agent agent(String nume, String prenume, String email, LocalDate data) {
        return agenti.add(
                new Agent(
                        persoane.getNextID(),
                        nume,
                        prenume,
                        "0700000001",
                        email,
                        data.minusYears(1),
                        new BigDecimal("3000")));
    }

    private Proprietate proprietate(
            Persoana proprietar,
            Zona zona,
            TipProprietate tip,
            String adresa,
            String suprafata,
            int camere,
            int etaj) {
        return proprietati.add(
                new Proprietate(
                        proprietati.getNextID(),
                        proprietar,
                        zona,
                        tip,
                        adresa,
                        new BigDecimal(suprafata),
                        camere,
                        etaj,
                        "",
                        "Proprietate de exemplu"));
    }
}
