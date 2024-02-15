package org.ikasan.studio.generator;

import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.model.ikasan.instance.*;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentPropertyMeta;
import org.ikasan.studio.model.ikasan.meta.IkasanExceptionResolutionMeta;

import java.util.List;

public class TestFixtures {
    public static final String DEFAULT_PACKAGE = "org.ikasan";

    public static Module getIkasanModule() {
        Module ikasanModule = new Module();
        ikasanModule.setComponentName("My Integration Module");
        ikasanModule.setApplicationPackageName("org.myApp");
        ikasanModule.setPort("8080");
        ikasanModule.setH2DbPortNumber("12452");
        ikasanModule.setH2WebPortNumber("12452");
        ikasanModule.setH2WebPortNumber("12452");
        return ikasanModule;
    }

    /**
     * Create a fully populated
     * See resources/studio/componentDefinitions/CUSTOM_CONVERTER_en_GB.csv
     * @return a FullyPopulatedCustomConverter
     */
    public static FlowElement getFullyPopulatedCustomConverterComponent(Flow ikasanFlow) {
        FlowElement component = FlowElementFactory.createFlowElement(null, null);;
//        FlowElement component = FlowElement.createFlowElement(IkasanComponentMetax.CUSTOM_CONVERTER, ikasanFlow);
        component.setComponentName("testCustomConverter");

        // Mandatory properties
        component.setPropertyValue("BespokeClassName", "MyConverterClass");
        component.setPropertyValue("FromType", java.lang.String.class);
        component.setPropertyValue("ToType", java.lang.Integer.class);
        return component;
    }

    /**
     * Create a fully populated FILTER
     * See resources/studio/componentDefinitions/MESSAGE_FILTER_en_GB.csv
     * @return a FullyPopulatedCustomConverter
     */
    public static FlowElement getFullyPopulatedFilterComponent(Flow ikasanFlow) {
        FlowElement component = FlowElementFactory.createFlowElement(null, null);;
//FlowElement component = FlowElement.createFlowElement(IkasanComponentMetax.MESSAGE_FILTER, ikasanFlow);
        component.setComponentName("testFilterComponent");

        // Mandatory properties
        component.setPropertyValue("BespokeClassName", "MyMessageFilter");
        component.setPropertyValue("FromType", java.lang.String.class);
        component.setPropertyValue("Configuration", "MyConfigurationClass");
        component.setPropertyValue("ConfiguredResourceId", "MyConfiguredResourceId");
        component.setPropertyValue("Description", "Test description");
        component.setPropertyValue("IsConfiguredResource", true);
        return component;
    }

     /**
     * Create a fully populated FILTER
     * See resources/studio/componentDefinitions/MESSAGE_FILTER_en_GB.csv
     * @return a FullyPopulatedCustomConverter
     */
    public static FlowElement getMinimumPopulatedFilterComponent(Flow ikasanFlow) {
        FlowElement component = FlowElementFactory.createFlowElement(null, null);;
//FlowElement component = FlowElement.createFlowElement(IkasanComponentMetax.MESSAGE_FILTER, ikasanFlow);
        component.setComponentName("testFilterComponent");

        // Mandatory properties
        component.setPropertyValue("BespokeClassName", "MyMessageFilter");
        component.setPropertyValue("FromType", java.lang.String.class);
        return component;
    }

