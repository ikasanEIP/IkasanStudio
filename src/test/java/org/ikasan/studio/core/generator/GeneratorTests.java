package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GeneratorTests {
    Module module;

    public String generatePropertiesTemplateString(FlowElement flowElement) throws StudioBuildException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .metapackVersion(TestFixtures.TEST_IKASAN_PACK)
                .build();
        flow.setFlowRoute(FlowRoute.flowBuilder().flowElements(Collections.singletonList(flowElement)).flow(flow).build());
        module.addFlow(flow);
        String templateString = PropertiesTemplate.generateContents(module);
        assertNotNull(templateString);
        return templateString;
    }


    public String generateFlowTemplateString(FlowElement flowElement) throws StudioBuildException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .metapackVersion(TestFixtures.TEST_IKASAN_PACK)
                .build();
        module.addFlow(flow);
        flow.setFlowRoute(FlowRoute.flowBuilder().flowElements(Collections.singletonList(flowElement)).flow(flow).build());
        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, module, flow);
        assertNotNull(templateString);
        return templateString;
    }

    public String generateFlowWithExceptionResolverTemplateString(ExceptionResolver exceptionResolver) throws StudioBuildException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .metapackVersion(TestFixtures.TEST_IKASAN_PACK)
                .exceptionResolver(exceptionResolver)
                .build();
        module.addFlow(flow);

        String templateString = FlowTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, module, flow);
        assertNotNull(templateString);
        return templateString;
    }

    public String generateFlowsComponentFactoryTemplateString(FlowElement flowElement) throws StudioBuildException {
        Flow flow = TestFixtures.getUnbuiltFlow()
                .metapackVersion(TestFixtures.TEST_IKASAN_PACK)
                .build();
        module.addFlow(flow);
        flow.setFlowRoute(FlowRoute.flowBuilder().flowElements(Collections.singletonList(flowElement)).flow(flow).build());
        String templateString = FlowsComponentFactoryTemplate.generateContents(TestFixtures.DEFAULT_PACKAGE, module, flow);
        assertNotNull(templateString);
        return templateString;
    }
}
