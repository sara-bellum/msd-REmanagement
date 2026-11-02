package ro.agentie.application.events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import ro.agentie.application.*;

@Service
public class EventWorkflowService {
    private final ListingApplicationService listing;
    private final TranzactieApplicationService tranzactii;
    private final ApplicationEventPublisher publisher;

    public EventWorkflowService(
            ListingApplicationService l, TranzactieApplicationService t, ApplicationEventPublisher p) {
        listing = l;
        tranzactii = t;
        publisher = p;
    }

    @EventListener
    public void onCall(WorkflowCallEvent event) {
        switch (event.action()) {
            case ALOCARE_AUTOMATA -> listing.alocaAutomat(event.id());
            case ACTIVARE_ANUNT -> listing.activeazaAnunt(event.id());
            case FINALIZARE_TRANZACTIE -> tranzactii.finalizeazaTranzactie(event.id(), event.data());
        }
        publisher.publishEvent(
                new WorkflowResponseEvent(event.correlationId(), event.action(), event.id()));
    }
}
