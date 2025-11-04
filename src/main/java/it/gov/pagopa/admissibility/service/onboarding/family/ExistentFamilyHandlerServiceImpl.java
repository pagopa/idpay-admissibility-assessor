package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationCompletedDTO;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.enums.OnboardingFamilyEvaluationStatus;
import it.gov.pagopa.admissibility.exception.WaitingFamilyOnBoardingException;
import it.gov.pagopa.admissibility.exception.SkipAlreadyRankingFamilyOnBoardingException;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import it.gov.pagopa.admissibility.service.onboarding.notifier.OnboardingRescheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;

@Slf4j
@Service
public class ExistentFamilyHandlerServiceImpl implements ExistentFamilyHandlerService {

    private final Onboarding2EvaluationMapper mapper;
    private final OnboardingRescheduleService onboardingRescheduleService;
    private final Duration familyOnboardingInProgressDelayDuration;

    public ExistentFamilyHandlerServiceImpl(
            @Value("${app.onboarding-request.delay-family-in-progress.delay-minutes}") int familyOnboardingInProgressDelayMinutes,

            Onboarding2EvaluationMapper mapper,
            OnboardingRescheduleService onboardingRescheduleService) {
        this.mapper = mapper;
        this.onboardingRescheduleService = onboardingRescheduleService;

        this.familyOnboardingInProgressDelayDuration=Duration.ofMinutes(familyOnboardingInProgressDelayMinutes);
    }

    @Override
    public Mono<EvaluationDTO> handleExistentFamily(OnboardingDTO onboardingRequest, OnboardingFamilies family, InitiativeConfig initiativeConfig, Message<String> message) {
        log.info("[ONBOARDING_REQUEST] User family has been already onboarded: userId {}; familyId {}; initiativeId {}; onboardingFamilyStatus {}", onboardingRequest.getUserId(), family.getFamilyId(), onboardingRequest.getInitiativeId(), family.getStatus());
        onboardingRequest.setFamily(new Family(family.getFamilyId(), family.getMemberIds()));

        if(OnboardingFamilyEvaluationStatus.IN_PROGRESS.equals(family.getStatus())){
            if(onboardingRequest.getUserId().equals(family.getCreatedBy())){
                return Mono.empty();
            }
            onboardingRescheduleService.reschedule(
                    onboardingRequest,
                    OffsetDateTime.now().plus(familyOnboardingInProgressDelayDuration),
                    "Family %s onboarding IN_PROGRESS into initiative %s".formatted(family.getFamilyId(), family.getInitiativeId()),
                    message);
            return Mono.error(WaitingFamilyOnBoardingException::new);
        } else {
            return mapFamilyOnboardingResult(onboardingRequest, family, initiativeConfig);
        }
    }

    @Override
    public Mono<EvaluationDTO> mapFamilyOnboardingResult(OnboardingDTO onboardingRequest, OnboardingFamilies family, InitiativeConfig initiativeConfig) {
        if(initiativeConfig.isRankingInitiative()){
            return Mono.error(SkipAlreadyRankingFamilyOnBoardingException::new);
        }
        EvaluationDTO evaluation = mapper.apply(onboardingRequest, initiativeConfig, family.getOnboardingRejectionReasons());
        if(evaluation instanceof EvaluationCompletedDTO evaluationCompletedDTO){
            if(OnboardingFamilyEvaluationStatus.ONBOARDING_OK.equals(family.getStatus())){
                evaluationCompletedDTO.setStatus(OnboardingEvaluationStatus.JOINED);
            } else {
                evaluationCompletedDTO.setStatus(OnboardingEvaluationStatus.REJECTED);
            }
        }
        return Mono.just(evaluation);
    }

    @Override
    public Mono<EvaluationDTO> mapFamilyMemberAlreadyOnboardingResult(OnboardingDTO onboardingRequest, String familyId, InitiativeConfig initiativeConfig) {
        Family family = new Family(familyId, Collections.emptySet());
        onboardingRequest.setFamily(family);
        EvaluationDTO evaluation = mapper.apply(onboardingRequest, initiativeConfig, Collections.emptyList());
        if(evaluation instanceof EvaluationCompletedDTO evaluationCompletedDTO){
            evaluationCompletedDTO.setStatus(OnboardingEvaluationStatus.JOINED);
        }
        return Mono.just(evaluation);
    }
}
