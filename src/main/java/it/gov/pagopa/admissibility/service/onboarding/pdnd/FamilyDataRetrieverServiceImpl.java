package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.connector.rest.mock.FamilyMockRestClient;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
public class FamilyDataRetrieverServiceImpl implements FamilyDataRetrieverService {
    private final FamilyMockRestClient familyMockRestClient;

    private final ReactiveMongoTemplate mongoTemplate;

    public FamilyDataRetrieverServiceImpl(FamilyMockRestClient familyMockRestClient, ReactiveMongoTemplate mongoTemplate) {
        this.familyMockRestClient = familyMockRestClient;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Optional<Family>> retrieveFamily(OnboardingDTO onboardingRequest, Message<String> message) {
        // TODO call PDND and re-scheduling if dailyLimit occurred

        //TODO this is a mocked behavior! Replace with the real integration
        return familyMockRestClient.retrieveFamily(onboardingRequest.getUserId())
                .map(Optional::of);

    }
}
