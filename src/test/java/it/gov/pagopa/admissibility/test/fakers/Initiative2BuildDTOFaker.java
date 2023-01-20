package it.gov.pagopa.admissibility.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.dto.rule.*;
import it.gov.pagopa.admissibility.utils.AESUtil;
import it.gov.pagopa.admissibility.utils.TestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

public final class Initiative2BuildDTOFaker {
    private Initiative2BuildDTOFaker(){}

    //region Fields encrypt
    //The following fields for encrypt/decrypt are set with the same value they have in BaseIntegration properties
    public static final String AGID_FIELD_TOKEN_PAYLOAD = "{\"iss\":\"ISS%s\",\"sub\":\"SUB%s\",\"aud\":\"AUD%s\",\"jti\":\"jti\",\"exp\":1647454566}";
    private static final String PBE_ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final String ENCODING = "UTF-8";
    private static final String SALT = "SALT_SAMPLE";
    private static final String IV = "IV_SAMPLE";
    private static final int KEY_SIZE = 256;
    private static final int ITERATION_COUNT = 10000;
    private static final int GCM_TAG_LENGTH = 16;
    private static final String CIPHER_INSTANCE = "AES/GCM/NoPadding";
    private static final String PASSPHRASE = "passphrase";

    private static final AESUtil aesUtil = new AESUtil(CIPHER_INSTANCE, ENCODING, PBE_ALGORITHM, SALT, KEY_SIZE, ITERATION_COUNT, IV, GCM_TAG_LENGTH);
    //endregion
    private static final FakeValuesService fakeValuesServiceGlobal = new FakeValuesService(new Locale("it"), new RandomService());

    /** It will return an example of {@link Initiative2BuildDTO}. Providing a bias, it will return a pseudo-casual object */
    public static Initiative2BuildDTO mockInstance(Integer bias){
        return mockInstanceBuilder(bias).build();
    }
    public static Initiative2BuildDTO.Initiative2BuildDTOBuilder mockInstanceBuilder(Integer bias){
        Initiative2BuildDTO.Initiative2BuildDTOBuilder out = Initiative2BuildDTO.builder();

        FakeValuesService fakeValuesService = getFakeValuesService(bias);

        out.initiativeId(fakeValuesService.bothify(bias!=null? "id_%d".formatted(bias) : "?????"));
        out.initiativeName(fakeValuesService.bothify("?????"));
        out.organizationId(fakeValuesService.bothify("?????"));
        out.status(fakeValuesService.bothify(bias!=null? "status_%d".formatted(bias) : "?????"));

        final InitiativeBeneficiaryRuleDTO beneficiaryRule = new InitiativeBeneficiaryRuleDTO();
        beneficiaryRule.setAutomatedCriteria(new ArrayList<>());
        beneficiaryRule.getAutomatedCriteria().add(new AutomatedCriteriaDTO("AUTH1", CriteriaCodeConfigFaker.CRITERIA_CODE_ISEE, null, FilterOperator.GT, "10", null, null));
        beneficiaryRule.getAutomatedCriteria().add(new AutomatedCriteriaDTO("AUTH2", CriteriaCodeConfigFaker.CRITERIA_CODE_BIRTHDATE, "year", FilterOperator.GT, "10", null, null));
        beneficiaryRule.setApiKeyClientId(encrypt(getUuid(String.valueOf(bias)).toString()));
        beneficiaryRule.setApiKeyClientAssertion(
                encrypt(
                        getClientAssertion(
                                String.format("apiKeyClientAssertionFirstElement%d",bias),
                                getAgidTokenPayload(String.valueOf(bias)),
                                String.format("apiKeyClientAssertionFirstElement%d",bias)
                        )));

        out.beneficiaryRule(beneficiaryRule);

        out.general(
                InitiativeGeneralDTO.builder()
                        .name("NAME")
                        .budget(new BigDecimal("100000.00"))
                        .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF)
                        .beneficiaryKnown(Boolean.TRUE)
                        .beneficiaryBudget(new BigDecimal("1000.00"))
                        .startDate(LocalDate.of(2021, 1, 1))
                        .endDate(LocalDate.of(2025, 12, 1))
                        .rankingEnabled(false)
                        .build());


        out.additionalInfo(new InitiativeAdditionalInfoDTO(
                "SERVICENAME%s".formatted(bias),
                "ARGUMENT%s".formatted(bias),
                "DESCRIPTION%s".formatted(bias),
                List.of(ChannelsDTO.builder().type("web").contact("CONTACT%s".formatted(bias)).build())
        ));

        TestUtils.checkNotNullFields(out.build());
        return out;
    }

    private static FakeValuesService getFakeValuesService(Integer bias) {
        return bias == null ? fakeValuesServiceGlobal : new FakeValuesService(new Locale("it"), new RandomService(new Random(bias)));
    }

    public static String getAgidTokenPayload(String bias){
        return String.format(AGID_FIELD_TOKEN_PAYLOAD,bias,bias,bias);
    }

    public static String getClientAssertion(String firstElement, String middleElement, String lastElement){
        String divisor = ".";

        return getStringB64(firstElement)
                .concat(divisor)
                .concat(getStringB64(middleElement))
                .concat(divisor)
                .concat(getStringB64(lastElement));
    }

    public static String getStringB64(String s){
        return new String(Base64.getEncoder().encode(s.getBytes(StandardCharsets.UTF_8)));
    }

    public static String encrypt(String s){
        return aesUtil.encrypt(PASSPHRASE,s);
    }

    public static UUID getUuid(String seed){
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
