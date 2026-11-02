package ro.agentie.domain.listing;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Objects;
import ro.agentie.domain.shared.*;

public class Proprietate extends Entitate {
    private final Persoana proprietar;
    private final Zona zona;
    private final TipProprietate tipProprietate;
    @NotBlank private final String adresa;
    @Positive private final BigDecimal suprafata;
    private final int numarCamere, etaj;
    private final String facilitati, descriere;

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
