package it.gov.pagopa.admissibility.mock.isee;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.mock.isee.controller.IseeController;
import it.gov.pagopa.admissibility.mock.isee.model.Isee;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.ErrorManager;
import org.apache.commons.lang3.function.FailableConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.admissibility.mock.isee.service.IseeServiceImpl=WARN",
        "logging.level.it.gov.pagopa.common.web.exception.ErrorManager=WARN",
})
class IseeControllerImplIntegrationTest extends BaseIntegrationTest {

    private static final String USERID = "USERID_%d";
    private static final String MOCKED_ISEE_COLLECTION_NAME = "mocked_isee";
    private final List<FailableConsumer<Integer, Exception>> useCases = new ArrayList<>();
    private static final int parallelism = 8;
    private static final ExecutorService executor = Executors.newFixedThreadPool(parallelism);

    ErrorDTO errorValueResidenzialeExpectedDTO = new ErrorDTO("INVALID_VALUE","Invalid value for isee type %s".formatted(IseeTypologyEnum.RESIDENZIALE));
    ErrorDTO errorNotIseeExpectedDTO = new ErrorDTO("ISEE_NOT_PRESENT","The request has no isee");

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;
    @SpyBean
    protected ErrorManager errorManagerSpy;

    @Test
    void test() {

        int N = Math.max(useCases.size(), 50);

        List<? extends Future<?>> tasks = IntStream.range(0, N)
                .mapToObj(i -> executor.submit(() -> {
                    try {
                        useCases.get(i % useCases.size()).accept(i);
                    } catch (Exception e) {
                        throw new IllegalStateException("Unexpected exception thrown during test", e);
                    }
                }))
                .toList();

        for (int i = 0; i < tasks.size(); i++) {
            try {
                tasks.get(i).get();
            } catch (Exception e) {
                System.err.printf("UseCase %d (bias %d) failed %n", i % useCases.size(), i);
                Mockito.mockingDetails(errorManagerSpy).getInvocations()
                        .stream()
                        .filter(ex->!ex.getArgument(0).getClass().equals(RuntimeException.class))
                        .forEach(ex -> System.err.println("ErrorManager invocation: " + ex));
                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                } else if (e.getCause() instanceof AssertionFailedError assertionFailedError) {
                    throw assertionFailedError;
                }
                Assertions.fail(e);
            }
        }
    }

    {
        //usecase 0: iseeOk
        useCases.add(i -> {
            IseeController.IseeRequestDTO request = getIseeRequestDTO();

            extractResponse(createIsee(USERID.formatted(i), request), HttpStatus.CREATED, null);

            Map<String, BigDecimal> repositoryResult = mongoTemplate.findById(USERID.formatted(i), Isee.class, MOCKED_ISEE_COLLECTION_NAME)
                    .map(Isee::getIseeTypeMap).block();

            Assertions.assertNotNull(repositoryResult);
            Assertions.assertEquals(2, repositoryResult.size());
            request.getIseeTypeMap()
                    .forEach((type, value) -> {
                        BigDecimal storedValue = repositoryResult.get(type.name());
                        Assertions.assertNotNull(storedValue);
                        Assertions.assertEquals(0, storedValue.compareTo(value));
                    });
        });

        //usecase1: isee bad request
        useCases.add(i -> {

            //ISEE value 0
            IseeController.IseeRequestDTO request = getIseeRequestDTO();
            request.getIseeTypeMap().put(IseeTypologyEnum.RESIDENZIALE, BigDecimal.ZERO);

            checkResponse(
                    extractResponse(createIsee(USERID.formatted(i), request), HttpStatus.BAD_REQUEST, ErrorDTO.class),
                    errorValueResidenzialeExpectedDTO);

            checkNotStoredIsee(USERID.formatted(i));

            //ISEE negative value
            IseeController.IseeRequestDTO negativeRequest = getIseeRequestDTO();
            negativeRequest.getIseeTypeMap().put(IseeTypologyEnum.RESIDENZIALE, BigDecimal.valueOf(-1L));

            checkResponse(
                    extractResponse(createIsee(USERID.formatted(i), negativeRequest), HttpStatus.BAD_REQUEST, ErrorDTO.class),
                    errorValueResidenzialeExpectedDTO);

            checkNotStoredIsee(USERID.formatted(i));

            // not ISEE in request
            checkResponse(
                    extractResponse(createIsee(USERID.formatted(i), new IseeController.IseeRequestDTO()), HttpStatus.BAD_REQUEST, ErrorDTO.class),
                    errorNotIseeExpectedDTO);

            checkNotStoredIsee(USERID.formatted(i));

            // ISEE in request empty
            checkResponse(
                    extractResponse(createIsee(USERID.formatted(i), new IseeController.IseeRequestDTO(new HashMap<>())), HttpStatus.BAD_REQUEST, ErrorDTO.class),
                    errorNotIseeExpectedDTO);

            checkNotStoredIsee(USERID.formatted(i));

        });
    }

    private void checkResponse(ErrorDTO errorResult, ErrorDTO errorExpectedDTO) {
        Assertions.assertEquals(errorExpectedDTO, errorResult);
    }

    private void checkNotStoredIsee(String userId){
        Isee repositoryResult = mongoTemplate.findById(userId, Isee.class, MOCKED_ISEE_COLLECTION_NAME).block();
        Assertions.assertNull(repositoryResult);
    }

    private IseeController.IseeRequestDTO getIseeRequestDTO() {
        Map<IseeTypologyEnum, BigDecimal> iseeMap =new HashMap<>(Map.of(
                IseeTypologyEnum.ORDINARIO, BigDecimal.TEN,
                IseeTypologyEnum.UNIVERSITARIO, BigDecimal.TEN
        ));

        return IseeController.IseeRequestDTO.builder()
                .iseeTypeMap(iseeMap)
                .build();
    }

    private WebTestClient.ResponseSpec createIsee(String userId, IseeController.IseeRequestDTO iseeRequestDTO){
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/idpay/isee/mock/{userId}")
                        .build(userId))
                .body(BodyInserters.fromValue(iseeRequestDTO))
                .exchange();
    }

    protected <T> T extractResponse(WebTestClient.ResponseSpec response, HttpStatus expectedHttpStatus, Class<T> expectedBodyClass) {
        response = response.expectStatus().value(httpStatus -> Assertions.assertEquals(expectedHttpStatus.value(), httpStatus));
        if (expectedBodyClass != null) {
            return response.expectBody(expectedBodyClass).returnResult().getResponseBody();
        }
        return null;
    }
}