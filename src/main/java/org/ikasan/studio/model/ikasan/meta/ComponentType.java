package org.ikasan.studio.model.ikasan.meta;

import org.ikasan.studio.StudioUtils;

/**
 * The component types are so fundament to Ikasan, we can include them here, if ever they change between
 * versions we will need to re-work using the version meta pack.
 */
public enum ComponentType
{
    Broker("org.ikasan.spec.component.endpoint.Broker"),
    Consumer("org.ikasan.spec.component.endpoint.Consumer"),
    Converter("org.ikasan.spec.component.transformation.Converter"),
    ExceptionResolver("org.ikasan.exceptionResolver.ExceptionResolver"),
    Filer("org.ikasan.spec.component.filter.Filter"),
    Flow("org.ikasan.spec.flow.Flow"),
    Module("org.ikasan.spec.module.Module"),
    Producer("org.ikasan.spec.component.endpoint.Producer"),
    Router("Router"),
    Splitter("org.ikasan.spec.component.splitting.Splitter"),
    Translater ("Translater"),
    Other ("Other");

    public final String classType;
    
    ComponentType(String classType) {
        this.classType = classType;
    }

    /**
     * Given a classTypeString, try to match it against a ComponentType
     * @param classTypeString to search
     * @return the ComponentType or found or Other
     */
    public static ComponentType parseClassType(String classTypeString) {
        for (ComponentType componentType : ComponentType.values()) {
            if (componentType.classType.equals(classTypeString) || StudioUtils.getLastToken("\\.", componentType.classType).equals(classTypeString)) {
                return componentType;
            }
        }
        return Other;
    }

    /**
     * Given a stringContainingComponentType, try to match it against a ComponentType
     * @param stringContainingComponentType to search
     * @return the ComponentType or found or Other
     */
    public static ComponentType parseComponentTypeContains(String stringContainingComponentType) {
        for (ComponentType componentType : ComponentType.values()) {
            if (stringContainingComponentType.contains(componentType.toString())) {
                return componentType;
            }
        }
        return Other;
    }
}
