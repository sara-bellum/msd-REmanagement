package ro.agentie.domain.client;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import ro.agentie.domain.listing.*;
import ro.agentie.domain.shared.*;

// A request is an aggregate root: preferred zones are its children, while the client is shared.
@Entity
@Table(name = "cereri", uniqueConstraints = @UniqueConstraint(columnNames = "domain_id"))
public class Cerere extends Entitate {
    // Required for JPA hydration; normal creation uses the validated domain constructor.
    protected Cerere() {}

    // Reference an existing entity without cascading its deletion from this object.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Persoana client;

    // Store enum names so reordering enum constants does not change saved values.
    @Enumerated(EnumType.STRING)
    private TipCerere tipCerere;

    // Store enum names so reordering enum constants does not change saved values.
    @Enumerated(EnumType.STRING)
    private TipProprietate tipProprietate;

    @Column(precision = 19, scale = 2)
    private BigDecimal bugetMin;

    @Column(precision = 19, scale = 2)
    private BigDecimal bugetMax;

    @Column(precision = 19, scale = 2)
    private BigDecimal suprafataDorita;

    private int nrCamereDorite;
    private LocalDate dataCerere;

    // Store enum names so reordering enum constants does not change saved values.
    @Enumerated(EnumType.STRING)
    private StareCerere stare = StareCerere.NOUA;

    // Cascade owned children; removing a child from this collection deletes its row.
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cerere_id")
    @OrderBy("domainId ASC")
    private List<CerereZona> zone = new ArrayList<>();

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

    // The child domain ID is local to this request; Entitate supplies its separate database key.
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

    // Matching remains a domain rule; JPA annotations must not change its conditions.
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