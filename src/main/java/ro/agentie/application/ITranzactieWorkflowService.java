package ro.agentie.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import ro.agentie.domain.transaction.ParteNegociere;

public interface ITranzactieWorkflowService {
    Integer initiazaTranzactie(Integer interesId, Integer agentId, LocalDate data);

    Integer adaugaPropunere(
            Integer id, BigDecimal suma, ParteNegociere parte, LocalDate data, String observatii);

    void acceptaPropunerea(Integer id);

    void finalizeazaTranzactie(Integer id, LocalDate data);

    void anuleazaTranzactie(Integer id, String motiv, LocalDate data);
}
