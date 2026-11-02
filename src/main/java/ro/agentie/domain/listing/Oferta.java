package ro.agentie.domain.listing;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import ro.agentie.domain.shared.*;

public class Oferta extends Entitate {
    private final Proprietate proprietate;
    private final TipOferta tipOferta;
    private Agent agent;
    @NotBlank private String descriereScurta;
    @Positive private BigDecimal pretListare;
    private final LocalDate dataCreare;
    private StareOferta stare = StareOferta.NOUA;
    private Integer tranzactieRezervare;
    private int nextImage;
    private final List<ImgOferta> imagini = new ArrayList<>();

    public Oferta(
            Integer id,
            Proprietate proprietate,
            TipOferta tip,
            String descriere,
            BigDecimal pret,
            LocalDate data) {
        super(id);
        this.proprietate = Objects.requireNonNull(proprietate);
        tipOferta = Objects.requireNonNull(tip);
        descriereScurta = Reguli.text(descriere, "Descriere");
        pretListare = Reguli.pozitiv(pret, "Pretul");
        dataCreare = Objects.requireNonNull(data);
    }

    public void alocaAgent(Agent nou) {
        Reguli.cere(
                Set.of(StareOferta.NOUA, StareOferta.ALOCATA, StareOferta.ACTIVA).contains(stare),
                "Oferta nu poate fi alocata in starea " + stare);
        Objects.requireNonNull(nou);
        Reguli.cere(nou.acoperaZona(proprietate.getZona()), "Agentul nu acopera zona proprietatii!");
        Reguli.cere(!nou.esteProprietarul(this), "Agentul nu poate gestiona propria proprietate!");
        Agent vechi = agent;
        agent = nou;
        if (vechi != null && vechi != nou) vechi.eliminaDinPortofoliu(this);
        nou.inregistreazaInPortofoliu(this);
        if (stare == StareOferta.NOUA) stare = StareOferta.ALOCATA;
    }

    public void activeaza() {
        Reguli.cere(stare == StareOferta.ALOCATA, "Doar o oferta alocata poate fi activata!");
        stare = StareOferta.ACTIVA;
    }

    public void rezerva(Integer tranzactieId) {
        Reguli.cere(esteDisponibila(), "Oferta nu mai este activa!");
        Reguli.cere(tranzactieId != null && tranzactieId > 0, "Tranzactie invalida!");
        tranzactieRezervare = tranzactieId;
        stare = StareOferta.REZERVATA;
    }

    public void verificaRezervare(Integer id) {
        Reguli.cere(
                stare == StareOferta.REZERVATA && Objects.equals(tranzactieRezervare, id),
                "Rezervarea nu apartine tranzactiei!");
    }

    public void elibereaza(Integer id) {
        verificaRezervare(id);
        stare = StareOferta.ACTIVA;
        tranzactieRezervare = null;
    }

    public void finalizeaza(Integer id) {
        verificaRezervare(id);
        stare = tipOferta == TipOferta.VANZARE ? StareOferta.VANDUTA : StareOferta.INCHIRIATA;
        tranzactieRezervare = null;
    }

    public void arhiveaza() {
        Reguli.cere(
                stare != StareOferta.REZERVATA && stare != StareOferta.ARHIVATA,
                "Oferta nu poate fi arhivata!");
        stare = StareOferta.ARHIVATA;
    }

    private void verificaEditabila() {
        Reguli.cere(
                Set.of(StareOferta.NOUA, StareOferta.ALOCATA, StareOferta.ACTIVA).contains(stare),
                "Oferta nu mai poate fi modificata!");
    }

    public void modificaPret(BigDecimal pret) {
        verificaEditabila();
        pretListare = Reguli.pozitiv(pret, "Pretul");
    }

    public void modificaDescriere(String text) {
        verificaEditabila();
        descriereScurta = Reguli.text(text, "Descriere");
    }

    public ImgOferta adaugaImagine(String url, String descriere, LocalDate data) {
        verificaEditabila();
        ImgOferta img = new ImgOferta(++nextImage, url, descriere, data);
        img.seteazaOrdine(imagini.size());
        if (imagini.isEmpty()) img.seteazaPrincipala(true);
        imagini.add(img);
        return img;
    }

    public void stergeImagine(Integer id) {
        verificaEditabila();
        ImgOferta img = imagine(id);
        imagini.remove(img);
        if (img.isEstePrincipala() && !imagini.isEmpty()) imagini.get(0).seteazaPrincipala(true);
        for (int i = 0; i < imagini.size(); i++) imagini.get(i).seteazaOrdine(i);
    }

    public void seteazaImaginePrincipala(Integer id) {
        verificaEditabila();
        ImgOferta selected = imagine(id);
        imagini.forEach(i -> i.seteazaPrincipala(i == selected));
    }

    public void seteazaOrdineImagine(Integer id, int ordine) {
        verificaEditabila();
        Reguli.cere(ordine >= 0 && ordine < imagini.size(), "Ordine invalida!");
        ImgOferta img = imagine(id);
        imagini.remove(img);
        imagini.add(ordine, img);
        for (int i = 0; i < imagini.size(); i++) imagini.get(i).seteazaOrdine(i);
    }

    private ImgOferta imagine(Integer id) {
        return imagini.stream()
                .filter(i -> i.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RegulaBusinessException("Imagine inexistenta!"));
    }

    public ImgOferta getImaginePrincipala() {
        return imagini.stream().filter(ImgOferta::isEstePrincipala).findFirst().orElse(null);
    }

    public boolean esteDisponibila() {
        return stare == StareOferta.ACTIVA;
    }

    public Integer getNrOferta() {
        return getId();
    }

    public Proprietate getProprietate() {
        return proprietate;
    }

    public Agent getAgent() {
        return agent;
    }

    public TipOferta getTipOferta() {
        return tipOferta;
    }

    public String getDescriereScurta() {
        return descriereScurta;
    }

    public BigDecimal getPretListare() {
        return pretListare;
    }

    public LocalDate getDataCreare() {
        return dataCreare;
    }

    public StareOferta getStare() {
        return stare;
    }

    public List<ImgOferta> getImagini() {
        return List.copyOf(imagini);
    }
}
