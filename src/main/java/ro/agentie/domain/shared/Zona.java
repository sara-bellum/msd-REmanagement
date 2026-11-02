package ro.agentie.domain.shared;

public final class Zona extends Entitate {
    private final String numeZona, localitate, judet;

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
