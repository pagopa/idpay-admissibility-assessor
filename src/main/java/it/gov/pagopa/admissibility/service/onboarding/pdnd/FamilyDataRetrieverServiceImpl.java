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

        String mockedFamilyId = "FAMILYID_" + onboardingRequest.getUserId();
        if (mockedFamilyId.matches(".*_FAMILYMEMBER\\d+$")) {
            mockedFamilyId = mockedFamilyId.substring(0, mockedFamilyId.indexOf("_FAMILYMEMBER"));
        }

        return searchMockCollection(onboardingRequest.getUserId())
                .map(Optional::of)
                .switchIfEmpty(
                        Mono.just(Optional.of(Family.builder()
                                .familyId(mockedFamilyId)
                                .memberIds(new HashSet<>(List.of(
                                        onboardingRequest.getUserId(),
                                        mockedFamilyId,
                                        mockedFamilyId + "_FAMILYMEMBER1",
                                        mockedFamilyId + "_FAMILYMEMBER2",
                                        mockedFamilyId + "_FAMILYMEMBER3"
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
