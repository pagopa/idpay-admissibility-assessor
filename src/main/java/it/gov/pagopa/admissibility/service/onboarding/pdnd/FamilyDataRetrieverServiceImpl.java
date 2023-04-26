package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;

@Service
public class FamilyDataRetrieverServiceImpl implements FamilyDataRetrieverService {

    @Override
    public Mono<Family> retrieveFamily(OnboardingDTO onboardingRequest, Message<String> message) {
        // TODO call PDND and re-scheduling if dailyLimit occurred

        String mockedFamilyId = onboardingRequest.getUserId();
        if(mockedFamilyId.matches("_FAMILYMEMBER\\d+$")){
            mockedFamilyId = mockedFamilyId.substring(0, mockedFamilyId.indexOf("_FAMILYMEMBER"));
        }

        return Mono.just(Family.builder()
                .familyId(mockedFamilyId)
                .memberIds(new HashSet<>(List.of(
                        onboardingRequest.getUserId(),
                        mockedFamilyId,
                        mockedFamilyId+"_FAMILYMEMBER1",
                        mockedFamilyId+"_FAMILYMEMBER2",
                        mockedFamilyId+"_FAMILYMEMBER3"
                )))
                .build());
    }
}
