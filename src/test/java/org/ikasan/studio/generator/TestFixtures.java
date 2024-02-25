package org.ikasan.studio.generator;

import org.ikasan.studio.model.ikasan.instance.Flow;
import org.ikasan.studio.model.ikasan.instance.FlowElement;
import org.ikasan.studio.model.ikasan.instance.FlowElementFactory;
import org.ikasan.studio.model.ikasan.instance.Module;

public class TestFixtures {
    public static final String DEFAULT_PACKAGE = "org.ikasan";

    public static Module getIkasanModule() {
        return Module.moduleBuilder()
                .name("My Integration Module")
                .applicationPackageName("org.myApp")
                .port("8080")
                .h2PortNumber("12452")
                .h2WebPortNumber("12452")
                .build();
    }

    /**
     * Create a fully populated FILTER
     * See resources/studio/componentDefinitions/MESSAGE_FILTER_en_GB.csv
     *
     * @return a FullyPopulatedCustomConverter
     */
    public static FlowElement getFullyPopulatedFilterComponent(Flow ikasanFlow) {
        FlowElement component = FlowElementFactory.createFlowElement(null, null);

//FlowElement component = FlowElement.createFlowElement(IkasanComponentMetax.MESSAGE_FILTER, ikasanFlow);
        component.setComponentName("testFilterComponent");

        // Mandatory properties
        component.setPropertyValue("BespokeClassName", "MyMessageFilter");
        component.setPropertyValue("FromType", java.lang.String.class);
        component.setPropertyValue("Configuration", "MyConfigurationClass");
        component.setPropertyValue("ConfiguredResourceId", "MyConfiguredResourceId");
        component.setPropertyValue("Description", "Test description");
        component.setPropertyValue("IsConfiguredResource", true);
        return component;
    }

    /**
     * Create a fully populated FILTER
     * See resources/studio/componentDefinitions/MESSAGE_FILTER_en_GB.csv
     *
     * @return a FullyPopulatedCustomConverter
     */
    public static FlowElement getMinimumPopulatedFilterComponent(Flow ikasanFlow) {
        FlowElement component = FlowElementFactory.createFlowElement(null, null);

//FlowElement component = FlowElement.createFlowElement(IkasanComponentMetax.MESSAGE_FILTER, ikasanFlow);
        component.setComponentName("testFilterComponent");

        // Mandatory properties
        component.setPropertyValue("BespokeClassName", "MyMessageFilter");
        component.setPropertyValue("FromType", java.lang.String.class);
        return component;
    }

}
