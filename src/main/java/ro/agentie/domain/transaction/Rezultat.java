package ro.agentie.domain.transaction;

import java.math.*;
import java.time.LocalDate;
import java.util.*;
import ro.agentie.domain.client.*;
import ro.agentie.domain.listing.*;
import ro.agentie.domain.shared.*;

/** Aggregate root for negotiation, contract and participants. No public state setters. */
public class Rezultat extends Entitate {
    private final Interes interes;
    private final Agent agentResponsabil;
    private final TipRezultat tipRezultat;
    private final LocalDate dataRezultat;
    private LocalDate dataFinalizare;
    private BigDecimal pretConvenit;
    private BigDecimal comisionCumparator = BigDecimal.ZERO, comisionVanzator = BigDecimal.ZERO;
    private boolean comisionCumparatorPlatit, comisionVanzatorPlatit;
    private String motivAnulare;
    private StareRezultat stare = StareRezultat.INITIAT;
    private Contract contract;
    private final List<PropunerePret> propuneri = new ArrayList<>();
    private final List<AgentRezultat> participanti = new ArrayList<>();

    public Rezultat(Integer id, Interes interes, Agent agent, LocalDate data) {
        super(id);
        this.interes = Objects.requireNonNull(interes);
        agentResponsabil = Objects.requireNonNull(agent);
        dataRezultat = Objects.requireNonNull(data);
        // All rejection checks precede changes to other aggregate roots.
        interes.verificaIntrareInTranzactie();
        Reguli.cere(interes.getOferta().esteDisponibila(), "Oferta nu mai este activa!");
        Reguli.cere(!data.isBefore(interes.getDataInteres()), "Data tranzactiei precede interesul!");
        verificaAgent(agent);
        tipRezultat =
                interes.getOferta().getTipOferta() == TipOferta.VANZARE
                        ? TipRezultat.VANZARE
                        : TipRezultat.INCHIRIERE;
        interes.getOferta().rezerva(id);
        interes.intraInTranzactie();
        participanti.add(new AgentRezultat(1, agent, this));
    }

    private void verificaAgent(Agent a) {
        Objects.requireNonNull(a);
        Reguli.cere(
                !a.esteAceeasiPersoanaCu(interes.getClient()) && !a.esteProprietarul(interes.getOferta()),
                "Agentul nu poate intermedia o tranzactie in care este parte!");
    }

    public void adaugaAgentRezultat(Agent agent) {
        Reguli.cere(
                Set.of(
                                StareRezultat.INITIAT,
                                StareRezultat.NEGOCIERE,
                                StareRezultat.CONVENIT,
                                StareRezultat.PREGATIRE_CONTRACT)
                        .contains(stare),
                "Participantii nu mai pot fi modificati!");
        verificaAgent(agent);
        Reguli.cere(
                participanti.stream().noneMatch(p -> p.getAgent().esteAceeasiPersoanaCu(agent)),
                "Agentul participa deja!");
        Reguli.cere(participanti.size() < 4, "Nu se pot adauga mai mult de 4 agenti la un rezultat!");
        participanti.add(new AgentRezultat(participanti.size() + 1, agent, this));
    }

    public PropunerePret adaugaPropunere(
            BigDecimal suma, ParteNegociere parte, LocalDate data, String observatii) {
        Reguli.cere(
                stare == StareRezultat.INITIAT || stare == StareRezultat.NEGOCIERE,
                "Negocierea nu este permisa!");
        Reguli.cere(data != null && !data.isBefore(dataRezultat), "Data propunerii este invalida!");
        if (!propuneri.isEmpty())
            Reguli.cere(
                    !data.isBefore(propuneri.get(propuneri.size() - 1).getDataPropunere()),
                    "Data propunerii precede ultima propunere!");
        if (propuneri.isEmpty())
            Reguli.cere(
                    parte == ParteNegociere.CUMPARATOR,
                    "Prima propunere financiara este facuta de cumparator / chirias!");
        PropunerePret pending = getPropunereInAsteptare();
        if (pending != null)
            Reguli.cere(
                    pending.getParte() != parte, "Aceeasi parte nu poate propune de doua ori fara raspuns!");
        PropunerePret p = new PropunerePret(propuneri.size() + 1, suma, parte, data, observatii);
        if (pending != null) pending.marcheazaContrapropusa();
        propuneri.add(p);
        stare = StareRezultat.NEGOCIERE;
        return p;
    }

    public void acceptaPropunerea() {
        Reguli.cere(stare == StareRezultat.NEGOCIERE, "Tranzactia nu este in negociere!");
        PropunerePret p = asteptare();
        p.accepta();
        pretConvenit = p.getSuma();
        stare = StareRezultat.CONVENIT;
    }

