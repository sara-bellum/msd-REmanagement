package ro.agentie.infrastructure.jpa;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ro.agentie.domain.client.Interes;
import ro.agentie.domain.listing.*;
import ro.agentie.domain.shared.Agent;
import ro.agentie.domain.transaction.Rezultat;
import ro.agentie.infrastructure.*;

// Keep object construction compatible with the memory factory; delegate database seeding.
@Component
@Profile("!memory")
public class JpaRealEstateEntityFactory implements IRealEstateEntityFactory {
    private final DemoDataSeeder demo;

    public JpaRealEstateEntityFactory(DemoDataSeeder demo) {
        this.demo = demo;
    }

    public void initDomainServiceEntities() {
        demo.seed();
    }

    public SampleData getSampleData() {
        return demo.getSampleData();
    }

    public Oferta buildOferta(
            Integer id, Proprietate p, TipOferta tip, String descriere, BigDecimal pret, LocalDate data) {
        return new Oferta(id, p, tip, descriere, pret, data);
    }

    public Rezultat buildRezultat(Integer id, Interes i, Agent a, LocalDate data) {
        return new Rezultat(id, i, a, data);
    }
}