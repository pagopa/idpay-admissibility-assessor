package it.gov.pagopa.admissibility.controller;

import it.gov.pagopa.admissibility.model.mock.Isee;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
public class MockIseeControllerImpl implements MockIseeController {
    private final ReactiveMongoTemplate mongoTemplate;

    public MockIseeControllerImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Void> createIsee(String fiscalCode, IseeRequestDTO iseeRequestDTO) {
        //CallPDV se arriva come fiscalcode
        return Mono.just(fiscalCode)
//                .flatMap(request -> callPDV)
                .map(userId -> buildAndCheckIseeEntity(userId,iseeRequestDTO))
                .map(isee -> mongoTemplate.save(isee, "mocked_isee"))
                .then();
    }

    private Isee buildAndCheckIseeEntity(String userId, IseeRequestDTO iseeRequestDTO){
        Map<String, BigDecimal> iseeTypeMap = new HashMap<>();
        iseeRequestDTO.getIseeTypeMap()
                .forEach((type, value) -> {
                    if(!chekValue(value)){
                        throw new ClientExceptionWithBody(HttpStatus.BAD_REQUEST,
                                "INVALID_VALUE",
                                "Invalid value for isee type %s".formatted(type.name()));
                    }
                    iseeTypeMap.put(type.name(), value);
                });

        return Isee.builder()
                .userId(userId)
                .iseeTypeMap(iseeTypeMap)
                .build();
    }

    private boolean chekValue(BigDecimal value){
        return BigDecimal.ZERO.compareTo(value)>0;
    }

}
