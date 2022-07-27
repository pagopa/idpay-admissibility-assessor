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
        OnboardingDTO out = new OnboardingDTO();

        FakeValuesService fakeValuesService = getFakeValuesService(bias);

        out.setUserId(fakeValuesService.bothify(bias!=null? "userId_%d".formatted(bias) : "?????"));
        out.setInitiativeId(bias!=null? "id_%d".formatted(bias%initiativeNumber) : "?????");
        out.setTc(true);
        out.setStatus(bias!=null? "status_%d".formatted(bias) : "?????");
        out.setPdndAccept(true);
        out.setSelfDeclarationList((bias%initiativeNumber)==0?Map.of("ISEE",true,"BIRTHDATE", false):Map.of("ISEE",true,"BIRTHDATE", true)); //Map selfDeclarationList
        out.setTcAcceptTimestamp(LocalDateTime.of(2022,10,2,10,0,0));
        out.setCriteriaConsensusTimestamp(LocalDateTime.of(2022,10,2,10,0,0));
        out.setIsee(new BigDecimal(20));
        out.setBirthDate(new DataNascita());
        out.getBirthDate().setAnno("1990");
        out.getBirthDate().setEta(32);
        return  out;
    }

    private static FakeValuesService getFakeValuesService(Integer bias) {
        return bias == null ? fakeValuesServiceGlobal : new FakeValuesService(new Locale("it"), new RandomService(new Random(bias)));
    }
}
