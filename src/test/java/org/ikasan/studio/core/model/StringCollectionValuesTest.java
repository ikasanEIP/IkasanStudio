package org.ikasan.studio.core.model;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StringCollectionValuesTest {
    @Test void retainsLegacyWhitespaceEmptyEntriesAndOrder() {
        assertEquals(List.of(" first", "second "), StringCollectionValues.normalize(List.class, " first  , \tsecond "));
        assertEquals(List.of("first", "", "second", ""), StringCollectionValues.normalize(List.class, "first,,second,"));
        assertEquals(List.of("first\u2003", "\u2003second"), StringCollectionValues.normalize(List.class, "first\u2003,\u2003second"));
        assertEquals(List.of(" first", "second "), StringCollectionValues.normalize(List.class, "[ first  , \tsecond ]"));
        assertEquals(List.of(" a ", "b,c", " a "), StringCollectionValues.normalize(List.class, "[\" a \",\"b,c\",\" a \"]"));
        assertNull(StringCollectionValues.normalize(List.class, " \t\r\n"));
    }

    @Test void longWhitespaceRunsWithNoCommaDoNotTriggerQuadraticScanning() {
        String value = "recipient" + " ".repeat(200_000);
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            assertEquals(List.of(value), StringCollectionValues.normalize(List.class, value));
            assertEquals(List.of(value), StringCollectionValues.normalize(List.class, "[" + value + "]"));
            assertEquals(List.of("recipient", "later"), StringCollectionValues.normalize(List.class, value + ",later"));
        });
    }
}
