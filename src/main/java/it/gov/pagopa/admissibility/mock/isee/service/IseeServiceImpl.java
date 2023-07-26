package it.gov.pagopa.admissibility.mock.isee.service;

import it.gov.pagopa.admissibility.mock.isee.controller.IseeController;
import it.gov.pagopa.admissibility.mock.isee.model.Isee;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class IseeServiceImpl implements IseeService{
    private final ReactiveMongoTemplate mongoTemplate;

    public IseeServiceImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Isee> saveIsee(String userId, IseeController.IseeRequestDTO iseeRequestDTO) {
        return buildAndCheckIseeEntity(userId,iseeRequestDTO)
                .flatMap(isee -> mongoTemplate.save(isee, "mocked_isee"));
    }

    private Mono<Isee> buildAndCheckIseeEntity(String userId, IseeController.IseeRequestDTO iseeRequestDTO){
        Map<String, BigDecimal> iseeTypeMap = new HashMap<>();
        iseeRequestDTO.getIseeTypeMap()
                .forEach((type, value) -> {
                    if(BigDecimal.ZERO.compareTo(value) >= 0){
                        throw new ClientExceptionWithBody(HttpStatus.BAD_REQUEST,
                                "INVALID_VALUE",
                                "Invalid value for isee type %s".formatted(type.name()));
                    }
                    iseeTypeMap.put(type.name(), value.setScale(2, RoundingMode.HALF_DOWN));
                });

        return Mono.just(Isee.builder()
                .userId(userId)
                .iseeTypeMap(iseeTypeMap)
                .build());
    }
}