    /**
     * Create a fully populated
     * See also resources/studio/componentDefinitions/FTP_CONSUMER_en_GB.csv
     * @return a FullyPopulatedFtpConsumerComponent
     */
    public static FlowElement getFullyPopulatedFtpConsumerComponent(Flow ikasanFlow) {
        FlowElement component = FlowElementFactory.createFlowElement(null, null);;
//FlowElement component = FlowElement.createFlowElement(IkasanComponentMetax.FTP_CONSUMER, ikasanFlow);
        component.setComponentName("testFtpConsumer");

        // Mandatory properties
        component.setPropertyValue("CronExpression", "*/5 * * * * ?");
        component.setPropertyValue("FilenamePattern", "*Test.txt");
        component.setPropertyValue("Password", "secret");
        component.setPropertyValue("RemoteHost", "myRemortHost");
        component.setPropertyValue("RemotePort", "1024");
        component.setPropertyValue("SourceDirectory", "/test/source/directory/");
        component.setPropertyValue("Username", "myLoginName");

        // Optional properties
        component.setPropertyValue("Active", true);
        component.setPropertyValue("AgeOfFiles", 10);
        component.setPropertyValue("Checksum", true);
        component.setPropertyValue("Chronological", true);
        component.setPropertyValue("ChunkSize", 1048577);
        component.setPropertyValue("Chunking", true);
        component.setPropertyValue("CleanupJournalOnComplete", true);
        component.setPropertyValue("ClientID", "myClientId");
        component.setPropertyValue("Configuration", "MyConfigurationClass");
        component.setPropertyValue("ConfiguredResourceId", "myUniqueConfiguredResourceIdName");
        component.setPropertyValue("ConnectionTimeout", 600001);
        component.setPropertyValue("DataTimeout", 300001);
        component.setPropertyValue("Destructive", true);
        component.setPropertyValue("FTPS", true);
        component.setPropertyValue("FilterDuplicates", true);
        component.setPropertyValue("FilterOnFilename", true);
        component.setPropertyValue("FilterOnLastModifiedDate", true);
        component.setPropertyValue("FtpsIsImplicit", true);
        component.setPropertyValue("FtpsKeyStoreFilePassword", "myFtpsKeyStoreFilePassword");
        component.setPropertyValue("FtpsKeyStoreFilePath", "/test/ftps/keystore");
        component.setPropertyValue("FtpsPort", 987);
        component.setPropertyValue("FtpsProtocol", "SSL");
        component.setPropertyValue("IgnoreMisfire",  true);
        component.setPropertyValue("IsRecursive", true);
        component.setPropertyValue("ManagedEventIdentifierService", "myManagedEventIdentifierServiceClass");
        component.setPropertyValue("ManagedResourceRecoveryManager", "myManagedResourceRecoveryManagerClass");
        component.setPropertyValue("MaxEagerCallbacks", 1);
        component.setPropertyValue("MaxRetryAttempts", 10);
        component.setPropertyValue("MaxRows", 11);
        component.setPropertyValue("MessageProvider", "myMessageProviderClass");
        component.setPropertyValue("MinAge", 12);
        component.setPropertyValue("MoveOnSuccessNewPath", "/test/move/on/success");
        component.setPropertyValue("MoveOnSuccess", true);
        component.setPropertyValue("PasswordFilePath", "/test/password/file/path");
        component.setPropertyValue("RenameOnSuccessExtension", "newExtension");
        component.setPropertyValue("RenameOnSuccess", true);
        component.setPropertyValue("ScheduledJobGroupName",  "myScheduledJobGroupName");
        component.setPropertyValue("ScheduledJobName", "myScheduledJobName");
        component.setPropertyValue("SocketTimeout", 22);
        component.setPropertyValue("SourceDirectoryURLFactory", "myDirectoryURLFactoryClass");
        component.setPropertyValue("SystemKey", "mySystemKey");
        component.setPropertyValue("TransactionManager", "myTransactionManagerClass");

        return component;
    }

    /**
     * Create a fully populated
     * See also resources/studio/componentDefinitions/FTP_PRODUCER_en_GB.csv
     * @return a FullyPopulatedFtpProducerComponent
     */
    public static FlowElement getFullyPopulatedFtpProducerComponent(Flow ikasanFlow) {
        FlowElement component = FlowElementFactory.createFlowElement(null, null);;
//FlowElement component = FlowElement.createFlowElement(IkasanComponentMetax.FTP_PRODUCER, ikasanFlow);
        component.setComponentName("testFtpProducer");

        // Mandatory properties
        component.setPropertyValue("ClientID", "myClientID");
        component.setPropertyValue("OutputDirectory", "myOutputDirectory");
        component.setPropertyValue("Password", "secret");
        component.setPropertyValue("RemoteHost", "myRemortHost");
        component.setPropertyValue("RemotePort", "1024");
        component.setPropertyValue("Username", "myLoginName");

        // Optional properties
        component.setPropertyValue("Active", true);
        component.setPropertyValue("ChecksumDelivered", true);
        component.setPropertyValue("CleanupJournalOnComplete", true);
        component.setPropertyValue("ClientID", "myClientId");
        component.setPropertyValue("Configuration", "MyConfigurationClass");
        component.setPropertyValue("ConfiguredResourceId", "myUniqueConfiguredResourceIdName");
        component.setPropertyValue("CreateParentDirectory", true);
        component.setPropertyValue("CriticalOnStartup", true);
        component.setPropertyValue("DataTimeout", 300001);
        component.setPropertyValue("FTPS", true);
        component.setPropertyValue("FtpsIsImplicit", true);
        component.setPropertyValue("FtpsKeyStoreFilePassword", "myFtpsKeyStoreFilePassword");
        component.setPropertyValue("FtpsKeyStoreFilePath", "/test/ftps/keystore");
        component.setPropertyValue("FtpsPort", 987);
        component.setPropertyValue("FtpsProtocol", "SSL");
        component.setPropertyValue("ManagedResourceRecoveryManager", "myManagedResourceRecoveryManagerClass");
        component.setPropertyValue("MaxRetryAttempts", 10);
        component.setPropertyValue("Overwrite", true);
        component.setPropertyValue("RenameExtension", "newExtension");
        component.setPropertyValue("SocketTimeout", 22);
        component.setPropertyValue("SystemKey", "mySystemKey");
        component.setPropertyValue("TempFileName", "myTempFileName");
        component.setPropertyValue("TransactionManager", "myTransactionManagerClass");
        component.setPropertyValue("Unzip", true);
        return component;
    }

