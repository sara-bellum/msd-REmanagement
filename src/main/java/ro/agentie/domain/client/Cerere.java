package ro.agentie.domain.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import ro.agentie.domain.listing.*;
import ro.agentie.domain.shared.*;

public class Cerere extends Entitate {
    private final Persoana client;
    private final TipCerere tipCerere;
    private final TipProprietate tipProprietate;
    private final BigDecimal bugetMin, bugetMax, suprafataDorita;
    private final int nrCamereDorite;
    private final LocalDate dataCerere;
    private StareCerere stare = StareCerere.NOUA;
    private final List<CerereZona> zone = new ArrayList<>();

    public Cerere(
            Integer id,
            Persoana client,
            TipCerere tip,
            TipProprietate proprietate,
            BigDecimal min,
            BigDecimal max,
            BigDecimal suprafata,
            int camere,
            LocalDate data) {
        super(id);
        this.client = Objects.requireNonNull(client);
        tipCerere = Objects.requireNonNull(tip);
        tipProprietate = Objects.requireNonNull(proprietate);
        bugetMin = Reguli.nenegativ(min, "Buget minim");
        bugetMax = Reguli.pozitiv(max, "Buget maxim");
        Reguli.cere(min.compareTo(max) <= 0, "Bugetul minim depaseste bugetul maxim!");
        suprafataDorita = Reguli.nenegativ(suprafata, "Suprafata");
        Reguli.cere(camere >= 0, "Numar camere invalid!");
        nrCamereDorite = camere;
        dataCerere = Objects.requireNonNull(data);
    }

    public CerereZona adaugaZonaPreferata(Zona zona, int prioritate) {
        Reguli.cere(
                stare == StareCerere.NOUA || stare == StareCerere.ACTIVA,
                "Cererea nu mai poate fi modificata!");
        Reguli.cere(
                zone.stream().noneMatch(z -> z.getZona().equals(zona)), "Zona preferata exista deja!");
        CerereZona z = new CerereZona(zone.size() + 1, zona, prioritate);
        zone.add(z);
        return z;
    }

    public void activeaza() {
        Reguli.cere(stare == StareCerere.NOUA, "Cererea nu este noua!");
        Reguli.cere(!zone.isEmpty(), "Cererea necesita cel putin o zona preferata!");
        stare = StareCerere.ACTIVA;
    }

    public void marcheazaSatisfacuta() {
        Reguli.cere(stare == StareCerere.ACTIVA, "Cererea nu este activa!");
        stare = StareCerere.SATISFACUTA;
    }

    public void anuleaza() {
        Reguli.cere(stare == StareCerere.NOUA || stare == StareCerere.ACTIVA, "Cererea este inchisa!");
        stare = StareCerere.ANULATA;
    }

    public boolean estePotrivitaCu(Oferta o) {
        if (o == null
                || stare != StareCerere.ACTIVA
                || !o.esteDisponibila()
                || client.esteProprietarul(o)) return false;
        Proprietate p = o.getProprietate();
        return ((tipCerere == TipCerere.CUMPARARE && o.getTipOferta() == TipOferta.VANZARE)
                || (tipCerere == TipCerere.INCHIRIERE && o.getTipOferta() == TipOferta.INCHIRIERE))
                && p.getTipProprietate() == tipProprietate
                && o.getPretListare().compareTo(bugetMin) >= 0
                && o.getPretListare().compareTo(bugetMax) <= 0
                && p.getSuprafata().compareTo(suprafataDorita) >= 0
                && p.getNumarCamere() >= nrCamereDorite
                && zone.stream().anyMatch(z -> z.getZona().equals(p.getZona()));
    }

    public Integer getNrCerere() {
        return getId();
    }

    public Persoana getClient() {
        return client;
    }

    public TipCerere getTipCerere() {
        return tipCerere;
    }

    public TipProprietate getTipProprietate() {
        return tipProprietate;
    }

    public BigDecimal getBugetMin() {
        return bugetMin;
    }

    public BigDecimal getBugetMax() {
        return bugetMax;
    }

    public BigDecimal getSuprafataDorita() {
        return suprafataDorita;
    }

    public int getNrCamereDorite() {
        return nrCamereDorite;
    }

    public LocalDate getDataCerere() {
        return dataCerere;
    }

    public StareCerere getStare() {
        return stare;
    }

    public List<CerereZona> getZonePreferate() {
        return List.copyOf(zone);
    }
}
