package ro.agentie.infrastructure.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import ro.agentie.domain.shared.Entitate;

// Spring Data uses technical Long keys; application services still use domain Integer IDs.
// This generic contract is not instantiated as a repository by itself.
@NoRepositoryBean
public interface DomainJpaRepository<T extends Entitate> extends JpaRepository<T, Long> {
    // Preserve lookups by the ID already used by application callers.
    Optional<T> findByDomainId(Integer id);
}