package org.ikasan.studio.io;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PojoDeserialisationTest {
    @Test
    public void testBark() {
        String expectedString = "woof";
        assertEquals(expectedString, "woof");
    }
}