    /**
     * Create a fully populated
     * See resources/studio/componentDefinitions/SFTP_CONSUMER_en_GB.csv
     * @return a FullyPopulatedSftpConsumerComponent
     */
    public static FlowElement getFullyPopulatedSftpConsumerComponent(Flow ikasanFlow) {
        FlowElement component = FlowElementFactory.createFlowElement(null, null);;
//FlowElement component = FlowElement.createFlowElement(IkasanComponentMetax.SFTP_CONSUMER, ikasanFlow);
        component.setComponentName("testSftpConsumer");

        // Mandatory properties
        component.setPropertyValue("CronExpression", "*/5 * * * * ?");
        component.setPropertyValue("FilenamePattern", "*Test.txt");
        component.setPropertyValue("Password", "secret");
        component.setPropertyValue("RemoteHost", "myRemortHost");
        component.setPropertyValue("RemotePort", "1024");
        component.setPropertyValue("SourceDirectory", "/test/source/directory/");
        component.setPropertyValue("Username", "myLoginName");

        // Optional properties
        component.setPropertyValue("AgeOfFiles", 10);
        component.setPropertyValue("Checksum", true);
        component.setPropertyValue("Chronological", true);
        component.setPropertyValue("ChunkSize", 1048577);
        component.setPropertyValue("Chunking", true);
        component.setPropertyValue("CleanupJournalOnComplete", true);
        component.setPropertyValue("ClientID", "myClientId");
        component.setPropertyValue("Configuration", "MyConfigurationClass");
        component.setPropertyValue("ConfiguredResourceId", "myUniqueConfiguredResourceIdName");
        component.setPropertyValue("ConnectionTimeout", 600001);
        component.setPropertyValue("CriticalOnStartup", true);
        component.setPropertyValue("Destructive", true);
        component.setPropertyValue("Eager", true);
        component.setPropertyValue("FilterDuplicates", true);
        component.setPropertyValue("FilterOnFilename", true);
        component.setPropertyValue("FilterOnLastModifiedDate", true);
        component.setPropertyValue("IgnoreMisfire", true);
        component.setPropertyValue("IsRecursive", true);
        component.setPropertyValue("KnownHostsFilename", "myKnownHostsFilename");
        component.setPropertyValue("ManagedEventIdentifierService", "myManagedEventIdentifierServiceClass");
        component.setPropertyValue("ManagedResourceRecoveryManager", "myManagedResourceRecoveryManagerClass");
        component.setPropertyValue("MaxEagerCallbacks", 1);
        component.setPropertyValue("MaxRetryAttempts", 10);
        component.setPropertyValue("MaxRows", 11);
        component.setPropertyValue("MessageProvider", "myMessageProviderClass");
        component.setPropertyValue("MinAge", 12);
        component.setPropertyValue("MoveOnSuccessNewPath", "/test/move/on/success");
        component.setPropertyValue("MoveOnSuccess", true);
        component.setPropertyValue("PreferredKeyExchangeAlgorithm", "myPreferredKeyExchangeAlgorithm");
        component.setPropertyValue("PrivateKeyFilename", "myPrivateKeyFilename");
        component.setPropertyValue("RenameOnSuccess",  true);
        component.setPropertyValue("RenameOnSuccessExtension", "newExtension");
        component.setPropertyValue("ScheduledJobGroupName", "myScheduledJobGroupName");
        component.setPropertyValue("ScheduledJobName", "myScheduledJobName");
        component.setPropertyValue("SourceDirectoryURLFactory", "myDirectoryURLFactoryClass");
        component.setPropertyValue("TransactionManager", "myTransactionManagerClass");
        component.setPropertyValue("Timezone", "GMT");
        return component;
    }

