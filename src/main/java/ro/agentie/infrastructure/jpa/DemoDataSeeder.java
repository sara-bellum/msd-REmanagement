package ro.agentie.infrastructure.jpa;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.agentie.domain.shared.Reguli;
import ro.agentie.infrastructure.*;

/** Seeds fictional Iasi examples only on explicit request, atomically and once per database. */
// Seed records and their durable marker in the same transaction so retries stay safe.
@Service
@Profile("!memory")
public class DemoDataSeeder {
    private final JdbcTemplate jdbc;
    private final java.util.function.Supplier<RealEstateEntityFactory> builders;

    public DemoDataSeeder(
            JdbcTemplate jdbc,
            IPersoanaEntityRepository persoane,
            IAgentEntityRepository agenti,
            IZonaEntityRepository zone,
            IProprietateEntityRepository proprietati,
            IOfertaEntityRepository oferte,
            ICerereEntityRepository cereri,
            IInteresEntityRepository interese) {
        this.jdbc = jdbc;
        // A plain helper instance: its @PostConstruct is not invoked by Spring.
        builders =
                () ->
                        new RealEstateEntityFactory(
                                persoane, agenti, zone, proprietati, oferte, cereri, interese);
    }

    // Seed data and its marker atomically; a failed attempt must leave neither partially committed.
    @Transactional
    public void seed() {
        // Serialize simultaneous demo startups; the marker is durable, not an instance flag.
        jdbc.execute("select pg_advisory_xact_lock(20261114)");
        if (jdbc.queryForObject("select count(*) from demo_dataset", Integer.class) > 0) return;
        // Fresh helper per seed attempt: a rolled-back attempt must never cache nonexistent IDs.
        SampleData s = buildFresh();
        jdbc.update(
                "insert into demo_dataset values (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                s.dataReferinta(),
                s.proprietarId(),
                s.cumparatorId(),
                s.zonaCopouId(),
                s.agentVanzareId(),
                s.ofertaVanzareId(),
                s.cerereCumparareId(),
                s.ofertaInchiriereId(),
                s.interesInchiriereId(),
                s.vizionareInchiriereId(),
                s.agentInchiriereId());
    }

    private SampleData buildFresh() {
        RealEstateEntityFactory builder = builders.get();
        builder.initDomainServiceEntities();
        return builder.getSampleData();
    }

    @Transactional(readOnly = true)
    // Read durable IDs from PostgreSQL instead of trusting a process-local cache.
    public SampleData getSampleData() {
        var rows =
                jdbc.query(
                        "select * from demo_dataset where id=1",
                        (rs, n) ->
                                new SampleData(
                                        rs.getDate("data_referinta").toLocalDate(),
                                        rs.getInt("proprietar_id"),
                                        rs.getInt("cumparator_id"),
                                        rs.getInt("zona_copou_id"),
                                        rs.getInt("agent_vanzare_id"),
                                        rs.getInt("oferta_vanzare_id"),
                                        rs.getInt("cerere_cumparare_id"),
                                        rs.getInt("oferta_inchiriere_id"),
                                        rs.getInt("interes_inchiriere_id"),
                                        rs.getInt("vizionare_inchiriere_id"),
                                        rs.getInt("agent_inchiriere_id")));
        Reguli.cere(
                !rows.isEmpty(),
                "Date demo inexistente. Porniti cu profilul demo sau apelati initializarea explicit.");
        return rows.getFirst();
    }
}