package ro.agentie.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import ro.agentie.domain.listing.*;

/** CU1 input when registering the property together with its first listing. */
public record CreareAnuntCommand(
        Integer proprietarId,
        Integer zonaId,
        TipProprietate tipProprietate,
        String adresa,
        BigDecimal suprafata,
        int numarCamere,
        int etaj,
        String facilitati,
        String descriereProprietate,
        TipOferta tipOferta,
        String descriereAnunt,
        BigDecimal pret,
        LocalDate data) {}
