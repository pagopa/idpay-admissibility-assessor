package it.gov.pagopa.service.onboarding;

import it.gov.pagopa.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.dto.onboarding.OnboardingDroolsDTO;
import it.gov.pagopa.dto.onboarding.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.dto.onboarding.mapper.Onboarding2OnboardingDroolsMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.command.Command;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@Slf4j
class RuleEngineServiceImplTest {

    @Test
    void applyRules() {
        // Given
        OnboardingContextHolderService onboardingContextHolderService = Mockito.mock(OnboardingContextHolderServiceImpl.class);
        Onboarding2EvaluationMapper onboarding2EvaluationMapper = Mockito.mock(Onboarding2EvaluationMapper.class);
        Onboarding2OnboardingDroolsMapper onboarding2OnboardingDroolsMapper = Mockito.mock(Onboarding2OnboardingDroolsMapper.class);

        RuleEngineService ruleEngineService = new RuleEngineServiceImpl(onboardingContextHolderService, onboarding2EvaluationMapper, onboarding2OnboardingDroolsMapper);

        OnboardingDTO onboardingDTO = Mockito.mock(OnboardingDTO.class);

        OnboardingDroolsDTO onboardingDroolsDTO = new OnboardingDroolsDTO();
        Mockito.when(onboarding2OnboardingDroolsMapper.apply(Mockito.same(onboardingDTO))).thenReturn(onboardingDroolsDTO);

        KieContainer kieContainer = Mockito.mock(KieContainer.class);
        Mockito.when(onboardingContextHolderService.getKieContainer()).thenReturn(kieContainer);
        StatelessKieSession statelessKieSession = Mockito.mock(StatelessKieSession.class);
        Mockito.when(kieContainer.newStatelessKieSession()).thenReturn(statelessKieSession);

        EvaluationDTO evaluationDTO = Mockito.mock(EvaluationDTO.class);
        Mockito.when(onboarding2EvaluationMapper.apply(Mockito.same(onboardingDTO), Mockito.any())).thenReturn(evaluationDTO);

        // When
        ruleEngineService.applyRules(onboardingDTO);

        // Then
        Mockito.verify(onboarding2OnboardingDroolsMapper).apply(Mockito.same(onboardingDTO));
        Mockito.verify(onboardingContextHolderService).getKieContainer();
        Mockito.verify(statelessKieSession).execute(Mockito.any(Command.class));
        Mockito.verify(onboarding2EvaluationMapper).apply(Mockito.same(onboardingDroolsDTO), );
    }
}
