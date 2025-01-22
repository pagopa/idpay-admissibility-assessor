package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.config.PagoPaAnprPdndConfig;
import it.gov.pagopa.admissibility.connector.repository.AnprInfoRepository;
import it.gov.pagopa.admissibility.connector.rest.anpr.service.AnprC021RestClient;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.model.AnprInfo;
import it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeService;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FamilyDataRetrieverServiceImpl implements FamilyDataRetrieverService {
    private final AnprC021RestClient anprC021RestClient;
    private final PagoPaAnprPdndConfig pdndInitiativeConfig;
    private final UserFiscalCodeService userFiscalCodeService;
    private final AnprInfoRepository anprInfoRepository;

    public FamilyDataRetrieverServiceImpl(AnprC021RestClient anprC021RestClient, PagoPaAnprPdndConfig pdndInitiativeConfig, UserFiscalCodeService userFiscalCodeService, AnprInfoRepository anprInfoRepository) {
        this.anprC021RestClient = anprC021RestClient;
        this.pdndInitiativeConfig = pdndInitiativeConfig;
        this.userFiscalCodeService = userFiscalCodeService;
        this.anprInfoRepository = anprInfoRepository;
    }

    @Override
    public Mono<Optional<Family>> retrieveFamily(OnboardingDTO onboardingRequest, Message<String> message) {
        // TODO call PDND and re-scheduling if dailyLimit occurred

        return userFiscalCodeService.getUserFiscalCode(onboardingRequest.getUserId())
                .flatMap(fiscalCode -> anprC021RestClient.invoke(fiscalCode, pdndInitiativeConfig.getPagopaPdndConfiguration().get("c021")))
                .publishOn(Schedulers.boundedElastic())
                .flatMap(response ->
                        {
                            assert response.getListaSoggetti() != null && response.getListaSoggetti().getDatiSoggetto() != null;
                            assert response.getListaSoggetti().getDatiSoggetto() != null;
                            Set<String> childIds = new HashSet<>();
                            return Flux.fromIterable(response.getListaSoggetti().getDatiSoggetto())
                                .flatMap(datiSoggetto -> {
                                    assert datiSoggetto.getGeneralita() != null;
                                    assert datiSoggetto.getGeneralita().getCodiceFiscale() != null;
                                    return userFiscalCodeService.getUserId(datiSoggetto.getGeneralita().getCodiceFiscale().getCodFiscale())
                                            .map(fiscalCodeHashed -> {
                                                if (datiSoggetto.getLegameSoggetto()!= null && "3".equals(datiSoggetto.getLegameSoggetto().getCodiceLegame())) {
                                                    childIds.add(fiscalCodeHashed);
                                                }
                                                return fiscalCodeHashed;
                                            });
                                })
                                .collect(Collectors.toSet())
                                .flatMap(memberIds -> {
                                    AnprInfo anprInfo = buildAnprInfo(response.getIdOperazioneANPR(), onboardingRequest.getInitiativeId(), onboardingRequest.getUserId());
                                    anprInfo.setChildListIds(childIds);
                                    return anprInfoRepository.save(anprInfo).map(anprInfoSaved -> {
                                        Family family = new Family();
                                        family.setFamilyId(response.getIdOperazioneANPR());
                                        family.setMemberIds(memberIds);
                                        return Optional.of(family);
                                    });
                                });
                        }
                );

    }

    private AnprInfo buildAnprInfo(String familyId, String initiativeId, String userId) {
        return new AnprInfo(familyId, initiativeId, userId, new HashSet<>());
    }
}
