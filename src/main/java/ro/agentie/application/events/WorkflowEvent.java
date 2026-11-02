package ro.agentie.application.events;

import java.time.LocalDateTime;

/** Synchronous Spring event published after a successful workflow operation. */
public record WorkflowEvent(String actiune, Integer entitateId, LocalDateTime moment) {
  public WorkflowEvent(String actiune, Integer id) {
        this(actiune, id, LocalDateTime.now());
    }
}