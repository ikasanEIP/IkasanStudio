package org.ikasan.studio.model.Ikasan;

import org.ikasan.studio.StudioUtils;

/**
 * The different elements (components) that might be in a flow
 */
public enum IkasanFlowComponentCategory {
    BROKER("broker", "org.ikasan.spec.component.endpoint.Broker"),
    CONSUMER("consumer", "org.ikasan.spec.component.endpoint.Consumer"),
    CONVERTER("converter", "org.ikasan.spec.component.transformation.Converter"),
    DESCRIPTION("withDescription", "String"),
    ENDPOINT("endPoint", "String"),
    EXCEPTION_RESOLVER("exceptionResolver", "org.ikasan.exceptionResolver.ExceptionResolver"),
    FILTER("filter", "org.ikasan.spec.component.filter.Filter"),
    PRODUCER("producer", "org.ikasan.spec.component.endpoint.Producer"),
    ROUTER("router", "router"),
    SPLITTER("splitter", "org.ikasan.spec.component.splitting.Splitter"),
    TRANSLATER("translater", "translater"),
    UNKNOWN("unknown", "unknown");

    public final String associatedMethodName;
    public final String baseClass;

    IkasanFlowComponentCategory(String associatedMethodName, String associatedBaseClass) {
        this.associatedMethodName = associatedMethodName;
        this.baseClass = associatedBaseClass;
    }

    public static IkasanFlowComponentCategory parseMethodName(String methodName) {
        for (IkasanFlowComponentCategory name : IkasanFlowComponentCategory.values()) {
            if (name.associatedMethodName.equals(methodName)) {
                return name;
            }
        }
        return UNKNOWN;
    }

    public static IkasanFlowComponentCategory parseBaseClass(String baseClassString) {
        for (IkasanFlowComponentCategory ikasanFlowComponentCategory : IkasanFlowComponentCategory.values()) {
            if (ikasanFlowComponentCategory.baseClass.equals(baseClassString) || StudioUtils.getLastToken("\\.", ikasanFlowComponentCategory.baseClass).equals(baseClassString)) {
                return ikasanFlowComponentCategory;
            }
        }
        return UNKNOWN;
    }

    public static boolean isIkasanComponent(String componentClassString) {
        IkasanFlowComponentCategory ikasanFlowComponentCategory = parseBaseClass(componentClassString);
        if (ikasanFlowComponentCategory != null && ikasanFlowComponentCategory != UNKNOWN) {
            return true;
        } else {
            return false;
        }
    }
}
