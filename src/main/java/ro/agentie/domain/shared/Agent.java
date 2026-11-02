package ro.agentie.domain.shared;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import ro.agentie.domain.listing.*;

public class Agent extends Persoana {
    private final LocalDate dataAngajare;
    private final BigDecimal salariuBaza;
    private final List<AgentZona> zone = new ArrayList<>();
    private final Set<Oferta> portofoliu = new LinkedHashSet<>();

    public Agent(
            Integer id,
            String nume,
            String prenume,
            String telefon,
            String email,
            LocalDate angajare,
            BigDecimal salariu) {
        super(id, nume, prenume, telefon, email, "", TipPersoana.AGENT_IMOBILIAR);
        dataAngajare = Objects.requireNonNull(angajare);
        salariuBaza = Reguli.nenegativ(salariu, "Salariu");
    }

    public AgentZona adaugaZona(Zona zona, LocalDate inceput, LocalDate sfarsit) {
        AgentZona az = new AgentZona(zone.size() + 1, zona, inceput, sfarsit);
        zone.add(az);
        return az;
    }

    public boolean acoperaZona(Zona zona) {
        return acoperaZona(zona, LocalDate.now());
    }

    public boolean acoperaZona(Zona zona, LocalDate data) {
        return zone.stream().anyMatch(z -> z.getZona().equals(zona) && z.esteActivaLa(data));
    }

    /** Called only after Oferta has accepted the assignment; avoids a second source of truth. */
    public void inregistreazaInPortofoliu(Oferta oferta) {
        Reguli.cere(oferta.getAgent() == this, "Oferta nu este alocata acestui agent!");
        portofoliu.add(oferta);
    }

    public void eliminaDinPortofoliu(Oferta oferta) {
        Reguli.cere(oferta.getAgent() != this, "Oferta este inca alocata agentului!");
        portofoliu.remove(oferta);
    }

    public long getNumarOferteInLucru() {
        return portofoliu.stream()
                .filter(
                        o ->
                                Set.of(StareOferta.ALOCATA, StareOferta.ACTIVA, StareOferta.REZERVATA)
                                        .contains(o.getStare()))
                .count();
    }

    public Map<StareOferta, Long> getStatisticiPortofoliu() {
        return portofoliu.stream()
                .collect(Collectors.groupingBy(Oferta::getStare, Collectors.counting()));
    }

    public List<AgentZona> getZone() {
        return List.copyOf(zone);
    }

    public Set<Oferta> getPortofoliu() {
        return Set.copyOf(portofoliu);
    }

    public LocalDate getDataAngajare() {
        return dataAngajare;
    }

    public BigDecimal getSalariuBaza() {
        return salariuBaza;
    }
}
