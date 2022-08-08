package it.gov.pagopa.admissibility.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.DataNascita;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public final class OnboardingDTOFaker {
    private OnboardingDTOFaker(){}

    private static final FakeValuesService fakeValuesServiceGlobal = new FakeValuesService(new Locale("it"), new RandomService());

    public static OnboardingDTO mockInstance(Integer bias,Integer initiativeNumber){
        return mockInstanceBuilder(bias, initiativeNumber).build();
    }

    public static OnboardingDTO.OnboardingDTOBuilder mockInstanceBuilder(Integer bias,Integer initiativeNumber){
        OnboardingDTO.OnboardingDTOBuilder out = OnboardingDTO.builder();

        FakeValuesService fakeValuesService = getFakeValuesService(bias);

        out.userId(fakeValuesService.bothify(bias!=null? "userId_%d".formatted(bias) : "?????"));
        out.initiativeId(bias!=null? "id_%d".formatted(bias%initiativeNumber) : "?????");
        out.tc(true);
        out.status(bias!=null? "status_%d".formatted(bias) : "?????");
        out.pdndAccept(true);
        out.selfDeclarationList(Map.of("ISEE",true,"BIRTHDATE", true));
        out.tcAcceptTimestamp(LocalDateTime.of(2022,10,2,10,0,0));
        out.criteriaConsensusTimestamp(LocalDateTime.of(2022,10,2,10,0,0));
        out.isee(new BigDecimal(20));

        final DataNascita birthDate = new DataNascita();
        birthDate.setAnno("1990");
        birthDate.setEta(32);
        out.birthDate(birthDate);
        return  out;
    }

    private static FakeValuesService getFakeValuesService(Integer bias) {
        return bias == null ? fakeValuesServiceGlobal : new FakeValuesService(new Locale("it"), new RandomService(new Random(bias)));
    }
}
