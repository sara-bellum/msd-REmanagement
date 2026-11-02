package ro.agentie.domain.client;

import ro.agentie.domain.shared.*;

public final class CerereZona extends Entitate {
    private final Zona zona;
    private final int prioritate;

    public CerereZona(Integer id, Zona zona, int prioritate) {
        super(id);
        this.zona = java.util.Objects.requireNonNull(zona);
        Reguli.cere(prioritate > 0, "Prioritatea trebuie sa fie pozitiva!");
        this.prioritate = prioritate;
    }

    public Zona getZona() {
        return zona;
    }

    public int getPrioritate() {
        return prioritate;
    }
}
