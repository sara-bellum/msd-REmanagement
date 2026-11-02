package ro.agentie.domain.shared;

import java.util.Objects;

/** Stable identity. Mutable business fields are deliberately excluded from equality. */
public abstract class Entitate {
    private final Integer id;

    protected Entitate(Integer id) {
        Reguli.cere(id != null && id > 0, "Identificator invalid!");
        this.id = id;
    }

    public final Integer getId() {
        return id;
    }

    @Override
    public final boolean equals(Object other) {
        return other != null && getClass() == other.getClass() && id.equals(((Entitate) other).id);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), id);
    }
}
