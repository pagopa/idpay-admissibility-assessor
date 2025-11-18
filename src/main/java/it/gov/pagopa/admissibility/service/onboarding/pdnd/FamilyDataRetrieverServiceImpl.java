package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.config.PagoPaAnprPdndConfig;
import it.gov.pagopa.admissibility.connector.rest.anpr.service.AnprC021RestClient;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.family.status.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.family.status.assessment.client.dto.TipoDatiSoggettiEnteDTO;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.utils.AuditUtilities;
import it.gov.pagopa.common.reactive.pdnd.exception.PdndRetrieveFamilyServiceTooManyRequestException;
import it.gov.pagopa.common.reactive.pdnd.exception.PdndServiceTooManyRequestException;
import it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FamilyDataRetrieverServiceImpl implements FamilyDataRetrieverService {
    private final AnprC021RestClient anprC021RestClient;
    private final PagoPaAnprPdndConfig pagoPaAnprPdndConfig;
    private final UserFiscalCodeService userFiscalCodeService;
    private final AuditUtilities auditUtilities;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public FamilyDataRetrieverServiceImpl(AnprC021RestClient anprC021RestClient,
                                          PagoPaAnprPdndConfig pagoPaAnprPdndConfig,
                                          UserFiscalCodeService userFiscalCodeService, AuditUtilities auditUtilities) {
        this.anprC021RestClient = anprC021RestClient;
        this.pagoPaAnprPdndConfig = pagoPaAnprPdndConfig;
        this.userFiscalCodeService = userFiscalCodeService;
        this.auditUtilities = auditUtilities;
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
                .doOnNext(rispostaE002OKDTO -> auditUtilities.logAnprFamilies(onboardingRequest.getUserId(), onboardingRequest.getInitiativeId(), rispostaE002OKDTO.getIdOperazioneANPR(), LocalDateTime.now().toString()))
                .flatMap(this::processAnprResponse)
                .onErrorResume(PdndServiceTooManyRequestException.class, e ->
                    Mono.error(new PdndRetrieveFamilyServiceTooManyRequestException("[PDND][TOO_MANY_REQUEST] Pdnd retrieve family service has bee invoked too times", e)
                ));
    }

    private Mono<RispostaE002OKDTO> invokeAnprC021(String fiscalCode) {
        PdndInitiativeConfig pdndInitiativeConfig = pagoPaAnprPdndConfig.getPagopaPdndConfiguration().get("c021");
        return anprC021RestClient.invoke(fiscalCode, pdndInitiativeConfig);
    }

    private Mono<Optional<Family>> processAnprResponse(RispostaE002OKDTO response) {
        if (response.getListaSoggetti() == null || response.getListaSoggetti().getDatiSoggetto() == null) {
            throw new IllegalArgumentException("Invalid ANPR response: missing required data.");
        }

        log.info("[FAMILY_DATA_RETRIEVER] Processed family with {} members.", response.getListaSoggetti().getDatiSoggetto().size());
        return Flux.fromIterable(response.getListaSoggetti().getDatiSoggetto())
                .filter(datiSoggetto -> datiSoggetto.getGeneralita() != null
                        && datiSoggetto.getGeneralita().getCodiceFiscale() != null
                        &&datiSoggetto.getGeneralita().getCodiceFiscale().getCodFiscale() != null
                        && !isUnder18(datiSoggetto))
                .map(tipoDatiSoggettiEnteDTO -> tipoDatiSoggettiEnteDTO.getGeneralita().getCodiceFiscale().getCodFiscale())
                .collectSortedList(Comparator.comparing(fc -> fc))
                .flatMap(list -> {
                    if(list.isEmpty()){
                        return Mono.error(new IllegalStateException("The families contain only members under 18 or without a fiscal code."));
                    }
                    else {
                        return processFamilyData(list); }
                });
    }

    private Mono<Optional<Family>> processFamilyData(List<String> userMembers) {
        try {
            String familyId = calculateFamilyHash(userMembers);
            return  Flux.fromIterable(userMembers)
                    .flatMap(userFiscalCodeService::getUserId)
                    .collect(Collectors.toSet())
                    .flatMap(membersId -> buildFamily(familyId, membersId));
        } catch (NoSuchAlgorithmException e) {
            log.error("[FAMILY_DATA_RETRIEVER] Error while calculating the hashed familyId");
            return Mono.error(e);
        }
    }

    private static String calculateFamilyHash(List<String> userMembers) throws NoSuchAlgorithmException {
        String concatenazioneCodiciFiscali = userMembers.stream()
                .map(FamilyDataRetrieverServiceImpl::normalizeCodiceFiscale)
                .sorted()
                .collect(Collectors.joining());

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(concatenazioneCodiciFiscali.getBytes());

        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    private static String normalizeCodiceFiscale(String codiceFiscale) {
        if (codiceFiscale == null) return "";
        return codiceFiscale.trim().toUpperCase();
    }

    private Mono<Optional<Family>> buildFamily(String familyId, Set<String> memberIds) {
        Family family = Family.builder()
                .familyId(familyId)
                .memberIds(memberIds)
                .build();
        return Mono.just(Optional.of(family));
    }
    private boolean isUnder18(TipoDatiSoggettiEnteDTO datiSoggetto) {
        if (datiSoggetto.getGeneralita() == null || datiSoggetto.getGeneralita().getDataNascita() == null) {
            return false;
        }
        LocalDate birthDate = LocalDate.parse(datiSoggetto.getGeneralita().getDataNascita(), formatter);
        final LocalDate today = LocalDate.now();
        return birthDate.isAfter(today.minusYears(18));
    }
}
