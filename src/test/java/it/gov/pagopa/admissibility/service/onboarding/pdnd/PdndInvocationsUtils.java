package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.*;

import java.util.List;

public class PdndInvocationsUtils {

    public static RispostaE002OKDTO buildAnprAnswer() {

        return new RispostaE002OKDTO().listaSoggetti(buildListaSoggetti());
    }

    private static TipoListaSoggettiDTO buildListaSoggetti() {
        TipoGeneralitaDTO generalita = new TipoGeneralitaDTO();
        generalita.setDataNascita("2001-02-04");
        generalita.setSenzaGiornoMese("2001");

        TipoComuneDTO comune = new TipoComuneDTO();
        comune.setNomeComune("Milano");
        comune.setSiglaProvinciaIstat("MI");

        TipoIndirizzoDTO indirizzo = new TipoIndirizzoDTO();
        indirizzo.setCap("20143");
        indirizzo.setComune(comune);

        TipoResidenzaDTO residenza = new TipoResidenzaDTO();
        residenza.setIndirizzo(indirizzo);

        TipoDatiSoggettiEnteDTO datiSoggetto = new TipoDatiSoggettiEnteDTO();
        datiSoggetto.setGeneralita(generalita);
        datiSoggetto.setResidenza(List.of(residenza));

        TipoListaSoggettiDTO listaSoggetti = new TipoListaSoggettiDTO();
        listaSoggetti.setDatiSoggetto(List.of(datiSoggetto));

        return listaSoggetti;
    }
}
