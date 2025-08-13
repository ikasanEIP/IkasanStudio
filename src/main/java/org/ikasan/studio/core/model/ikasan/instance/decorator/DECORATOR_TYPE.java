package org.ikasan.studio.core.model.ikasan.instance.decorator;

/**
 * Enum representing the type of decorator.
 *
 * A Decorator could be a wiretap, log wiretap, or an unknown type.
 */
public enum DECORATOR_TYPE {
    Wiretap, LogWiretap, Unknown;

    public static DECORATOR_TYPE safeValueOf(String name) {
        for (DECORATOR_TYPE decoratorType : DECORATOR_TYPE.values()) {
            if (decoratorType.name().equals(name)) {
                return decoratorType;
            }
        }
        return Unknown;
    }
}
