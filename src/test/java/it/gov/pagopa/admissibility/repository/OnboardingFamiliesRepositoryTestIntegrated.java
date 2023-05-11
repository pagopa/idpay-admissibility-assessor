package it.gov.pagopa.admissibility.repository;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(locations = {
        "classpath:/mongodbEmbeddedDisabled.properties",
        "classpath:/secrets/mongodbConnectionString.properties"
})
class OnboardingFamiliesRepositoryTestIntegrated extends OnboardingFamiliesRepositoryTest {
}
