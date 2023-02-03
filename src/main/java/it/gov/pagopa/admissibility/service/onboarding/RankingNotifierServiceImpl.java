package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.RankingRequestDTO;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

@Service
public class RankingNotifierServiceImpl implements RankingNotifierService{
    private final StreamBridge streamBridge;

    public RankingNotifierServiceImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    /** Declared just to let know Spring to connect the producer at startup */
    @Configuration
    static class RankingNotifierProducerConfig {
        @Bean
        public Supplier<Flux<Message<RankingRequestDTO>>> rankingRequest() {
            return Flux::empty;
        }
    }

    @Override
    public boolean notify(RankingRequestDTO rankingRequestDTO) {
        return streamBridge.send("rankingRequest-out-0",
                buildMessage(rankingRequestDTO));
    }
    public static Message<RankingRequestDTO> buildMessage(RankingRequestDTO rankingRequestDTO){
        return MessageBuilder.withPayload(rankingRequestDTO).build();
    }
}
