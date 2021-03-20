package org.ikasan.studio.generator;

import org.ikasan.component.endpoint.filetransfer.ManagedEventFileTransferIdentifierService;
import org.ikasan.component.endpoint.jms.consumer.MessageProvider;
import org.ikasan.spec.event.EventFactory;
import org.ikasan.spec.event.ManagedEventIdentifierService;
import org.ikasan.spec.management.ManagedResourceRecoveryManager;
import org.ikasan.studio.model.ikasan.IkasanComponentType;
import org.ikasan.studio.model.ikasan.IkasanFlow;
import org.ikasan.studio.model.ikasan.IkasanFlowComponent;
import org.ikasan.studio.model.ikasan.IkasanModule;

import java.util.List;

public class TestFixtures {
    public static final String DEFAULT_PACKAGE = "org.ikasan";

    /**
     * Create a fully populated
     * See also resources/studio/componentDefinitions/FTP_CONSUMER.csv
     * @return a FullyPopulatedFtpComponent
     */
    public static IkasanFlowComponent getFullyPopulatedFtpComponent(IkasanFlow ikasanFlow) {
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
        component.setPropertyValue("Active", component.getType().getProperties().get("Active"), true);
        component.setPropertyValue("AgeOfFiles", component.getType().getProperties().get("AgeOfFiles"), 10);
        component.setPropertyValue("Checksum", component.getType().getProperties().get("Checksum"), true);
        component.setPropertyValue("Chronological", component.getType().getProperties().get("Chronological"), true);
        component.setPropertyValue("ChunkSize", component.getType().getProperties().get("ChunkSize"), 1048577);
        component.setPropertyValue("Chunking", component.getType().getProperties().get("Chunking"), true);
        component.setPropertyValue("CleanupJournalOnComplete", component.getType().getProperties().get("CleanupJournalOnComplete"), true);
        component.setPropertyValue("ClientID", component.getType().getProperties().get("ClientID"), "myClientId");

        // Maybe specialist properties require 'advanced' options and make the component 'beskpoke'
//        org.ikasan.endpoint.ftp.consumer.FtpConsumerConfiguration consumerConfiguration = new org.ikasan.endpoint.ftp.consumer.FtpConsumerConfiguration();
//        component.setPropertyValue("Configuration", component.getType().getProperties().get("Configuration"), consumerConfiguration);

        component.setPropertyValue("ConnectionTimeout", component.getType().getProperties().get("ConnectionTimeout"), 600001);
        component.setPropertyValue("DataTimeout", component.getType().getProperties().get("DataTimeout"), 300001);
        component.setPropertyValue("Destructive", component.getType().getProperties().get("Destructive"), true);
        component.setPropertyValue("FTPS", component.getType().getProperties().get("FTPS"), true);
        component.setPropertyValue("FilterDuplicates", component.getType().getProperties().get("FilterDuplicates"), true);
        component.setPropertyValue("FilterOnFilename", component.getType().getProperties().get("FilterOnFilename"), true);
        component.setPropertyValue("FilterOnLastModifiedDate", component.getType().getProperties().get("FilterOnLastModifiedDate"), true);
        component.setPropertyValue("FtpsIsImplicit", component.getType().getProperties().get("FtpsIsImplicit"), true);
        component.setPropertyValue("FtpsKeyStoreFilePassword", component.getType().getProperties().get("FtpsKeyStoreFilePassword"), "myFtpsKeyStoreFilePassword");
        component.setPropertyValue("FtpsKeyStoreFilePath", component.getType().getProperties().get("FtpsKeyStoreFilePath"), "/test/ftps/keystore");
        component.setPropertyValue("FtpsPort", component.getType().getProperties().get("FtpsPort"), 987);
        component.setPropertyValue("FtpsProtocol", component.getType().getProperties().get("FtpsProtocol"), "SSL");
        component.setPropertyValue("IgnoreMisfire", component.getType().getProperties().get("IgnoreMisfire"), true);
        component.setPropertyValue("IsRecursive", component.getType().getProperties().get("IsRecursive"), true);

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
        component.setPropertyValue("MaxEagerCallbacks", component.getType().getProperties().get("MaxEagerCallbacks"), 1);
        component.setPropertyValue("MaxRetryAttempts", component.getType().getProperties().get("MaxRetryAttempts"), 10);
        component.setPropertyValue("MaxRows", component.getType().getProperties().get("MaxRows"), 11);

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

        component.setPropertyValue("MinAge", component.getType().getProperties().get("MinAge"), 12);
        component.setPropertyValue("MoveOnSuccessNewPath", component.getType().getProperties().get("MoveOnSuccessNewPath"), "/test/move/on/success");
        component.setPropertyValue("MoveOnSuccess", component.getType().getProperties().get("MoveOnSuccess"), true);
        component.setPropertyValue("PasswordFilePath", component.getType().getProperties().get("PasswordFilePath"), "/test/password/file/path");
        component.setPropertyValue("RenameOnSuccessExtension", component.getType().getProperties().get("RenameOnSuccessExtension"), "ok");
        component.setPropertyValue("RenameOnSuccess", component.getType().getProperties().get("RenameOnSuccess"), true);
        component.setPropertyValue("ScheduledJobGroupName", component.getType().getProperties().get("ScheduledJobGroupName"), "myScheduledJobGroupName");
        component.setPropertyValue("ScheduledJobName", component.getType().getProperties().get("ScheduledJobName"), "myScheduledJobName");
        component.setPropertyValue("SocketTimeout", component.getType().getProperties().get("SocketTimeout"), 22);

        org.ikasan.framework.factory.DirectoryURLFactory directoryURLFactory = new org.ikasan.framework.factory.DirectoryURLFactory() {
            @Override
            public List<String> getDirectoriesURLs(String path) {
                return null;
            }
        };
        component.setPropertyValue("SourceDirectoryURLFactory", component.getType().getProperties().get("SourceDirectoryURLFactory"), directoryURLFactory);

        component.setPropertyValue("SystemKey", component.getType().getProperties().get("SystemKey"), "mySystemKey");

        // Maybe specialist properties require 'advanced' options and make the component 'beskpoke'
//        org.springframework.transaction.jta.JtaTransactionManager jtaTransactionManager = new org.springframework.transaction.jta.JtaTransactionManager();
//        component.setPropertyValue("TransactionManager", component.getType().getProperties().get("TransactionManager"), jtaTransactionManager);

        return component;
    }

