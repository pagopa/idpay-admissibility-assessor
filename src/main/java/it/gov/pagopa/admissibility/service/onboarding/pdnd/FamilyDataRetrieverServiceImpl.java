package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.config.PagoPaAnprPdndConfig;
import it.gov.pagopa.admissibility.connector.repository.AnprInfoRepository;
import it.gov.pagopa.admissibility.connector.rest.anpr.service.AnprC021RestClient;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.family.status.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.family.status.assessment.client.dto.TipoDatiSoggettiEnteDTO;
import it.gov.pagopa.admissibility.model.AnprInfo;
import it.gov.pagopa.admissibility.model.Child;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeService;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FamilyDataRetrieverServiceImpl implements FamilyDataRetrieverService {
    private final AnprC021RestClient anprC021RestClient;
    private final PagoPaAnprPdndConfig pagoPaAnprPdndConfig;
    private final UserFiscalCodeService userFiscalCodeService;
    private final AnprInfoRepository anprInfoRepository;

    public FamilyDataRetrieverServiceImpl(AnprC021RestClient anprC021RestClient,
                                          PagoPaAnprPdndConfig pagoPaAnprPdndConfig,
                                          UserFiscalCodeService userFiscalCodeService,
                                          AnprInfoRepository anprInfoRepository) {
        this.anprC021RestClient = anprC021RestClient;
        this.pagoPaAnprPdndConfig = pagoPaAnprPdndConfig;
        this.userFiscalCodeService = userFiscalCodeService;
        this.anprInfoRepository = anprInfoRepository;
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
                .flatMap(response -> processAnprResponse(response, onboardingRequest,initiativeName,organizationName));
    }

    private Mono<RispostaE002OKDTO> invokeAnprC021(String fiscalCode) {
        PdndInitiativeConfig pdndInitiativeConfig = pagoPaAnprPdndConfig.getPagopaPdndConfiguration().get("c021");
        return anprC021RestClient.invoke(fiscalCode, pdndInitiativeConfig);
    }

    private Mono<Optional<Family>> processAnprResponse(RispostaE002OKDTO response, OnboardingDTO onboardingRequest,  String initiativeName, String organizationName) {
        if (response.getListaSoggetti() == null || response.getListaSoggetti().getDatiSoggetto() == null) {
            throw new IllegalArgumentException("Invalid ANPR response: missing required data.");
        }

        List<Child> childList = new ArrayList<>();
        List<Integer> underAgeNumber = new ArrayList<>();
        underAgeNumber.add(0);
        return Flux.fromIterable(response.getListaSoggetti().getDatiSoggetto())
                .flatMap(datiSoggetto -> processDatiSoggetto(datiSoggetto, childList, underAgeNumber))
                .collect(Collectors.toSet())
                .flatMap(memberIds -> saveAnprInfoAndBuildFamily(response, onboardingRequest, memberIds, childList,initiativeName,organizationName,underAgeNumber));
    }

    private Mono<String> processDatiSoggetto(TipoDatiSoggettiEnteDTO datiSoggetto, List<Child> childList, List<Integer> underAgeNumber) {
        if (datiSoggetto.getGeneralita() == null
                || datiSoggetto.getGeneralita().getCodiceFiscale() == null
                || datiSoggetto.getGeneralita().getCodiceFiscale().getCodFiscale() == null) {
            throw new IllegalArgumentException("Invalid DatiSoggetto: missing general data or fiscal code.");
        }

        String fiscalCode = datiSoggetto.getGeneralita().getCodiceFiscale().getCodFiscale();
        return userFiscalCodeService.getUserId(fiscalCode)
                .doOnNext(fiscalCodeHashed -> {
                    if(isChildUnder18(datiSoggetto)){
                        underAgeNumber.set(0, underAgeNumber.get(0) + 1);
                        if (isChildOnboarded(datiSoggetto)) {
                            String nomeFiglio = datiSoggetto.getGeneralita().getNome();
                            String cognomeFiglio = datiSoggetto.getGeneralita().getCognome();
                            childList.add(new Child(fiscalCodeHashed, nomeFiglio, cognomeFiglio));
                            }
                    }
                });
    }

    private Mono<Optional<Family>> saveAnprInfoAndBuildFamily(RispostaE002OKDTO response, OnboardingDTO onboardingRequest, Set<String> memberIds, List<Child> childList, String initiativeName, String organizationName, List<Integer> underAgeNumber) {
        if (organizationName.equalsIgnoreCase("comune di guidonia montecelio") &&
            initiativeName.toLowerCase().contains("bonus") &&
            childList.isEmpty())
            return Mono.empty();

        AnprInfo anprInfo = buildAnprInfo(response.getIdOperazioneANPR(), onboardingRequest.getInitiativeId(), onboardingRequest.getUserId(), underAgeNumber.get(0));
        anprInfo.setChildList(childList);
        return anprInfoRepository.save(anprInfo)
                .map(savedInfo -> buildFamily(response.getIdOperazioneANPR(), memberIds));
    }

    private Optional<Family> buildFamily(String familyId, Set<String> memberIds) {
        Family family = Family.builder()
                .familyId(familyId)
                .memberIds(memberIds)
                .build();
        return Optional.of(family);
    }

    private boolean isChildOnboarded(TipoDatiSoggettiEnteDTO datiSoggetto) {
        return datiSoggetto.getLegameSoggetto() != null &&
               "3".equals(datiSoggetto.getLegameSoggetto().getCodiceLegame());

    }

    private boolean isChildUnder18(TipoDatiSoggettiEnteDTO datiSoggetto) {
        if (datiSoggetto.getGeneralita() == null || datiSoggetto.getGeneralita().getDataNascita() == null) {
            return false;
        }
        LocalDate birthDate = LocalDate.parse(datiSoggetto.getGeneralita().getDataNascita(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        final LocalDate today = LocalDate.now();
        return birthDate.isAfter(today.minusYears(18));
    }

    private AnprInfo buildAnprInfo(String familyId, String initiativeId, String userId, Integer underAgeNumber) {
        return new AnprInfo(familyId, initiativeId, userId, new ArrayList<>(), underAgeNumber);
    }
}
