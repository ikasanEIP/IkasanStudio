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

        // Maybe specialist properties require 'advanced' options and make the component 'beskpoke'
//        org.ikasan.endpoint.ftp.consumer.FtpConsumerConfiguration consumerConfiguration = new org.ikasan.endpoint.ftp.consumer.FtpConsumerConfiguration();
//        component.setPropertyValue("Configuration", component.getType().getProperties().get("Configuration"), consumerConfiguration);

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

        // Maybe specialist properties require 'advanced' options and make the component 'beskpoke'
//        ManagedEventIdentifierService managedEventIdentifierService = new ManagedEventIdentifierService()
//        component.setPropertyValue("ManagedEventIdentifierService", component.getType().getProperties().get("ManagedEventIdentifierService"), true);

//        ManagedResourceRecoveryManager managedResourceRecoveryManager = new ManagedResourceRecoveryManager() {
//            @Override
//            public void recover(Throwable throwable) {}
//            @Override
//            public boolean isRecovering() {return false;}
//            @Override
//            public void cancel() {}
//        };
//        component.setPropertyValue("ManagedResourceRecoveryManager", component.getType().getProperties().get("ManagedResourceRecoveryManager"), true);
        component.setPropertyValue("MaxEagerCallbacks", 1);
        component.setPropertyValue("MaxRetryAttempts", 10);
        component.setPropertyValue("MaxRows", 11);

        // Maybe specialist properties require 'advanced' options and make the component 'beskpoke'
//        MessageProvider messageProvider = new MessageProvider() {
//            @Override
//            public void stop() {            }
//            @Override
//            public void start() {            }
//            @Override
//            public boolean isRunning() {                return false;            }
//        };
//        component.setPropertyValue("MessageProvider", component.getType().getProperties().get("MessageProvider"), messageProvider);

        component.setPropertyValue("MinAge", 12);
        component.setPropertyValue("MoveOnSuccessNewPath", "/test/move/on/success");
        component.setPropertyValue("MoveOnSuccess", true);
        component.setPropertyValue("PasswordFilePath", "/test/password/file/path");
        component.setPropertyValue("RenameOnSuccessExtension", "newExtension");
        component.setPropertyValue("RenameOnSuccess", true);
        component.setPropertyValue("ScheduledJobGroupName",  "myScheduledJobGroupName");
        component.setPropertyValue("ScheduledJobName", "myScheduledJobName");
        component.setPropertyValue("SocketTimeout", 22);

        org.ikasan.framework.factory.DirectoryURLFactory directoryURLFactory = new org.ikasan.framework.factory.DirectoryURLFactory() {
            @Override
            public List<String> getDirectoriesURLs(String path) {
                return null;
            }
        };
        component.setPropertyValue("SourceDirectoryURLFactory", directoryURLFactory);

        component.setPropertyValue("SystemKey", "mySystemKey");

        // Maybe specialist properties require 'advanced' options and make the component 'beskpoke'
//        org.springframework.transaction.jta.JtaTransactionManager jtaTransactionManager = new org.springframework.transaction.jta.JtaTransactionManager();
//        component.setPropertyValue("TransactionManager", component.getType().getProperties().get("TransactionManager"), jtaTransactionManager);

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

        // This is an advanced option that is best dealt with by special checkbox since it will mean the component becomes 'bespoke'
        // There are probably a number of configurations like this, need to review with Mick and Andrzej
//        org.ikasan.endpoint.ftp.consumer.FtpConsumerConfiguration consumerConfiguration = new org.ikasan.endpoint.ftp.consumer.FtpConsumerConfiguration();
//        component.setPropertyValue("Configuration", component.getType().getProperties().get("Configuration"), consumerConfiguration);
//        component.setPropertyValue("ConfiguredResourceId", component.getType().getProperties().get("ConfiguredResourceId"), "myConfiguredResourceId");

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

        // Maybe specialist properties require 'advanced' options and make the component 'beskpoke'
//        ManagedEventIdentifierService managedEventIdentifierService = new ManagedEventIdentifierService()
//        component.setPropertyValue("ManagedEventIdentifierService", component.getType().getProperties().get("ManagedEventIdentifierService"), true);

//        ManagedResourceRecoveryManager managedResourceRecoveryManager = new ManagedResourceRecoveryManager() {
//            @Override
//            public void recover(Throwable throwable) {}
//            @Override
//            public boolean isRecovering() {return false;}
//            @Override
//            public void cancel() {}
//        };
//        component.setPropertyValue("ManagedResourceRecoveryManager", true);


        component.setPropertyValue("MaxEagerCallbacks", 1);
        component.setPropertyValue("MaxRetryAttempts", 10);
        component.setPropertyValue("MaxRows", 11);

        // Maybe specialist properties require 'advanced' options and make the component 'beskpoke'
//        MessageProvider messageProvider = new MessageProvider() {
//            @Override
//            public void stop() {            }
//            @Override
//            public void start() {            }
//            @Override
//            public boolean isRunning() {                return false;            }
//        };
//        component.setPropertyValue("MessageProvider", component.getType().getProperties().get("MessageProvider"), messageProvider);


        component.setPropertyValue("MinAge", 12);
        component.setPropertyValue("MoveOnSuccessNewPath", "/test/move/on/success");
        component.setPropertyValue("MoveOnSuccess", true);
        component.setPropertyValue("PreferredKeyExchangeAlgorithm", "myPreferredKeyExchangeAlgorithm");
        component.setPropertyValue("PrivateKeyFilename", "myPrivateKeyFilename");
        component.setPropertyValue("RenameOnSuccess",  true);
        component.setPropertyValue("RenameOnSuccessExtension", "newExtension");
        component.setPropertyValue("ScheduledJobGroupName", "myScheduledJobGroupName");
        component.setPropertyValue("ScheduledJobName", "myScheduledJobName");

        org.ikasan.framework.factory.DirectoryURLFactory directoryURLFactory = new org.ikasan.framework.factory.DirectoryURLFactory() {
            @Override
            public List<String> getDirectoriesURLs(String path) {
                return null;
            }
        };
        component.setPropertyValue("SourceDirectoryURLFactory", directoryURLFactory);

        // Maybe specialist properties require 'advanced' options and make the component 'beskpoke'
//        org.springframework.transaction.jta.JtaTransactionManager jtaTransactionManager = new org.springframework.transaction.jta.JtaTransactionManager();
//        component.setPropertyValue("TransactionManager", component.getType().getProperties().get("TransactionManager"), jtaTransactionManager);
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
