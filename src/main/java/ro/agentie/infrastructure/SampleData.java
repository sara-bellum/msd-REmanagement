package ro.agentie.infrastructure;

import java.time.LocalDate;

/** IDs of the initialized examples; tests can retrieve them without assuming numeric IDs. */
public record SampleData(
        LocalDate dataReferinta,
        Integer proprietarId,
        Integer cumparatorId,
        Integer zonaCopouId,
        Integer agentVanzareId,
        Integer ofertaVanzareId,
        Integer cerereCumparareId,
        Integer ofertaInchiriereId,
        Integer interesInchiriereId,
        Integer vizionareInchiriereId,
        Integer agentInchiriereId) {}
