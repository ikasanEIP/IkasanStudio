package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.ikasan.studio.model.Ikasan.IkasanFlow;
import org.ikasan.studio.model.Ikasan.IkasanFlowComponent;
import org.ikasan.studio.model.Ikasan.IkasanFlowComponentType;
import org.ikasan.studio.model.Ikasan.IkasanModule;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class PropertiesTemplateTest extends TestCase {

    @Test
    public void testCreatePropertiesVelocity() throws IOException {
        IkasanModule testModule = new IkasanModule();
        testModule.setName("myModule");
        IkasanFlow ikasanFlow = new IkasanFlow();
        testModule.addAnonymousFlow(ikasanFlow);
        IkasanFlowComponent ftpConsumerComponent = IkasanFlowComponent.getInstance(IkasanFlowComponentType.FTP_CONSUMER, ikasanFlow);
        ftpConsumerComponent.setName("testFtpConsumer");
        ftpConsumerComponent.updatePropertyValue("CronExpression", "*/5 * * * * ?");
        ftpConsumerComponent.updatePropertyValue("FilenamePattern", "*Test.txt");
        ftpConsumerComponent.setPropertyValue("Active", ftpConsumerComponent.getType().getProperties().get("Active"), true);
        List<IkasanFlowComponent> components = ikasanFlow.getFlowComponentList() ;
        components.add(ftpConsumerComponent);

        IkasanFlowComponent namelessFtpConsumerComponent = IkasanFlowComponent.getInstance(IkasanFlowComponentType.FTP_CONSUMER, ikasanFlow);
        namelessFtpConsumerComponent.updatePropertyValue("FilenamePattern", "*SecondTest.txt");
        components.add(namelessFtpConsumerComponent);

        String templateString = PropertiesTemplate.createPropertiesVelocity(testModule);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedVelocityOutputFromTestFile(PropertiesTemplate.MODULE_PROPERTIES_FILENAME + ".properties")));
    }
}