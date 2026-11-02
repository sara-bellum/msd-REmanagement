package ro.agentie.application.events;

import java.util.UUID;

public record WorkflowResponseEvent(UUID correlationId, WorkflowAction action, Integer id) {}
