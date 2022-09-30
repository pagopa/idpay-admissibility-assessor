package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class OnboardingNoifierServiceImpl implements OnboardingNoifierService{

    private final StreamBridge streamBridge;

    public OnboardingNoifierServiceImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public boolean notify(EvaluationDTO evaluationDTO) {
        return streamBridge.send("admissibilityProcessor-out-0",
                MessageBuilder.withPayload(evaluationDTO).build());
    }
}
