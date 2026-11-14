package ro.agentie.infrastructure.jpa;

import ro.agentie.domain.listing.Oferta;

// Spring Data generates the implementation; common CRUD is inherited.
public interface OfertaJpaRepository extends DomainJpaRepository<Oferta> {
    java.util.List<Oferta> findByStare(ro.agentie.domain.listing.StareOferta stare);
}