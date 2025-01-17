package org.ikasan;

/**
* The component factory defines the details of the components and their configuration.
*
* This file is auto-generated by Ikasan Studio, do no edit.
*/
@org.springframework.context.annotation.Configuration
public class ComponentFactoryMyFlow1
{
@org.springframework.beans.factory.annotation.Value("${module.name}")
private String moduleName;

@javax.annotation.Resource
org.ikasan.builder.BuilderFactory builderFactory;

@org.springframework.beans.factory.annotation.Value("${jms.atobconvert.myflow1.myjspringjmsconsumer.connection.factory.jndi.initial}")
java.lang.String jmsAToBConvertMyFlow1MyJSpringJMSConsumerConnectionFactoryJndiInitial;
@org.springframework.beans.factory.annotation.Value("${jms.atobconvert.myflow1.myjspringjmsconsumer.connection.factory.jndi.provider.url}")
java.lang.String jmsAToBConvertMyFlow1MyJSpringJMSConsumerConnectionFactoryJndiProviderUrl;
@org.springframework.beans.factory.annotation.Value("${jms.atobconvert.myflow1.myjspringjmsconsumer.connection.factory.name}")
java.lang.String jmsAToBConvertMyFlow1MyJSpringJMSConsumerConnectionFactoryName;
@org.springframework.beans.factory.annotation.Value("${jms.atobconvert.myflow1.myjspringjmsconsumer.connection.factory.password}")
java.lang.String jmsAToBConvertMyFlow1MyJSpringJMSConsumerConnectionFactoryPassword;
@org.springframework.beans.factory.annotation.Value("${jms.atobconvert.myflow1.myjspringjmsconsumer.connection.factory.user}")
java.lang.String jmsAToBConvertMyFlow1MyJSpringJMSConsumerConnectionFactoryUser;
@org.springframework.beans.factory.annotation.Value("${jms.atobconvert.myflow1.myjspringjmsconsumer.destination}")
java.lang.String jmsAToBConvertMyFlow1MyJSpringJMSConsumerDestination;
@org.springframework.beans.factory.annotation.Value("${jms.atobconvert.myflow1.myjspringjmsconsumer.destination.jndi.initial}")
java.lang.String jmsAToBConvertMyFlow1MyJSpringJMSConsumerDestinationJndiInitial;
@org.springframework.beans.factory.annotation.Value("${jms.atobconvert.myflow1.myjspringjmsconsumer.destination.jndi.provider.url}")
java.lang.String jmsAToBConvertMyFlow1MyJSpringJMSConsumerDestinationJndiProviderUrl;
@org.springframework.beans.factory.annotation.Value("${jms.atobconvert.myflow1.myjspringjmsconsumer.destination.jndi.password}")
java.lang.String jmsAToBConvertMyFlow1MyJSpringJMSConsumerDestinationJndiPassword;
@org.springframework.beans.factory.annotation.Value("${jms.atobconvert.myflow1.myjspringjmsconsumer.destination.jndi.user}")
java.lang.String jmsAToBConvertMyFlow1MyJSpringJMSConsumerDestinationJndiUser;
@javax.annotation.Resource
org.ikasan.component.endpoint.filesystem.messageprovider.FileConsumerConfiguration myConfigurationClass;
@javax.annotation.Resource
javax.jms.ConnectionFactory myConnectionFactory;
@javax.annotation.Resource
java.util.Map<String, String> key1value1key2value2;
@javax.annotation.Resource
org.ikasan.spec.event.EventFactory orgIkasanMyflowMyEventFactory;
@javax.annotation.Resource
org.ikasan.spec.event.ManagedRelatedEventIdentifierService orgIkasanSpecEventManagedRelatedEventIdentifierService;
@javax.annotation.Resource
org.ikasan.component.endpoint.quartz.consumer.MessageProvider myMessageProviderClass;
@javax.annotation.Resource
org.springframework.transaction.jta.JtaTransactionManager myTransactionManagerClass;

public org.ikasan.spec.component.endpoint.Consumer getMyJSpringJMSConsumer() {
return builderFactory.getComponentBuilder().springJMSConsumer()
.setAutoContentConversion(true)
.setAutoSplitBatch(true)
.setBatchMode(true)
.setBatchMode(1)
.setCacheLevel(1)
.setConcurrentConsumers(2)
.setConfiguration(myConfigurationClass)
.setConfiguredResourceId("aToBConvertmyFlow1myJSpringJMSConsumer")
.setConnectionFactory(myConnectionFactory)
.setConnectionFactoryJNDIProperties(key1value1key2value2)
.setConnectionFactoryJndiPropertyFactoryInitial(jmsAToBConvertMyFlow1MyJSpringJMSConsumerConnectionFactoryJndiInitial)
.setConnectionFactoryJndiPropertyProviderUrl(jmsAToBConvertMyFlow1MyJSpringJMSConsumerConnectionFactoryJndiProviderUrl)
.setConnectionFactoryJndiPropertySecurityCredentials(myConnectionFactoryJndiPropertySecurityCredentials)
.setConnectionFactoryJndiPropertySecurityPrincipal(myConnectionFactoryJndiPropertySecurityPrincipal)
.setConnectionFactoryJndiPropertyUrlPkgPrefixes(myConnectionFactoryJndiPropertyUrlPkgPrefixes)
.setConnectionFactoryName(jmsAToBConvertMyFlow1MyJSpringJMSConsumerConnectionFactoryName)
.setConnectionFactoryPassword(jmsAToBConvertMyFlow1MyJSpringJMSConsumerConnectionFactoryPassword)
.setConnectionFactoryUsername(jmsAToBConvertMyFlow1MyJSpringJMSConsumerConnectionFactoryUser)
.setConnectionPassword(myConnectionPassword)
.setConnectionUsername(myConnectionUsername)
.setDestinationJndiName(jmsAToBConvertMyFlow1MyJSpringJMSConsumerDestination)
.setDestinationJndiProperties(myDestinationJndiProperties)
.setDestinationJndiPropertyFactoryInitial(jmsAToBConvertMyFlow1MyJSpringJMSConsumerDestinationJndiInitial)
.setDestinationJndiPropertyProviderUrl(jmsAToBConvertMyFlow1MyJSpringJMSConsumerDestinationJndiProviderUrl)
.setDestinationJndiPropertySecurityCredentials(jmsAToBConvertMyFlow1MyJSpringJMSConsumerDestinationJndiPassword)
.setDestinationJndiPropertySecurityPrincipal(jmsAToBConvertMyFlow1MyJSpringJMSConsumerDestinationJndiUser)
.setDestinationJndiPropertyUrlPkgPrefixes(org.myapp)
.setDurable(true)
.setDurableSubscriptionName(myDurableSubscription)
.setEventFactory(orgIkasanMyflowMyEventFactory)
.setManagedIdentifierService(orgIkasanSpecEventManagedRelatedEventIdentifierService)
.setMaxConcurrentConsumers(1000)
.setMessageProvider(myMessageProviderClass)
.setPubSubDomain(myPubSubDomain)
.setReceiveTimeout(1000)
.setSessionAcknowledgeMode(1)
.setSessionTransacted(true)
.setTransactionManager(myTransactionManagerClass)
.build();
}}