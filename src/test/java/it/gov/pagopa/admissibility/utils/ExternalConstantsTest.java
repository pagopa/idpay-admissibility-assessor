package it.gov.pagopa.admissibility.utils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {ExternalConstants.class})
@TestPropertySource(properties = "external.constant.entityEnabledList=entity1,entity2,entity3")
class ExternalConstantsTest {

    @Autowired
    private ExternalConstants externalConstants;

    @Test
    void testEntityEnabledList() {
        assertEquals(Arrays.asList("entity1", "entity2", "entity3"), externalConstants.getEntityEnabledList());
    }
}
