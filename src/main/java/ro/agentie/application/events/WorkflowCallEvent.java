package ro.agentie.application.events;

import java.time.LocalDate;
import java.util.UUID;

/** Typed command event; correlation ID lets consumers associate the response. */
public record WorkflowCallEvent(
        UUID correlationId, WorkflowAction action, Integer id, LocalDate data) {}