    /**
     * Create a fully populated
     * See resources/studio/componentDefinitions/FTP_CONSUMER.csv
     * @return a FullyPopulatedSftpComponent
     */
    public static IkasanFlowComponent getFullyPopulatedSftpComponent(IkasanFlow ikasanFlow) {
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
        component.setPropertyValue("AgeOfFiles", component.getType().getProperties().get("AgeOfFiles"), 10);
        component.setPropertyValue("Checksum", component.getType().getProperties().get("Checksum"), true);
        component.setPropertyValue("Chronological", component.getType().getProperties().get("Chronological"), true);
        component.setPropertyValue("ChunkSize", component.getType().getProperties().get("ChunkSize"), 1048577);
        component.setPropertyValue("Chunking", component.getType().getProperties().get("Chunking"), true);
        component.setPropertyValue("CleanupJournalOnComplete", component.getType().getProperties().get("CleanupJournalOnComplete"), true);
        component.setPropertyValue("ClientID", component.getType().getProperties().get("ClientID"), "myClientId");

        // This is an advanced option that is best dealt with by special checkbox since it will mean the component becomes 'bespoke'
        // There are probably a number of configurations like this, need to review with Mick and Andrzej
//        org.ikasan.endpoint.ftp.consumer.FtpConsumerConfiguration consumerConfiguration = new org.ikasan.endpoint.ftp.consumer.FtpConsumerConfiguration();
//        component.setPropertyValue("Configuration", component.getType().getProperties().get("Configuration"), consumerConfiguration);
//        component.setPropertyValue("ConfiguredResourceId", component.getType().getProperties().get("ConfiguredResourceId"), "myConfiguredResourceId");

        component.setPropertyValue("ConnectionTimeout", component.getType().getProperties().get("ConnectionTimeout"), 600001);
        component.setPropertyValue("CriticalOnStartup", component.getType().getProperties().get("CriticalOnStartup"), true);
        component.setPropertyValue("Destructive", component.getType().getProperties().get("Destructive"), true);
        component.setPropertyValue("Eager", component.getType().getProperties().get("Eager"), true);
        component.setPropertyValue("FilterDuplicates", component.getType().getProperties().get("FilterDuplicates"), true);

        component.setPropertyValue("FilterOnFilename", component.getType().getProperties().get("FilterOnFilename"), true);
        component.setPropertyValue("FilterOnLastModifiedDate", component.getType().getProperties().get("FilterOnLastModifiedDate"), true);
        component.setPropertyValue("IgnoreMisfire", component.getType().getProperties().get("IgnoreMisfire"), true);
        component.setPropertyValue("IsRecursive", component.getType().getProperties().get("IsRecursive"), true);
        component.setPropertyValue("KnownHostsFilename", component.getType().getProperties().get("KnownHostsFilename"), "myKnownHostsFilename");

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


        component.setPropertyValue("MaxEagerCallbacks", component.getType().getProperties().get("MaxEagerCallbacks"), 1);
        component.setPropertyValue("MaxRetryAttempts", component.getType().getProperties().get("MaxRetryAttempts"), 10);
        component.setPropertyValue("MaxRows", component.getType().getProperties().get("MaxRows"), 11);

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


        component.setPropertyValue("MinAge", component.getType().getProperties().get("MinAge"), 12);
        component.setPropertyValue("MoveOnSuccessNewPath", component.getType().getProperties().get("MoveOnSuccessNewPath"), "/test/move/on/success");
        component.setPropertyValue("MoveOnSuccess", component.getType().getProperties().get("MoveOnSuccess"), true);
        component.setPropertyValue("PreferredKeyExchangeAlgorithm", component.getType().getProperties().get("PreferredKeyExchangeAlgorithm"), "myPreferredKeyExchangeAlgorithm");
        component.setPropertyValue("PrivateKeyFilename", component.getType().getProperties().get("PrivateKeyFilename"), "myPrivateKeyFilename");
        component.setPropertyValue("RenameOnSuccess", component.getType().getProperties().get("RenameOnSuccess"), true);
        component.setPropertyValue("RenameOnSuccessExtension", component.getType().getProperties().get("RenameOnSuccessExtension"), "ok");
        component.setPropertyValue("ScheduledJobGroupName", component.getType().getProperties().get("ScheduledJobGroupName"), "myScheduledJobGroupName");
        component.setPropertyValue("ScheduledJobName", component.getType().getProperties().get("ScheduledJobName"), "myScheduledJobName");

        org.ikasan.framework.factory.DirectoryURLFactory directoryURLFactory = new org.ikasan.framework.factory.DirectoryURLFactory() {
            @Override
            public List<String> getDirectoriesURLs(String path) {
                return null;
            }
        };
        component.setPropertyValue("SourceDirectoryURLFactory", component.getType().getProperties().get("SourceDirectoryURLFactory"), directoryURLFactory);

        // Maybe specialist properties require 'advanced' options and make the component 'beskpoke'
//        org.springframework.transaction.jta.JtaTransactionManager jtaTransactionManager = new org.springframework.transaction.jta.JtaTransactionManager();
//        component.setPropertyValue("TransactionManager", component.getType().getProperties().get("TransactionManager"), jtaTransactionManager);
        component.setPropertyValue("Timezone", component.getType().getProperties().get("Timezone"), "GMT");

        return component;
    }

