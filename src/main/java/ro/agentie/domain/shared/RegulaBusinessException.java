package ro.agentie.domain.shared;

/** A rejected business operation; messages match the acceptance scenarios. */
public class RegulaBusinessException extends IllegalStateException {
    public RegulaBusinessException(String message) {
        super(message);
    }
}
