package ro.agentie.domain.client;

import java.time.LocalDateTime;
import java.util.Objects;
import ro.agentie.domain.shared.*;

public final class Vizionare extends Entitate {
    private final Interes interes;
    private final Agent agent;
    private LocalDateTime dataOra;
    private String note, feedback;
    private Integer nivelInteres;
    private DecizieClient decizie;
    private StareVizionare stare = StareVizionare.SOLICITATA;

    Vizionare(Integer id, Interes interes, Agent agent, LocalDateTime data) {
        super(id);
        this.interes = Objects.requireNonNull(interes);
        this.agent = Objects.requireNonNull(agent);
        dataOra = Objects.requireNonNull(data);
    }

    public void confirma() {
        Reguli.cere(stare == StareVizionare.SOLICITATA, "Vizionarea nu este solicitata!");
        Reguli.cere(
                interes.getStare() == StareInteres.ACTIV && interes.getOferta().esteDisponibila(),
                "Oferta sau interesul nu mai este activ!");
        stare = StareVizionare.CONFIRMATA;
    }

    public void reprogrameaza(LocalDateTime dataNoua) {
        Reguli.cere(esteProgramata(), "Vizionarea nu mai poate fi reprogramata!");
        Reguli.cere(
                interes.getStare() == StareInteres.ACTIV && interes.getOferta().esteDisponibila(),
                "Oferta sau interesul nu mai este activ!");
        dataOra = Objects.requireNonNull(dataNoua);
        stare = StareVizionare.SOLICITATA;
    }

    public void anuleaza(String motiv) {
        Reguli.cere(esteProgramata(), "Vizionarea este inchisa!");
        note = Reguli.text(motiv, "Motiv");
        stare = StareVizionare.ANULATA;
    }

    public void marcheazaNeprezentare() {
        Reguli.cere(stare == StareVizionare.CONFIRMATA, "Vizionarea nu este confirmata!");
        stare = StareVizionare.NEPREZENTARE;
    }

    public void inregistreazaRezultat(String feedback, int nivel, DecizieClient decizie) {
        Reguli.cere(stare == StareVizionare.CONFIRMATA, "Rezultatul necesita o vizionare confirmata!");
        Reguli.cere(interes.getStare() == StareInteres.ACTIV, "Interesul nu este activ!");
        Reguli.cere(nivel >= 1 && nivel <= 5, "Nivelul de interes trebuie sa fie intre 1 si 5!");
        String text = Reguli.text(feedback, "Feedback");
        Objects.requireNonNull(decizie);
        this.feedback = text;
        nivelInteres = nivel;
        this.decizie = decizie;
        stare = StareVizionare.EFECTUATA;
        if (decizie == DecizieClient.RESPINGE) interes.respinge();
    }

    public boolean esteProgramata() {
        return stare == StareVizionare.SOLICITATA || stare == StareVizionare.CONFIRMATA;
    }

    public Integer getIdVizionare() {
        return getId();
    }

    public Interes getInteres() {
        return interes;
    }

    public Agent getAgent() {
        return agent;
    }

    public LocalDateTime getDataOra() {
        return dataOra;
    }

    public String getNote() {
        return note;
    }

    public String getFeedback() {
        return feedback;
    }

    public Integer getNivelInteres() {
        return nivelInteres;
    }

    public DecizieClient getDecizie() {
        return decizie;
    }

    public StareVizionare getStare() {
        return stare;
    }
}
