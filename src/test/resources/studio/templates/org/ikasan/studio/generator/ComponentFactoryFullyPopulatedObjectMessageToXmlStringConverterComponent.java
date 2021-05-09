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
java.util.Map<Class, javax.xml.bind.annotation.adapters.XmlAdapter> myXmlAdapterMap;

public org.ikasan.spec.component.transformation.Converter get() {
return builderFactory.getComponentBuilder().objectToXmlStringConverter()
.setSchemaLocation("http://foo.com/domain example.xsd")
.setObjectClass(String.class)
.setConfiguration(myConfigurationClass)
.setSchema("mySchema")
.setNamespaceURI("myNamespaceURI")
.setXmlAdapterMap(myXmlAdapterMap)
.setRootName("myRootName")
.setObjectClasses({'String.class','String.class'})
.setConfiguredResourceId("myUniqueConfiguredResourceIdName")
.setValidate(true)
.setUseNamespacePrefix(true)
.setRouteOnValidationException(true)
.setNoNamespaceSchema(true)
.setFastFailOnConfigurationLoad(true)
.setNamespacePrefix("myNamespacePrefix")
.build();
}}