    /**
     * Create a fully populated
     * See also resources/studio/componentDefinitions/SFTP_PRODUCER_en_GB.csv
     * @return a FullyPopulatedSftpProducerComponent
     */
    public static FlowElement getFullyPopulatedSftpProducerComponent(Flow ikasanFlow) {
        FlowElement component = FlowElementFactory.createFlowElement(null, null);;
//FlowElement component = FlowElement.createFlowElement(IkasanComponentMetax.SFTP_PRODUCER, ikasanFlow);
        component.setComponentName("testSftpProducer");

        // Mandatory properties
        component.setPropertyValue("ClientID", "myClientID");
        component.setPropertyValue("OutputDirectory", "myOutputDirectory");
        component.setPropertyValue("Password", "secret");
        component.setPropertyValue("RemoteHost", "myRemortHost");
        component.setPropertyValue("RemotePort", "1024");
        component.setPropertyValue("Username", "myLoginName");

        // Optional properties
        component.setPropertyValue("ChecksumDelivered", true);
        component.setPropertyValue("CleanUpChunks", true);
        component.setPropertyValue("CleanupJournalOnComplete", true);
        component.setPropertyValue("Configuration", "MyConfigurationClass");
        component.setPropertyValue("ConfiguredResourceId", "myUniqueConfiguredResourceIdName");
        component.setPropertyValue("ConnectionTimeout", 300001);
        component.setPropertyValue("CreateParentDirectory", true);
        component.setPropertyValue("CriticalOnStartup", true);
        component.setPropertyValue("KnownHostsFilename", "myKnownHostsFilename");
        component.setPropertyValue("ManagedResourceRecoveryManager", "myManagedResourceRecoveryManagerClass");
        component.setPropertyValue("MaxRetryAttempts", 10);
        component.setPropertyValue("PreferredKeyExchangeAlgorithm", "myPreferredKeyExchangeAlgorithm");
        component.setPropertyValue("PrivateKeyFilename", "myPrivateKeyFilename");
        component.setPropertyValue("RenameExtension", "newExtension");
        component.setPropertyValue("TempFileName", "myTempFileName");
        component.setPropertyValue("Unzip", true);
        return component;
    }

    /**
     * Create a fully populated
     * See resources/studio/componentDefinitions/LOCAL_FILE_CONSUMER_en_GB.csv
     * @return a FullyPopulatedLocalFileConverter
     */
    public static FlowElement getFullyPopulatedLocalFileConsumerComponent(Flow ikasanFlow) {
        FlowElement component = FlowElementFactory.createFlowElement(null, null);;
//FlowElement component = FlowElement.createFlowElement(IkasanComponentMetax.LOCAL_FILE_CONSUMER, ikasanFlow);
        component.setComponentName("testLocalFileConsumer");

        // Mandatory properties
        component.setPropertyValue("CronExpression", "*/5 * * * * ?");
        component.setPropertyValue("Filenames", "*Test.txt");
        component.setPropertyValue("Configuration", "MyConfigurationClass");
        component.setPropertyValue("ConfiguredResourceId", "myUniqueConfiguredResourceIdName");
        component.setPropertyValue("CriticalOnStartup", true);
        component.setPropertyValue("DirectoryDepth", 1);
        component.setPropertyValue("Eager", true);
        component.setPropertyValue("Encoding", "UTF-8");
        component.setPropertyValue("EventFactory", "myEventFactoryClassName");
        component.setPropertyValue("IgnoreFileRenameWhilstScanning", true);
        component.setPropertyValue("IgnoreMisfire", true);
        component.setPropertyValue("ManagedResourceRecoveryManager", "myManagedResourceRecoveryManagerClass");
        component.setPropertyValue("MaxEagerCallbacks", 1);
        component.setPropertyValue("MessageProvider", "myMessageProviderClass");
        component.setPropertyValue("MessageProviderPostProcessor", "MyMessageProviderPostProcessor");
        component.setPropertyValue("ScheduledJobGroupName", "myScheduledJobGroupName");
        component.setPropertyValue("ScheduledJobName", "myScheduledJobName");
        component.setPropertyValue("SortAscending", true);
        component.setPropertyValue("SortByModifiedDateTime",  12);
        component.setPropertyValue("Timezone", "GMT");
        return component;
    }

    /**
     * Create a fully populated
     * See also resources/studio/componentDefinitions/SCHEDULED_CONSUMER_en_GB.csv
     * @return a FullyPopulatedScheduledConsumerComponent
     */
    public static FlowElement getFullyPopulatedScheduledConsumerComponent(Flow ikasanFlow) {
        FlowElement component = FlowElementFactory.createFlowElement(null, null);;
//FlowElement component = FlowElement.createFlowElement(IkasanComponentMetax.SCHEDULED_CONSUMER, ikasanFlow);
        component.setComponentName("testScheduledConsumer");

        // Mandatory properties
        component.setPropertyValue("CronExpression", "*/5 * * * * ?");

        // Optional properties
        component.setPropertyValue("Configuration", "MyConfigurationClass");
        component.setPropertyValue("ConfiguredResourceId", "myUniqueConfiguredResourceIdName");
        component.setPropertyValue("CriticalOnStartup", true);
        component.setPropertyValue("Eager", true);
        component.setPropertyValue("EventFactory", "myEventFactory");
        component.setPropertyValue("IgnoreMisfire",  true);
        component.setPropertyValue("ManagedEventIdentifierService", "myManagedEventIdentifierServiceClass");
        component.setPropertyValue("ManagedResourceRecoveryManager", "myManagedResourceRecoveryManagerClass");
        component.setPropertyValue("MaxEagerCallbacks", 1);
        component.setPropertyValue("MessageProvider", "myMessageProviderClass");
        component.setPropertyValue("ScheduledJobGroupName",  "myScheduledJobGroupName");
        component.setPropertyValue("ScheduledJobName", "myScheduledJobName");
        component.setPropertyValue("Timezone", "UTC");

        return component;
    }

