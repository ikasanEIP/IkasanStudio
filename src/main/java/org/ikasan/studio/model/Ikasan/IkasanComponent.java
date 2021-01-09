package org.ikasan.studio.model.Ikasan;

import org.ikasan.studio.Utils;

/**
 * The different elements (components) that might be in a flow
 */
public enum IkasanComponent {
    BROKER("org.ikasan.spec.component.endpoint.Broker"),
    CONSUMER("org.ikasan.spec.component.endpoint.Consumer"),
    CONVERTER("org.ikasan.spec.component.transformation.Converter"),
    EXCEPTION_RESOLVER("org.ikasan.exceptionResolver.ExceptionResolver"),
    FILTER("org.ikasan.spec.component.filter.Filter"),
    PRODUCER("org.ikasan.spec.component.endpoint.Producer"),
    ROUTER("router"),
    SPLITTER("org.ikasan.spec.component.splitting.Splitter"),
    TRANSLATER("translater"),
    UNKNOWN("unknown");

    public final String baseClass;

    IkasanComponent(String associatedMethodName) {
        this.baseClass = associatedMethodName;
    }

    public static IkasanComponent parseMethodName(String methodName) {
        for (IkasanComponent name : IkasanComponent.values()) {
            if (name.baseClass.equals(methodName)) {
                return name;
            }
        }
        return UNKNOWN;
    }

    public static IkasanComponent parseBaseClass(String baseClassString) {
        for (IkasanComponent ikasanComponent : IkasanComponent.values()) {
            if (ikasanComponent.baseClass.equals(baseClassString) || Utils.getLastToken("\\.", ikasanComponent.baseClass).equals(baseClassString)) {
                return ikasanComponent;
            }
        }
        return UNKNOWN;
    }

    public static boolean isIkasanComponent(String componentClassString) {
        IkasanComponent ikasanComponent = parseBaseClass(componentClassString);
        if (ikasanComponent != null && ikasanComponent != UNKNOWN) {
            return true;
        } else {
            return false;
        }
    }
}
