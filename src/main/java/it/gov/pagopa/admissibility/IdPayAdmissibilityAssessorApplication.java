package it.gov.pagopa.admissibility;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
@ConfigurationPropertiesScan
public class IdPayAdmissibilityAssessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdPayAdmissibilityAssessorApplication.class, args);
    }
}
