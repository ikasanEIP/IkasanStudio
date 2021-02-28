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

public class PropertiesTemplateTest extends TestCase {
    IkasanModule testModule;
    IkasanFlow ikasanFlow;

    @Before
    public void setUp() {
        testModule = new IkasanModule();
        testModule.setName("myModule");
        ikasanFlow = new IkasanFlow();
        testModule.addAnonymousFlow(ikasanFlow);
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/application_emptyFlow.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreatePropertiesVelocity_emptyFlow() throws IOException {
        String templateString = PropertiesTemplate.createPropertiesVelocity(testModule);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedVelocityOutputFromTestFile(PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_emptyFlow.properties")));
    }


    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/application_namelessAndNamed.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreatePropertiesVelocity_namelessAndNamedComponents() throws IOException {
        List<IkasanFlowComponent> components = ikasanFlow.getFlowComponentList() ;

        IkasanFlowComponent ftpConsumerComponent = IkasanFlowComponent.getInstance(IkasanFlowComponentType.FTP_CONSUMER, ikasanFlow);
        ftpConsumerComponent.setName("testFtpConsumer");
        ftpConsumerComponent.updatePropertyValue("FilenamePattern", "*Test.txt");
        components.add(ftpConsumerComponent);

        IkasanFlowComponent namelessFtpConsumerComponent = IkasanFlowComponent.getInstance(IkasanFlowComponentType.FTP_CONSUMER, ikasanFlow);
        namelessFtpConsumerComponent.updatePropertyValue("FilenamePattern", "*SecondTest.txt");
        components.add(namelessFtpConsumerComponent);

        String templateString = PropertiesTemplate.createPropertiesVelocity(testModule);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedVelocityOutputFromTestFile(PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_namelessAndNamed" + ".properties")));
    }

    /**
     * @See resources/studio/templates/org/ikasan/studio/generator/application_fullyPopulatedFtpComponent.properties
     * @throws IOException if the template cant be generated
     */
    @Test
    public void testCreatePropertiesVelocity_fullyPopulatedFtpComponent() throws IOException {
        ikasanFlow.getFlowComponentList().add(TestFixtures.getFullyPopulatedFtpComponent(ikasanFlow));

        String templateString = PropertiesTemplate.createPropertiesVelocity(testModule);
        Assert.assertThat(templateString, is(notNullValue()));
        Assert.assertThat(templateString, is(GeneratorTestUtils.getExptectedVelocityOutputFromTestFile(PropertiesTemplate.MODULE_PROPERTIES_FILENAME + "_fullyPopulatedFtpComponent.properties")));
    }
}