    /**
     * Create a fully populated
     * See resources/studio/componentDefinitions/SPRING_JMS_CONSUMER_en_GB.csv
     * @return a FullyPopulatedJmsConsumer
     */
    public static FlowElement getFullyPopulatedSpringJmsConsumerComponent(Flow ikasanFlow) {
        FlowElement component = FlowElementFactory.createFlowElement(null, null);;
//FlowElement component = FlowElement.createFlowElement(IkasanComponentMetax.SPRING_JMS_CONSUMER, ikasanFlow);
        component.setComponentName("testJmsConsumer");
        component.setPropertyValue("AutoContentConversion", "true");
        component.setPropertyValue("AutoSplitBatch", "true");
        component.setPropertyValue("BatchMode", "true");
        component.setPropertyValue("BatchSize", 10);
        component.setPropertyValue("CacheLevel", 1);
        component.setPropertyValue("ConcurrentConsumers", 10);
        component.setPropertyValue("Configuration", "MyConfigurationClass");
        component.setPropertyValue("ConfiguredResourceId", "myUniqueConfiguredResourceIdName");
        component.setPropertyValue("ConnectionFactory", "myConnectionFactory");
        component.setPropertyValue("ConnectionFactoryJNDIProperties", "{key1:'value1',key2:'value2'}");
        component.setPropertyValue("ConnectionFactoryJndiPropertyFactoryInitial", "myConnectionFactoryJndiPropertyFactoryInitial");
        component.setPropertyValue("ConnectionFactoryJndiPropertyProviderUrl", "myConnectionFactoryJndiPropertyProviderUrl");
        component.setPropertyValue("ConnectionFactoryJndiPropertySecurityCredentials", "myConnectionFactoryJndiPropertySecurityCredentials");
        component.setPropertyValue("ConnectionFactoryJndiPropertySecurityPrincipal", "myConnectionFactoryJndiPropertySecurityPrincipal");
        component.setPropertyValue("ConnectionFactoryJndiPropertyUrlPkgPrefixes", "myConnectionFactoryJndiPropertyUrlPkgPrefixes");
        component.setPropertyValue("ConnectionFactoryName", "myConnectionFactoryName");
        component.setPropertyValue("ConnectionFactoryPassword", "myConnectionFactoryPassword");
        component.setPropertyValue("ConnectionFactoryUsername", "myConnectionFactoryUsername");
        component.setPropertyValue("ConnectionPassword", "myConnectionPassword");
        component.setPropertyValue("ConnectionUsername", "myConnectionUsername");
        component.setPropertyValue("DestinationJndiName", "myDestinationJndiName");
        component.setPropertyValue("DestinationJndiProperties", "myDestinationJndiProperties");
        component.setPropertyValue("DestinationJndiPropertyFactoryInitial", "myDestinationJndiPropertyFactoryInitial");
        component.setPropertyValue("DestinationJndiPropertyProviderUrl", "myDestinationJndiPropertyProviderUrl");
        component.setPropertyValue("DestinationJndiPropertySecurityCredentials", "myDestinationJndiPropertySecurityCredentials");
        component.setPropertyValue("DestinationJndiPropertySecurityPrincipal", "myDestinationJndiPropertySecurityPrincipal");
        component.setPropertyValue("DestinationJndiPropertyUrlPkgPrefixes", "org.myapp");
        component.setPropertyValue("Durable", "true");
        component.setPropertyValue("DurableSubscriptionName", "myDurableSubscriptionName");
        component.setPropertyValue("EventFactory", "myEventFactoryClassName");
        component.setPropertyValue("ManagedIdentifierService", "myManagedIdentifierService");
        component.setPropertyValue("MaxConcurrentConsumers", 11);
        component.setPropertyValue("MessageProvider", "myMessageProvider");
        component.setPropertyValue("PubSubDomain", "myPubSubDomain");
        component.setPropertyValue("ReceiveTimeout", 1000L);
        component.setPropertyValue("SessionAcknowledgeMode", 1);
        component.setPropertyValue("SessionTransacted", "true");
        component.setPropertyValue("TransactionManager", "myTransactionManagerClass");

        return component;
    }

