package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class AuthoritiesDataRetrieverServiceImpl implements AuthoritiesDataRetrieverService{
    private final Long delaySeconds;
    private final boolean nextDay;
    private final OnboardingContextHolderService onboardingContextHolderService;

    private final StreamBridge streamBridge;

    public AuthoritiesDataRetrieverServiceImpl(OnboardingContextHolderService onboardingContextHolderService,
                                               StreamBridge streamBridge,
                                               @Value("${app.onboarding-request.delay-message.delay-duration}") Long delaySeconds,
                                               @Value("${app.onboarding-request.delay-message.next-day}") boolean nextDay) {
        this.onboardingContextHolderService = onboardingContextHolderService;
        this.streamBridge = streamBridge;
        this.delaySeconds = delaySeconds;
        this.nextDay = nextDay;
    }

    @Override
    public Mono<OnboardingDTO> retrieve(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Message<String> message) {
        log.trace("[ONBOARDING_REQUEST] [AUTOMATED_CRITERIA_FIELD_FILL] retrieving automated criteria of user {} into initiative {}", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());

        /* TODO
        * for each initiativeConfig.automatedCriteriaCode,
        *       retrieve the associated authority and field from the Config map (use CriteriaCodeService),
        *       if the OnboardingDTO field's value is null
        *           call the PDND service giving it the token and authority and store the value into the OnboardingDTO relative field
        *           if the call gave threshold error postpone the message and short circuit for the other invocation for the current date
        * if all the calls were successful return a Mono with the request
        */
        if(onboardingRequest.getIsee()==null && is2retrieve(initiativeConfig, OnboardingConstants.CRITERIA_CODE_ISEE)) {
            Map<String, BigDecimal> iseeMockMap = new HashMap<>();
            List<IseeTypologyEnum> iseeList = new ArrayList<>(Arrays.asList(IseeTypologyEnum.values()));

            int randomTipology = new Random(onboardingRequest.getUserId().hashCode()).nextInt(1,5);
            for(int i = 0; i < randomTipology; i++) {
                Random value = new Random((onboardingRequest.getUserId()+iseeList.get(i)).hashCode());
                iseeMockMap.put(iseeList.get(i).name(), new BigDecimal(value.nextInt(1_000, 100_000)));
            }

            for(AutomatedCriteriaDTO automatedCriteriaDTO: initiativeConfig.getAutomatedCriteria()){
                if(automatedCriteriaDTO.getCode().equals(OnboardingConstants.CRITERIA_CODE_ISEE)){
                    List<IseeTypologyEnum> iseeTypologyEnums = automatedCriteriaDTO.getIseeTypes();
                    for(IseeTypologyEnum iseeTypologyEnum: iseeTypologyEnums){
                        if(iseeMockMap.containsKey(iseeTypologyEnum.name())){
                            onboardingRequest.setIsee(iseeMockMap.get(iseeTypologyEnum.name()));
                            break;
                        }
                    }
                }
            }
            if(onboardingRequest.getIsee()==null){
                onboardingRequest.setIsee(new BigDecimal(-1));
            }


            log.info("[ONBOARDING_REQUEST][MOCK_ISEE] User having id {} ISEE: {}", onboardingRequest.getUserId(), iseeMockMap);

           // BigDecimal iseeValue = BigDecimal.valueOf(new Random((onboardingRequest.getUserId()+prioritaryIsee).hashCode()).nextInt(1_000, 100_000));
           // onboardingRequest.setIsee(iseeValue);
        }

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

        if(onboardingRequest.getBirthDate()==null && is2retrieve(initiativeConfig, OnboardingConstants.CRITERIA_CODE_BIRTHDATE)) {
            int age = userIdBasedIntegerGenerator(onboardingRequest).nextInt(18, 99);
            onboardingRequest.setBirthDate(BirthDate.builder()
                    .age(age)
                    .year((LocalDate.now().getYear()-age)+"")
                    .build());
        }
        return Mono.just(onboardingRequest);
    }

    private static Random userIdBasedIntegerGenerator(OnboardingDTO onboardingRequest) {
        @SuppressWarnings("squid:S2245")
        Random random = new Random(onboardingRequest.getUserId().hashCode());
        return random;
    }

    private boolean is2retrieve(InitiativeConfig initiativeConfig, String criteriaCode) {
        return (initiativeConfig.getAutomatedCriteriaCodes()!=null && initiativeConfig.getAutomatedCriteriaCodes().contains(criteriaCode))
                ||
                (initiativeConfig.getRankingFields()!=null && initiativeConfig.getRankingFields().stream().anyMatch(rankingFieldCodes -> criteriaCode.equals(rankingFieldCodes.getFieldCode())));
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
