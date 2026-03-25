package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import it.gov.pagopa.admissibility.connector.pdnd.PdndServicesInvocation;
import it.gov.pagopa.admissibility.connector.rest.anpr.mapper.TipoResidenzaDTO2ResidenceMapper;
import it.gov.pagopa.admissibility.dto.anpr.response.PdndResponseBase;
import it.gov.pagopa.admissibility.dto.anpr.response.PdndResponseVisitor;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.enums.PdndResponseType;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaKODTO;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnprDataRetrieverServiceImplTest {

    private AnprDataRetrieverServiceImpl service;
    private AnprC001RestClient anprC001RestClient;
    private CriteriaCodeService criteriaCodeService;
    private TipoResidenzaDTO2ResidenceMapper residenceMapper;

    @BeforeEach
    void setUp() {
        anprC001RestClient = mock(AnprC001RestClient.class);
        criteriaCodeService = mock(CriteriaCodeService.class);
        residenceMapper = mock(TipoResidenzaDTO2ResidenceMapper.class);

        service = new AnprDataRetrieverServiceImpl(anprC001RestClient, criteriaCodeService, residenceMapper);
    }

    @Test
    void testInvokeResidence_NotEmptyList() {
        String fiscalCode = "RSSMRA85M01H501Z";
        PdndInitiativeConfig config = new PdndInitiativeConfig();
        OnboardingDTO onboarding = new OnboardingDTO();

        PdndServicesInvocation invocation1 = new PdndServicesInvocation(true, List.of(),true, true, true, "isse");

        when(anprC001RestClient.invoke(fiscalCode, config))
                .thenReturn(Mono.just(new PdndResponseBase<RispostaE002OKDTO, RispostaKODTO>(PdndResponseType.OK) {
                    @Override
                    public <R> R accept(PdndResponseVisitor<RispostaE002OKDTO, RispostaKODTO, R> visitor) {
                        return null;
                    }
                }));

        Residence mockResidence = new Residence();
        when(residenceMapper.apply(any())).thenReturn(mockResidence);
        CriteriaCodeConfig criteriaCodeConfig = new CriteriaCodeConfig();
        when(criteriaCodeService.getCriteriaCodeConfig(any())).thenReturn(criteriaCodeConfig);
        StepVerifier.create(service.invoke(fiscalCode, config, invocation1, onboarding))
                .expectNextCount(1)
                .verifyComplete();

    }

    @Test
    void testInvokeResidence_EmptyList() {
        String fiscalCode = "RSSMRA85M01H501Z";
        PdndInitiativeConfig config = new PdndInitiativeConfig();
        OnboardingDTO onboarding = new OnboardingDTO();

        PdndServicesInvocation invocation1 = new PdndServicesInvocation(true, List.of(),false, false, true, "isse");

        when(anprC001RestClient.invoke(fiscalCode, config))
                .thenReturn(Mono.just(new PdndResponseBase<RispostaE002OKDTO, RispostaKODTO>(PdndResponseType.OK) {
                    @Override
                    public <R> R accept(PdndResponseVisitor<RispostaE002OKDTO, RispostaKODTO, R> visitor) {
                        return null;
                    }
                }));

        Residence mockResidence = new Residence();
        when(residenceMapper.apply(any())).thenReturn(mockResidence);

        StepVerifier.create(service.invoke(fiscalCode, config, invocation1, onboarding))
                .expectNext(Optional.of(Collections.emptyList()))
                .verifyComplete();

    }

}