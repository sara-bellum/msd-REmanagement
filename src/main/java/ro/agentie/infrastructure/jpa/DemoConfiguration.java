package ro.agentie.infrastructure.jpa;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;

// Demo data is opt-in and is initialized after the Spring application context starts.
@Configuration
// Never seed during memory tests or a normal PostgreSQL startup without demo enabled.
@Profile("demo & !memory")
public class DemoConfiguration {
    @Bean
    ApplicationRunner seedDemo(DemoDataSeeder seeder) {
        return args -> seeder.seed();
    }
}