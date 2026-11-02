package ro.agentie.services;

import ro.agentie.domain.listing.StareOferta;

public record OfertaSummaryView(
        Integer nrOferta, String adresa, StareOferta stare, String agent, int numarImagini) {}
