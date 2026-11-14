package ro.agentie.domain.listing;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Objects;
import ro.agentie.domain.shared.*;

// A property references an existing owner and zone, each with an independent lifecycle.
@Entity
@Table(name = "proprietati", uniqueConstraints = @UniqueConstraint(columnNames = "domain_id"))
public class Proprietate extends Entitate {
    protected Proprietate() {}

    // This is a reference to an independently managed entity; no delete cascade is configured.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietar_id", nullable = false)
    private Persoana proprietar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_id", nullable = false)
    private Zona zona;

    @Enumerated(EnumType.STRING)
    private TipProprietate tipProprietate;

    @NotBlank private String adresa;
    @Positive private BigDecimal suprafata;
    private int numarCamere, etaj;
    private String facilitati, descriere;

    public Proprietate(
            Integer id,
            Persoana proprietar,
            Zona zona,
            TipProprietate tip,
            String adresa,
            BigDecimal suprafata,
            int camere,
            int etaj,
            String facilitati,
            String descriere) {
        super(id);
        this.proprietar = Objects.requireNonNull(proprietar);
        this.zona = Objects.requireNonNull(zona);
        this.tipProprietate = Objects.requireNonNull(tip);
        this.adresa = Reguli.text(adresa, "Adresa");
        this.suprafata = Reguli.pozitiv(suprafata, "Suprafata");
        Reguli.cere(camere >= 0, "Numarul de camere nu poate fi negativ!");
        this.numarCamere = camere;
        this.etaj = etaj;
        this.facilitati = facilitati;
        this.descriere = descriere;
    }

    public Integer getCodProprietate() {
        return getId();
    }

    public Persoana getProprietar() {
        return proprietar;
    }

    public Zona getZona() {
        return zona;
    }

    public TipProprietate getTipProprietate() {
        return tipProprietate;
    }

    public String getAdresa() {
        return adresa;
    }

    public BigDecimal getSuprafata() {
        return suprafata;
    }

    public int getNumarCamere() {
        return numarCamere;
    }

    public int getEtaj() {
        return etaj;
    }

    public String getFacilitati() {
        return facilitati;
    }

    public String getDescriere() {
        return descriere;
    }
}