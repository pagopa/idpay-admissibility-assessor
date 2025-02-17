package it.gov.pagopa.admissibility.utils;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Getter
@Setter
public class ExternalConstants {

    @Value("${external.constant.entityEnabledList}")
    private List<String> entityEnabledList;

}
