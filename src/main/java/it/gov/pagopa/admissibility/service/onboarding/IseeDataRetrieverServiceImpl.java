package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.extra.Isee;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
public class IseeDataRetrieverServiceImpl implements IseeDataRetrieverService {

    private final ReactiveMongoTemplate mongoTemplate;

    public IseeDataRetrieverServiceImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Map<String, BigDecimal>> retrieveUserIsee(String userId) {
        return mongoTemplate.findById(
                userId,
                Isee.class,
                "mocked_isee"
        ).map(Isee::getIseeTypeMap);
    }
}
