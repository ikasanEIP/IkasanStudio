package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.instance.*;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class AbstractGeneratorTestFixtures {
    Module module;

    public String generatePropertiesTemplateString(FlowElement flowElement) throws StudioBuildException, StudioGeneratorException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .metapackVersion(TestFixtures.TEST_IKASAN_PACK)
                .build();
        flowElement.setContainingFlowRoute(flow.getFlowRoute());
        flow.setFlowRoute(FlowRoute.flowRouteBuilder().flowElements(Collections.singletonList(flowElement)).flow(flow).build());
        module.addFlow(flow);
        String templateString = PropertiesTemplate.create(module);
        assertNotNull(templateString);
        return templateString;
    }


    public String generateFlowTemplateString(FlowElement flowElement) throws StudioBuildException, StudioGeneratorException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .metapackVersion(TestFixtures.TEST_IKASAN_PACK)
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

    public String generateUserImplementedComponentTemplate(FlowElement flowElement) throws StudioBuildException, StudioGeneratorException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .metapackVersion(TestFixtures.TEST_IKASAN_PACK)
                .build();
        module.addFlow(flow);
        flowElement.setContainingFlowRoute(flow.getFlowRoute());
        flow.setFlowRoute(FlowRoute.flowRouteBuilder().flowElements(Collections.singletonList(flowElement)).flow(flow).build());
        return generateUserImplementedComponentTemplateStringForModule(module, flowElement);
    }

    public String generateUserImplementedComponentTemplateStringForModule(Module module, FlowElement flowElement) throws StudioGeneratorException {
        String templateString = FlowsUserImplementedComponentTemplate.create(TestFixtures.DEFAULT_PACKAGE, module, module.getFlows().get(0), flowElement);
        assertNotNull(templateString);
        return templateString;
    }


    public String generateFlowWithExceptionResolverTemplateString(ExceptionResolver exceptionResolver) throws StudioBuildException, StudioGeneratorException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .metapackVersion(TestFixtures.TEST_IKASAN_PACK)
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
    public String generateFlowsComponentFactoryTemplateString(FlowElement flowElement) throws StudioBuildException, StudioGeneratorException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .metapackVersion(TestFixtures.TEST_IKASAN_PACK)
                .build();
        module.addFlow(flow);

        if (flowElement.getComponentMeta().isConsumer()) {
            flow.setConsumer(flowElement);
        } else {
            flowElement.setContainingFlowRoute(flow.getFlowRoute());
            flow.setFlowRoute(FlowRoute.flowRouteBuilder().flowElements(Collections.singletonList(flowElement)).flow(flow).build());
        }
//        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, module, flow);
        String templateString = FlowsComponentFactoryTemplate.create(TestFixtures.DEFAULT_PACKAGE, module, flow);
        assertNotNull(templateString);
        return templateString;
    }
}
