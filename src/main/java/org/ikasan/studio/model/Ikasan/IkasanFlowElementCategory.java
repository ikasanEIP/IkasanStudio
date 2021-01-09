package org.ikasan.studio.model.Ikasan;

/**
 * The different elements (components) that might be in a flow
 */
public enum IkasanFlowElementCategory {
    BROKER("broker"),
    CONSUMER("consumer"),
    CONVERTER("converter"),
    DESCRIPTION("withDescription"),
    ENDPOINT("endPoint"),
    FILTER("filter"),
    PRODUCER("producer"),
    ROUTER("router"),
    SPLITTER("splitter"),
    TRANSLATER("translater"),
    UNKNOWN("unknown");

    public final String associatedMethodName;

    IkasanFlowElementCategory(String associatedMethodName) {
        this.associatedMethodName = associatedMethodName;
    }

    public static IkasanFlowElementCategory parseMethodName(String methodName) {
        for (IkasanFlowElementCategory name : IkasanFlowElementCategory.values()) {
            if (name.associatedMethodName.equals(methodName)) {
                return name;
            }
        }
        return UNKNOWN;
    }
}
