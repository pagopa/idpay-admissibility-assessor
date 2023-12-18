package it.gov.pagopa.admissibility.config;

import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdmissibilityErrorManagerConfig {

  @Bean
  ErrorDTO defaultErrorDTO() {
    return new ErrorDTO(
        OnboardingConstants.ExceptionCode.GENERIC_ERROR,
        "A generic error occurred for payment"
    );
  }

  @Bean
  ErrorDTO tooManyRequestsErrorDTO() {
    return new ErrorDTO(OnboardingConstants.ExceptionCode.TOO_MANY_REQUESTS, "Too Many Requests");
  }
}
