package ro.agentie.domain.client;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import ro.agentie.domain.listing.*;
import ro.agentie.domain.shared.*;

// An interest links one client to a listing and optionally to a request; it owns its viewings.
@Entity
@Table(name = "interese", uniqueConstraints = @UniqueConstraint(columnNames = "domain_id"))
public class Interes extends Entitate {
    // Required for JPA hydration; normal creation uses the validated domain constructor.
    protected Interes() {}

    // Reference an existing entity without cascading its deletion from this object.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Persoana client;

    // Reference an existing entity without cascading its deletion from this object.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oferta_id", nullable = false)
    private Oferta oferta;

    // Reference an existing entity without cascading its deletion from this object.
    @ManyToOne(fetch = FetchType.LAZY)
    // A client may express interest without first registering a request.
    @JoinColumn(name = "cerere_id", nullable = true)
    private Cerere cerere;

    private LocalDate dataInteres;
    private String observatii;

    // Store enum names so reordering enum constants does not change saved values.
    @Enumerated(EnumType.STRING)
    private StareInteres stare = StareInteres.ACTIV;

    // Cascade owned children; removing a child from this collection deletes its row.
    // The child field named in mappedBy owns the foreign-key relationship.
    @OneToMany(mappedBy = "interes", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("domainId ASC")
    private List<Vizionare> vizionari = new ArrayList<>();

    public Interes(
            Integer id,
            Persoana client,
            Oferta oferta,
            Cerere cerere,
            LocalDate data,
            String observatii) {
        super(id);
        this.client = Objects.requireNonNull(client);
        this.oferta = Objects.requireNonNull(oferta);
        Reguli.cere(
                !client.esteProprietarul(oferta),
                "Proprietarul nu poate fi interesat de propria proprietate!");
        Reguli.cere(oferta.esteDisponibila(), "Oferta nu mai este activa!");
        if (cerere != null) {
            Reguli.cere(
                    client.esteAceeasiPersoanaCu(cerere.getClient()), "Cererea apartine altui client!");
            Reguli.cere(cerere.getStare() == StareCerere.ACTIVA, "Cererea nu este activa!");
            Reguli.cere(
                    (cerere.getTipCerere() == TipCerere.CUMPARARE)
                            == (oferta.getTipOferta() == TipOferta.VANZARE),
                    "Tipul cererii nu corespunde ofertei!");
        }
        this.cerere = cerere;
        dataInteres = Objects.requireNonNull(data);
        this.observatii = observatii;
    }

    // Add the child through the aggregate so its interest link and local ID stay consistent.
    public Vizionare programeazaVizionare(Agent agent, LocalDateTime dataOra) {
        Reguli.cere(stare == StareInteres.ACTIV, "Interesul nu este activ!");
        Reguli.cere(oferta.esteDisponibila(), "Oferta nu mai este activa!");
        Reguli.cere(
                vizionari.stream().noneMatch(Vizionare::esteProgramata),
                "Exista deja o vizionare programata pentru acest interes!");
        Reguli.cere(
                !agent.esteAceeasiPersoanaCu(client) && !agent.esteProprietarul(oferta),
                "Agentul nu poate intermedia propria vizionare!");
        Vizionare v = new Vizionare(vizionari.size() + 1, this, agent, dataOra);
        vizionari.add(v);
        return v;
    }

    public boolean aVizionatOferta() {
        return vizionari.stream().anyMatch(v -> v.getStare() == StareVizionare.EFECTUATA);
    }

    // A completed viewing and an active interest are prerequisites for a transaction.
    public void verificaIntrareInTranzactie() {
        Reguli.cere(stare == StareInteres.ACTIV, "Interesul nu este activ!");
        Reguli.cere(
                aVizionatOferta(), "Clientul trebuie sa fi vizionat proprietatea inainte de tranzactie!");
        Reguli.cere(
                cerere == null || cerere.getStare() == StareCerere.ACTIVA, "Cererea nu este activa!");
    }

    public void intraInTranzactie() {
        verificaIntrareInTranzactie();
        stare = StareInteres.IN_TRANZACTIE;
    }

    public void respinge() {
        Reguli.cere(stare == StareInteres.ACTIV, "Interesul nu este activ!");
        stare = StareInteres.RESPINS;
    }

    public void revinoLaActiv() {
        Reguli.cere(stare == StareInteres.IN_TRANZACTIE, "Interesul nu este in tranzactie!");
        stare = StareInteres.ACTIV;
    }

    public void finalizeaza() {
        Reguli.cere(stare == StareInteres.IN_TRANZACTIE, "Interesul nu este in tranzactie!");
        stare = StareInteres.FINALIZAT;
    }

    public void reactiveaza() {
        Reguli.cere(
                stare == StareInteres.RESPINS && oferta.esteDisponibila(),
                "Interesul nu poate fi reactivat!");
        stare = StareInteres.ACTIV;
    }

    public Vizionare getVizionare(Integer id) {
        return vizionari.stream()
                .filter(v -> v.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RegulaBusinessException("Vizionare inexistenta!"));
    }

    public Integer getIdInteres() {
        return getId();
    }

    public Persoana getClient() {
        return client;
    }

    public Oferta getOferta() {
        return oferta;
    }

    public Cerere getCerere() {
        return cerere;
    }

    public LocalDate getDataInteres() {
        return dataInteres;
    }

    public String getObservatii() {
        return observatii;
    }

    public StareInteres getStare() {
        return stare;
    }

    public List<Vizionare> getVizionari() {
        return List.copyOf(vizionari);
    }
}