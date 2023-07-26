package it.gov.pagopa.admissibility.mock.isee;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.mock.isee.controller.IseeController;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.mock.isee.model.Isee;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.math.BigDecimal;
import java.util.Map;

class IseeControllerImplTest extends BaseIntegrationTest {

    private static final String USERID = "USERID";
    private static final String MOCKED_ISEE_COLLECTION_NAME = "mocked_isee";
    @Autowired
    private WebTestClient webClient;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;
    @AfterEach
    void cleanData() {
        mongoTemplate.remove(
                Query.query(Criteria.where(Isee.Fields.userId).is(USERID)),
                MOCKED_ISEE_COLLECTION_NAME);
    }

    @Test
    void createIsee() {

        Map<IseeTypologyEnum, BigDecimal> iseeMap = Map.of(
                IseeTypologyEnum.ORDINARIO, BigDecimal.TEN,
                IseeTypologyEnum.UNIVERSITARIO, BigDecimal.TEN
        );

        IseeController.IseeRequestDTO request = IseeController.IseeRequestDTO.builder()
                .iseeTypeMap(iseeMap)
                .build();

        WebTestClient.ResponseSpec result = createIsee(USERID, request);
        result.expectStatus().isOk();

        Map<String, BigDecimal> repositoryResult = mongoTemplate.findById(USERID, Isee.class, MOCKED_ISEE_COLLECTION_NAME)
                .map(Isee::getIseeTypeMap).block();

        Assertions.assertNotNull(repositoryResult);
        Assertions.assertEquals(2, repositoryResult.size());

    }

    @Test
    void createIseeBadRequest() {

        Map<IseeTypologyEnum, BigDecimal> iseeMap = Map.of(
                IseeTypologyEnum.ORDINARIO, BigDecimal.TEN,
                IseeTypologyEnum.UNIVERSITARIO, BigDecimal.ZERO
        );

        IseeController.IseeRequestDTO request = IseeController.IseeRequestDTO.builder()
                .iseeTypeMap(iseeMap)
                .build();

        WebTestClient.ResponseSpec result = createIsee(USERID, request);
        result.expectStatus().isBadRequest();

        Isee repositoryResult = mongoTemplate.findById(USERID, Isee.class, MOCKED_ISEE_COLLECTION_NAME).block();

        Assertions.assertNull(repositoryResult);
    }


    private WebTestClient.ResponseSpec createIsee(String userId, IseeController.IseeRequestDTO iseeRequestDTO){
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/idpay/isee/mock/{userId}")
                        .build(userId))
                .body(BodyInserters.fromValue(iseeRequestDTO))
                .exchange();
    }
}