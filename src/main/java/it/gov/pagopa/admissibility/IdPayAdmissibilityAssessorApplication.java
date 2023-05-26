package it.gov.pagopa.admissibility;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
public class IdPayAdmissibilityAssessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdPayAdmissibilityAssessorApplication.class, args);
    }
}
