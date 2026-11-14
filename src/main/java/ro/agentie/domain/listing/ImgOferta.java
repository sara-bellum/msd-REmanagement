package ro.agentie.domain.listing;

import jakarta.persistence.*;
import java.time.LocalDate;
import ro.agentie.domain.shared.*;

// Image metadata belongs to its listing; removing it must not remove the property.
@Entity
@Table(name = "imagini_oferte")
public class ImgOferta extends Entitate {
    protected ImgOferta() {}

    private String url, descriere;
    private LocalDate dataIncarcare;
    private int ordineAfisare;
    private boolean estePrincipala;

    public ImgOferta(Integer id, String url, String descriere, LocalDate data) {
        super(id);
        this.url = Reguli.text(url, "URL imagine");
        this.descriere = descriere;
        this.dataIncarcare = java.util.Objects.requireNonNull(data);
    }

    void seteazaPrincipala(boolean valoare) {
        estePrincipala = valoare;
    }

    void seteazaOrdine(int ordine) {
        Reguli.cere(ordine >= 0, "Ordine invalida!");
        ordineAfisare = ordine;
    }

    public Integer getIdImagine() {
        return getId();
    }

    public String getUrl() {
        return url;
    }

    public String getDescriere() {
        return descriere;
    }

    public int getOrdineAfisare() {
        return ordineAfisare;
    }

    public boolean isEstePrincipala() {
        return estePrincipala;
    }

    public LocalDate getDataIncarcare() {
        return dataIncarcare;
    }
}