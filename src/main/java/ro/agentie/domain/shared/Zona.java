package ro.agentie.domain.shared;

import jakarta.persistence.*;

// Zones are shared reference entities and must not be deleted with a listing.
@Entity
@Table(name = "zone", uniqueConstraints = @UniqueConstraint(columnNames = "domain_id"))
public class Zona extends Entitate {
    protected Zona() {}

    private String numeZona, localitate, judet;

    public Zona(Integer id, String nume, String localitate, String judet) {
        super(id);
        this.numeZona = Reguli.text(nume, "Zona");
        this.localitate = Reguli.text(localitate, "Localitate");
        this.judet = Reguli.text(judet, "Judet");
    }

    public Integer getCodZona() {
        return getId();
    }

    public String getNumeZona() {
        return numeZona;
    }

    public String getLocalitate() {
        return localitate;
    }

    public String getJudet() {
        return judet;
    }
}