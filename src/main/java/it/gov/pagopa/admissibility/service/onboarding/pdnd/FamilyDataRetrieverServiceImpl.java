package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.config.PagoPaAnprPdndConfig;
import it.gov.pagopa.admissibility.connector.rest.anpr.service.AnprC021RestClient;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.family.status.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.family.status.assessment.client.dto.TipoDatiSoggettiEnteDTO;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeService;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FamilyDataRetrieverServiceImpl implements FamilyDataRetrieverService {
    private final AnprC021RestClient anprC021RestClient;
    private final PagoPaAnprPdndConfig pagoPaAnprPdndConfig;
    private final UserFiscalCodeService userFiscalCodeService;

    public FamilyDataRetrieverServiceImpl(AnprC021RestClient anprC021RestClient,
                                          PagoPaAnprPdndConfig pagoPaAnprPdndConfig,
                                          UserFiscalCodeService userFiscalCodeService) {
        this.anprC021RestClient = anprC021RestClient;
        this.pagoPaAnprPdndConfig = pagoPaAnprPdndConfig;
        this.userFiscalCodeService = userFiscalCodeService;
    }

    @Override
    public Mono<Optional<Family>> retrieveFamily(OnboardingDTO onboardingRequest,
                                                 Message<String> message,
                                                 String initiativeName,
                                                 String organizationName
                                                 ) {
        // TODO: Handle PDND call and implement re-scheduling if dailyLimit is reached

        return userFiscalCodeService.getUserFiscalCode(onboardingRequest.getUserId())
                .flatMap(this::invokeAnprC021)
                .publishOn(Schedulers.boundedElastic())
                .flatMap(this::processAnprResponse);
    }

    private Mono<RispostaE002OKDTO> invokeAnprC021(String fiscalCode) {
        PdndInitiativeConfig pdndInitiativeConfig = pagoPaAnprPdndConfig.getPagopaPdndConfiguration().get("c021");
        return anprC021RestClient.invoke(fiscalCode, pdndInitiativeConfig);
    }

    private Mono<Optional<Family>> processAnprResponse(RispostaE002OKDTO response) {
        if (response.getListaSoggetti() == null || response.getListaSoggetti().getDatiSoggetto() == null) {
            throw new IllegalArgumentException("Invalid ANPR response: missing required data.");
        }

        return Flux.fromIterable(response.getListaSoggetti().getDatiSoggetto())
                .flatMap(this::processDatiSoggetto)
                .collect(Collectors.toSet())
                .flatMap(memberIds -> buildFamily(response.getIdOperazioneANPR(), memberIds));
    }

    private Mono<String> processDatiSoggetto(TipoDatiSoggettiEnteDTO datiSoggetto) {
        if (datiSoggetto.getGeneralita() == null
                || datiSoggetto.getGeneralita().getCodiceFiscale() == null
                || datiSoggetto.getGeneralita().getCodiceFiscale().getCodFiscale() == null) {
            throw new IllegalArgumentException("Invalid DatiSoggetto: missing general data or fiscal code.");
        }

        String fiscalCode = datiSoggetto.getGeneralita().getCodiceFiscale().getCodFiscale();
        return userFiscalCodeService.getUserId(fiscalCode);
    }

    private Mono<Optional<Family>> buildFamily(String familyId, Set<String> memberIds) {
        Family family = Family.builder()
                .familyId(familyId)
                .memberIds(memberIds)
                .build();
        return Mono.just(Optional.of(family));
    }
}