    /**
     * Create a fully populated
     * See resources/studio/componentDefinitions/JMS_PRODUCER_en_GB.csv
     * @return a FullyPopulatedJmsProducer
     */
    public static FlowElement getFullyPopulatedJmsProducerComponent(Flow ikasanFlow) {
        FlowElement component = FlowElementFactory.createFlowElement(null, null);;
//FlowElement component = FlowElement.createFlowElement(IkasanComponentMetax.JMS_PRODUCER, ikasanFlow);
        component.setComponentName("testJmsConsumer");
        component.setPropertyValue("Configuration", "MyConfigurationClass");
        component.setPropertyValue("ConfiguredResourceId", "myUniqueConfiguredResourceIdName");
        component.setPropertyValue("ConnectionFactory", "myConnectionFactory");
        component.setPropertyValue("ConnectionFactoryJNDIProperties", "{key1:'value1',key2:'value2'}");
        component.setPropertyValue("ConnectionFactoryJndiPropertyFactoryInitial", "myConnectionFactoryJndiPropertyFactoryInitial");
        component.setPropertyValue("ConnectionFactoryJndiPropertyProviderUrl", "myConnectionFactoryJndiPropertyProviderUrl");
        component.setPropertyValue("ConnectionFactoryJndiPropertySecurityCredentials", "myConnectionFactoryJndiPropertySecurityCredentials");
        component.setPropertyValue("ConnectionFactoryJndiPropertySecurityPrincipal", "myConnectionFactoryJndiPropertySecurityPrincipal");
        component.setPropertyValue("ConnectionFactoryJndiPropertyUrlPkgPrefixes", "myConnectionFactoryJndiPropertyUrlPkgPrefixes");
        component.setPropertyValue("ConnectionFactoryName", "myConnectionFactoryName");
        component.setPropertyValue("ConnectionFactoryPassword", "myConnectionFactoryPassword");
        component.setPropertyValue("ConnectionFactoryUsername", "myConnectionFactoryUsername");
        component.setPropertyValue("ConnectionPassword", "myConnectionPassword");
        component.setPropertyValue("ConnectionUsername", "myConnectionUsername");
        component.setPropertyValue("DeliveryMode", 1);
        component.setPropertyValue("DeliveryPersistent", true);
        component.setPropertyValue("DestinationJndiName", "myDestinationJndiName");
        component.setPropertyValue("DestinationJndiProperties", "myDestinationJndiProperties");
        component.setPropertyValue("DestinationJndiPropertyFactoryInitial", "myDestinationJndiPropertyFactoryInitial");
        component.setPropertyValue("DestinationJndiPropertyProviderUrl", "myDestinationJndiPropertyProviderUrl");
        component.setPropertyValue("DestinationJndiPropertySecurityCredentials", "myDestinationJndiPropertySecurityCredentials");
        component.setPropertyValue("DestinationJndiPropertySecurityPrincipal", "myDestinationJndiPropertySecurityPrincipal");
        component.setPropertyValue("DestinationJndiPropertyUrlPkgPrefixes", "org.myapp");
        component.setPropertyValue("ExplicitQosEnabled", true);
        component.setPropertyValue("MessageConverter", "myMessageConverter");
        component.setPropertyValue("MessageIdEnabled", true);
        component.setPropertyValue("MessageTimestampEnabled", true);
        component.setPropertyValue("PostProcessor", "myPostProcessor");
        component.setPropertyValue("Priority", 1);
        component.setPropertyValue("PubSubDomain", "myPubSubDomain");
        component.setPropertyValue("PubSubNoLocal", true);
        component.setPropertyValue("ReceiveTimeout", 1000L);
        component.setPropertyValue("SessionAcknowledgeMode", 1);
        component.setPropertyValue("SessionAcknowledgeMode", "AUTO_ACKNOWLEDGE");
        component.setPropertyValue("SessionTransacted", true);
        component.setPropertyValue("TimeToLive", 100L);
        component.setPropertyValue("TransactionManager", "myTransactionManagerClass");

        return component;
    }

    /**
     * Create a fully populated dev null producer
     * See resources/studio/componentDefinitions/DEV_NULL_PRODUCER_en_GB.csv
     * @return a FullyPopulatedDevNullProducer
     */
    public static FlowElement getFullyPopulatedDevNullProducerComponent(Flow ikasanFlow) {
        FlowElement component = FlowElementFactory.createFlowElement(null, null);;
//FlowElement component = FlowElement.createFlowElement(IkasanComponentMetax.DEV_NULL_PRODUCER, ikasanFlow);
        component.setComponentName("testDevNullProducer");
        return component;
    }

