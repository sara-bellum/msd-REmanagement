package ro.agentie.domain.client;

import jakarta.persistence.*;
import ro.agentie.domain.shared.*;

// Preference data belongs to a request; the referenced zone has its own lifecycle.
@Entity
@Table(name = "cerere_zone")
public class CerereZona extends Entitate {
    // Required for JPA hydration; normal creation uses the validated domain constructor.
    protected CerereZona() {}

    // Reference an existing entity without cascading its deletion from this object.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_id", nullable = false)
    private Zona zona;

    private int prioritate;

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