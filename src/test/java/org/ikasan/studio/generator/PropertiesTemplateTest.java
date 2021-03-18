package org.ikasan.studio.generator;

import junit.framework.TestCase;
import org.ikasan.studio.model.ikasan.IkasanComponentType;
import org.ikasan.studio.model.ikasan.IkasanFlow;
import org.ikasan.studio.model.ikasan.IkasanFlowComponent;
import org.ikasan.studio.model.ikasan.IkasanModule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class PropertiesTemplateTest extends TestCase {
    IkasanModule testModule;
    IkasanFlow ikasanFlow;

    @Before
    public void setUp() {
        testModule = TestFixtures.getIkasanModule();
        testModule.setName("myModule");
        ikasanFlow = new IkasanFlow();
        ikasanFlow.setName("newFlow1");
        testModule.addFlow(ikasanFlow);
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/application_emptyFlow.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateProperties_emptyFlow() throws IOException {
        String templateString = PropertiesTemplate.generateContents(testModule);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_emptyFlow.properties")));
    }


    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/application_namelessAndNamed.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateProperties_namelessAndNamedComponents() throws IOException {
        List<IkasanFlowComponent> components = ikasanFlow.getFlowComponentList() ;

        IkasanFlowComponent ftpConsumerComponent = IkasanFlowComponent.getInstance(IkasanComponentType.FTP_CONSUMER, ikasanFlow);
        ftpConsumerComponent.setName("testFtpConsumer");
        ftpConsumerComponent.updatePropertyValue("FilenamePattern", "*Test.txt");
        components.add(ftpConsumerComponent);
        ftpConsumerComponent.getStandardProperties().size();
        IkasanFlowComponent namelessFtpConsumerComponent = IkasanFlowComponent.getInstance(IkasanComponentType.FTP_CONSUMER, ikasanFlow);
        namelessFtpConsumerComponent.updatePropertyValue("FilenamePattern", "*SecondTest.txt");
        components.add(namelessFtpConsumerComponent);

        String templateString = PropertiesTemplate.generateContents(testModule);

        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_namelessAndNamed" + ".properties")));
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/application_fullyPopulatedFtpComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateProperties_fullyPopulatedFtpComponent() throws IOException {
        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedFtpComponent(ikasanFlow));

        String templateString = PropertiesTemplate.generateContents(testModule);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedFtpComponent.properties")));
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/application_fullyPopulatedSftpComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreateProperties_fullyPopulatedSftpComponent() throws IOException {
        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedSftpComponent(ikasanFlow));

        String templateString = PropertiesTemplate.generateContents(testModule);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedFreemarkerOutputFromTestFile(PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedSftpComponent.properties")));
    }
}