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


@javax.annotation.Resource
org.ikasan.endpoint.ftp.producer.FtpProducerConfiguration myConfigurationClass;
@javax.annotation.Resource
java.util.Map<String, String> key1value1key2value2;

public org.ikasan.spec.component.endpoint.Producer getTestEmailProducer() {
return builderFactory.getComponentBuilder().emailProducer()
.setBccRecipient("myBccRecipient")
.setTransportProtocol("myTransportProtocol")
.setMailSubject("myMailSubject")
.setMailStoreProtocol("myMailStoreProtocol")
.setMailPopPort(100)
.setBccRecipients({'bcc1','bcc2'})
.setHasAttachments(true)
.setMailDebug(true)
.setMailSmtpPort(101)
.setMailPassword("myMailPassword")
.setToRecipient("myToRecipient")
.setEmailBody("myEmailBody")
.setCcRecipients({'cc1','cc2'})
.setUser("myUser")
.setMailPopClass("myMailPopClass")
.setToRecipients({'to1','to2'})
.setConfiguredResourceId("myUniqueConfiguredResourceIdName")
.setMailMimeAddressStrict(true)
.setMailSmtpHost("myMailSmtpHost")
.setCcRecipient("myCcRecipient")
.setCriticalOnStartup(true)
.setMailSmtpClass("myMailSmtpClass")
.setConfiguration(myConfigurationClass)
.setMailhost("myMailhostAddress")
.setMailSmtpUser("myMailSmtpUser")
.setMailRuntimeEnvironment("myMailRuntimeEnvironment")
.setEmailFormat("html")
.setMailPopUser("myMailPopUser")
.setFrom("FromAddress")
.setExtendedMailSessionProperties(key1value1key2value2)
.build();
}}