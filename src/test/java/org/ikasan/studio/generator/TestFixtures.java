package org.ikasan.studio.generator;

import org.ikasan.studio.model.ikasan.IkasanComponentType;
import org.ikasan.studio.model.ikasan.IkasanFlow;
import org.ikasan.studio.model.ikasan.IkasanFlowComponent;
import org.ikasan.studio.model.ikasan.IkasanModule;

import java.util.List;

public class TestFixtures {
    public static final String DEFAULT_PACKAGE = "org.ikasan";


    /**
     * Create a fully populated
     * See resources/studio/componentDefinitions/CUSTOM_CONVERTER_en_GB.csv
     * @return a FullyPopulatedCustomConverter
     */
    public static IkasanFlowComponent getFullyPopulatedCustomConverterComponent(IkasanFlow ikasanFlow) {
        IkasanFlowComponent component = IkasanFlowComponent.getInstance(IkasanComponentType.CUSTOM_CONVERTER, ikasanFlow);
        component.setName("testCustomConverter");

        // Mandatory properties
//        component.updatePropertyValue("ApplicationPackageName", "org.myApp");
        component.updatePropertyValue("BespokeClassName", "MyConverterClass");
        component.updatePropertyValue("FromType", java.lang.String.class);
        component.updatePropertyValue("ToType", java.lang.Integer.class);
        return component;
    }

    /**
     * Create a fully populated
     * See also resources/studio/componentDefinitions/FTP_CONSUMER_en_GB.csv
     * @return a FullyPopulatedFtpComponent
     */
    public static IkasanFlowComponent getFullyPopulatedFtpConsumerComponent(IkasanFlow ikasanFlow) {
        IkasanFlowComponent component = IkasanFlowComponent.getInstance(IkasanComponentType.FTP_CONSUMER, ikasanFlow);
        component.setName("testFtpConsumer");

        // Mandatory properties
        component.updatePropertyValue("CronExpression", "*/5 * * * * ?");
        component.updatePropertyValue("FilenamePattern", "*Test.txt");
        component.updatePropertyValue("Password", "secret");
        component.updatePropertyValue("RemoteHost", "myRemortHost");
        component.updatePropertyValue("RemotePort", "1024");
        component.updatePropertyValue("SourceDirectory", "/test/source/directory/");
        component.updatePropertyValue("Username", "myLoginName");

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
     * See resources/studio/componentDefinitions/FTP_CONSUMER_en_GB.csv
     * @return a FullyPopulatedSftpComponent
     */
    public static IkasanFlowComponent getFullyPopulatedSftpConsumerComponent(IkasanFlow ikasanFlow) {
        IkasanFlowComponent component = IkasanFlowComponent.getInstance(IkasanComponentType.SFTP_CONSUMER, ikasanFlow);
        component.setName("testSftpConsumer");

        // Mandatory properties
        component.updatePropertyValue("CronExpression", "*/5 * * * * ?");
        component.updatePropertyValue("FilenamePattern", "*Test.txt");
        component.updatePropertyValue("Password", "secret");
        component.updatePropertyValue("RemoteHost", "myRemortHost");
        component.updatePropertyValue("RemotePort", "1024");
        component.updatePropertyValue("SourceDirectory", "/test/source/directory/");
        component.updatePropertyValue("Username", "myLoginName");

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
     * See resources/studio/componentDefinitions/LOCAL_FILE_CONSUMER_en_GB.csv
     * @return a FullyPopulatedLocalFileConverter
     */
    public static IkasanFlowComponent getFullyPopulatedLocalFileConsumerComponent(IkasanFlow ikasanFlow) {
        IkasanFlowComponent component = IkasanFlowComponent.getInstance(IkasanComponentType.LOCAL_FILE_CONSUMER, ikasanFlow);
        component.setName("testLocalFileConsumer");

        // Mandatory properties
        component.updatePropertyValue("CronExpression", "*/5 * * * * ?");
        component.updatePropertyValue("Filenames", "*Test.txt");
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

    public static IkasanModule getIkasanModule() {
        IkasanModule ikasanModule = new IkasanModule();
        ikasanModule.setName("My Integration Module");
        ikasanModule.setApplicationPackageName("org.myApp");
        return ikasanModule;
    }
}
