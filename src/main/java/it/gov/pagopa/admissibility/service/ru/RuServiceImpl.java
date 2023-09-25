package it.gov.pagopa.admissibility.service.ru;

import it.gov.pagopa.admissibility.connector.repository.OnboardingFamiliesRepository;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import it.gov.pagopa.admissibility.service.commands.operations.DeleteInitiativeService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Set;
import java.util.stream.IntStream;
@Service
public class RuServiceImpl implements RuService{
    private final OnboardingFamiliesRepository onboardingFamiliesRepository;
    private final DeleteInitiativeService deleteInitiativeService;
    public RuServiceImpl(OnboardingFamiliesRepository onboardingFamiliesRepository, DeleteInitiativeService deleteInitiativeService) {
        this.onboardingFamiliesRepository = onboardingFamiliesRepository;
        this.deleteInitiativeService = deleteInitiativeService;
    }

    @Override
    public Mono<Void> createRecord(String initiativeId, int numberOfRecord) {
        return Mono.just(numberOfRecord)
                .map(n -> IntStream.range(0, numberOfRecord)
                        .mapToObj(i -> OnboardingFamilies.builder(Family.builder().familyId("FAMILYID%d".formatted(i)).memberIds(Set.of("member1", "member2")).build(), initiativeId).build()).toList())
                .flatMapMany(onboardingFamiliesRepository::saveAll)
                .then();
    }

    @Override
    public Mono<Void> deleteOnboardingFamiliesExpand(String initiativeId, int pageSize, long delay) {
        return deleteInitiativeService.execute(initiativeId, pageSize, delay)
                .then();
    }

    @Override
    public Mono<Void> deleteOnboardingFamiliesRangeLimit(String initiativeId, int pageSize, long delay) {
        return onboardingFamiliesRepository.findByInitiativeId(initiativeId)
                .limitRate(pageSize, 0)
                .flatMap(of -> onboardingFamiliesRepository.deleteById(of.getId()))
                .delayElements(Duration.ofMillis(delay))
                .then();
    }
}
