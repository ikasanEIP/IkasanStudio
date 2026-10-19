package org.example.metapack;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ComponentsTest {
    @Test void convertsUsingStableLocaleAndRejectsNull() {
        var converter = new UppercaseConverter();
        assertEquals("HELLO", converter.convert("Hello"));
        assertEquals("", converter.convert(""));
        assertThrows(NullPointerException.class, () -> converter.convert(null));
    }
    @Test void sampleTickBecomesUppercaseGreeting() {
        assertEquals("HELLO FROM THE EXAMPLE PACK", new UppercaseConverter().convert(new SampleTextConverter().convert(new Object())));
    }
    @Test void producerAcceptsConvertedPayload() { new LoggingProducer().invoke(new UppercaseConverter().convert("hello")); }
}
