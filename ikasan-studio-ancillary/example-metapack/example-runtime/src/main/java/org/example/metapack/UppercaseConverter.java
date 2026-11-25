package org.example.metapack;

import java.util.Locale;
import java.util.Objects;
import org.ikasan.spec.component.transformation.Converter;

/** Stateless conversion; Locale.ROOT makes results independent of the host locale. */
public final class UppercaseConverter implements Converter<String, String> {
    @Override public String convert(String payload) {
        return Objects.requireNonNull(payload, "payload").toUpperCase(Locale.ROOT);
    }
}
