package it.gov.pagopa.admissibility.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Set;

@Document("anpr_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true, builderMethodName = "hiddenBuilder", buildMethodName = "hiddenBuild")
@FieldNameConstants
public class AnprInfo {

    private String familyId;
    private String initiativeId;
    private String userId;
    private Set<String> childListIds;//TODO to be removed

    //TODO chiarire se il nome e il cognome possono essere salvate in chiaro su DB
    private List<Child> childList;
}
