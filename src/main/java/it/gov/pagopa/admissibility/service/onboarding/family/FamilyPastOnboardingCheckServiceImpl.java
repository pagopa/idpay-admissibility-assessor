package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.connector.repository.onboarding.OnboardingRepository;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.rule.InitiativeGeneralDTO;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.onboarding.Onboarding;
import it.gov.pagopa.admissibility.model.onboarding.OnboardingStatusInfo;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FamilyPastOnboardingCheckServiceImpl implements FamilyPastOnboardingCheckService {

    private static final Set<String> BONUS_ALREADY_USED_DETAILS_KO = Set.of(
            OnboardingConstants.DETAIL_KO_USER_BONUS_ALREADY_USED,
            OnboardingConstants.DETAIL_KO_FAMILY_BONUS_ALREADY_USED);
    private static final String DETAIL_KO_SEPARATOR = ",";

    private final OnboardingRepository onboardingRepository;
    private final Onboarding2EvaluationMapper onboarding2EvaluationMapper;

    public FamilyPastOnboardingCheckServiceImpl(OnboardingRepository onboardingRepository,
                                                Onboarding2EvaluationMapper onboarding2EvaluationMapper) {
        this.onboardingRepository = onboardingRepository;
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
            return Mono.empty();
        }

        return onboardingRepository.findByIdIn(onboardingIds)
                .any(FamilyPastOnboardingCheckServiceImpl::isBonusAlreadyUsed)
                .flatMap(bonusAlreadyUsed -> {
                    if (Boolean.TRUE.equals(bonusAlreadyUsed)) {
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
                && Boolean.TRUE.equals(initiativeConfig.getCheckPastOnboardings());
    }

    private static Set<String> retrieveOtherMembersOnboardingIds(OnboardingDTO onboardingRequest) {
        if (onboardingRequest.getFamily() == null || onboardingRequest.getFamily().getMemberIds() == null) {
            return Set.of();
        }
        return onboardingRequest.getFamily().getMemberIds().stream()
                .filter(memberId -> !memberId.equals(onboardingRequest.getUserId()))
                .map(memberId -> Onboarding.buildId(onboardingRequest.getInitiativeId(), memberId))
                .collect(Collectors.toSet());
    }

    private static boolean isBonusAlreadyUsed(OnboardingStatusInfo onboarding) {
        return OnboardingEvaluationStatus.ONBOARDING_KO.name().equals(onboarding.getStatus())
                && onboarding.getDetailKO() != null
                && Arrays.stream(onboarding.getDetailKO().split(DETAIL_KO_SEPARATOR))
                .map(String::trim)
                .anyMatch(BONUS_ALREADY_USED_DETAILS_KO::contains);
    }

    private static OnboardingRejectionReason buildRejectionReason() {
        return OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.FAMILY_BONUS_ALREADY_USED)
                .code(OnboardingConstants.REJECTION_REASON_FAMILY_BONUS_ALREADY_USED)
                .detail("Un componente del nucleo familiare ha già usufruito del bonus")
                .build();
    }
}

