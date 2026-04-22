package it.gov.pagopa.admissibility;


import it.gov.pagopa.admissibility.config.CriteriaCodeConfigs;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
@EnableConfigurationProperties(CriteriaCodeConfigs.class)
public class IdPayAdmissibilityAssessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdPayAdmissibilityAssessorApplication.class, args);
    }
}
