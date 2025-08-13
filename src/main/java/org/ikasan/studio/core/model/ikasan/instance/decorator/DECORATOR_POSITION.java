package org.ikasan.studio.core.model.ikasan.instance.decorator;

/**
 * Enum representing the position of a decorator.
 *
 * A Decorator could be a wiretap, log wiretap
 *
 * Its position is either before a flow component (e,g, broker) or after the flow component
 */
public enum DECORATOR_POSITION {
    BEFORE,
    AFTER,
    UNKNOWN;

    /**
     * Safely retrieves the DECORATOR_POSITION enum constant by name.
     * Returns UNKNOWN if the name does not match any constant.
     *
     * @param name the name of the enum constant in its string from
     * @return the matching DECORATOR_POSITION or UNKNOWN
     */
    public static DECORATOR_POSITION safeValueOf(String name) {
        for (DECORATOR_POSITION decoratorPosition : DECORATOR_POSITION.values()) {
            if (decoratorPosition.name().equals(name)) {
                return decoratorPosition;
            }
        }
        return UNKNOWN;
    }
}
