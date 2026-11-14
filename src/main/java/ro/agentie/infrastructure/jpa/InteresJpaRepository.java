package ro.agentie.infrastructure.jpa;

import ro.agentie.domain.client.Interes;

// Spring Data generates the implementation; generic CRUD and domain-ID lookup are inherited.
public interface InteresJpaRepository extends DomainJpaRepository<Interes> {
    // Derived query follows client.domainId, not the technical client persistence key.
    java.util.List<Interes> findByClient_DomainId(Integer clientId);
}