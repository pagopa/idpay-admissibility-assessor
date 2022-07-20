package it.gov.pagopa.admissibility.service.build;


import it.gov.pagopa.admissibility.dto.build.Initiative2BuildDTO;
import reactor.core.publisher.Flux;

/**
 * This component given an initiative will:
 * <ol>
 *     <li>translate it into drools syntax</li>
 *     <li>store it inside DB</li>
 *     <li>update the kieContainer</li>
 *     <li>notify the new kieContainer</li>
 * </ol>
 * */
public interface BeneficiaryRuleMediatorService {
    void execute(Flux<Initiative2BuildDTO> initiativeBeneficiaryRuleDTOFlux);
}
