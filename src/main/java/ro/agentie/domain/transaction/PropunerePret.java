package ro.agentie.domain.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import ro.agentie.domain.shared.*;

public final class PropunerePret extends Entitate {
    private final BigDecimal suma;
    private final ParteNegociere parte;
    private final LocalDate dataPropunere;
    private final String observatii;
    private StarePropunere stare = StarePropunere.IN_ASTEPTARE;

    PropunerePret(
            Integer id, BigDecimal suma, ParteNegociere parte, LocalDate data, String observatii) {
        super(id);
        this.suma = Reguli.pozitiv(suma, "Suma");
        this.parte = Objects.requireNonNull(parte);
        dataPropunere = Objects.requireNonNull(data);
        this.observatii = observatii;
    }

    void accepta() {
        verifica();
        stare = StarePropunere.ACCEPTATA;
    }

    void respinge() {
        verifica();
        stare = StarePropunere.RESPINSA;
    }

    void marcheazaContrapropusa() {
        verifica();
        stare = StarePropunere.CONTRAPROPUSA;
    }

    private void verifica() {
        Reguli.cere(stare == StarePropunere.IN_ASTEPTARE, "Propunerea nu este in asteptare!");
    }

    public Integer getIdPropunere() {
        return getId();
    }

    public BigDecimal getSuma() {
        return suma;
    }

    public ParteNegociere getParte() {
        return parte;
    }

    public LocalDate getDataPropunere() {
        return dataPropunere;
    }

    public String getObservatii() {
        return observatii;
    }

    public StarePropunere getStare() {
        return stare;
    }
}
