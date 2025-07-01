package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class AbstractGeneratorTestFixtures {

    public String generatePropertiesTemplateString(String metaPackVersion, Module module, FlowElement flowElement) throws StudioBuildException, StudioGeneratorException {
        Flow flow = TestFixtures.getUnbuiltFlow(metaPackVersion)
                .metapackVersion(metaPackVersion)
                .build();
        flowElement.setContainingFlowRoute(flow.getFlowRoute());
        flow.setFlowRoute(FlowRoute.flowRouteBuilder().flowElements(Collections.singletonList(flowElement)).flow(flow).build());
        module.addFlow(flow);
        String templateString = PropertiesTemplate.create(module);
        assertNotNull(templateString);
        return templateString;
    }


    public String generateFlowTemplateString(String metaPackVersion, Module module, FlowElement flowElement) throws StudioBuildException, StudioGeneratorException {
        Flow flow = TestFixtures.getUnbuiltFlow(metaPackVersion)
                .metapackVersion(metaPackVersion)
                .build();
        module.addFlow(flow);
        flowElement.setContainingFlowRoute(flow.getFlowRoute());
        flow.setFlowRoute(FlowRoute.flowRouteBuilder().flowElements(Collections.singletonList(flowElement)).flow(flow).build());
        return generateFlowTemlateStringForModule(module);
    }

    public String generateFlowTemlateStringForModule(Module module) throws StudioGeneratorException {
        String templateString = FlowTemplate.create(TestFixtures.DEFAULT_PACKAGE, module, module.getFlows().get(0));
        assertNotNull(templateString);
        return templateString;
    }

    public String generateUserImplementedComponentTemplate(String metaPackVersion, Module module, FlowElement flowElement) throws StudioBuildException, StudioGeneratorException {
        Flow flow = TestFixtures.getUnbuiltFlow(metaPackVersion)
                .metapackVersion(metaPackVersion)
                .build();
        module.addFlow(flow);
        flowElement.setContainingFlowRoute(flow.getFlowRoute());
        flow.setFlowRoute(FlowRoute.flowRouteBuilder().flowElements(Collections.singletonList(flowElement)).flow(flow).build());
        return generateUserImplementedComponentTemplateStringForModule(metaPackVersion, module, flowElement);
    }

    public String generateUserImplementedComponentTemplateStringForModule(String metaPackVersion, Module module, FlowElement flowElement) throws StudioGeneratorException {
        String templateString = FlowsUserImplementedComponentTemplate.create(TestFixtures.DEFAULT_PACKAGE, module, module.getFlows().get(0), flowElement);
        assertNotNull(templateString);
        return templateString;
    }


    public String generateFlowWithExceptionResolverTemplateString(String metaPackVersion, Module module, ExceptionResolver exceptionResolver) throws StudioBuildException, StudioGeneratorException {
        Flow flow = TestFixtures.getUnbuiltFlow(metaPackVersion)
                .metapackVersion(metaPackVersion)
                .exceptionResolver(exceptionResolver)
                .build();
        exceptionResolver.setContainingFlowRoute(flow.getFlowRoute());
        flow.setFlowRoute(FlowRoute.flowRouteBuilder().flow(flow).build());
        module.addFlow(flow);

        String templateString = FlowTemplate.create(TestFixtures.DEFAULT_PACKAGE, module, flow);
        assertNotNull(templateString);
        return templateString;
    }

    /**
     * Embed the supplied flow element in a standard flow/module, passing it to the FlowsComponentFactoryTemplate to generate the Java code
     * for the given flow element
     * @param flowElement to be tested
     * @return A string containing the generated code for the given flow element + standard module and flow wrapper.
     * @throws StudioBuildException if problems getting component details
     * @throws StudioGeneratorException if problems occurred during code generation
     */
    public String generateFlowsComponentFactoryTemplateString(String metaPackVersion, Module module, FlowElement flowElement) throws StudioBuildException, StudioGeneratorException {
        Flow flow = TestFixtures.getUnbuiltFlow(metaPackVersion)
                .metapackVersion(metaPackVersion)
                .build();
        module.addFlow(flow);

        if (flowElement.getComponentMeta().isConsumer()) {
            flow.setConsumer(flowElement);
        } else {
            flowElement.setContainingFlowRoute(flow.getFlowRoute());
            flow.setFlowRoute(FlowRoute.flowRouteBuilder().flowElements(Collections.singletonList(flowElement)).flow(flow).build());
        }
        String templateString = FlowsComponentFactoryTemplate.create(TestFixtures.DEFAULT_PACKAGE, module, flow);
        assertNotNull(templateString);
        return templateString;
    }
}
