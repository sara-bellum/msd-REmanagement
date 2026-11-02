package ro.agentie.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import ro.agentie.domain.client.Interes;
import ro.agentie.domain.listing.*;
import ro.agentie.domain.shared.*;
import ro.agentie.domain.transaction.Rezultat;

public interface IRealEstateEntityFactory {
    void initDomainServiceEntities();

    SampleData getSampleData();

    Oferta buildOferta(
            Integer id,
            Proprietate proprietate,
            TipOferta tip,
            String descriere,
            BigDecimal pret,
            LocalDate data);

    Rezultat buildRezultat(Integer id, Interes interes, Agent agent, LocalDate data);
}
