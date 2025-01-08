package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.connector.rest.anpr.service.AnprC001RestClient;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeService;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FamilyDataRetrieverServiceImpl implements FamilyDataRetrieverService {
    private final AnprC001RestClient anprC001RestClient;
    private final PdndInitiativeConfig pdndInitiativeConfig;

    private final UserFiscalCodeService userFiscalCodeService;

    public FamilyDataRetrieverServiceImpl(AnprC001RestClient anprC001RestClient, PdndInitiativeConfig pdndInitiativeConfig, UserFiscalCodeService userFiscalCodeService) {
        this.anprC001RestClient = anprC001RestClient;
        this.pdndInitiativeConfig = pdndInitiativeConfig;
        this.userFiscalCodeService = userFiscalCodeService;
    }

    @Override
    public Mono<Optional<Family>> retrieveFamily(OnboardingDTO onboardingRequest, Message<String> message) {
        // TODO call PDND and re-scheduling if dailyLimit occurred


        return userFiscalCodeService.getUserFiscalCode(onboardingRequest.getUserId())
                .flatMap(fiscalCode -> anprC001RestClient.invoke(fiscalCode, pdndInitiativeConfig))
                .map(response -> {
                    Family family = new Family();
                    family.setFamilyId(response.getIdOperazioneANPR());
                    Set<String> memberIds = response.getListaSoggetti().getDatiSoggetto()
                            .stream()
                            .map(datiSoggetto -> datiSoggetto.getIdentificativi().getIdANPR())
                            .collect(Collectors.toSet());

                    family.setMemberIds(memberIds);

                    return Optional.of(family);
                });

    }
}
