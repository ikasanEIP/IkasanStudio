package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.ikasan.studio.model.Ikasan.IkasanFlow;
import org.ikasan.studio.model.Ikasan.IkasanModule;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class FlowTemplateTest extends TestCase {

    @Test
    public void testCreateModuleVelocityWith_oneFlow() throws IOException {
        String TEST_FLOW_NAME = "MyFlow1";
        IkasanModule ikasanModule = new IkasanModule();
        IkasanFlow ikasanFlow = new IkasanFlow();
        ikasanFlow.setName(TEST_FLOW_NAME);
        ikasanFlow.setDescription("MyFlowDescription");

        String templateString = FlowTemplate.generateContents(ikasanModule, ikasanFlow);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedVelocityOutputFromTestFile(TEST_FLOW_NAME + "_oneFlow.java")));
    }
}