    public void respingePropunerea() {
        Reguli.cere(stare == StareRezultat.NEGOCIERE, "Tranzactia nu este in negociere!");
        PropunerePret p = asteptare();
        p.respinge();
    }

    private PropunerePret asteptare() {
        PropunerePret p = getPropunereInAsteptare();
        Reguli.cere(p != null, "Nu exista propunere in asteptare!");
        return p;
    }

    public PropunerePret getPropunereInAsteptare() {
        return propuneri.stream()
                .filter(p -> p.getStare() == StarePropunere.IN_ASTEPTARE)
                .findFirst()
                .orElse(null);
    }

    public Contract pregatesteContract() {
        Reguli.cere(
                stare == StareRezultat.CONVENIT && contract == null,
                "Contractul poate fi creat doar dupa acordul de pret!");
        contract = new Contract(getId(), tipRezultat, pretConvenit);
        stare = StareRezultat.PREGATIRE_CONTRACT;
        return contract;
    }

    public void modificaConditiiContract(
            MetodaPlata metoda,
            BigDecimal avans,
            String grafic,
            LocalDate inceput,
            LocalDate sfarsit,
            String clauze) {
        verificaPregatire();
        contract.modificaConditii(metoda, avans, grafic, inceput, sfarsit, clauze);
    }

    public void trimiteContractLaNegociere() {
        verificaPregatire();
        contract.trimiteLaNegociere();
    }

    public void convineContract() {
        verificaPregatire();
        contract.convine();
    }

    public void semneazaContract(LocalDate data) {
        verificaPregatire();
        Reguli.cere(data != null && !data.isBefore(dataRezultat), "Data semnarii este invalida!");
        contract.semneaza(data);
        if (tipRezultat == TipRezultat.INCHIRIERE) contract.activeaza();
        stare = StareRezultat.SEMNAT;
    }

    private void verificaPregatire() {
        Reguli.cere(
                stare == StareRezultat.PREGATIRE_CONTRACT && contract != null,
                "Tranzactia nu este in pregatirea contractului!");
    }

    public void stabilesteComisioane(BigDecimal cumparator, BigDecimal vanzator) {
        Reguli.cere(
                stare != StareRezultat.FINALIZAT && stare != StareRezultat.ANULAT,
                "Tranzactia este inchisa!");
        Reguli.cere(
                !comisionCumparatorPlatit && !comisionVanzatorPlatit,
                "Comisioanele platite nu mai pot fi modificate!");
        Reguli.nenegativ(cumparator, "Comision cumparator");
        Reguli.nenegativ(vanzator, "Comision vanzator");
        Reguli.cere(
                cumparator.scale() <= 2 && vanzator.scale() <= 2, "Comisioanele au maximum doua zecimale!");
        comisionCumparator = cumparator;
        comisionVanzator = vanzator;
    }

    public void inregistreazaPlataAvans() {
        verificaSemnat();
        contract.inregistreazaPlataAvans();
    }

    public void inregistreazaPlataComisionCumparator() {
        verificaSemnat();
        comisionCumparatorPlatit = true;
    }

    public void inregistreazaPlataComisionVanzator() {
        verificaSemnat();
        comisionVanzatorPlatit = true;
    }

    private void verificaSemnat() {
        Reguli.cere(stare == StareRezultat.SEMNAT, "Tranzactia nu este semnata!");
    }

    public boolean comisioanePlatite() {
        return (comisionCumparator.signum() == 0 || comisionCumparatorPlatit)
                && (comisionVanzator.signum() == 0 || comisionVanzatorPlatit);
    }

    public void finalizeaza(LocalDate data) {
        verificaSemnat();
        Reguli.cere(
                data != null && !data.isBefore(dataRezultat) && !data.isBefore(contract.getDataSemnare()),
                "Data finalizarii este invalida!");
        Reguli.cere(
                contract.obligatiiFinanciareIndeplinite(),
                "Avansul prevazut in contract nu a fost platit!");
        Reguli.cere(comisioanePlatite(), "Comisioanele agentiei nu au fost platite integral!");
        Reguli.cere(
                interes.getStare() == StareInteres.IN_TRANZACTIE, "Interesul nu este in tranzactie!");
        Reguli.cere(
                interes.getCerere() == null || interes.getCerere().getStare() == StareCerere.ACTIVA,
                "Cererea nu este activa!");
        Reguli.cere(
                contract.getStare() == StareContract.SEMNAT || contract.getStare() == StareContract.ACTIV,
                "Contractul nu permite finalizarea!");
        Oferta o = interes.getOferta();
        o.verificaRezervare(getId());
        // All validation above; no rejecting operation follows the first state change.
        o.finalizeaza(getId());
        interes.finalizeaza();
        if (interes.getCerere() != null) interes.getCerere().marcheazaSatisfacuta();
        if (tipRezultat == TipRezultat.VANZARE) contract.finalizeaza();
        stare = StareRezultat.FINALIZAT;
        dataFinalizare = data;
    }

