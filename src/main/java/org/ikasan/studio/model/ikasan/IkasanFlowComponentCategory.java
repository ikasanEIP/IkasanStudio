package org.ikasan.studio.model.ikasan;

import org.ikasan.studio.StudioUtils;

/**
 * The different elements (components) that might be in a flow
 */
public enum IkasanFlowComponentCategory {
    BROKER(20, "broker", "org.ikasan.spec.component.endpoint.Broker"),
    CONSUMER(10,"consumer", "org.ikasan.spec.component.endpoint.Consumer"),
    CONVERTER(30, "converter", "org.ikasan.spec.component.transformation.Converter"),
    DESCRIPTION(1001, "withDescription", "String"),
    ENDPOINT(1002, "endPoint", "String"),
    EXCEPTION_RESOLVER(90,"exceptionResolver", "org.ikasan.exceptionResolver.ExceptionResolver"),
    FILTER(40, "filter", "org.ikasan.spec.component.filter.Filter"),
    PRODUCER(80, "producer", "org.ikasan.spec.component.endpoint.Producer"),
    ROUTER(50, "router", "router"),
    SPLITTER(60, "splitter", "org.ikasan.spec.component.splitting.Splitter"),
    TRANSLATER(70, "translater", "translater"),
    UNKNOWN(100, "unknown", "unknown");

    private final Integer displayOrder;
    public final String associatedMethodName;
    public final String baseClass;

    IkasanFlowComponentCategory(Integer displayOrder, String associatedMethodName, String associatedBaseClass) {
        this.displayOrder = displayOrder;
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

    public String getAssociatedMethodName() {
        return associatedMethodName;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public String getBaseClass() {
        return baseClass;
    }
}
