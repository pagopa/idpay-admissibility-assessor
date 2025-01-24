package it.gov.pagopa.admissibility.dto.rule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SelfCriteriaTextDTOTest {

    private SelfCriteriaTextDTO selfCriteriaTextDTO;

    @BeforeEach
    public void setUp() {
        selfCriteriaTextDTO = new SelfCriteriaTextDTO("Test description", "Test value", "Test code");
    }

    @Test
    void testGetDescription() {
        assertEquals("Test description", selfCriteriaTextDTO.getDescription());
    }

    @Test
    void testGetValue() {
        assertEquals("Test value", selfCriteriaTextDTO.getValue());
    }

    @Test
    void testGetCode() {
        assertEquals("Test code", selfCriteriaTextDTO.getCode());
    }

    @Test
    void testToString() {
        String expectedString = "SelfCriteriaTextDTO(description=Test description, value=Test value, code=Test code)";
        assertEquals(expectedString, selfCriteriaTextDTO.toString());
    }

    @Test
    void testEqualsAndHashCode() {
        SelfCriteriaTextDTO other = new SelfCriteriaTextDTO("Test description", "Test value", "Test code");
        assertEquals(selfCriteriaTextDTO, other);
        assertEquals(selfCriteriaTextDTO.hashCode(), other.hashCode());

        SelfCriteriaTextDTO different = new SelfCriteriaTextDTO("Different", "Different", "Different");
        assertNotEquals(selfCriteriaTextDTO, different);
    }
}