    /**
     * Create a fully populated
     * See resources/studio/componentDefinitions/CUSTOM_CONVERTER.csv
     * @return a FullyPopulatedCustomConverter
     */
    public static IkasanFlowComponent getFullyPopulatedCustomConverter(IkasanFlow ikasanFlow) {
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
     * See resources/studio/componentDefinitions/LOCAL_FILE_CONSUMER.csv
     * @return a FullyPopulatedLocalFileConverter
     */
    public static IkasanFlowComponent getFullyPopulatedLocalFileConverter(IkasanFlow ikasanFlow) {
        IkasanFlowComponent component = IkasanFlowComponent.getInstance(IkasanComponentType.LOCAL_FILE_CONSUMER, ikasanFlow);
        component.setName("testLocalFileConsumer");

        // Mandatory properties
        component.updatePropertyValue("CronExpression", "*/5 * * * * ?");
        component.updatePropertyValue("FileNames", "*Test.txt");

        // This is an advanced option that is best dealt with by special checkbox since it will mean the component becomes 'bespoke'
        // There are probably a number of configurations like this, need to review with Mick and Andrzej
//        org.ikasan.endpoint.ftp.consumer.FtpConsumerConfiguration consumerConfiguration = new org.ikasan.endpoint.ftp.consumer.FtpConsumerConfiguration();
//        component.setPropertyValue("Configuration", component.getType().getProperties().get("Configuration"), consumerConfiguration);
//        component.setPropertyValue("ConfiguredResourceId", component.getType().getProperties().get("ConfigurationId"), "myConfiguredResourceId");
        component.setPropertyValue("CriticalOnStartup", component.getType().getProperties().get("CriticalOnStartup"), true);
        component.setPropertyValue("DirectoryDepth", component.getType().getProperties().get("DirectoryDepth"), 1);
        component.setPropertyValue("Eager", component.getType().getProperties().get("Eager"), true);
        component.setPropertyValue("Encoding", component.getType().getProperties().get("Encoding"), "UTF-8");
//        EventFactory myEventFactory = new EventFactory() {
//            @Override
//            public Object newEvent(Object o, Object o2) {return null;}
//            @Override
//            public Object newEvent(Object o, Object relatedIdentifier, Object o2) { return null; }
//            @Override
//            public Object newEvent(Object o, Object relatedIdentifier, long timestamp, Object o2) { return null; }
//        };
//        component.setPropertyValue("EventFactory", component.getType().getProperties().get("EventFactory"), "myEventFactory");

        component.setPropertyValue("IgnoreFileRenameWhilstScanning", component.getType().getProperties().get("IgnoreFileRenameWhilstScanning"), true);
        component.setPropertyValue("IgnoreMisfire", component.getType().getProperties().get("IgnoreMisfire"), true);
        component.setPropertyValue("IsRecursive", component.getType().getProperties().get("IsRecursive"), true);

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
        component.setPropertyValue("MaxEagerCallbacks", component.getType().getProperties().get("MaxEagerCallbacks"), 1);

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

//        MessageProviderPostProcessor myMessageProviderPostProcessor = new MessageProviderPostProcessor() {
//            @Override
//            public void invoke(Object o) {                            }        };
//        component.setPropertyValue("MessageProviderPostProcessor", component.getType().getProperties().get("MessageProviderPostProcessor"), myMessageProviderPostProcessor);
        component.setPropertyValue("ScheduledJobGroupName", component.getType().getProperties().get("ScheduledJobGroupName"), "myScheduledJobGroupName");
        component.setPropertyValue("ScheduledJobName", component.getType().getProperties().get("ScheduledJobName"), "myScheduledJobName");
        component.setPropertyValue("SortAscending", component.getType().getProperties().get("SortAscending"), true);
        component.setPropertyValue("SortByModifiedDateTime", component.getType().getProperties().get("SortByModifiedDateTime"), 12);
        component.setPropertyValue("Timezone", component.getType().getProperties().get("Timezone"), "GMT");
        return component;
    }

    public static IkasanModule getIkasanModule() {
        IkasanModule ikasanModule = new IkasanModule();
        ikasanModule.setName("My Integration Module");
        ikasanModule.setApplicationPackageName("org.myApp");
        return ikasanModule;
    }
}
