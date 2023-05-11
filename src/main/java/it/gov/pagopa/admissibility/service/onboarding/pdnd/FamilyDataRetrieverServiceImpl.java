package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
public class FamilyDataRetrieverServiceImpl implements FamilyDataRetrieverService {

    private final ReactiveMongoTemplate mongoTemplate;

    public FamilyDataRetrieverServiceImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Optional<Family>> retrieveFamily(OnboardingDTO onboardingRequest, Message<String> message) {
        // TODO call PDND and re-scheduling if dailyLimit occurred

        return searchMockCollection(onboardingRequest.getUserId())
                .map(Optional::of)
                .switchIfEmpty(
                        Mono.just(Optional.of(Family.builder()
                                .familyId("FAMILYID_" + onboardingRequest.getUserId())
                                .memberIds(new HashSet<>(List.of(
                                        onboardingRequest.getUserId()
                                )))
                                .build()))
                );
    }

    private Mono<Family> searchMockCollection(String userId) {
        return mongoTemplate.find(
                        new Query(Criteria.where("memberIds").is(userId)),
                        MockedFamily.class
                ).cast(Family.class)
                .next();
    }

    @Document("mocked_families")
    @SuperBuilder
    @AllArgsConstructor
    public static class MockedFamily extends Family {
        @Id
        @Override
        public String getFamilyId() {
            return super.getFamilyId();
        }
    }
}
