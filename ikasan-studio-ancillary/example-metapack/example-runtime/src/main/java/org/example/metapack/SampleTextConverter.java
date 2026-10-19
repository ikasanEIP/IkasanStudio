package org.example.metapack;

/** Turns a scheduler tick into business data; scheduling stays owned by Ikasan. */
public final class SampleTextConverter implements org.ikasan.spec.component.transformation.Converter<Object, String> {
    @Override public String convert(Object tick) { return "Hello from the example pack"; }
}
