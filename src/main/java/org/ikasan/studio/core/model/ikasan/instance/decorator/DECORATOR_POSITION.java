package org.ikasan.studio.core.model.ikasan.instance.decorator;

public enum DECORATOR_POSITION {
    BEFORE, AFTER, UNKNOWN;

    public static DECORATOR_POSITION safeValueOf(String name) {
        for (DECORATOR_POSITION decoratorPosition : DECORATOR_POSITION.values()) {
            if (decoratorPosition.name().equals(name)) {
                return decoratorPosition;
            }
        }
        return UNKNOWN;
    }
}
