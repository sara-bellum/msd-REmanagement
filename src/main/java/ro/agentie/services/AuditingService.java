package ro.agentie.services;

import java.util.*;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import ro.agentie.application.events.WorkflowEvent;

@Service
public class AuditingService {
    private final List<WorkflowEvent> evenimente = new ArrayList<>();

    @EventListener
    public void audit(WorkflowEvent event) {
        evenimente.add(event);
    }

    public List<WorkflowEvent> getEvenimente() {
        return List.copyOf(evenimente);
    }
}
