package it.gov.pagopa.admissibility.utils.json;

import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

class PageModuleTest {

    @Test
    void test() {
        // Given
        String testString = "TEST";
        PageRequest pageRequest = PageRequest.of(0,1);
        PageImpl<?> page = new PageImpl<>(List.of(testString), pageRequest, 1);

        // When
        String result = TestUtils.jsonSerializer(page);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(
                "{\"content\":[\"%s\"],\"pageable\":{\"page\":0,\"size\":1,\"sort\":{\"orders\":[]}},\"total\":%d}"
                        .formatted(testString, 1),
                result
        );
    }
}