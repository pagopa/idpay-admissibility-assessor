package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.exception.OnboardingException;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class AuthoritiesDataRetrieverServiceImpl implements AuthoritiesDataRetrieverService {
    private final Long delaySeconds;
    private final boolean nextDay;
    private final OnboardingContextHolderService onboardingContextHolderService;
    private final CriteriaCodeService criteriaCodeService;
    private final ReactiveMongoTemplate mongoTemplate;

    private final StreamBridge streamBridge;

    public AuthoritiesDataRetrieverServiceImpl(OnboardingContextHolderService onboardingContextHolderService,
                                               StreamBridge streamBridge,
                                               @Value("${app.onboarding-request.delay-message.delay-duration}") Long delaySeconds,
                                               @Value("${app.onboarding-request.delay-message.next-day}") boolean nextDay,
                                               CriteriaCodeService criteriaCodeService,
                                               ReactiveMongoTemplate mongoTemplate) {
        this.onboardingContextHolderService = onboardingContextHolderService;
        this.streamBridge = streamBridge;
        this.delaySeconds = delaySeconds;
        this.nextDay = nextDay;
        this.criteriaCodeService = criteriaCodeService;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<OnboardingDTO> retrieve(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Message<String> message) {
        log.trace("[ONBOARDING_REQUEST] [AUTOMATED_CRITERIA_FIELD_FILL] retrieving automated criteria of user {} into initiative {}", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());


        return Mono.just(onboardingRequest)
                // ISEE
                .flatMap(o -> {
                    if (o.getIsee() == null && is2retrieve(initiativeConfig, OnboardingConstants.CRITERIA_CODE_ISEE)) {
                        return retrieveIsee(o, initiativeConfig);
                    }

                    return Mono.just(o);
                })
                // RESIDENCE
                .doOnNext(o -> {
                    if (onboardingRequest.getResidence() == null && is2retrieve(initiativeConfig, OnboardingConstants.CRITERIA_CODE_RESIDENCE)) {
                        onboardingRequest.setResidence(
                                userIdBasedIntegerGenerator(onboardingRequest).nextInt(0, 2) == 0
                                        ? Residence.builder()
                                        .city("Milano")
                                        .cityCouncil("Milano")
                                        .province("Milano")
                                        .region("Lombardia")
                                        .postalCode("20124")
                                        .nation("Italia")
                                        .build()
                                        : Residence.builder()
                                        .city("Roma")
                                        .cityCouncil("Roma")
                                        .province("Roma")
                                        .region("Lazio")
                                        .postalCode("00187")
                                        .nation("Italia")
                                        .build()

                        );
                    }
                })
                // BIRTHDATE
                .doOnNext(o -> {
                    if (onboardingRequest.getBirthDate() == null && is2retrieve(initiativeConfig, OnboardingConstants.CRITERIA_CODE_BIRTHDATE)) {
                        int age = userIdBasedIntegerGenerator(onboardingRequest).nextInt(18, 99);
                        onboardingRequest.setBirthDate(BirthDate.builder()
                                .age(age)
                                .year((LocalDate.now().getYear() - age) + "")
                                .build());
                    }
                });
    }

    private Mono<OnboardingDTO> retrieveIsee(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig) {
        return this.retrieveUserIsee(onboardingRequest.getUserId())
                .switchIfEmpty(mockIsee(onboardingRequest))
                .doOnNext(m -> setIseeIfCorrespondingType(onboardingRequest, initiativeConfig, m))
                .map(m -> {
                    CriteriaCodeConfig criteriaCodeConfig = criteriaCodeService.getCriteriaCodeConfig(OnboardingConstants.CRITERIA_CODE_ISEE);

                    if (onboardingRequest.getIsee() == null) {
                        throw new OnboardingException(
                                List.of(new OnboardingRejectionReason(
                                        OnboardingRejectionReason.OnboardingRejectionReasonType.ISEE_TYPE_KO,
                                        OnboardingConstants.REJECTION_REASON_ISEE_TYPE_KO,
                                        criteriaCodeConfig.getAuthority(),
                                        criteriaCodeConfig.getAuthorityLabel(),
                                        "ISEE non disponibile"
                                )),
                                "User having id %s has not compatible type for initiative %s".formatted(onboardingRequest.getUserId(), initiativeConfig.getInitiativeId()));
                    }

                    return onboardingRequest;
                });
    }

    private Mono<Map<String, BigDecimal>> mockIsee(OnboardingDTO onboardingRequest) {
        Map<String, BigDecimal> iseeMockMap = new HashMap<>();
        List<IseeTypologyEnum> iseeList = Arrays.asList(IseeTypologyEnum.values());

        int randomTypology = new Random(onboardingRequest.getUserId().hashCode()).nextInt(0, iseeList.size());
        for (int i = 0; i < randomTypology; i++) {
            Random value = new Random((onboardingRequest.getUserId() + iseeList.get(i)).hashCode());
            iseeMockMap.put(iseeList.get(i).name(), new BigDecimal(value.nextInt(1_000, 100_000)));
        }

        log.info("[ONBOARDING_REQUEST][MOCK_ISEE] User having id {} ISEE: {}", onboardingRequest.getUserId(), iseeMockMap);

        return Mono.just(iseeMockMap);
    }

    private Mono<Map<String,BigDecimal>> retrieveUserIsee(String userId) {
            return mongoTemplate.findById(
                    userId,
                    Isee.class,
                    "mocked_isee"
            ).map(Isee::getIseeTypeMap);
    }

    private void setIseeIfCorrespondingType(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Map<String, BigDecimal> iseeMap) {
        for (AutomatedCriteriaDTO automatedCriteriaDTO : initiativeConfig.getAutomatedCriteria()) {
            if (automatedCriteriaDTO.getCode().equals(OnboardingConstants.CRITERIA_CODE_ISEE)) {
                for (IseeTypologyEnum iseeTypologyEnum : automatedCriteriaDTO.getIseeTypes()) {
                    if (iseeMap.containsKey(iseeTypologyEnum.name())) {
                        onboardingRequest.setIsee(iseeMap.get(iseeTypologyEnum.name()));
                        break;
                    }
                }
            }
        }
    }

    private static Random userIdBasedIntegerGenerator(OnboardingDTO onboardingRequest) {
        @SuppressWarnings("squid:S2245")
        Random random = new Random(onboardingRequest.getUserId().hashCode());
        return random;
    }

    private boolean is2retrieve(InitiativeConfig initiativeConfig, String criteriaCode) {
        return (initiativeConfig.getAutomatedCriteriaCodes() != null && initiativeConfig.getAutomatedCriteriaCodes().contains(criteriaCode))
                ||
                (initiativeConfig.getRankingFields() != null && initiativeConfig.getRankingFields().stream().anyMatch(rankingFieldCodes -> criteriaCode.equals(rankingFieldCodes.getFieldCode())));
    }

    /* TODO send message with schedule delay for servicebus
    private void rischeduleOnboardingRequest(OnboardingDTO onboardingRequest, Message<String> message) {
        log.info("[ONBOARDING_REQUEST] [RETRIEVE_ERROR] PDND calls threshold reached");
        MessageBuilder<OnboardingDTO> delayedMessage = MessageBuilder.withPayload(onboardingRequest)
                .setHeaders(new MessageHeaderAccessor(message))
                .setHeader(ServiceBusMessageHeaders.SCHEDULED_ENQUEUE_TIME, calcDelay());
        streamBridge.send("admissibilityDelayProducer-out-0", delayedMessage.build());
    }

    private OffsetDateTime calcDelay() {
        LocalDate today = LocalDate.now();
        if(this.nextDay) {
            LocalTime midnight = LocalTime.MIDNIGHT;
            return LocalDateTime.of(today, midnight).plusDays(1).atZone(ZoneId.of("Europe/Rome")).toOffsetDateTime();
        } else {
            return LocalDateTime.now().plusSeconds(this.delaySeconds).atZone(ZoneId.of("Europe/Rome")).toOffsetDateTime();
        }
    }
    */
}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldNameConstants
class Isee {

    @Id
    private String userId;
    private Map<String,BigDecimal> iseeTypeMap;
}
