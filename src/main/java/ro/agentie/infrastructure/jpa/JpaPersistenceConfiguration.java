package ro.agentie.infrastructure.jpa;

import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// Explicit repository scanning keeps PostgreSQL repositories out of the memory profile.
@Configuration
@Profile("!memory")
// Scan the concrete repositories only when this profile-specific configuration is active.
@EnableJpaRepositories(basePackageClasses = DomainJpaRepository.class)
public class JpaPersistenceConfiguration {}
