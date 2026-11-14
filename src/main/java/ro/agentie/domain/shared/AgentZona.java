package ro.agentie.domain.shared;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;

// This assignment belongs to an agent and references a shared geographical zone.
@Entity
@Table(name = "agent_zone")
public class AgentZona extends Entitate {
    protected AgentZona() {}

    // This is a reference to an independently managed entity; no delete cascade is configured.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_id", nullable = false)
    private Zona zona;

    private LocalDate dataInceput;
    private LocalDate dataSfarsit;
    private boolean zonaActiva = true;

    public AgentZona(Integer id, Zona zona, LocalDate inceput, LocalDate sfarsit) {
        super(id);
        this.zona = Objects.requireNonNull(zona);
        this.dataInceput = Objects.requireNonNull(inceput);
        Reguli.cere(sfarsit == null || !sfarsit.isBefore(inceput), "Perioada zonei este invalida!");
        this.dataSfarsit = sfarsit;
    }

    public boolean esteActivaLa(LocalDate data) {
        return zonaActiva
                && !data.isBefore(dataInceput)
                && (dataSfarsit == null || !data.isAfter(dataSfarsit));
    }

    public void dezactiveaza() {
        zonaActiva = false;
    }

    public Zona getZona() {
        return zona;
    }

    public LocalDate getDataInceput() {
        return dataInceput;
    }

    public LocalDate getDataSfarsit() {
        return dataSfarsit;
    }

    public boolean isZonaActiva() {
        return zonaActiva;
    }
}