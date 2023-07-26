package it.gov.pagopa.admissibility.controller;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.model.mock.Isee;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.math.BigDecimal;
import java.util.Map;

class MockIseeControllerImplTest extends BaseIntegrationTest {

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    @Test
    void createIsee() {
        String userid = "USERID";
        Map<IseeTypologyEnum, BigDecimal> iseeMap = Map.of(
                IseeTypologyEnum.ORDINARIO, BigDecimal.TEN,
                IseeTypologyEnum.UNIVERSITARIO, BigDecimal.TEN
        );

        MockIseeController.IseeRequestDTO request = MockIseeController.IseeRequestDTO.builder()
                .iseeTypeMap(iseeMap)
                .build();

        WebTestClient.ResponseSpec result = createIsee(userid, request);
        result.expectStatus().isOk();

        Map<String, BigDecimal> repositoryResult = mongoTemplate.findById(userid, Isee.class, "mocked_isee")
                .map(Isee::getIseeTypeMap).block();

        Assertions.assertNotNull(repositoryResult);
        Assertions.assertEquals(2, repositoryResult.size());


    }

    private WebTestClient.ResponseSpec createIsee(String userId, MockIseeController.IseeRequestDTO iseeRequestDTO){
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/idpay/isee/mock/{userId}")
                        .build(userId))
                .body(BodyInserters.fromValue(iseeRequestDTO))
                .exchange();
    }
}