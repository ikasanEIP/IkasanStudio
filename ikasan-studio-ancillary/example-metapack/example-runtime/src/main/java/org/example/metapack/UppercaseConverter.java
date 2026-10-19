package org.example.metapack;

/** Stateless conversion; Locale.ROOT makes results independent of the host locale. */
public final class UppercaseConverter implements org.ikasan.spec.component.transformation.Converter<String, String> {
    @Override public String convert(String payload) {
        return java.util.Objects.requireNonNull(payload, "payload").toUpperCase(java.util.Locale.ROOT);
    }
}