    public void anuleaza(String motiv, LocalDate data) {
        Reguli.cere(
                stare != StareRezultat.SEMNAT
                        && stare != StareRezultat.FINALIZAT
                        && stare != StareRezultat.ANULAT,
                "Tranzactia nu mai poate fi anulata!");
        String text = Reguli.text(motiv, "Motiv");
        Reguli.cere(data != null && !data.isBefore(dataRezultat), "Data anularii este invalida!");
        interes.getOferta().verificaRezervare(getId());
        Reguli.cere(
                interes.getStare() == StareInteres.IN_TRANZACTIE, "Interesul nu este in tranzactie!");
        if (contract != null) contract.reziliaza();
        PropunerePret p = getPropunereInAsteptare();
        if (p != null) p.respinge();
        interes.getOferta().elibereaza(getId());
        interes.revinoLaActiv();
        motivAnulare = text;
        stare = StareRezultat.ANULAT;
    }

    public void incheieInchiriere() {
        Reguli.cere(
                stare == StareRezultat.FINALIZAT && tipRezultat == TipRezultat.INCHIRIERE,
                "Nu exista inchiriere finalizata!");
        contract.finalizeaza();
    }

    public void reziliazaInchiriere(String motiv) {
        Reguli.cere(
                stare == StareRezultat.FINALIZAT
                        && tipRezultat == TipRezultat.INCHIRIERE
                        && contract.getStare() == StareContract.ACTIV,
                "Nu exista contract de inchiriere activ!");
        Reguli.text(motiv, "Motiv");
        contract.reziliaza();
    }

    /** Agent pool is 40%; distribute remainder cents deterministically in participant order. */
    public BigDecimal getComisionAgent(Agent agent) {
        Reguli.cere(
                stare == StareRezultat.FINALIZAT, "Comisionul agentului se calculeaza dupa finalizare!");
        int index = -1;
        for (int i = 0; i < participanti.size(); i++)
            if (participanti.get(i).getAgent().esteAceeasiPersoanaCu(agent)) index = i;
        Reguli.cere(index >= 0, "Agentul nu participa la tranzactie!");
        BigDecimal pool =
                getComisionAgentieTotal()
                        .multiply(new BigDecimal("0.40"))
                        .setScale(2, RoundingMode.HALF_UP);
        BigDecimal share = pool.divide(BigDecimal.valueOf(participanti.size()), 2, RoundingMode.DOWN);
        int remainder =
                pool.subtract(share.multiply(BigDecimal.valueOf(participanti.size())))
                        .movePointRight(2)
                        .intValueExact();
        return index < remainder ? share.add(new BigDecimal("0.01")) : share;
    }

    public Integer getNrRezultat() {
        return getId();
    }

    public Interes getInteres() {
        return interes;
    }

    public Oferta getOferta() {
        return interes.getOferta();
    }

    public Agent getAgentResponsabil() {
        return agentResponsabil;
    }

    public TipRezultat getTipRezultat() {
        return tipRezultat;
    }

    public StareRezultat getStare() {
        return stare;
    }

    public BigDecimal getPretConvenit() {
        return pretConvenit;
    }

    public BigDecimal getComisionCumparator() {
        return comisionCumparator;
    }

    public BigDecimal getComisionVanzator() {
        return comisionVanzator;
    }

    public BigDecimal getComisionAgentieTotal() {
        return comisionCumparator.add(comisionVanzator);
    }

    public boolean isComisionCumparatorPlatit() {
        return comisionCumparatorPlatit;
    }

    public boolean isComisionVanzatorPlatit() {
        return comisionVanzatorPlatit;
    }

    public LocalDate getDataRezultat() {
        return dataRezultat;
    }

    public LocalDate getDataFinalizare() {
        return dataFinalizare;
    }

    public String getMotivAnulare() {
        return motivAnulare;
    }

    public Contract getContract() {
        return contract;
    }

    public List<PropunerePret> getPropuneri() {
        return List.copyOf(propuneri);
    }

    public List<AgentRezultat> getParticipanti() {
        return List.copyOf(participanti);
    }
}
