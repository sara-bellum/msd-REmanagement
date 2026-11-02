package ro.agentie.application;

import java.time.*;
import ro.agentie.domain.client.DecizieClient;

public interface IClientWorkflowService {
    Integer inregistreazaInteres(
            Integer clientId, Integer ofertaId, Integer cerereId, LocalDate data);

    Integer programeazaVizionare(Integer interesId, Integer agentId, LocalDateTime dataOra);

    void confirmaVizionare(Integer interesId, Integer vizionareId);

    void inregistreazaRezultatVizionare(
            Integer interesId, Integer vizionareId, String feedback, int nivel, DecizieClient decizie);
}
