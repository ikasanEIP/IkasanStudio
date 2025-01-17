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

@org.springframework.beans.factory.annotation.Value("${jms.atobconvert.myflow1.myjmsproducer.connection.factory.jndi.initial}")
java.lang.String jmsAToBConvertMyFlow1MyJMSProducerConnectionFactoryJndiInitial;
@org.springframework.beans.factory.annotation.Value("${jms.atobconvert.myflow1.myjmsproducer.connection.factory.jndi.provider.url}")
java.lang.String jmsAToBConvertMyFlow1MyJMSProducerConnectionFactoryJndiProviderUrl;
@org.springframework.beans.factory.annotation.Value("${jms.atobconvert.myflow1.myjmsproducer.connection.factory.name}")
java.lang.String jmsAToBConvertMyFlow1MyJMSProducerConnectionFactoryName;
@org.springframework.beans.factory.annotation.Value("${jms.atobconvert.myflow1.myjmsproducer.connection.factory.password}")
java.lang.String jmsAToBConvertMyFlow1MyJMSProducerConnectionFactoryPassword;
@org.springframework.beans.factory.annotation.Value("${jms.atobconvert.myflow1.myjmsproducer.connection.factory.user}")
java.lang.String jmsAToBConvertMyFlow1MyJMSProducerConnectionFactoryUser;
@org.springframework.beans.factory.annotation.Value("${jms.atobconvert.myflow1.myjmsproducer.destination}")
java.lang.String jmsAToBConvertMyFlow1MyJMSProducerDestination;
@org.springframework.beans.factory.annotation.Value("${jms.atobconvert.myflow1.myjmsproducer.destination.jndi.initial}")
java.lang.String jmsAToBConvertMyFlow1MyJMSProducerDestinationJndiInitial;
@org.springframework.beans.factory.annotation.Value("${jms.atobconvert.myflow1.myjmsproducer.destination.jndi.provider.url}")
java.lang.String jmsAToBConvertMyFlow1MyJMSProducerDestinationJndiProviderUrl;
@org.springframework.beans.factory.annotation.Value("${jms.atobconvert.myflow1.myjmsproducer.destination.jndi.password}")
java.lang.String jmsAToBConvertMyFlow1MyJMSProducerDestinationJndiPassword;
@org.springframework.beans.factory.annotation.Value("${jms.atobconvert.myflow1.myjmsproducer.destination.jndi.user}")
java.lang.String jmsAToBConvertMyFlow1MyJMSProducerDestinationJndiUser;
@javax.annotation.Resource
org.ikasan.component.endpoint.filesystem.messageprovider.FileConsumerConfiguration myConfigurationClass;
@javax.annotation.Resource
javax.jms.ConnectionFactory myConnectionFactory;
@javax.annotation.Resource
java.util.Map<String, String> key1value1key2value2;
@javax.annotation.Resource
org.springframework.jms.support.converter.MessageConverter myMessageConverter;
@javax.annotation.Resource
org.ikasan.component.endpoint.jms.producer.PostProcessor myPostProcessor;
@javax.annotation.Resource
org.springframework.transaction.jta.JtaTransactionManager myTransactionManagerClass;

public org.ikasan.spec.component.endpoint.Producer getMyJMSProducer() {
return builderFactory.getComponentBuilder().jmsProducer()
.setConfiguration(myConfigurationClass)
.setConfiguredResourceId("aToBConvertMyFlow1MyJMSProducer")
.setConnectionFactory(myConnectionFactory)
.setConnectionFactoryJNDIProperties(key1value1key2value2)
.setConnectionFactoryJndiPropertyFactoryInitial(jmsAToBConvertMyFlow1MyJMSProducerConnectionFactoryJndiInitial)
.setConnectionFactoryJndiPropertyProviderUrl(jmsAToBConvertMyFlow1MyJMSProducerConnectionFactoryJndiProviderUrl)
.setConnectionFactoryJndiPropertySecurityCredentials(myConnectionFactoryJndiPropertySecurityCredentials)
.setConnectionFactoryJndiPropertySecurityPrincipal(myConnectionFactoryJndiPropertySecurityPrincipal)
.setConnectionFactoryJndiPropertyUrlPkgPrefixes(myConnectionFactoryJndiPropertyUrlPkgPrefixes)
.setConnectionFactoryName(jmsAToBConvertMyFlow1MyJMSProducerConnectionFactoryName)
.setConnectionFactoryPassword(jmsAToBConvertMyFlow1MyJMSProducerConnectionFactoryPassword)
.setConnectionFactoryUsername(jmsAToBConvertMyFlow1MyJMSProducerConnectionFactoryUser)
.setConnectionPassword(myConnectionPassword)
.setConnectionUsername(myConnectionUsername)
.setDeliveryMode(1)
.setDeliveryPersistent(true)
.setDestinationJndiName(jmsAToBConvertMyFlow1MyJMSProducerDestination)
.setDestinationJndiProperties(myDestinationJndiProperties)
.setDestinationJndiPropertyFactoryInitial(jmsAToBConvertMyFlow1MyJMSProducerDestinationJndiInitial)
.setDestinationJndiPropertyProviderUrl(jmsAToBConvertMyFlow1MyJMSProducerDestinationJndiProviderUrl)
.setDestinationJndiPropertySecurityCredentials(jmsAToBConvertMyFlow1MyJMSProducerDestinationJndiPassword)
.setDestinationJndiPropertySecurityPrincipal(jmsAToBConvertMyFlow1MyJMSProducerDestinationJndiUser)
.setDestinationJndiPropertyUrlPkgPrefixes(org.myapp)
.setExplicitQosEnabled(true)
.setMessageConverter(myMessageConverter)
.setMessageIdEnabled(true)
.setMessageTimestampEnabled(true)
.setPostProcessor(myPostProcessor)
.setPriority(1)
.setPubSubDomain(myPubSubDomain)
.setPubSubNoLocal(true)
.setReceiveTimeout(1000)
.setSessionAcknowledgeMode(1)
.setSessionAcknowledgeModeName(AUTO_ACKNOWLEDGE)
.setSessionTransacted(true)
.setTimeToLive(100)
.setTransactionManager(myTransactionManagerClass)
.build();
}}