    /**
     * Create a fully populated email producer
     * See resources/studio/componentDefinitions/EMAIL_PRODUCER_en_GB.csv
     * @return a FullyPopulatedEmailProducer
     */
    public static FlowElement getFullyPopulatedEmailProducerComponent(Flow ikasanFlow) {
        FlowElement component = FlowElementFactory.createFlowElement(null, null);;
//FlowElement component = FlowElement.createFlowElement(IkasanComponentMetax.EMAIL_PRODUCER, ikasanFlow);
        component.setComponentName("testEmailProducer");
        component.setPropertyValue("BccRecipient", "myBccRecipient");
        component.setPropertyValue("BccRecipients", "{'bcc1','bcc2'}");
        component.setPropertyValue("CcRecipient", "myCcRecipient");
        component.setPropertyValue("CcRecipients", "{'cc1','cc2'}");
        component.setPropertyValue("Configuration", "MyConfigurationClass");
        component.setPropertyValue("ConfiguredResourceId", "myUniqueConfiguredResourceIdName");
        component.setPropertyValue("CriticalOnStartup", true);
        component.setPropertyValue("EmailBody", "myEmailBody");
        component.setPropertyValue("EmailFormat", "html");
        component.setPropertyValue("ExtendedMailSessionProperties", "{key1:'value1',key2:'value2'}");
        component.setPropertyValue("From", "FromAddress");
        component.setPropertyValue("HasAttachments", true);
        component.setPropertyValue("MailDebug", true);
        component.setPropertyValue("Mailhost", "myMailhostAddress");
        component.setPropertyValue("MailMimeAddressStrict", true);
        component.setPropertyValue("MailPopClass", "myMailPopClass");
        component.setPropertyValue("MailPopPort", 100);
        component.setPropertyValue("MailPopUser", "myMailPopUser");
        component.setPropertyValue("MailSmtpClass", "myMailSmtpClass");
        component.setPropertyValue("MailSmtpHost", "myMailSmtpHost");
        component.setPropertyValue("MailSmtpPort", 101);
        component.setPropertyValue("MailSmtpUser", "myMailSmtpUser");
        component.setPropertyValue("MailStoreProtocol", "myMailStoreProtocol");
        component.setPropertyValue("MailSubject", "myMailSubject");
        component.setPropertyValue("MailPassword", "myMailPassword");
        component.setPropertyValue("MailRuntimeEnvironment", "myMailRuntimeEnvironment");
        component.setPropertyValue("ToRecipient", "myToRecipient");
        component.setPropertyValue("ToRecipients", "{'to1','to2'}");
        component.setPropertyValue("TransportProtocol", "myTransportProtocol");
        component.setPropertyValue("User", "myUser");
        return component;
    }

    /**
     * Create a fully populated email producer
     * See resources/studio/componentDefinitions/OBJECT_MESSAGE_TO_XML_STRING_CONVERTER_en_GB.csv
     * @return a FullyPopulatedEmailProducer
     */
    public static FlowElement getFullyPopulatedObjectMessageToObjectConverterComponent(Flow ikasanFlow) {
        FlowElement component = FlowElementFactory.createFlowElement(null, null);;
//FlowElement component = FlowElement.createFlowElement(IkasanComponentMetax.OBJECT_MESSAGE_TO_OBJECT_CONVERTER, ikasanFlow);
        component.setPropertyValue("Name", "MyObjectToObjectconverter");
        return component;
    }

    /**
     * Create a fully populated email producer
     * See resources/studio/componentDefinitions/OBJECT_MESSAGE_TO_XML_STRING_CONVERTER_en_GB.csv
     * @return a FullyPopulatedEmailProducer
     */
    public static FlowElement getFullyPopulatedObjectMessageToXmlStringConverterComponent(Flow ikasanFlow) {
        FlowElement component = FlowElementFactory.createFlowElement(null, null);;
//FlowElement component = FlowElement.createFlowElement(IkasanComponentMetax.OBJECT_MESSAGE_TO_XML_STRING_CONVERTER, ikasanFlow);
        component.setPropertyValue("Configuration", "MyConfigurationClass");
        component.setPropertyValue("ConfiguredResourceId", "myUniqueConfiguredResourceIdName");
        component.setPropertyValue("RootName", "myRootName");
        component.setPropertyValue("RootClassName", "java.lang.String");
        component.setPropertyValue("FastFailOnConfigurationLoad", true);
        component.setPropertyValue("NamespacePrefix", "myNamespacePrefix");
        component.setPropertyValue("NamespaceURI", "myNamespaceURI");
        component.setPropertyValue("NoNamespaceSchema", true);
        component.setPropertyValue("ObjectClass", "String.class");
        component.setPropertyValue("ObjectClasses", "{'String.class','String.class'}");
        component.setPropertyValue("RouteOnValidationException", true);
        component.setPropertyValue("Schema", "mySchema");
        component.setPropertyValue("SchemaLocation", "http://foo.com/domain example.xsd");
        component.setPropertyValue("UseNamespacePrefix", true);
        component.setPropertyValue("Validate", true);
        component.setPropertyValue("XmlAdapterMap", "myXmlAdapterMap");
        return component;
    }

