package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.connector.rest.onboarding.OnboardingRestClient;
import it.gov.pagopa.admissibility.dto.onboarding.*;
import it.gov.pagopa.admissibility.dto.rule.InitiativeGeneralDTO;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FamilyPastOnboardingCheckServiceImpl implements FamilyPastOnboardingCheckService {

    private final OnboardingRestClient onboardingRestClient;
    private final Onboarding2EvaluationMapper onboarding2EvaluationMapper;

    public FamilyPastOnboardingCheckServiceImpl(OnboardingRestClient onboardingRestClient,
                                                Onboarding2EvaluationMapper onboarding2EvaluationMapper) {
        this.onboardingRestClient = onboardingRestClient;
        this.onboarding2EvaluationMapper = onboarding2EvaluationMapper;
    }

    @Override
    public Mono<EvaluationDTO> checkFamilyPastOnboardings(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig) {
        if (!isCheckEnabled(initiativeConfig)) {
            return Mono.empty();
        }

        // Family memberIds already contain only adult members (minors are filtered out when retrieving data from ANPR)
        Set<String> onboardingIds = retrieveOtherMembersOnboardingIds(onboardingRequest);
        if (onboardingIds.isEmpty()) {
            log.info("[ONBOARDING_REQUEST] No other adult family members found for userId {}; initiativeId {}", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());
            return Mono.empty();
        }
        CheckPastOnboardingsSeriesReqDTO request = new CheckPastOnboardingsSeriesReqDTO(onboardingIds.stream().toList());

        // call api and handle response
        return onboardingRestClient.checkPastOnboardingsSeries(onboardingRequest.getInitiativeId(), request)
                .flatMap(response -> {
                    validateResponse(onboardingIds, response);
                    if (isBonusAlreadyUsed(response)) {
                        log.info("[ONBOARDING_REQUEST] [ONBOARDING_KO] [FAMILY_BONUS_ALREADY_USED] A family member of user {} already used a past edition of initiative {}",
                                onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());
                        return Mono.just(onboarding2EvaluationMapper.apply(onboardingRequest, initiativeConfig, List.of(buildRejectionReason())));
                    }
                    return Mono.empty();
                });
    }

    private static boolean isCheckEnabled(InitiativeConfig initiativeConfig) {
        return initiativeConfig != null
                && InitiativeGeneralDTO.BeneficiaryTypeEnum.NF.equals(initiativeConfig.getBeneficiaryType())
                && Boolean.TRUE.equals(initiativeConfig.getCheckPastOnboardingsSeries());
    }

    private static Set<String> retrieveOtherMembersOnboardingIds(OnboardingDTO onboardingRequest) {
        if (onboardingRequest.getFamily() == null || onboardingRequest.getFamily().getMemberIds() == null) {
            return Set.of();
        }
        return onboardingRequest.getFamily().getMemberIds().stream()
                .filter(memberId -> !memberId.equals(onboardingRequest.getUserId()))
                .collect(Collectors.toSet());
    }

    private static void validateResponse(Set<String> requestedUserIds, CheckPastOnboardingsSeriesResDTO response) {
        if (response == null || response.getPastOnboardings() == null) {
            throw new IllegalArgumentException("Invalid response from checkPastOnboardingsSeries API: response or pastOnboardings is null");
        }

        var results = response.getPastOnboardings();
        if (results.stream().anyMatch(result -> result == null || result.getUserId() == null)) {
            throw new IllegalStateException("Invalid response: a result or userId is missing");
        }

        Set<String> responseUserIds = results.stream()
                .map(CheckPastOnboardingsSeriesResElemDTO::getUserId)
                .collect(Collectors.toSet());

        if (responseUserIds.size() != results.size()
                || !responseUserIds.equals(requestedUserIds)) {
            throw new IllegalStateException("Invalid response: userIds do not match the request");
        }
    }

    private static boolean isBonusAlreadyUsed(CheckPastOnboardingsSeriesResDTO response) {
        return response.getPastOnboardings().stream()
                .anyMatch(CheckPastOnboardingsSeriesResElemDTO::isOnboarded);
    }

    private static OnboardingRejectionReason buildRejectionReason() {
        return OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.FAMILY_BONUS_ALREADY_USED)
                .code(OnboardingConstants.REJECTION_REASON_FAMILY_BONUS_ALREADY_USED)
                .detail("Un componente del nucleo familiare ha già usufruito del bonus")
                .build();
    }
}
