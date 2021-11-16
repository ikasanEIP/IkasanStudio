package org.ikasan.studio.model;

import com.intellij.psi.PsiClass;
import com.intellij.psi.search.ProjectScope;
import org.hamcrest.Matchers;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.IkasanFlow;
import org.ikasan.studio.model.ikasan.IkasanFlowComponent;
import org.ikasan.studio.model.ikasan.IkasanModule;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.ikasan.studio.generator.TestUtils.getConfiguredPropertyValues;

public class PIPSIIkasanModelImsImTest extends PIPSIIkasanModelAbstractTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    // This callback will set the path to the correct data file used in the test
    public String getTestDataDir() {
        return "/ikasanStandardSampleApps/jmsIm/";
    }
    @Test
    public void test_parse_of_jmsIm_standard_module() {
        IkasanModule ikasanModule = Context.getIkasanModule(TEST_PROJECT_KEY);
        final PsiClass moduleConfigClass = myJavaFacade.findClass("com.ikasan.sample.spring.boot.ModuleConfig", ProjectScope.getAllScope(myProject));
        Assert.assertThat(moduleConfigClass, is(notNullValue()));
        pipsiIkasanModel.setModuleConfigClazz(moduleConfigClass);
        pipsiIkasanModel.updateIkasanModule();

        Assert.assertThat(ikasanModule.getName(), is("jms-im"));
        Assert.assertThat(ikasanModule.getDescription(), is("Sample Module"));
        Assert.assertThat(ikasanModule.getFlows().size(), is(1));
        Assert.assertThat(ikasanModule.getViewHandler(), is(notNullValue()));

        List<IkasanFlow> flows = ikasanModule.getFlows();
        IkasanFlow flow1 = flows.get(0);

        Assert.assertThat(flow1.getViewHandler(), is(notNullValue()));
        Assert.assertThat(flow1.getName(), is("JMS to JMS Flow"));
        Assert.assertThat(flow1.getDescription(), is("Flow demonstrates usage of JMS Concumer and JMS Producer"));

        Assert.assertThat(flow1.getFlowComponentList().size(), is(3));

        IkasanFlowComponent jmsConsumer = flow1.getFlowComponentList().get(0);
        Assert.assertThat(jmsConsumer.getName(), is("JMS Consumer"));
        Assert.assertThat(jmsConsumer.getConfiguredProperties().size(), is(5));
        Assert.assertThat(getConfiguredPropertyValues(jmsConsumer.getConfiguredProperties()),
                Matchers.is("AutoContentConversion->true,ConfiguredResourceId->jmsConsumer,ConnectionFactory->consumerConnectionFactory,DestinationJndiName->source,Name->JMS Consumer"));

        IkasanFlowComponent exceptionGeneratingBroker = flow1.getFlowComponentList().get(1);
        Assert.assertThat(exceptionGeneratingBroker.getName(), is("Exception Generating Broker"));
        Assert.assertThat(getConfiguredPropertyValues(exceptionGeneratingBroker.getConfiguredProperties()),
                Matchers.is("Name->Exception Generating Broker"));

        IkasanFlowComponent jmsProducer = flow1.getFlowComponentList().get(2);
        Assert.assertThat(jmsProducer.getName(), is("JMS Producer"));
        Assert.assertThat(getConfiguredPropertyValues(jmsProducer.getConfiguredProperties()),
                Matchers.is("ConfiguredResourceId->jmsProducer,ConnectionFactory->producerConnectionFactory,ConnectionFactoryJndiPropertyFactoryInitial->org.apache.activemq.jndi.ActiveMQInitialContextFactory," +
                        "ConnectionFactoryName->ConnectionFactory,ConnectionFactoryPassword->admin,ConnectionFactoryUsername->admin,DestinationJndiName->target,Name->JMS Producer"));

        Assert.assertThat(((HashMap) flow1.getIkasanExceptionResolver().getIkasanExceptionResolutionMap()).size(), is(2));
        Set actions = new TreeSet(((HashMap) flow1.getIkasanExceptionResolver().getIkasanExceptionResolutionMap()).keySet());
        Assert.assertThat(actions.toString(), is("[com.ikasan.sample.spring.boot.SampleGeneratedException.class, org.ikasan.spec.component.endpoint.EndpointException.class]"));
    }
}