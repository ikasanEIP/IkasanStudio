package org.ikasan.studio.model.ikasan;

import org.ikasan.studio.StudioUtils;

/**
 * The different elements (components) that might be in a flow
 */
public enum IkasanComponentCategory {
    MODULE(01, "", "org.ikasan.spec.module.Module"),
    FLOW(02, "", "org.ikasan.spec.flow.Flow"),
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

    IkasanComponentCategory(Integer displayOrder, String associatedMethodName, String associatedBaseClass) {
        this.displayOrder = displayOrder;
        this.associatedMethodName = associatedMethodName;
        this.baseClass = associatedBaseClass;
    }

    /**
     * Given a method name (broker, consumer etc), try to match its against a component Category
     * @param methodName to search
     * @return the IkasanComponentCategory or found or UNKNOWN
     */
    public static IkasanComponentCategory parseMethodName(String methodName) {
        for (IkasanComponentCategory name : IkasanComponentCategory.values()) {
            if (name.associatedMethodName.equals(methodName)) {
                return name;
            }
        }
        return UNKNOWN;
    }

    /**
     * Given a base class / interface, try to match it against a component Category
     * @param baseClassString to search (cannonical for Ikasan methods)
     * @return the IkasanComponentCategory or found or UNKNOWN
     */
    public static IkasanComponentCategory parseBaseClass(String baseClassString) {
        for (IkasanComponentCategory ikasanComponentCategory : IkasanComponentCategory.values()) {
            if (ikasanComponentCategory.baseClass.equals(baseClassString) || StudioUtils.getLastToken("\\.", ikasanComponentCategory.baseClass).equals(baseClassString)) {
                return ikasanComponentCategory;
            }
        }
        return UNKNOWN;
    }

    /**
     * Given a base class / interface, determine if it matches an IkasanComponentCategory
     * @param componentClassString to check
     * @return true if the componentClassString could be matched against an IkasanComponentCategory
     */
    public static boolean isIkasanComponent(String componentClassString) {
        IkasanComponentCategory ikasanComponentCategory = parseBaseClass(componentClassString);
        if (ikasanComponentCategory != null && ikasanComponentCategory != UNKNOWN) {
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