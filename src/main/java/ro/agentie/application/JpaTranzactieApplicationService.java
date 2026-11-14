package ro.agentie.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.agentie.domain.shared.RegulaBusinessException;
import ro.agentie.domain.transaction.*;
import ro.agentie.infrastructure.*;

// Reuse initiation/cancellation behavior and explicitly reject workflows not yet mapped to SQL.
@Service
// The original complete workflow service remains registered only in the memory profile.
@Profile("!memory")
// Inherited supported operations share one database transaction through this Spring bean.
@Transactional
public class JpaTranzactieApplicationService extends TranzactieApplicationService {
    public JpaTranzactieApplicationService(
            IRezultatEntityRepository r,
            IInteresEntityRepository i,
            IAgentEntityRepository a,
            IRealEstateEntityFactory f,
            ApplicationEventPublisher events) {
        super(r, i, a, f, events);
    }

    @Override
    // Keep all advanced API entry points explicitly disabled until their data is persistable.
    public Integer adaugaPropunere(
            Integer id, BigDecimal suma, ParteNegociere parte, LocalDate data, String observatii) {
        throw new RegulaBusinessException(
                "Operatie amanata: persistenta negocierii, contractelor si platilor nu este disponibila.");
    }

    @Override
    public void acceptaPropunerea(Integer id) {
        throw new RegulaBusinessException(
                "Operatie amanata: persistenta negocierii, contractelor si platilor nu este disponibila.");
    }

    @Override
    public void respingePropunerea(Integer id) {
        throw new RegulaBusinessException(
                "Operatie amanata: persistenta negocierii, contractelor si platilor nu este disponibila.");
    }

    @Override
    public void adaugaAgent(Integer id, Integer agentId) {
        throw new RegulaBusinessException(
                "Operatie amanata: persistenta negocierii, contractelor si platilor nu este disponibila.");
    }

    @Override
    public Contract pregatesteContract(Integer id) {
        throw new RegulaBusinessException(
                "Operatie amanata: persistenta negocierii, contractelor si platilor nu este disponibila.");
    }

    @Override
    public void modificaConditiiContract(
            Integer id,
            MetodaPlata metoda,
            BigDecimal avans,
            String grafic,
            LocalDate inceput,
            LocalDate sfarsit,
            String clauze) {
        throw new RegulaBusinessException(
                "Operatie amanata: persistenta negocierii, contractelor si platilor nu este disponibila.");
    }

    @Override
    public void trimiteContractLaNegociere(Integer id) {
        throw new RegulaBusinessException(
                "Operatie amanata: persistenta negocierii, contractelor si platilor nu este disponibila.");
    }

    @Override
    public void convineContract(Integer id) {
        throw new RegulaBusinessException(
                "Operatie amanata: persistenta negocierii, contractelor si platilor nu este disponibila.");
    }

    @Override
    public void semneazaContract(Integer id, LocalDate data) {
        throw new RegulaBusinessException(
                "Operatie amanata: persistenta negocierii, contractelor si platilor nu este disponibila.");
    }

    @Override
    public void stabilesteComisioane(Integer id, BigDecimal cumparator, BigDecimal vanzator) {
        throw new RegulaBusinessException(
                "Operatie amanata: persistenta negocierii, contractelor si platilor nu este disponibila.");
    }

    @Override
    public void inregistreazaPlataAvans(Integer id) {
        throw new RegulaBusinessException(
                "Operatie amanata: persistenta negocierii, contractelor si platilor nu este disponibila.");
    }

    @Override
    public void inregistreazaPlataComisionCumparator(Integer id) {
        throw new RegulaBusinessException(
                "Operatie amanata: persistenta negocierii, contractelor si platilor nu este disponibila.");
    }

    @Override
    public void inregistreazaPlataComisionVanzator(Integer id) {
        throw new RegulaBusinessException(
                "Operatie amanata: persistenta negocierii, contractelor si platilor nu este disponibila.");
    }

    @Override
    public void finalizeazaTranzactie(Integer id, LocalDate data) {
        throw new RegulaBusinessException(
                "Operatie amanata: persistenta negocierii, contractelor si platilor nu este disponibila.");
    }
}