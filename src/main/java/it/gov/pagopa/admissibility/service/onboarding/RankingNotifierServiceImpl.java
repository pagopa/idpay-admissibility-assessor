package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.RankingRequestDTO;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class RankingNotifierServiceImpl implements RankingNotifierService{
    private final StreamBridge streamBridge;

    public RankingNotifierServiceImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }
    @Override
    public boolean notify(RankingRequestDTO rankingRequestDTO) {
        return streamBridge.send("admissibilityProcessor-out-1",
                buildMessage(rankingRequestDTO));
    }
    public static Message<RankingRequestDTO> buildMessage(RankingRequestDTO rankingRequestDTO){
        return MessageBuilder.withPayload(rankingRequestDTO).build();
    }
}
