package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.ikasan.studio.model.Ikasan.IkasanFlow;
import org.ikasan.studio.model.Ikasan.IkasanFlowComponent;
import org.ikasan.studio.model.Ikasan.IkasanFlowComponentType;
import org.ikasan.studio.model.Ikasan.IkasanModule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class FlowsComponentFactoryTemplateTest extends TestCase {
    IkasanModule ikasanModule = new IkasanModule();
    IkasanFlow ikasanFlow = new IkasanFlow();
    private static String TEST_FLOW_NAME = "MyFlow1";
    private static String TEST_COMPONENT_FACTORY = "ComponentFactory";

    @Before
    public void setUp() {
        ikasanModule = new IkasanModule();
        ikasanFlow = new IkasanFlow();
        ikasanFlow.setName(TEST_FLOW_NAME);
        ikasanFlow.setDescription("MyFlowDescription");
    }

    @Test
    public void testCreateFlowWith_ftpConsumer() throws IOException {
        List<IkasanFlowComponent> components = ikasanFlow.getFlowComponentList() ;
        IkasanFlowComponent component = IkasanFlowComponent.getInstance(IkasanFlowComponentType.FTP_CONSUMER, ikasanFlow);
        component.setName("testFtpConsumer");
        component.updatePropertyValue("CronExpression", "*/5 * * * * ?");
        component.updatePropertyValue("FilenamePattern", "*Test.txt");
        component.setPropertyValue("Active", component.getType().getProperties().get("Active"), true);
        components.add(component);

        String templateString = FlowsComponentFactoryTemplate.generateContents(ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedVelocityOutputFromTestFile(TEST_COMPONENT_FACTORY + ".java")));
    }
}