    /**
     * Create a fully populated email producer
     * See resources/studio/componentDefinitions/XML_STRING_TO_OBJECT_CONVERTER_en_GB.csv
     * @return a FullyPopulatedEmailProducer
     */
    public static FlowElement getFullyPopulatedXmlStringObjectMessageConverterComponent(Flow ikasanFlow) {
        FlowElement component = FlowElementFactory.createFlowElement(null, null);;
//FlowElement component = FlowElement.createFlowElement(IkasanComponentMetax.XML_STRING_TO_OBJECT_CONVERTER, ikasanFlow);
        component.setPropertyValue("AutoConvertElementToValue", true);
        component.setPropertyValue("Configuration", "MyConfigurationClass");
        component.setPropertyValue("ConfiguredResourceId", "myUniqueConfiguredResourceIdName");
        component.setPropertyValue("ClassToBeBound", "String.class");
        component.setPropertyValue("ClassesToBeBound", "{'String.class','String.class'}");
        component.setPropertyValue("ContextPath", "/bob");
        component.setPropertyValue("ContextPaths", "{'/aa','/bb'}");
        component.setPropertyValue("MarshallerProperties", "{key1:'value1',key2:'value2'}");
        component.setPropertyValue("UnmarshallerProperties", "{key1:'value1',key2:'value2'}");
        component.setPropertyValue("ValidationEventHandler", "myValidationEventHandler");
        return component;
    }

    public static List<IkasanComponentProperty> getPropertiesForAction(String actionName) {
        List<IkasanComponentPropertyMeta> actionParams = IkasanExceptionResolutionMeta.getPropertyMetaListForAction(actionName);
        return IkasanComponentProperty.generateIkasanComponentPropertyList(actionParams);
    }

    /**
     * Create a fully populated email producer
     * See resources/studio/componentDefinitions/XML_STRING_TO_OBJECT_CONVERTER_en_GB.csv
     * @return a FullyPopulatedEmailProducer
     */
//    public static Flow populateFlowExceptionResolver(Flow ikasanFlow) {
//        ExceptionResolver ikasanExceptionResolver = (ExceptionResolver) FlowElement.createFlowElement(IkasanComponentMetax.EXCEPTION_RESOLVER, ikasanFlow);
//        ikasanFlow.setExceptionResolver(ikasanExceptionResolver);
//
//        List<IkasanComponentProperty> retryProperties = getPropertiesForAction("retry");
//        retryProperties.get(0).setValue(200);
//        retryProperties.get(1).setValue(10);
//        ExceptionResolution ikasanExceptionResolution = new ExceptionResolution(
//                "org.ikasan.spec.component.routing.RouterException.class", "retry", retryProperties);
//        ikasanExceptionResolver.addExceptionResolution(ikasanExceptionResolution);
//        ikasanExceptionResolver.addExceptionResolution(new ExceptionResolution(
//                "org.ikasan.spec.component.transformation.TransformationException.class", "ignoreException"));
//        ikasanExceptionResolver.addExceptionResolution(new ExceptionResolution(
//                "org.ikasan.spec.component.splitting.SplitterException.class", "excludeEvent"));
//        ikasanExceptionResolver.addExceptionResolution(new ExceptionResolution(
//                "org.ikasan.spec.component.endpoint.EndpointException.class", "retryIndefinitely"));
//        List<IkasanComponentProperty> scheduledCronProperties = getPropertiesForAction("scheduledCronRetry");
//        scheduledCronProperties.get(0).setValue("* * * * *");
//        scheduledCronProperties.get(1).setValue(100);
//        ikasanExceptionResolver.addExceptionResolution(new ExceptionResolution(
//                "java.util.concurrent.TimeoutException.class", "retryIndefinitely", scheduledCronProperties));
//        ikasanExceptionResolver.addExceptionResolution(new ExceptionResolution(
//                "org.ikasan.spec.component.filter.FilterException.class", "retryIndefinitely"));
//
//        return ikasanFlow;
//    }
}
