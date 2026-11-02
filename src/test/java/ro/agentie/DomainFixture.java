package ro.agentie;

import java.math.BigDecimal;
import java.time.*;
import ro.agentie.domain.client.*;
import ro.agentie.domain.listing.*;
import ro.agentie.domain.shared.*;
import ro.agentie.domain.transaction.*;

/** Fixed business dates make tests repeatable; zone coverage is valid across current dates. */
class DomainFixture {
    static final LocalDate DATE = LocalDate.of(2030, 1, 10);

    static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    final Zona zona = new Zona(1, "Copou", "Iasi", "Iasi");
    final Zona altaZona = new Zona(2, "Canta", "Iasi", "Iasi");
    final Persoana proprietar = persoana(1, "Dumitru", "Ion");
    final Persoana client = persoana(2, "Moraru", "Teodor");
    final Agent agent = agent(3);
    final Agent agent2 = agent(4);
    final Proprietate proprietate =
            new Proprietate(
                    1,
                    proprietar,
                    zona,
                    TipProprietate.APARTAMENT,
                    "Bd. Carol I 40",
                    money("78"),
                    3,
                    2,
                    "balcon",
                    "Apartament");
    final Oferta oferta =
            new Oferta(1, proprietate, TipOferta.VANZARE, "Apartament Copou", money("85000"), DATE);

    Persoana persoana(int id, String nume, String prenume) {
        return new Persoana(
                id, nume, prenume, "0700000000", "client@example.test", "Iasi", TipPersoana.CLIENT);
    }

    Agent agent(int id) {
        Agent a =
                new Agent(
                        id,
                        "Agent",
                        "Numar" + id,
                        "0700000001",
                        "agent@example.test",
                        LocalDate.of(2020, 1, 1),
                        money("3000"));
        a.adaugaZona(zona, LocalDate.of(2000, 1, 1), null);
        return a;
    }

    void activeaza() {
        oferta.alocaAgent(agent);
        oferta.activeaza();
    }

    Cerere cerere() {
        Cerere c =
                new Cerere(
                        1,
                        client,
                        TipCerere.CUMPARARE,
                        TipProprietate.APARTAMENT,
                        money("70000"),
                        money("90000"),
                        money("60"),
                        2,
                        DATE);
        c.adaugaZonaPreferata(zona, 1);
        c.activeaza();
        return c;
    }

    Interes interes(Cerere c) {
        return new Interes(1, client, oferta, c, DATE, "");
    }

    Interes vizionat(Cerere c) {
        Interes i = interes(c);
        Vizionare v = i.programeazaVizionare(agent, DATE.atTime(17, 0));
        v.confirma();
        v.inregistreazaRezultat("Doreste negociere", 5, DecizieClient.CONTINUA);
        return i;
    }

    Rezultat rezultat() {
        activeaza();
        return new Rezultat(1, vizionat(null), agent, DATE);
    }

    void convenit(Rezultat r) {
        r.adaugaPropunere(money("83000"), ParteNegociere.CUMPARATOR, DATE, "");
        r.acceptaPropunerea();
    }

    void semnat(Rezultat r, BigDecimal avans) {
        convenit(r);
        r.pregatesteContract();
        r.modificaConditiiContract(MetodaPlata.TRANSFER_BANCAR, avans, "", DATE, DATE.plusYears(1), "");
        r.convineContract();
        r.semneazaContract(DATE);
    }
}
