package it.gov.pagopa.admissibility.utils.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

class PageModuleTest {

    @Test
    void test() throws JsonProcessingException {
        // Given
        String testString = "TEST";
        PageRequest pageRequest = PageRequest.of(0,1);
        PageImpl<String> expectedPage = new PageImpl<>(List.of(testString), pageRequest, 1);

        // When
        String serialized = TestUtils.jsonSerializer(expectedPage);
        Page<String> result = TestUtils.objectMapper.readValue(serialized, new TypeReference<>() {});

        // Then
        Assertions.assertNotNull(serialized);
        Assertions.assertEquals(
                "{\"content\":[\"%s\"],\"first\":true,\"last\":true,\"totalPages\":1,\"totalElements\":1,\"numberOfElements\":1,\"size\":1,\"number\":0,\"sort\":[]}"
                        .formatted(testString),
                serialized
        );

        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedPage, result);
    }
}