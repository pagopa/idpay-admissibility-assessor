package it.gov.pagopa.admissibility.service.build;


import org.springframework.messaging.Message;
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
public interface BeneficiaryRuleBuilderMediatorService {
    void execute(Flux<Message<String>> initiativeBeneficiaryRuleDTOFlux);
}
