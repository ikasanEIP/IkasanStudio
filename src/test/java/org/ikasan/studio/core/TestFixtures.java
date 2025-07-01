package org.ikasan.studio.core;

import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.decorator.Decorator;
import org.ikasan.studio.core.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.meta.ExceptionResolverMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;

import java.util.*;
import java.util.stream.Stream;

import static org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta.*;
/**
 * Some of these text fixtures will be exported to the Meta Pack project
 * Ideally the Ikasan Packs should be loosely coupled with the IDE so that most
 * of the knowledge and detail of what each pack support comes from the pack, not the IDE.
 */
public class TestFixtures {
    public static final String DEFAULT_PACKAGE = "org.ikasan";
    public static final String VERSION_INDEPENDENT_META_PACK = "Vtest.x";
//    public static final String metaPackVersion = "Vtest.x";
//    public static final String metaPackVersion = "V3.3.x";
//    public static final String metaPackVersion = "V3.3.7";
    public static final String META_IKASAN_PACK_3_3_7 = "V3.3.7";
    public static final String META_IKASAN_PACK_3_3_3 = "V3.3.3";
//    public static final String V3_IKASAN_PACK = "V3.3.x";
    public static final String TEST_FLOW_NAME = "MyFlow1";
    public static final String TEST_FLOW_DESCRIPTION = "MyFlowDescription";
    public static final String TEST_CRON_EXPRESSION = "0 0/1 * * * ?";

    public static Stream<String> metaPacksToTest() {
        return Stream.of("V3.3.3", "V3.3.7");
    }

    public static Module getMyFirstModuleIkasanModule(String metaPackVersion, List<Flow> flows) throws StudioBuildException {
        return Module.moduleBuilder()
            .version(metaPackVersion)
            .name("A to B convert")
            .description("My first module")
            .applicationPackageName("co.uk.test")
            .port("8091")
            .h2PortNumber("8092")
            .h2WebPortNumber("8093")
            .flows(flows)
            .build();
    }
//
//    public static Module getMyFirstModuleIkasanModule(List<Flow> flowsString metaPackVersion) throws StudioBuildException {
//        return Module.moduleBuilder()
//            .version(metaPackVersion)
//            .name("A to B convert")
//            .description("My first module")
//            .applicationPackageName("co.uk.test")
//            .port("8091")
//            .h2PortNumber("8092")
//            .h2WebPortNumber("8093")
//            .flows(flows)
//            .build();
//    }

    public static Flow.FlowBuilder getUnbuiltFlow(String metaPackVersion) {
        return Flow.flowBuilder()
            .metapackVersion(metaPackVersion)
            .description(TEST_FLOW_DESCRIPTION)
            .name(TEST_FLOW_NAME);
    }

    // -------------------------- Broker -------------------------
    public static FlowElement getBroker(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Broker");
        FlowElement flowElement =  FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Broker")
            .build();
        flowElement.setPropertyValue(FROM_TYPE, "java.lang.String");
        flowElement.setPropertyValue(TO_TYPE, "java.lang.Integer");
        flowElement.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, "myBroker");
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    public static FlowElement getGenericBroker(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Generic Broker");
        FlowElement flowElement =  FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Broker")
            .build();
        flowElement.setPropertyValue(FROM_TYPE, "java.lang.String");
        flowElement.setPropertyValue(TO_TYPE, "java.lang.Integer");
        flowElement.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, "myBroker");
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    // -------------------------- Consumers -------------------------
    public static FlowElement getEventGeneratingConsumer(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Event Generating Consumer");
        return FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Event Generating Consumer")
            .build();
    }

    public static FlowElement getEventGeneratingConsumerWithWiretaps(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Event Generating Consumer");
        return FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Event Generating Consumer")
            .build();
    }

    public static FlowElement getFtpConsumer(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "FTP Consumer");
        FlowElement flowElement = FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My FTP Consumer")
            .build();
        // Mandatory properties
        flowElement.setPropertyValue("cronExpression", "*/5 * * * * ?");
        flowElement.setPropertyValue("filenamePattern", "*Test.txt");
        flowElement.setPropertyValue("password", "secret");
        flowElement.setPropertyValue("remoteHost", "myRemortHost");
        flowElement.setPropertyValue("remotePort", "1024");
        flowElement.setPropertyValue("sourceDirectory", "/test/source/directory/");
        flowElement.setPropertyValue("username", "myLoginName");

