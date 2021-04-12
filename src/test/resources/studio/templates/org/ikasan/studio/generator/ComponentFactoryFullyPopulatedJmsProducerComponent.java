package org.ikasan;

/**
* The component factory defines the details of the components and their configuration.
*
* This file is auto-generated by Ikasan Studio, do no edit.
*/
@org.springframework.context.annotation.Configuration
public class ComponentFactory
{
@org.springframework.beans.factory.annotation.Value("${module.name}")
private String moduleName;

@javax.annotation.Resource
org.ikasan.builder.BuilderFactory builderFactory;


@javax.annotation.Resource
org.ikasan.component.endpoint.filesystem.messageprovider.FileConsumerConfiguration myConfigurationClass;
@javax.annotation.Resource
javax.jms.ConnectionFactory myConnectionFactory;
@javax.annotation.Resource
org.ikasan.component.endpoint.jms.producer.PostProcessor myPostProcessor;
@javax.annotation.Resource
org.springframework.jms.support.converter.MessageConverter myMessageConverter;
@javax.annotation.Resource
org.springframework.transaction.jta.JtaTransactionManager myTransactionManagerClass;

public org.ikasan.spec.component.endpoint.Producer getTestJmsConsumer() {
return builderFactory.getComponentBuilder().jmsProducer()
.setDeliveryPersistent(true)
.setConnectionFactoryName("myConnectionFactoryName")
.setMessageIdEnabled(true)
.setConfiguration(myConfigurationClass)
.setDestinationJndiProperties("myDestinationJndiProperties")
.setDestinationJndiName("myDestinationJndiName")
.setDestinationJndiPropertySecurityPrincipal("myDestinationJndiPropertySecurityPrincipal")
.setReceiveTimeout(1000)
.setDestinationJndiPropertyFactoryInitial(myDestinationJndiPropertyFactoryInitial)
.setSessionAcknowledgeMode(AUTO_ACKNOWLEDGE)
.setConnectionFactory(myConnectionFactory)
.setConnectionFactoryPassword("myConnectionFactoryPassword")
.setConnectionFactoryJndiPropertyFactoryInitial(myConnectionFactoryJndiPropertyFactoryInitial)
.setDestinationJndiPropertyProviderUrl(myDestinationJndiPropertyProviderUrl)
.setMessageTimestampEnabled(true)
.setPostProcessor(myPostProcessor)
.setConnectionPassword("myConnectionPassword")
.setDestinationJndiPropertySecurityCredentials("myDestinationJndiPropertySecurityCredentials")
.setConnectionFactoryJndiPropertySecurityPrincipal("myConnectionFactoryJndiPropertySecurityPrincipal")
.setTimeToLive(100)
.setPriority(1)
.setDestinationJndiPropertyUrlPkgPrefixes("org.myapp")
.setConnectionFactoryJndiPropertySecurityCredentials("myConnectionFactoryJndiPropertySecurityCredentials")
.setMessageConverter(myMessageConverter)
.setPubSubNoLocal(true)
.setExplicitQosEnabled(true)
.setConnectionFactoryJndiPropertyProviderUrl(myConnectionFactoryJndiPropertyProviderUrl)
.setConnectionFactoryJndiPropertyUrlPkgPrefixes("myConnectionFactoryJndiPropertyUrlPkgPrefixes")
.setSessionTransacted(true)
.setConnectionFactoryJNDIProperties({key1:'value1',key2:'value2'})
.setConnectionUsername("myConnectionUsername")
.setConnectionFactoryUsername("myConnectionFactoryUsername")
.setConfiguredResourceId("myUniqueConfiguredResourceIdName")
.setTransactionManager(myTransactionManagerClass)
.setDeliveryMode(1)
.setPubSubDomain(myPubSubDomain)
.build();
}}