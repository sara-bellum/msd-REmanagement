package ro.agentie.domain.shared;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.*;
import ro.agentie.domain.listing.Oferta;

// A person is stored once; Agent extends the same identity through JOINED inheritance.
@Entity
@Table(name = "persoane", uniqueConstraints = @UniqueConstraint(columnNames = "domain_id"))
// Parent and subclass rows share one primary key rather than duplicate person data.
@Inheritance(strategy = InheritanceType.JOINED)
public class Persoana extends Entitate {
    protected Persoana() {}

    @NotBlank private String nume;
    @NotBlank private String prenume;
    private String telefon;
    @Email private String email;
    private String adresa;

    @Enumerated(EnumType.STRING)
    private TipPersoana tipPersoana;

    public Persoana(
            Integer id,
            String nume,
            String prenume,
            String telefon,
            String email,
            String adresa,
            TipPersoana tip) {
        super(id);
        this.nume = Reguli.text(nume, "Nume");
        this.prenume = Reguli.text(prenume, "Prenume");
        this.telefon = Reguli.text(telefon, "Telefon");
        this.email = email;
        this.adresa = adresa;
        this.tipPersoana = Objects.requireNonNull(tip);
    }

    public void actualizeazaContact(String telefon, String email, String adresa) {
        String valid = Reguli.text(telefon, "Telefon");
        this.telefon = valid;
        this.email = email;
        this.adresa = adresa;
    }

    public boolean esteAceeasiPersoanaCu(Persoana p) {
        return p != null && getId().equals(p.getId());
    }

    public boolean esteProprietarul(Oferta o) {
        return esteAceeasiPersoanaCu(o.getProprietate().getProprietar());
    }

    public Integer getCodPersoana() {
        return getId();
    }

    public String getNume() {
        return nume;
    }

    public String getPrenume() {
        return prenume;
    }

    public String getNumeComplet() {
        return nume + " " + prenume;
    }

    public String getTelefon() {
        return telefon;
    }

    public String getEmail() {
        return email;
    }

    public String getAdresa() {
        return adresa;
    }

    public TipPersoana getTipPersoana() {
        return tipPersoana;
    }
}