        // Optional properties
        flowElement.setPropertyValue("active", true);
        flowElement.setPropertyValue("ageOfFiles", 10);
        flowElement.setPropertyValue("checksum", true);
        flowElement.setPropertyValue("chronological", true);
        flowElement.setPropertyValue("chunkSize", 1048577);
        flowElement.setPropertyValue("chunking", true);
        flowElement.setPropertyValue("cleanupJournalOnComplete", true);
        flowElement.setPropertyValue("clientID", "myClientId");
        flowElement.setPropertyValue("configuration", "MyConfigurationClass");
        flowElement.setPropertyValue("configurationId", "myUniqueConfiguredResourceIdName");
        flowElement.setPropertyValue("connectionTimeout", 600001);
        flowElement.setPropertyValue("dataTimeout", 300001);
        flowElement.setPropertyValue("destructive", true);
        flowElement.setPropertyValue("ftps", true);
        flowElement.setPropertyValue("filterDuplicates", true);
        flowElement.setPropertyValue("filterOnFilename", true);
        flowElement.setPropertyValue("filterOnLastModifiedDate", true);
        flowElement.setPropertyValue("ftpsIsImplicit", true);
        flowElement.setPropertyValue("ftpsKeyStoreFilePassword", "myFtpsKeyStoreFilePassword");
        flowElement.setPropertyValue("ftpsKeyStoreFilePath", "/test/ftps/keystore");
        flowElement.setPropertyValue("ftpsPort", 987);
        flowElement.setPropertyValue("ftpsProtocol", "SSL");
        flowElement.setPropertyValue("ignoreMisfire",  true);
        flowElement.setPropertyValue("isRecursive", true);
        flowElement.setPropertyValue("managedEventIdentifierService", "myManagedEventIdentifierServiceClass");
        flowElement.setPropertyValue("managedResourceRecoveryManager", "myManagedResourceRecoveryManagerClass");
        flowElement.setPropertyValue("maxEagerCallbacks", 1);
        flowElement.setPropertyValue("maxRetryAttempts", 10);
        flowElement.setPropertyValue("maxRows", 11);
        flowElement.setPropertyValue("messageProvider", "myMessageProviderClass");
        flowElement.setPropertyValue("minAge", 12);
        flowElement.setPropertyValue("moveOnSuccessNewPath", "/test/move/on/success");
        flowElement.setPropertyValue("moveOnSuccess", true);
        flowElement.setPropertyValue("passwordFilePath", "/test/password/file/path");
        flowElement.setPropertyValue("renameOnSuccessExtension", "newExtension");
        flowElement.setPropertyValue("renameOnSuccess", true);
        flowElement.setPropertyValue("scheduledJobGroupName",  "myScheduledJobGroupName");
        flowElement.setPropertyValue("scheduledJobName", "myScheduledJobName");
        flowElement.setPropertyValue("socketTimeout", 22);
        flowElement.setPropertyValue("sourceDirectoryURLFactory", "myDirectoryURLFactoryClass");
        flowElement.setPropertyValue("systemKey", "mySystemKey");
        flowElement.setPropertyValue("transactionManager", "myTransactionManagerClass");
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }
    public static FlowElement getSftpConsumer(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "SFTP Consumer");
        FlowElement flowElement = FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My SFTP Consumer")
            .build();
        // Mandatory properties
        flowElement.setPropertyValue("cronExpression", "*/5 * * * * ?");
        flowElement.setPropertyValue("filenamePattern", "*Test.txt");
        flowElement.setPropertyValue("password", "secret");
        flowElement.setPropertyValue("remoteHost", "myRemortHost");
        flowElement.setPropertyValue("remotePort", "1024");
        flowElement.setPropertyValue("sourceDirectory", "/test/source/directory/");
        flowElement.setPropertyValue("username", "myLoginName");

        // Optional properties
        flowElement.setPropertyValue("ageOfFiles", 10);
        flowElement.setPropertyValue("checksum", true);
        flowElement.setPropertyValue("chronological", true);
        flowElement.setPropertyValue("chunkSize", 1048577);
        flowElement.setPropertyValue("chunking", true);
        flowElement.setPropertyValue("cleanupJournalOnComplete", true);
        flowElement.setPropertyValue("clientID", "myClientId");
        flowElement.setPropertyValue("configuration", "MyConfigurationClass");
        flowElement.setPropertyValue("configurationId", "myUniqueConfiguredResourceIdName");
        flowElement.setPropertyValue("connectionTimeout", 600001);
        flowElement.setPropertyValue("criticalOnStartup", true);
        flowElement.setPropertyValue("dataTimeout", 300001);
        flowElement.setPropertyValue("destructive", true);
        flowElement.setPropertyValue("eager", true);
        flowElement.setPropertyValue("eventFactory", "org.ikasan.myflow.myEventFactory");

        flowElement.setPropertyValue("ignoreMisfire",  true);
        flowElement.setPropertyValue("isRecursive", true);
        flowElement.setPropertyValue("knownHostFilename", "~/.ssh/known_hosts");
        flowElement.setPropertyValue("managedEventIdentifierService", "myManagedEventIdentifierServiceClass");
        flowElement.setPropertyValue("managedResourceRecoveryManager", "myManagedResourceRecoveryManagerClass");
        flowElement.setPropertyValue("maxEagerCallbacks", 1);
        flowElement.setPropertyValue("maxRetryAttempts", 10);
        flowElement.setPropertyValue("maxRows", 11);
        flowElement.setPropertyValue("messageProvider", "myMessageProviderClass");
        flowElement.setPropertyValue("minAge", 12);
        flowElement.setPropertyValue("moveOnSuccessNewPath", "/test/move/on/success");
        flowElement.setPropertyValue("moveOnSuccess", true);
        flowElement.setPropertyValue("renameOnSuccessExtension", "newExtension");
        flowElement.setPropertyValue("renameOnSuccess", true);
        flowElement.setPropertyValue("preferredKeyExchangeAlgorithm", "DFS");
        flowElement.setPropertyValue("privateKeyFilename", "~/.ssh/meyfile.jks");
        flowElement.setPropertyValue("scheduledJobGroupName",  "myScheduledJobGroupName");
        flowElement.setPropertyValue("scheduledJobName", "myScheduledJobName");
        flowElement.setPropertyValue("sourceDirectoryURLFactory", "myDirectoryURLFactoryClass");
        flowElement.setPropertyValue("systemKey", "mySystemKey");
        flowElement.setPropertyValue("transactionManager", "myTransactionManagerClass");
        flowElement.setPropertyValue("timeZone", "UTC");
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    public static FlowElement getEndpointForLocalFileConsumer(String metaPackVersion) throws StudioBuildException {
        FlowElement localFileConsumer = getLocalFileConsumer(metaPackVersion);
        return IkasanComponentLibrary.getEndpointForGivenComponent(metaPackVersion, localFileConsumer);
    }

    public static FlowElement getLocalFileConsumer(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Local File Consumer");
        FlowElement flowElement = FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Local File Consumer")
            .build();
        flowElement.setPropertyValue("configuration", "org.ikasan.myflow.configuration");
        flowElement.setPropertyValue("configurationId", "bob");
        flowElement.setPropertyValue("criticalOnStartup", true);
        flowElement.setPropertyValue("cronExpression", TEST_CRON_EXPRESSION);
        flowElement.setPropertyValue("directoryDepth", 1);
        flowElement.setPropertyValue("eager", true);
        flowElement.setPropertyValue("encoding", "UTF-8");
        flowElement.setPropertyValue("eventFactory", "org.ikasan.myflow.myEventFactory");
        flowElement.setPropertyValue("filenames", "myFile,anotherFile");
        flowElement.setPropertyValue("ignoreFileRenameWhilstScanning", true);
        flowElement.setPropertyValue("ignoreMisfire", true);
        flowElement.setPropertyValue("includeHeader", true);
        flowElement.setPropertyValue("includeTrailer", true);
        flowElement.setPropertyValue("logMatchedFilenames", true);
        flowElement.setPropertyValue("managedEventIdentifierService", "org.ikasan.myflow.myEventService");
        flowElement.setPropertyValue("maxEagerCallbacks", 1);
        flowElement.setPropertyValue("messageProvider", "org.ikasan.myflow.myMessageProvider");
        flowElement.setPropertyValue("messageProviderPostProcessor", "org.ikasan.myflow.myMssageProviderPostProcessor");
        flowElement.setPropertyValue("sortAscending", true);
        flowElement.setPropertyValue("sortByModifiedDateTime", true);
        flowElement.setPropertyValue("timezone", "UTC");
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    public static FlowElement getLocalFileConsumerMandatoryOnly(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Local File Consumer");
        FlowElement flowElement = FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Local File Consumer")
            .build();
        flowElement.setPropertyValue("cronExpression", TEST_CRON_EXPRESSION);
        flowElement.setPropertyValue("filenames", "myFile,anotherFile");
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    public static FlowElement getSpringJmsConsumer(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Spring JMS Consumer");
        FlowElement flowElement = FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My JSpring JMS Consumer")
            .build();

        flowElement.setPropertyValue("autoContentConversion", true);
        flowElement.setPropertyValue("autoSplitBatch", true);
        flowElement.setPropertyValue("batchMode", true);
        flowElement.setPropertyValue("batchSize", 1);
        flowElement.setPropertyValue("cacheLevel", 1);
        flowElement.setPropertyValue("concurrentConsumers", 2);
        flowElement.setPropertyValue("configuration", "MyConfigurationClass");
        flowElement.setPropertyValue("configurationId", "__module__flow__component");
        flowElement.setPropertyValue("connectionFactory", "myConnectionFactory");
        flowElement.setPropertyValue("connectionFactoryJNDIProperties", "{key1:'value1',key2:'value2'}");
        flowElement.setPropertyValue("connectionFactoryJndiPropertyFactoryInitial", "myConnectionFactoryJndiPropertyFactoryInitial");
        flowElement.setPropertyValue("connectionFactoryJndiPropertyProviderUrl", "myConnectionFactoryJndiPropertyProviderUrl");
        flowElement.setPropertyValue("connectionFactoryJndiPropertySecurityCredentials", "myConnectionFactoryJndiPropertySecurityCredentials");
        flowElement.setPropertyValue("connectionFactoryJndiPropertySecurityPrincipal", "myConnectionFactoryJndiPropertySecurityPrincipal");
        flowElement.setPropertyValue("connectionFactoryJndiPropertyUrlPkgPrefixes", "myConnectionFactoryJndiPropertyUrlPkgPrefixes");
        flowElement.setPropertyValue("connectionFactoryName", "ConnectionFactory");
        flowElement.setPropertyValue("connectionFactoryPassword", "myConnectionFactoryPassword");
        flowElement.setPropertyValue("connectionFactoryUsername", "myConnectionFactoryUsername");
        flowElement.setPropertyValue("connectionPassword", "myConnectionPassword");
        flowElement.setPropertyValue("connectionUsername", "myConnectionUsername");
        flowElement.setPropertyValue("destinationJndiName", "myDestinationJndiName");
        flowElement.setPropertyValue("destinationJndiProperties", "myDestinationJndiProperties");
        flowElement.setPropertyValue("destinationJndiPropertyFactoryInitial", "myDestinationJndiPropertyFactoryInitial");
        flowElement.setPropertyValue("destinationJndiPropertyProviderUrl", "myDestinationJndiPropertyProviderUrl");
        flowElement.setPropertyValue("destinationJndiPropertySecurityCredentials", "myDestinationJndiPropertySecurityCredentials");
        flowElement.setPropertyValue("destinationJndiPropertySecurityPrincipal", "myDestinationJndiPropertySecurityPrincipal");
        flowElement.setPropertyValue("destinationJndiPropertyUrlPkgPrefixes", "org.myapp");
        flowElement.setPropertyValue("durable", true);
        flowElement.setPropertyValue("durableSubscriptionName", "myDurableSubscription");
        flowElement.setPropertyValue("eventFactory", "org.ikasan.myflow.myEventFactory");
        flowElement.setPropertyValue("managedIdentifierService", "org.ikasan.spec.event.ManagedRelatedEventIdentifierService");
        flowElement.setPropertyValue("maxConcurrentConsumers", 1000L);
        flowElement.setPropertyValue("messageProvider", "myMessageProviderClass");
        flowElement.setPropertyValue("pubSubDomain", "myPubSubDomain");
        flowElement.setPropertyValue("receiveTimeout", 1000L);
        flowElement.setPropertyValue("sessionAcknowledgeMode", 1);
        flowElement.setPropertyValue("sessionAcknowledgeModeName", "AUTO_ACKNOWLEDGE");
        flowElement.setPropertyValue("sessionTransacted", true);
        flowElement.setPropertyValue("transactionManager", "myTransactionManagerClass");
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    public static FlowElement getBasicAmqSpringJmsConsumer(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Basic AMQ Spring JMS Consumer");
        FlowElement flowElement = FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Basic AMQ JSpring JMS Consumer")
            .build();

        flowElement.setPropertyValue("configuredResourceId", "__module__flow__component");
        flowElement.setPropertyValue("connectionFactoryJndiPropertyFactoryInitial", "myConnectionFactoryJndiPropertyFactoryInitial");
        flowElement.setPropertyValue("connectionFactoryJndiPropertyProviderUrl", "myConnectionFactoryJndiPropertyProviderUrl");
        flowElement.setPropertyValue("connectionFactoryName", "ConnectionFactory");
        flowElement.setPropertyValue("connectionFactoryPassword", "myConnectionFactoryPassword");
        flowElement.setPropertyValue("connectionFactoryUsername", "myConnectionFactoryUsername");
        flowElement.setPropertyValue("destinationJndiName", "myDestinationJndiName");
        flowElement.setPropertyValue("destinationJndiPropertyFactoryInitial", "myDestinationJndiPropertyFactoryInitial");
        flowElement.setPropertyValue("destinationJndiPropertyProviderUrl", "myDestinationJndiPropertyProviderUrl");
        flowElement.setPropertyValue("destinationJndiPropertySecurityCredentials", "myDestinationJndiPropertySecurityCredentials");
        flowElement.setPropertyValue("destinationJndiPropertySecurityPrincipal", "myDestinationJndiPropertySecurityPrincipal");
        flowElement.setPropertyValue("pubSubDomain", "myPubSubDomain");
        flowElement.setPropertyValue("sessionTransacted", true);
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    public static FlowElement getScheduledConsumer(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Scheduled Consumer");
        FlowElement flowElement = FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Scheduled Consumer")
            .build();
        flowElement.setPropertyValue("cronExpression", TEST_CRON_EXPRESSION);
        flowElement.setPropertyValue("configuration", "org.ikasan.myflow.configuration");
        flowElement.setPropertyValue("configurationId", "bob");
        flowElement.setPropertyValue("criticalOnStartup", true);
        flowElement.setPropertyValue("eager", true);
        flowElement.setPropertyValue("eventFactory", "org.ikasan.myflow.myEventFactory");
        flowElement.setPropertyValue("ignoreMisfire", true);
        flowElement.setPropertyValue("managedEventIdentifierService", "org.ikasan.myflow.myEventService");
        flowElement.setPropertyValue("managedResourceRecoveryManager", "org.ikasan.myflow.myManagedResourceRecoveryManager");
        flowElement.setPropertyValue("maxEagerCallbacks", 10);
        flowElement.setPropertyValue("messageProvider", "org.ikasan.myflow.myMessageProvider");
        flowElement.setPropertyValue("timezone", "UTC");

        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    // ------------------------- Converters -------------------------

    public static FlowElement getCustomConverter(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Custom Converter");
        FlowElement flowElement =  FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Custom Converter")
            .build();
        flowElement.setPropertyValue(FROM_TYPE, "java.lang.String");
        flowElement.setPropertyValue(TO_TYPE, "java.lang.Integer");
        flowElement.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, "myConverter");
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    public static FlowElement getObjectMessageToObjectConverter(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Object Message To Object Converter");
        return FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Object Message To Object Converter")
            .build();
    }

    public static FlowElement getObjectMessageToXmlStringtConverter(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Object Message To XML String Converter");
        FlowElement flowElement =  FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Object Message To XML String Converter")
            .build();
        flowElement.setPropertyValue("configuration", "MyConfigurationClass");
        flowElement.setPropertyValue("configurationId", "myUniqueConfiguredResourceIdName");
        flowElement.setPropertyValue("fastFailOnConfigurationLoad", true);
        flowElement.setPropertyValue("namespacePrefix", "myNamespacePrefix");
        flowElement.setPropertyValue("namespaceURI", "myNamespaceURI");
        flowElement.setPropertyValue("noNamespaceSchema", true);
        flowElement.setPropertyValue("objectClass", String.class);
        flowElement.setPropertyValue("objectClasses", "String.class,String.class");
        flowElement.setPropertyValue("rootClassName", "java.lang.String");
        flowElement.setPropertyValue("rootName", "myRootName");
        flowElement.setPropertyValue("routeOnValidationException", true);
        flowElement.setPropertyValue("schema", "mySchema");
        flowElement.setPropertyValue("schemaLocation", "http://foo.com/domain example.xsd");
        flowElement.setPropertyValue("useNamespacePrefix", true);
        flowElement.setPropertyValue("validate", true);
        flowElement.setPropertyValue("xmlAdapterMap", "myXmlAdapterMap");
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    // ------------------------- Translators -------------------------
    public static FlowElement getCustomTranslator(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Custom Translator");
        FlowElement flowElement =  FlowElement.flowElementBuilder()
                .componentMeta(meta)
                .componentName("My Custom Translator")
                .build();
        flowElement.setPropertyValue(TYPE, "java.lang.String");
        flowElement.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, "myTranslator");
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    // ------------------------- Debug -------------------------

    public static FlowElement getDebugTransition(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Debug Transition");
        FlowElement flowElement =  FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .build();
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }


    // ------------------------- ExceptionResolver -------------------------

    public static ExceptionResolver getExceptionResolver(String metaPackVersion) throws StudioBuildException {
        ExceptionResolution jmsExceptionResolution = ExceptionResolution.exceptionResolutionBuilder()
            .metapackVersion(metaPackVersion)
            .exceptionsCaught("javax.jms.JMSException.class")
            .theAction("retry")
            .componentProperties(getRetryProperties(metaPackVersion))
            .build();
        ExceptionResolution resourceExceptionResolution = ExceptionResolution.exceptionResolutionBuilder()
            .metapackVersion(metaPackVersion)
            .exceptionsCaught("javax.resource.ResourceException.class")
            .theAction("ignore")
            .build();

        Map<String, ExceptionResolution> exceptionResolutionMap = new HashMap<>();
        exceptionResolutionMap.put(jmsExceptionResolution.getExceptionsCaught(), jmsExceptionResolution);
        exceptionResolutionMap.put(resourceExceptionResolution.getExceptionsCaught(), resourceExceptionResolution);

        return ExceptionResolver.exceptionResolverBuilder()
            .metapackVersion(metaPackVersion)
            .ikasanExceptionResolutionMap(exceptionResolutionMap)
            .build();
    }

    // ------------------------- Filters -------------------------

    public static FlowElement getMessageFilter(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Custom Message Filter");
        FlowElement flowElement =  FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Message Filter")
            .build();

        flowElement.setPropertyValue(FROM_TYPE, "java.lang.String");
        flowElement.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, "myFilter");
        flowElement.setPropertyValue("configurationId", "MyResourceID");
        flowElement.setPropertyValue("configuration", "MyConfigurationClass");
        flowElement.setPropertyValue("configuredResource", true);
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    public static FlowElement getDefaultMessageFilter(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Default Message Filter");
        FlowElement flowElement =  FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Message Filter")
            .build();

        flowElement.setPropertyValue(FROM_TYPE, "java.lang.String");
        flowElement.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, "myFilter");
        flowElement.setPropertyValue("configurationId", "MyResourceID");
        flowElement.setPropertyValue("configuration", "MyConfigurationClass");
        flowElement.setPropertyValue("configuredResource", true);
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    public static FlowElement getGenericFilter(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Generic Filter");
        FlowElement flowElement =  FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Generic Filter")
            .build();

        flowElement.setPropertyValue(FROM_TYPE, "java.lang.String");
        flowElement.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, "myFilter");
        flowElement.setPropertyValue("configurationId", "MyResourceID");
        flowElement.setPropertyValue("configuration", "MyConfigurationClass");
        flowElement.setPropertyValue("configuredResource", true);
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    // ------------------------- Producers ---------------------------
    public static FlowElement getDevNullProducer(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Dev Null Producer");
        return FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My DevNull Producer")
            .build();
    }
    public static FlowElement getDevNullProducerWithWiretaps(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Dev Null Producer");
        return FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My DevNull Producer")
            .decorators(getGoodWiretaps())
            .build();
    }

    private static List<Decorator> getGoodWiretaps() {
        Decorator decorator1 = Decorator.decoratorBuilder()
            .type("Wiretap")
            .name("BEFORE bob")
            .configurationId("3450")
            .configurable(false)
            .build();
        Decorator decorator2 = Decorator.decoratorBuilder()
            .type("LogWiretap")
            .name("BEFORE bob")
            .configurationId("3451")
            .configurable(true)
            .build();
        Decorator decorator3 = Decorator.decoratorBuilder()
            .type("LogWiretap")
            .name("AFTER bob")
            .configurationId("3452")
            .configurable(true)
            .build();
        Decorator decorator4 = Decorator.decoratorBuilder()
            .type("Wiretap")
            .name("AFTER bob")
            .configurationId("3453")
            .configurable(true)
            .build();
        List<Decorator> decoratorList = new ArrayList<>();
        decoratorList.add(decorator1);
        decoratorList.add(decorator2);
        decoratorList.add(decorator3);
        decoratorList.add(decorator4);
        return decoratorList;
    }

    public static FlowElement getDevNullProducerWithFaultyWiretaps(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Dev Null Producer");
        List<Decorator> decoratorList = getGoodWiretaps();
        Decorator badDecorator1 = Decorator.decoratorBuilder()
            .type("XWiretap")
            .name("BEFORE bob")
            .configurationId("3450")
            .configurable(false)
            .build();
        Decorator badDecorator2 = Decorator.decoratorBuilder()
            .type("LogWiretap")
            .name("Before bob")
            .configurationId("3451")
            .configurable(true)
            .build();
        Decorator duplicateDecorator = Decorator.decoratorBuilder()
            .type("Wiretap")
            .name("AFTER bob")
            .configurationId("3453")
            .configurable(true)
            .build();

        FlowElement devNullProducer = FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My DevNull Producer")
            .decorators(decoratorList)
            .build();

        devNullProducer.addDecorator(badDecorator1);
        devNullProducer.addDecorator(badDecorator2);
        devNullProducer.addDecorator(duplicateDecorator);
        return devNullProducer;
    }

    public static FlowElement getEmailProducer(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Email Producer");
        FlowElement flowElement = FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Email Producer")
            .build();
        flowElement.setPropertyValue("bccRecipient", "myBccRecipient");
        flowElement.setPropertyValue("bccRecipients", "bcc1,bcc2");
        flowElement.setPropertyValue("ccRecipient", "myCcRecipient");
        flowElement.setPropertyValue("ccRecipients","cc1,cc2");
        flowElement.setPropertyValue("configuration", "myConfigurationClass");
        flowElement.setPropertyValue("configurationId", "myUniqueConfiguredResourceIdName");
        flowElement.setPropertyValue("criticalOnStartup", true);
        flowElement.setPropertyValue("emailBody", "myEmailBody");
        flowElement.setPropertyValue("emailFormat", "html");
        flowElement.setPropertyValue("extendedMailSessionProperties", "key1value1key2value2");
        flowElement.setPropertyValue("from", "FromAddress");
        flowElement.setPropertyValue("hasAttachments", true);
        flowElement.setPropertyValue("mailDebug", true);
        flowElement.setPropertyValue("mailMimeAddressStrict", true);
        flowElement.setPropertyValue("mailPassword", "myMailPassword");
        flowElement.setPropertyValue("mailPopClass", "myMailPopClass");
        flowElement.setPropertyValue("mailPopPort", 100);
        flowElement.setPropertyValue("mailPopUser", "myMailPopUser");
        flowElement.setPropertyValue("mailRuntimeEnvironment", "myMailRuntimeEnvironment");
        flowElement.setPropertyValue("mailSmtpClass", "myMailSmtpClass");
        flowElement.setPropertyValue("mailSmtpHost", "myMailSmtpHost");
        flowElement.setPropertyValue("mailSmtpPort", 101);
        flowElement.setPropertyValue("mailSmtpUser", "myMailSmtpUser");
        flowElement.setPropertyValue("mailStoreProtocol", "myMailStoreProtocol");
        flowElement.setPropertyValue("mailSubject", "myMailSubject");
        flowElement.setPropertyValue("mailhost", "myMailhostAddress");
        flowElement.setPropertyValue("toRecipient", "myToRecipient");
        flowElement.setPropertyValue("toRecipients", "to1,to2");
        flowElement.setPropertyValue("transportProtocol", "myTransportProtocol");
        flowElement.setPropertyValue("user", "myUser");
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    public static FlowElement getFtpProducer(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "FTP Producer");
        FlowElement flowElement = FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My FTP Producer")
            .build();
        flowElement.setPropertyValue("active", true);
        flowElement.setPropertyValue("checksumDelivered", true);
        flowElement.setPropertyValue("cleanupJournalOnComplete", true);
        flowElement.setPropertyValue("clientID", "myClientId");
        flowElement.setPropertyValue("configuration", "MyConfigurationClass");
        flowElement.setPropertyValue("configurationId", "myUniqueConfiguredResourceIdName");
        flowElement.setPropertyValue("createParentDirectory", true);
        flowElement.setPropertyValue("criticalOnStartup", true);
        flowElement.setPropertyValue("dataTimeout", 300001);
        flowElement.setPropertyValue("ftps", true);
        flowElement.setPropertyValue("ftpsIsImplicit", true);
        flowElement.setPropertyValue("ftpsKeyStoreFilePassword", "myFtpsKeyStoreFilePassword");
        flowElement.setPropertyValue("ftpsKeyStoreFilePath", "/test/ftps/keystore");
        flowElement.setPropertyValue("ftpsPort", 987);
        flowElement.setPropertyValue("ftpsProtocol", "SSL");
        flowElement.setPropertyValue("managedResourceRecoveryManager", "myManagedResourceRecoveryManagerClass");
        flowElement.setPropertyValue("maxRetryAttempts", 10);
        flowElement.setPropertyValue("outputDirectory", "/tmp/output");
        flowElement.setPropertyValue("overwrite", true);
        flowElement.setPropertyValue("password", "secret");
        flowElement.setPropertyValue("remoteHost", "myRemortHost");
        flowElement.setPropertyValue("remotePort", "1024");
        flowElement.setPropertyValue("renameExtension", "tmp");
        flowElement.setPropertyValue("socketTimeout", 22);
        flowElement.setPropertyValue("systemKey", "mySystemKey");
        flowElement.setPropertyValue("tempFileName", "myTempFiilename");
        flowElement.setPropertyValue("transactionManager", "myTransactionManagerClass");
        flowElement.setPropertyValue("unzip", true);
        flowElement.setPropertyValue("username", "myLoginName");
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    public static FlowElement getJmsProducer(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "JMS Producer");
        FlowElement flowElement = FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My JMS Producer")
            .build();

        flowElement.setPropertyValue("configuration", "MyConfigurationClass");
        flowElement.setPropertyValue("configuredResourceId", "__module.__flow.__component");
        flowElement.setPropertyValue("connectionFactory", "myConnectionFactory");
        flowElement.setPropertyValue("connectionFactoryJNDIProperties", "{key1:'value1',key2:'value2'}");
        flowElement.setPropertyValue("connectionFactoryJndiPropertyFactoryInitial", "myConnectionFactoryJndiPropertyFactoryInitial");
        flowElement.setPropertyValue("connectionFactoryJndiPropertyProviderUrl", "myConnectionFactoryJndiPropertyProviderUrl");
        flowElement.setPropertyValue("connectionFactoryJndiPropertySecurityCredentials", "myConnectionFactoryJndiPropertySecurityCredentials");
        flowElement.setPropertyValue("connectionFactoryJndiPropertySecurityPrincipal", "myConnectionFactoryJndiPropertySecurityPrincipal");
        flowElement.setPropertyValue("connectionFactoryJndiPropertyUrlPkgPrefixes", "myConnectionFactoryJndiPropertyUrlPkgPrefixes");
        flowElement.setPropertyValue("connectionFactoryName", "ConnectionFactory");
        flowElement.setPropertyValue("connectionFactoryPassword", "myConnectionFactoryPassword");
        flowElement.setPropertyValue("connectionFactoryUsername", "myConnectionFactoryUsername");
        flowElement.setPropertyValue("connectionPassword", "myConnectionPassword");
        flowElement.setPropertyValue("connectionUsername", "myConnectionUsername");
        flowElement.setPropertyValue("deliveryMode", 1);
        flowElement.setPropertyValue("deliveryPersistent", true);
        flowElement.setPropertyValue("destinationJndiName", "myDestinationJndiName");
        flowElement.setPropertyValue("destinationJndiProperties", "myDestinationJndiProperties");
        flowElement.setPropertyValue("destinationJndiPropertyFactoryInitial", "myDestinationJndiPropertyFactoryInitial");
        flowElement.setPropertyValue("destinationJndiPropertyProviderUrl", "myDestinationJndiPropertyProviderUrl");
        flowElement.setPropertyValue("destinationJndiPropertySecurityCredentials", "myDestinationJndiPropertySecurityCredentials");
        flowElement.setPropertyValue("destinationJndiPropertySecurityPrincipal", "myDestinationJndiPropertySecurityPrincipal");
        flowElement.setPropertyValue("destinationJndiPropertyUrlPkgPrefixes", "org.myapp");
        flowElement.setPropertyValue("explicitQosEnabled", true);
        flowElement.setPropertyValue("messageConverter", "myMessageConverter");
        flowElement.setPropertyValue("messageIdEnabled", true);
        flowElement.setPropertyValue("messageTimestampEnabled", true);
        flowElement.setPropertyValue("postProcessor", "myPostProcessor");
        flowElement.setPropertyValue("priority", 1);
        flowElement.setPropertyValue("pubSubDomain", "myPubSubDomain");
        flowElement.setPropertyValue("pubSubNoLocal", true);
        flowElement.setPropertyValue("receiveTimeout", 1000L);
        flowElement.setPropertyValue("sessionAcknowledgeMode", 1);
        flowElement.setPropertyValue("sessionAcknowledgeModeName", "AUTO_ACKNOWLEDGE");
        flowElement.setPropertyValue("sessionTransacted", true);
        flowElement.setPropertyValue("timeToLive", 100L);
        flowElement.setPropertyValue("transactionManager", "myTransactionManagerClass");
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    public static FlowElement getBasicAmqJmsProducer(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "JMS Producer");
        FlowElement flowElement = FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My JMS Producer")
            .build();

        flowElement.setPropertyValue("configuredResourceId", "myUniqueConfiguredResourceIdName");
        flowElement.setPropertyValue("connectionFactoryJndiPropertyFactoryInitial", "myConnectionFactoryJndiPropertyFactoryInitial");
        flowElement.setPropertyValue("connectionFactoryJndiPropertyProviderUrl", "myConnectionFactoryJndiPropertyProviderUrl");
        flowElement.setPropertyValue("connectionFactoryName", "ConnectionFactory");
        flowElement.setPropertyValue("connectionFactoryPassword", "myConnectionFactoryPassword");
        flowElement.setPropertyValue("connectionFactoryUsername", "myConnectionFactoryUsername");
        flowElement.setPropertyValue("destinationJndiName", "myDestinationJndiName");
        flowElement.setPropertyValue("destinationJndiPropertyFactoryInitial", "myDestinationJndiPropertyFactoryInitial");
        flowElement.setPropertyValue("destinationJndiPropertyProviderUrl", "myDestinationJndiPropertyProviderUrl");
        flowElement.setPropertyValue("destinationJndiPropertySecurityCredentials", "myDestinationJndiPropertySecurityCredentials");
        flowElement.setPropertyValue("destinationJndiPropertySecurityPrincipal", "myDestinationJndiPropertySecurityPrincipal");
        flowElement.setPropertyValue("pubSubDomain", "myPubSubDomain");
        flowElement.setPropertyValue("sessionTransacted", true);
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    public static FlowElement getSftpProducer(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "SFTP Producer");
        FlowElement flowElement = FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My SFTP Producer")
            .build();

        flowElement.setPropertyValue("checksumDelivered", true);
        flowElement.setPropertyValue("cleanupChunks", true);
        flowElement.setPropertyValue("cleanupJournalOnComplete", true);
        flowElement.setPropertyValue("clientID", "myClientId");
        flowElement.setPropertyValue("configuration", "MyConfigurationClass");
        flowElement.setPropertyValue("configurationId", "myUniqueConfiguredResourceIdName");
        flowElement.setPropertyValue("createParentDirectory", true);
        flowElement.setPropertyValue("criticalOnStartup", true);
        flowElement.setPropertyValue("knownHostFilename", "~/.ssh/knownhosts");
        flowElement.setPropertyValue("managedResourceRecoveryManager", "myManagedResourceRecoveryManagerClass");
        flowElement.setPropertyValue("maxRetryAttempts", 10);
        flowElement.setPropertyValue("outputDirectory", "/tmp/output");
        flowElement.setPropertyValue("password", "secret");
        flowElement.setPropertyValue("preferredKeyExchangeAlgorithm", "DFS");
        flowElement.setPropertyValue("privateKeyFilename", "~/.ssh/meyfile.jks");
        flowElement.setPropertyValue("remoteHost", "myRemortHost");
        flowElement.setPropertyValue("remotePort", "1024");
        flowElement.setPropertyValue("renameExtension", "tmp");
        flowElement.setPropertyValue("tempFileName", "myTempFiilename");
        flowElement.setPropertyValue("unzip", true);
        flowElement.setPropertyValue("username", "myLoginName");
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    public static FlowElement getLoggingProducer(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Logging Producer");

        FlowElement flowElement = FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Logging Producer")
            .build();
        flowElement.setPropertyValue("configurationId", "MyResourceID");
        flowElement.setPropertyValue("regExpPattern", "this");
        flowElement.setPropertyValue("replacementText", "that");
        flowElement.setPropertyValue("logEveryNth", 2);
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    // ------------------------- Router -------------------------

    public static FlowElement getMultiRecipientRouter(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Multi Recipient Router");
        List<String> routes = Arrays.asList("route1", "route2");

        FlowElement flowElement =  FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Multi Recipient Router")
            .build();
        flowElement.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, "myMultiRecipientRouter");
        flowElement.setPropertyValue("configurationId", "MyResourceID");
        flowElement.setPropertyValue("configuration", "MyConfigurationClass");
        flowElement.setPropertyValue("configuredResource", true);
        flowElement.setPropertyValue(FROM_TYPE, "java.lang.String");
        flowElement.setPropertyValue(ROUTE_NAMES, routes);
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    public static FlowElement getSingleRecipientRouter(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Single Recipient Router");
        List<String> routes = Arrays.asList("route1", "route2");

        FlowElement flowElement =  FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Single Recipient Router")
            .build();
        flowElement.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, "mySingleRecipientRouter");
        flowElement.setPropertyValue("configuredResourceId", "MyResourceID");
        flowElement.setPropertyValue("configuration", "MyConfigurationClass");
        flowElement.setPropertyValue("configuredResource", true);
        flowElement.setPropertyValue(FROM_TYPE, "java.lang.String");
        flowElement.setPropertyValue(ROUTE_NAMES, routes);
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    // ------------------------- Splitter -------------------------
    public static FlowElement getCustomSplitter(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Custom Splitter");
        FlowElement flowElement =  FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Custom Splitter")
            .build();
        flowElement.setPropertyValue(FROM_TYPE, "java.lang.String");
        flowElement.setPropertyValue(TO_TYPE, "java.lang.String");
        flowElement.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, "mySplitter");
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    public static FlowElement getDefaultListSplitter(String metaPackVersion) throws StudioBuildException {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Default List Splitter");
        FlowElement flowElement =  FlowElement.flowElementBuilder()
            .componentMeta(meta)
            .componentName("My Default List Splitter")
            .build();
        flowElement.setPropertyValue(FROM_TYPE, "java.lang.String");
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    // ------------------------ Flow Combinations --------------------

    public static Flow getExceptionResolverFlow(String metaPackVersion) throws StudioBuildException {
        ExceptionResolver exceptionResolver = getExceptionResolver(metaPackVersion);
        return getUnbuiltFlow(metaPackVersion)
            .exceptionResolver(exceptionResolver)
            .build();
    }

    private static Map<String, ComponentProperty> getRetryProperties(String metaPackVersion) throws StudioBuildException {
        ExceptionResolverMeta exceptionResolverMeta = (ExceptionResolverMeta)IkasanComponentLibrary.getIkasanComponentByKeyMandatory(metaPackVersion, "Exception Resolver");
        Map<String, ComponentPropertyMeta> retryPropertiesMeta = exceptionResolverMeta.getExceptionActionWithName("retry").getActionProperties();

        Map<String, ComponentProperty>  componentPropertyHashMap = new HashMap<>();
        componentPropertyHashMap.put("delay", new ComponentProperty(retryPropertiesMeta.get("delay"), 1));
        componentPropertyHashMap.put("interval", new ComponentProperty(retryPropertiesMeta.get("interval"), 2));

        return componentPropertyHashMap;
    }

    public static Flow getEventGeneratingConsumerCustomConverterDevNullProducerFlow(String metaPackVersion) throws StudioBuildException {
        FlowElement eventGeneratingConsumer = getEventGeneratingConsumer(metaPackVersion);
        FlowElement customConverter = getCustomConverter(metaPackVersion);
        FlowElement devNullProducer = TestFixtures.getDevNullProducer(metaPackVersion);
        Flow flow = getUnbuiltFlow(metaPackVersion)
            .consumer(eventGeneratingConsumer)
            .build();
        flow.setFlowRoute(FlowRoute.flowRouteBuilder().flow(flow).flowElements(new ArrayList<>(Arrays.asList(customConverter,devNullProducer))).build());
        return flow;
    }

    public static Flow getEventGeneratingConsumerCustomConverterDevNullProducerWithWiretapsFlow(String metaPackVersion) throws StudioBuildException {
        FlowElement eventGeneratingConsumer = getEventGeneratingConsumer(metaPackVersion);
        FlowElement customConverter = getCustomConverter(metaPackVersion);
        FlowElement devNullProducer = TestFixtures.getDevNullProducerWithWiretaps(metaPackVersion);
        Flow flow = getUnbuiltFlow(metaPackVersion)
                .consumer(eventGeneratingConsumer)
                .build();
        flow.setFlowRoute(FlowRoute.flowRouteBuilder().flow(flow).flowElements(new ArrayList<>(Arrays.asList(customConverter,devNullProducer))).build());
        return flow;
    }

    public static Flow getEventGeneratingConsumerCustomConverterDevNullProducerWithFaultyWiretapsFlow(String metaPackVersion) throws StudioBuildException {
        FlowElement eventGeneratingConsumer = getEventGeneratingConsumer(metaPackVersion);
        FlowElement customConverter = getCustomConverter(metaPackVersion);
        FlowElement devNullProducer = TestFixtures.getDevNullProducerWithFaultyWiretaps(metaPackVersion);
        Flow flow = getUnbuiltFlow(metaPackVersion)
                .consumer(eventGeneratingConsumer)
                .build();
        flow.setFlowRoute(FlowRoute.flowRouteBuilder().flow(flow).flowElements(new ArrayList<>(Arrays.asList(customConverter,devNullProducer))).build());
        return flow;
    }

    public static Flow getEventGeneratingConsumerCustomConverterDevNullProducerFlowWithDebugOnEachElement(String metaPackVersion) throws StudioBuildException {
        FlowElement eventGeneratingConsumer = getEventGeneratingConsumer(metaPackVersion);
        FlowElement customConverterDebug = getDebugTransition(metaPackVersion);
        FlowElement customConverter = getCustomConverter(metaPackVersion);
        FlowElement devNullProducerDebug = getDebugTransition(metaPackVersion);
        FlowElement devNullProducer = getDevNullProducer(metaPackVersion);
        Flow flow = getUnbuiltFlow(metaPackVersion)
                .consumer(eventGeneratingConsumer)
                .build();
        flow.setFlowRoute(FlowRoute.flowRouteBuilder().flow(flow).flowElements(new ArrayList<>(Arrays.asList(customConverterDebug, customConverter, devNullProducerDebug, devNullProducer))).build());
        return flow;
    }

    public static Flow getEventGeneratingConsumerRouterFlow(String metaPackVersion) throws StudioBuildException {
        FlowElement eventGeneratingConsumer = getEventGeneratingConsumer(metaPackVersion);
        FlowElement customConverter = getCustomConverter(metaPackVersion);
        FlowElement router = getMultiRecipientRouter(metaPackVersion);
        FlowElement devNullProducer1 = TestFixtures.getDevNullProducer(metaPackVersion);
        devNullProducer1.setComponentName("My DevNull Producer1");
        FlowElement devNullProducer2 = TestFixtures.getDevNullProducer(metaPackVersion);
        devNullProducer2.setComponentName("My DevNull Producer2");

        Flow flow = getUnbuiltFlow(metaPackVersion)
                .consumer(eventGeneratingConsumer)
                .build();

        FlowRoute firstRoute = FlowRoute.flowRouteBuilder().flow(flow).routeName("route1").flowElements(new ArrayList<>(Collections.singletonList(devNullProducer1))).build();
        FlowRoute secondRoute = FlowRoute.flowRouteBuilder().flow(flow).routeName("route2").flowElements(new ArrayList<>(Collections.singletonList(devNullProducer2))).build();
        List<FlowRoute> childRoutes = new ArrayList<>();
        childRoutes.add(firstRoute);
        childRoutes.add(secondRoute);

        flow.setFlowRoute(FlowRoute.flowRouteBuilder()
                .flow(flow)
                .flowElements(new ArrayList<>(Arrays.asList(customConverter, router)))
                .childRoutes(childRoutes)
                .build());
        return flow;
    }
}
