package ro.agentie.domain.shared;

import java.math.BigDecimal;

public final class Reguli {
    private Reguli() {}

    public static void cere(boolean valid, String mesaj) {
        if (!valid) throw new RegulaBusinessException(mesaj);
    }

    public static String text(String value, String field) {
        cere(value != null && !value.isBlank(), field + " este obligatoriu!");
        return value.trim();
    }

    public static BigDecimal pozitiv(BigDecimal value, String field) {
        cere(value != null && value.signum() > 0, field + " trebuie sa fie pozitiv!");
        return value;
    }

    public static BigDecimal nenegativ(BigDecimal value, String field) {
        cere(value != null && value.signum() >= 0, field + " nu poate fi negativ!");
        return value;
    }
}
