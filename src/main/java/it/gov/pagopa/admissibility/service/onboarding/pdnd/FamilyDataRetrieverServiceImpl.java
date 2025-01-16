package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.connector.rest.anpr.service.AnprC021RestClient;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeService;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FamilyDataRetrieverServiceImpl implements FamilyDataRetrieverService {
    private final AnprC021RestClient anprC021RestClient;
    private final PdndInitiativeConfig pdndInitiativeConfig;

    private final UserFiscalCodeService userFiscalCodeService;

    public FamilyDataRetrieverServiceImpl(AnprC021RestClient anprC021RestClient, PdndInitiativeConfig pdndInitiativeConfig, UserFiscalCodeService userFiscalCodeService) {
        this.anprC021RestClient = anprC021RestClient;
        this.pdndInitiativeConfig = pdndInitiativeConfig;
        this.userFiscalCodeService = userFiscalCodeService;
    }

    @Override
    public Mono<Optional<Family>> retrieveFamily(OnboardingDTO onboardingRequest, Message<String> message) {
        // TODO call PDND and re-scheduling if dailyLimit occurred

        return userFiscalCodeService.getUserFiscalCode(onboardingRequest.getUserId())
                .flatMap(fiscalCode -> anprC021RestClient.invoke(fiscalCode, pdndInitiativeConfig))
                .publishOn(Schedulers.boundedElastic())
                .flatMap(response ->
                    Flux.fromIterable(response.getListaSoggetti().getDatiSoggetto())
                        .flatMap(datiSoggetto -> userFiscalCodeService.getUserId(datiSoggetto.getGeneralita().getCodiceFiscale().getCodFiscale()))
                        .collect(Collectors.toSet())
                        .map(memberIds -> {
                            Family family = new Family();
                            family.setFamilyId(response.getIdOperazioneANPR());
                            family.setMemberIds(memberIds);
                            return Optional.of(family);
                        })
                );

    }
}
