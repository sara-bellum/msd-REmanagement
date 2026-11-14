package ro.agentie;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

// Verify SQL migrations without requiring the partially integrated JPA application context.
class FlywayMigrationTest {
    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    @Test
    void migratesDisposableDatabaseAndCanRunAgain() throws Exception {
        String url = env("TEST_DB_URL",
                "jdbc:postgresql://localhost:5433/real_estate_test");
        String user = env("TEST_DB_USERNAME", "test");
        String password = env("TEST_DB_PASSWORD", "test");

        // Check the actual connected database before allowing any schema changes.
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("select current_database()")) {
            assertTrue(result.next());
            assertTrue(result.getString(1).endsWith("_test"),
                    "Use a disposable database whose name ends in _test");
        }

        // No application context: this test can run before every JPA domain is mapped.
        Flyway flyway = Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .load();
        flyway.migrate();

        // A repeat migration must validate history without reapplying successful scripts.
        assertEquals(0, flyway.migrate().migrationsExecuted);

        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {
            try (ResultSet result = statement.executeQuery(
                    "select count(*) from public.flyway_schema_history "
                            + "where version = '1' and success = true")) {
                assertTrue(result.next());
                assertEquals(1, result.getInt(1));
            }
            for (String table : List.of("persoane", "agenti", "zone",
                    "proprietati", "oferte", "cereri", "interese",
                    "vizionari", "rezultate", "demo_dataset")) {
                // Table names come only from this fixed list, never from external input.
                try (ResultSet result = statement.executeQuery(
                        "select to_regclass('public." + table + "')")) {
                    assertTrue(result.next());
                    assertNotNull(result.getString(1), "Missing table: " + table);
                }
            }
        }
    }
}