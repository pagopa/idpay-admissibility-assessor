package it.gov.pagopa.admissibility.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import it.gov.pagopa.admissibility.exception.Severity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode
public class ErrorDTO {
    @NotBlank
    @ApiModelProperty(required = true, value = "Severity level of the error message", example = "ERROR")
    Severity severity;
    @NotBlank
    @ApiModelProperty(required = true, value = "Title of the error message", example = "Title")
    String title;
    @NotBlank
    @ApiModelProperty(required = true, value = "Content of the error message", example = "Message")
    String message;

}
