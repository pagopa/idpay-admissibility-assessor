package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
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

        String membersMockedBaseId = onboardingRequest.getUserId();
        if (membersMockedBaseId.matches(".*_FAMILYMEMBER\\d+$")) {
            membersMockedBaseId = membersMockedBaseId.substring(0, membersMockedBaseId.indexOf("_FAMILYMEMBER"));
        }

        return searchMockCollection(onboardingRequest.getUserId())
                .map(Optional::of)
                .switchIfEmpty(
                        Mono.just(Optional.of(Family.builder()
                                .familyId("FAMILYID_" + membersMockedBaseId)
                                .memberIds(new HashSet<>(List.of(
                                        onboardingRequest.getUserId(),
                                        membersMockedBaseId + "_FAMILYMEMBER0",
                                        membersMockedBaseId + "_FAMILYMEMBER1",
                                        membersMockedBaseId + "_FAMILYMEMBER2"
                                )))
                                .build()))
                );
    }

    private Mono<Family> searchMockCollection(String userId) {
        return mongoTemplate.find(
                new Query(Criteria.where("memberIds").is(userId)),
                Family.class,
                "mocked_families"
        ).next();
    }
}
