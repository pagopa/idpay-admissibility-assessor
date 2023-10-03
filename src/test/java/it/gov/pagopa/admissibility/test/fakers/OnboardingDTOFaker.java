package it.gov.pagopa.admissibility.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Random;

public final class OnboardingDTOFaker {
    private OnboardingDTOFaker(){}

    private static final FakeValuesService fakeValuesServiceGlobal = new FakeValuesService(new Locale("it"), new RandomService());

    public static OnboardingDTO mockInstance(Integer bias,String initiativeId){
        return mockInstanceBuilder(bias, initiativeId).build();
    }

    public static OnboardingDTO.OnboardingDTOBuilder mockInstanceBuilder(Integer bias,String initiativeId){
        OnboardingDTO.OnboardingDTOBuilder out = OnboardingDTO.builder();

        FakeValuesService fakeValuesService = getFakeValuesService(bias);

        out.userId(fakeValuesService.bothify(bias!=null? "userId_%d".formatted(bias) : "?????"));
        out.initiativeId(initiativeId);
        out.tc(true);
        out.status(bias!=null? "status_%d".formatted(bias) : "?????");
        out.pdndAccept(true);
        out.tcAcceptTimestamp(LocalDateTime.of(2022,10,2,10,0,0));
        out.criteriaConsensusTimestamp(LocalDateTime.of(2022,10,2,10,0,0));

        return  out;
    }

    private static FakeValuesService getFakeValuesService(Integer bias) {
        return bias == null ? fakeValuesServiceGlobal : new FakeValuesService(new Locale("it"), new RandomService(new Random(bias)));
    }
}
