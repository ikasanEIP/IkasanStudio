package org.ikasan.studio.model;

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

    public void test_parse_of_jmsIm_standard_module() {
        // @TODO this will migrate to JSON test
        System.out.println("Suspended till migration is complete");
    }
////        Module ikasanModule = Context.getMyFirstModuleIkasanModule(TEST_PROJECT_KEY);
////        final PsiClass moduleConfigClass = myJavaFacade.findClass("com.ikasan.sample.spring.boot.ModuleConfig", ProjectScope.getAllScope(myProject));
////        Assert.assertThat(moduleConfigClass, is(notNullValue()));
////        pipsiIkasanModel.setModuleConfigClazz(moduleConfigClass);
////
////        Assert.assertThat(ikasanModule.getComponentName(), is("jms-im"));
////        Assert.assertThat(ikasanModule.getDescription(), is("Sample Module"));
////        Assert.assertThat(ikasanModule.getFlows().size(), is(1));
////        Assert.assertThat(ikasanModule.getViewHandler(), is(notNullValue()));
////
////        List<Flow> flows = ikasanModule.getFlows();
////        Flow flow1 = flows.get(0);
////
////        Assert.assertThat(flow1.getViewHandler(), is(notNullValue()));
////        Assert.assertThat(flow1.getComponentName(), is("JMS to JMS Flow"));
////        Assert.assertThat(flow1.getDescription(), is("Flow demonstrates usage of JMS Concumer and JMS Producer"));
////
////        Assert.assertThat(flow1.getFlowElements().size(), is(3));
////
////        FlowElement jmsConsumer = flow1.getFlowElements().get(0);
////        Assert.assertThat(jmsConsumer.getComponentName(), is("JMS Consumer"));
////        Assert.assertThat(jmsConsumer.getConfiguredProperties().size(), is(5));
////        Assert.assertThat(getConfiguredPropertyValues(jmsConsumer.getConfiguredProperties()),
////                Matchers.is("AutoContentConversion->true,ConfiguredResourceId->jmsConsumer,ConnectionFactory->consumerConnectionFactory,DestinationJndiName->source,Name->JMS Consumer"));
////
////        FlowElement exceptionGeneratingBroker = flow1.getFlowElements().get(1);
////        Assert.assertThat(exceptionGeneratingBroker.getComponentName(), is("Exception Generating Broker"));
////        Assert.assertThat(getConfiguredPropertyValues(exceptionGeneratingBroker.getConfiguredProperties()),
////                Matchers.is("Name->Exception Generating Broker"));
////
////        FlowElement jmsProducer = flow1.getFlowElements().get(2);
////        Assert.assertThat(jmsProducer.getComponentName(), is("JMS Producer"));
////        Assert.assertThat(getConfiguredPropertyValues(jmsProducer.getConfiguredProperties()),
////                Matchers.is("ConfiguredResourceId->jmsProducer,ConnectionFactory->producerConnectionFactory,ConnectionFactoryJndiPropertyFactoryInitial->null,ConnectionFactoryName->null,ConnectionFactoryPassword->null," +
////                        "ConnectionFactoryUsername->null,DestinationJndiName->target,Name->JMS Producer"));
////
////        Assert.assertThat(((HashMap) flow1.getExceptionResolver().getIkasanExceptionResolutionMap()).size(), is(2));
////        Set actions = new TreeSet(((HashMap) flow1.getExceptionResolver().getIkasanExceptionResolutionMap()).keySet());
////        Assert.assertThat(actions.toString(), is("[com.ikasan.sample.spring.boot.SampleGeneratedException.class, org.ikasan.spec.component.endpoint.EndpointException.class]"));
//    }
}