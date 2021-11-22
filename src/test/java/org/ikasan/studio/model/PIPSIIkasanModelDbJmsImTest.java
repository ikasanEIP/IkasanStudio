package org.ikasan.studio.model;

import com.intellij.psi.PsiClass;
import com.intellij.psi.search.ProjectScope;
import org.hamcrest.Matchers;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.IkasanFlow;
import org.ikasan.studio.model.ikasan.IkasanModule;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.ikasan.studio.generator.TestUtils.getConfiguredPropertyValues;

public class PIPSIIkasanModelDbJmsImTest extends PIPSIIkasanModelAbstractTest {
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
        return "/ikasanStandardSampleApps/dbJmsIm/";
    }
    @Test
    public void test_parse_of_dbJmsIm_standard_module() {
        IkasanModule ikasanModule = Context.getIkasanModule(TEST_PROJECT_KEY);
        final PsiClass moduleConfigClass = myJavaFacade.findClass("com.ikasan.sample.spring.boot.ModuleFactory", ProjectScope.getAllScope(myProject));
        Assert.assertThat(moduleConfigClass, is(notNullValue()));
        pipsiIkasanModel.setModuleConfigClazz(moduleConfigClass);
        pipsiIkasanModel.updateIkasanModule();

        Assert.assertThat(ikasanModule.getName(), is("myIntegrationModule"));
        Assert.assertThat(ikasanModule.getDescription(), is("Sample DB consumer / producer module."));
        Assert.assertThat(ikasanModule.getFlows().size(), is(2));
        Assert.assertThat(ikasanModule.getViewHandler(), is(notNullValue()));

        List<IkasanFlow> flows = ikasanModule.getFlows();

        {   // scope protection
            IkasanFlow flow1 = flows.get(0);
            Assert.assertThat(flow1.getViewHandler(), is(notNullValue()));
            Assert.assertThat(flow1.getName(), is("dbToJMSFlow"));
            Assert.assertThat(flow1.getDescription(), is("Sample DB to JMS flow"));
//            Assert.assertThat(flow1.getOutput().getDescription(), is("jms.topic.test"));

//            Assert.assertThat(flow1.getFlowComponentList().size(), is(5));

            Assert.assertThat(getConfiguredPropertyValues(flow1.getFlowComponentList().get(0).getConfiguredProperties()),
                    Matchers.is("ConfiguredResourceId->null,CronExpression->null,MessageProvider->personMessageProvider,Name->DB Consumer"));

            Assert.assertThat(getConfiguredPropertyValues(flow1.getFlowComponentList().get(1).getConfiguredProperties()),
                    is("BespokeClassName->MyFilter,Configuration->com.ikasan.sample.spring.boot.MyFilterConfiguration,FromType->List<Person>,IsConfiguredResource->true,Name->My Filter"));

            Assert.assertThat(getConfiguredPropertyValues(flow1.getFlowComponentList().get(2).getConfiguredProperties()),
                    is("Name->Split list"));

            Assert.assertThat(getConfiguredPropertyValues(flow1.getFlowComponentList().get(3).getConfiguredProperties()),
                    is("Name->Person to XML,ObjectClass->Person.class"));

            Assert.assertThat(getConfiguredPropertyValues(flow1.getFlowComponentList().get(4).getConfiguredProperties()),
                    is("ConfiguredResourceId->null,ConnectionFactory->connectionFactory,ConnectionFactoryJndiPropertyFactoryInitial->null," +
                                    "ConnectionFactoryName->null,ConnectionFactoryPassword->null,ConnectionFactoryUsername->null,DeliveryMode->DeliveryMode.PERSISTENT," +
                                    "DeliveryPersistent->true,DestinationJndiName->jms.topic.test,ExplicitQosEnabled->true,MessageIdEnabled->true," +
                                    "MessageTimestampEnabled->true,Name->JMS Producer,PubSubDomain->false,SessionAcknowledgeMode->Session.SESSION_TRANSACTED,SessionTransacted->true"));

            Assert.assertThat(((HashMap) flow1.getIkasanExceptionResolver().getIkasanExceptionResolutionMap()).get("org.ikasan.spec.component.endpoint.EndpointException.class").toString(),
                    is("IkasanExceptionResoluation{theException=org.ikasan.spec.component.endpoint.EndpointException.class, theAction='retry', params=[IkasanComponentProperty{value=null, meta=IkasanComponentPropertyMeta{paramGroupNumber=1, causesUserCodeRegeneration=false, mandatory=true, userImplementedClass=false, userDefineResource=false, propertyName='delay', propertyConfigFileLabel='', propertyDataType=class java.lang.Integer, usageDataType=java.lang.Integer, validation=, validationMessage=null, validationPattern=null, defaultValue=100, helpText='The period to wait between retries.'}}, IkasanComponentProperty{value=null, meta=IkasanComponentPropertyMeta{paramGroupNumber=1, causesUserCodeRegeneration=false, mandatory=true, userImplementedClass=false, userDefineResource=false, propertyName='interval', propertyConfigFileLabel='', propertyDataType=class java.lang.Integer, usageDataType=java.lang.Integer, validation=, validationMessage=null, validationPattern=null, defaultValue=10, helpText='The maximum number of retries.'}}]}"));
            Assert.assertThat(((HashMap) flow1.getIkasanExceptionResolver().getIkasanExceptionResolutionMap()).get("org.ikasan.spec.component.splittingSplitterException.class").toString(),
                    is("IkasanExceptionResoluation{theException=org.ikasan.spec.component.splittingSplitterException.class, theAction='excludeEvent', params=[IkasanComponentProperty{value=null, meta=IkasanComponentPropertyMeta{paramGroupNumber=1, causesUserCodeRegeneration=false, mandatory=false, userImplementedClass=false, userDefineResource=false, propertyName='excludeEvent', propertyConfigFileLabel='', propertyDataType=null, usageDataType=null, validation=, validationMessage=null, validationPattern=null, defaultValue=null, helpText='Rollback any actions resulting from this inflight event and exclude it.'}}]}"));
            Assert.assertThat(((HashMap) flow1.getIkasanExceptionResolver().getIkasanExceptionResolutionMap()).get("javax.resource.ResourceException.class").toString(),
                    is("IkasanExceptionResoluation{theException=javax.resource.ResourceException.class, theAction='scheduledCronRetry', params=[IkasanComponentProperty{value=* * * * *, meta=IkasanComponentPropertyMeta{paramGroupNumber=1, causesUserCodeRegeneration=false, mandatory=true, userImplementedClass=false, userDefineResource=false, propertyName='cronExpression', propertyConfigFileLabel='', propertyDataType=class java.lang.String, usageDataType=java.lang.String, validation=, validationMessage=null, validationPattern=null, defaultValue=* * * * * *, helpText='The cron expression.'}}, IkasanComponentProperty{value=100, meta=IkasanComponentPropertyMeta{paramGroupNumber=1, causesUserCodeRegeneration=false, mandatory=true, userImplementedClass=false, userDefineResource=false, propertyName='maxRetries', propertyConfigFileLabel='', propertyDataType=class java.lang.Integer, usageDataType=java.lang.Integer, validation=, validationMessage=null, validationPattern=null, defaultValue=10, helpText='The maximum number of retries.'}}]}"));
            Assert.assertThat(((HashMap) flow1.getIkasanExceptionResolver().getIkasanExceptionResolutionMap()).get("org.ikasan.spec.component.filter.FilterException.class").toString(),
                    is("IkasanExceptionResoluation{theException=org.ikasan.spec.component.filter.FilterException.class, theAction='stop', params=[IkasanComponentProperty{value=null, meta=IkasanComponentPropertyMeta{paramGroupNumber=1, causesUserCodeRegeneration=false, mandatory=false, userImplementedClass=false, userDefineResource=false, propertyName='stop', propertyConfigFileLabel='', propertyDataType=null, usageDataType=null, validation=, validationMessage=null, validationPattern=null, defaultValue=null, helpText='Cause the flow to halt.'}}]}"));
            Assert.assertThat(((HashMap) flow1.getIkasanExceptionResolver().getIkasanExceptionResolutionMap()).get("javax.jms.JMSException.class").toString(),
                    is("IkasanExceptionResoluation{theException=javax.jms.JMSException.class, theAction='retry', params=[IkasanComponentProperty{value=100, meta=IkasanComponentPropertyMeta{paramGroupNumber=1, causesUserCodeRegeneration=false, mandatory=true, userImplementedClass=false, userDefineResource=false, propertyName='delay', propertyConfigFileLabel='', propertyDataType=class java.lang.Integer, usageDataType=java.lang.Integer, validation=, validationMessage=null, validationPattern=null, defaultValue=100, helpText='The period to wait between retries.'}}, IkasanComponentProperty{value=null, meta=IkasanComponentPropertyMeta{paramGroupNumber=1, causesUserCodeRegeneration=false, mandatory=true, userImplementedClass=false, userDefineResource=false, propertyName='interval', propertyConfigFileLabel='', propertyDataType=class java.lang.Integer, usageDataType=java.lang.Integer, validation=, validationMessage=null, validationPattern=null, defaultValue=10, helpText='The maximum number of retries.'}}]}"));
            Assert.assertThat(((HashMap) flow1.getIkasanExceptionResolver().getIkasanExceptionResolutionMap()).get("TimeoutException.class").toString(),
                    is("IkasanExceptionResoluation{theException=TimeoutException.class, theAction='stop', params=[IkasanComponentProperty{value=null, meta=IkasanComponentPropertyMeta{paramGroupNumber=1, causesUserCodeRegeneration=false, mandatory=false, userImplementedClass=false, userDefineResource=false, propertyName='stop', propertyConfigFileLabel='', propertyDataType=null, usageDataType=null, validation=, validationMessage=null, validationPattern=null, defaultValue=null, helpText='Cause the flow to halt.'}}]}"));
            Assert.assertThat(((HashMap) flow1.getIkasanExceptionResolver().getIkasanExceptionResolutionMap()).get("org.ikasan.spec.component.transformation.TransformationException.class").toString(),
                    is("IkasanExceptionResoluation{theException=org.ikasan.spec.component.transformation.TransformationException.class, theAction='ignoreException', params=[IkasanComponentProperty{value=null, meta=IkasanComponentPropertyMeta{paramGroupNumber=1, causesUserCodeRegeneration=false, mandatory=false, userImplementedClass=false, userDefineResource=false, propertyName='ignoreException', propertyConfigFileLabel='', propertyDataType=null, usageDataType=null, validation=, validationMessage=null, validationPattern=null, defaultValue=null, helpText='Ignore the exception and continue.'}}]}"));
            Assert.assertThat(((HashMap) flow1.getIkasanExceptionResolver().getIkasanExceptionResolutionMap()).get("org.ikasan.spec.component.routing.RouterException.class").toString(),
                    is("IkasanExceptionResoluation{theException=org.ikasan.spec.component.routing.RouterException.class, theAction='retry', params=[IkasanComponentProperty{value=200, meta=IkasanComponentPropertyMeta{paramGroupNumber=1, causesUserCodeRegeneration=false, mandatory=true, userImplementedClass=false, userDefineResource=false, propertyName='delay', propertyConfigFileLabel='', propertyDataType=class java.lang.Integer, usageDataType=java.lang.Integer, validation=, validationMessage=null, validationPattern=null, defaultValue=100, helpText='The period to wait between retries.'}}, IkasanComponentProperty{value=10, meta=IkasanComponentPropertyMeta{paramGroupNumber=1, causesUserCodeRegeneration=false, mandatory=true, userImplementedClass=false, userDefineResource=false, propertyName='interval', propertyConfigFileLabel='', propertyDataType=class java.lang.Integer, usageDataType=java.lang.Integer, validation=, validationMessage=null, validationPattern=null, defaultValue=10, helpText='The maximum number of retries.'}}]}"));
        }
        {   // scope protection
            IkasanFlow flow2 = flows.get(1);
            Assert.assertThat(flow2.getViewHandler(), is(notNullValue()));
            Assert.assertThat(flow2.getName(), is("jmsToDbFlow"));
            Assert.assertThat(flow2.getDescription(), is("Sample JMS to DB flow"));

            Assert.assertThat(flow2.getFlowComponentList().size(), is(3));
            Assert.assertThat(getConfiguredPropertyValues(flow2.getFlowComponentList().get(0).getConfiguredProperties()),
                    is("AutoContentConversion->true,AutoSplitBatch->true,BatchMode->false,BatchSize->1,CacheLevel->CACHE_CONNECTION,ConcurrentConsumers->1," +
                            "ConnectionFactory->connectionFactory,DestinationJndiName->jms.topic.test,Durable->true,DurableSubscriptionName->testDurableSubscription," +
                            "MaxConcurrentConsumers->1,Name->JMS Consumer,PubSubDomain->false,SessionAcknowledgeMode->Session.SESSION_TRANSACTED,SessionTransacted->true"));

            Assert.assertThat(getConfiguredPropertyValues(flow2.getFlowComponentList().get(1).getConfiguredProperties()),
                    is("ClassToBeBound->com.ikasan.sample.person.model.Person.class,Name->XML to Person"));

            Assert.assertThat(getConfiguredPropertyValues(flow2.getFlowComponentList().get(2).getConfiguredProperties()),
                    is("Name->DB Producer"));
        }
    }

}