package ro.agentie.domain.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import ro.agentie.domain.shared.*;

public final class Contract extends Entitate {
    private final TipRezultat tipContract;
    private final BigDecimal pretNegociat;
    private MetodaPlata metodaPlata;
    private BigDecimal avans = BigDecimal.ZERO;
    private boolean avansPlatit;
    private String graficPlati = "", clauze = "";
    private LocalDate dataInceput, dataSfarsit, dataSemnare;
    private StareContract stare = StareContract.CIORNA;

    Contract(Integer id, TipRezultat tip, BigDecimal pret) {
        super(id);
        tipContract = Objects.requireNonNull(tip);
        pretNegociat = Reguli.pozitiv(pret, "Pret");
    }

    void modificaConditii(
            MetodaPlata metoda,
            BigDecimal avans,
            String grafic,
            LocalDate inceput,
            LocalDate sfarsit,
            String clauze) {
        Reguli.cere(
                stare == StareContract.CIORNA || stare == StareContract.IN_NEGOCIERE,
                "Contractul nu mai poate fi modificat!");
        Objects.requireNonNull(metoda);
        Reguli.nenegativ(avans, "Avans");
        if (tipContract == TipRezultat.VANZARE)
            Reguli.cere(avans.compareTo(pretNegociat) <= 0, "Avansul depaseste pretul!");
        if (tipContract == TipRezultat.INCHIRIERE)
            Reguli.cere(
                    inceput != null && sfarsit != null && sfarsit.isAfter(inceput),
                    "Perioada de inchiriere este invalida!");
        metodaPlata = metoda;
        this.avans = avans;
        graficPlati = grafic;
        dataInceput = inceput;
        dataSfarsit = sfarsit;
        this.clauze = clauze;
    }

    void trimiteLaNegociere() {
        Reguli.cere(stare == StareContract.CIORNA, "Contractul nu este ciorna!");
        stare = StareContract.IN_NEGOCIERE;
    }

    void convine() {
        Reguli.cere(
                stare == StareContract.CIORNA || stare == StareContract.IN_NEGOCIERE,
                "Contractul nu poate fi convenit!");
        Reguli.cere(metodaPlata != null, "Metoda de plata este obligatorie!");
        if (tipContract == TipRezultat.INCHIRIERE)
            Reguli.cere(
                    dataInceput != null && dataSfarsit != null && dataSfarsit.isAfter(dataInceput),
                    "Perioada de inchiriere este invalida!");
        stare = StareContract.CONVENIT;
    }

    void semneaza(LocalDate data) {
        Reguli.cere(stare == StareContract.CONVENIT, "Contractul nu este convenit!");
        dataSemnare = Objects.requireNonNull(data);
        stare = StareContract.SEMNAT;
    }

    void activeaza() {
        Reguli.cere(
                stare == StareContract.SEMNAT && tipContract == TipRezultat.INCHIRIERE,
                "Contractul nu poate fi activat!");
        stare = StareContract.ACTIV;
    }

    void inregistreazaPlataAvans() {
        Reguli.cere(
                stare == StareContract.SEMNAT || stare == StareContract.ACTIV,
                "Contractul nu este semnat!");
        avansPlatit = true;
    }

    void finalizeaza() {
        Reguli.cere(
                stare == StareContract.SEMNAT || stare == StareContract.ACTIV,
                "Contractul nu poate fi finalizat!");
        stare = StareContract.FINALIZAT;
    }

    void reziliaza() {
        Reguli.cere(
                stare != StareContract.FINALIZAT && stare != StareContract.REZILIAT,
                "Contractul este inchis!");
        stare = StareContract.REZILIAT;
    }

    public boolean obligatiiFinanciareIndeplinite() {
        return avans.signum() == 0 || avansPlatit;
    }

    public Integer getNrContract() {
        return getId();
    }

    public TipRezultat getTipContract() {
        return tipContract;
    }

    public BigDecimal getPretNegociat() {
        return pretNegociat;
    }

    public BigDecimal getAvans() {
        return avans;
    }

    public boolean isAvansPlatit() {
        return avansPlatit;
    }

    public MetodaPlata getMetodaPlata() {
        return metodaPlata;
    }

    public String getGraficPlati() {
        return graficPlati;
    }

    public String getClauze() {
        return clauze;
    }

    public LocalDate getDataInceput() {
        return dataInceput;
    }

    public LocalDate getDataSfarsit() {
        return dataSfarsit;
    }

    public LocalDate getDataSemnare() {
        return dataSemnare;
    }

    public StareContract getStare() {
        return stare;
    }
}
