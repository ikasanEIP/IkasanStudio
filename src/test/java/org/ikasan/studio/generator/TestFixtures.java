package org.ikasan.studio.generator;

import org.ikasan.studio.model.Ikasan.IkasanFlow;
import org.ikasan.studio.model.Ikasan.IkasanFlowComponent;
import org.ikasan.studio.model.Ikasan.IkasanFlowComponentType;

import java.util.List;

public class TestFixtures {

    /**
     * Create a fully populated
     * @See resources/studio/componentDefinitions/FTP_CONSUMER.csv
     * @return
     */
    public static IkasanFlowComponent getFullyPopulatedFtpComponent(IkasanFlow ikasanFlow) {
        IkasanFlowComponent component = IkasanFlowComponent.getInstance(IkasanFlowComponentType.FTP_CONSUMER, ikasanFlow);
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

        org.ikasan.endpoint.ftp.consumer.FtpConsumerConfiguration consumerConfiguration = new org.ikasan.endpoint.ftp.consumer.FtpConsumerConfiguration();
        component.setPropertyValue("Configuration", component.getType().getProperties().get("Configuration"), consumerConfiguration);

        component.setPropertyValue("ConnectionTimeout", component.getType().getProperties().get("ConnectionTimeout"), 600001);
        component.setPropertyValue("DataTimeout", component.getType().getProperties().get("DataTimeout"), 300001);
        component.setPropertyValue("Destructive", component.getType().getProperties().get("Destructive"), true);
        component.setPropertyValue("FTPS", component.getType().getProperties().get("FTPS"), true);
        component.setPropertyValue("FilterDuplicates", component.getType().getProperties().get("FilterDuplicates"), true);
        component.setPropertyValue("FilterOnLastModifiedDate", component.getType().getProperties().get("FilterOnLastModifiedDate"), true);
        component.setPropertyValue("FtpsIsImplicit", component.getType().getProperties().get("FtpsIsImplicit"), true);
        component.setPropertyValue("FtpsKeyStoreFilePassword", component.getType().getProperties().get("FtpsKeyStoreFilePassword"), "myFtpsKeyStoreFilePassword");
        component.setPropertyValue("FtpsKeyStoreFilePath", component.getType().getProperties().get("FtpsKeyStoreFilePath"), "/test/ftps/keystore");
        component.setPropertyValue("FtpsPort", component.getType().getProperties().get("FtpsPort"), 987);
        component.setPropertyValue("FtpsProtocol", component.getType().getProperties().get("FtpsProtocol"), "SSL");
        component.setPropertyValue("IsRecursive", component.getType().getProperties().get("IsRecursive"), true);
        component.setPropertyValue("MaxRetryAttempts", component.getType().getProperties().get("MaxRetryAttempts"), 10);
        component.setPropertyValue("MaxRows", component.getType().getProperties().get("MaxRows"), 11);
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

        org.springframework.transaction.jta.JtaTransactionManager jtaTransactionManager = new org.springframework.transaction.jta.JtaTransactionManager();
        component.setPropertyValue("TransactionManager", component.getType().getProperties().get("TransactionManager"), jtaTransactionManager);

        return component;
    }
}
//    true|CronExpression|ftp.consumer.cronExpression|java.lang.String||Cron based expression dictating the invocation schedule for this component. Example, "*/5 * * * * ?" will fire every 5 seconds.
//    true|FilenamePattern|ftp.consumer.FilenamePattern|java.lang.String||Regular expression for matching file names to be transported
//    true|Password|ftp.consumer.Password|java.lang.String||password used to login to (S)FTP server where consumer needs to connect. Takes precedences over privateKeyFilename. If both provided user/password combination will be used to login rather then user/privateKeyFilename.
//    true|RemoteHost|ftp.consumer.RemoteHost|java.lang.String||Default(‘localhost’) host name of the remote (S)FTP server where consumer needs to connect.
//    true|RemotePort|ftp.consumer.RemotePort|java.lang.Integer||Default(22) port of the remote (S)FTP server where consumer needs to connect.
//    true|SourceDirectory|ftp.consumer.SourceDirectory|java.lang.String||Remote directory from which to discover files
//    true|Username|ftp.consumer.Username|java.lang.String||User name used to login to (S)FTP server where consumer needs to connect.
//    false|Active||java.lang.Boolean||Optional only available on FTP consumer. Default(False) Flag indicating whether the FTP connection is active or passive
//    false|AgeOfFiles||java.lang.Integer||Default(-1) file filter related option expressed in days. Given that meta data of processed files is being collected on every successful file consumptions, the ageOfFiles option relates to housekeeping of the meta information. On every successful file consumption as part of post commit process file (S)FTP consumer will attempt to delete records older than ageOfFiles records from file filter persistence table. The operation is skipped when ageOfFiles=-1
//    false|Checksum||java.lang.Boolean||Default(False) Flag indicating whether to verify integrity of retrieved file by comparing with a checksum supplied by the remote system.
//    false|Chronological||java.lang.Boolean||Default(False) Flag indicating whether the file processing should be based on chronological order of file latest updates.
//    false|ChunkSize||java.lang.Integer||Optional only applicable when chunking=true. Default(1048576) 1MB.
//    false|Chunking||java.lang.Boolean||Default(False) Flag indicating whether the file download should be performed in smaller distinguished data chunks of size defined by chunkSize configuration.
//    false|CleanupJournalOnComplete||java.lang.Boolean||Default(true) Existing (S)FTP consumer is using DB persistence tables to establish different operations it is performing as part of the usage of command pattern (FileDiscovery, FileRename, FileRetrive). That persistent information is be default cleaned up when cleanupJournalOnComplete=true. It can be occasionally useful to cleanupJournalOnComplete=false when performing some debugging.
//    false|ClientID||java.lang.String||file filter related option identifying consumer. clientId is stored as part of the meta information persisted about the processed file.
//    false|Configuration|ftp.consumer.configuration|org.ikasan.endpoint.ftp.consumer.FtpConsumerConfiguration||Encapsulates all the properties for the (s)ftp configuration into a single bean.
//    false|ConnectionTimeout||java.lang.Integer||Default(60000) expressed in milliseconds. Internal (S)FTP connector connection timeout value.
//    false|DataTimeout||java.lang.Integer||Optional only available on FTP consumer. Default(300000) expressed in milliseconds. Internal FTP connector data connection timeout value.
//    false|Destructive||java.lang.Boolean||Default(False) Flag indicating whether the processed file should be deleted after successful consumption
//    false|FTPS||java.lang.Boolean||Optional only available on FTP consumer. Default(false) used to determine if connection is using FTPs
//    false|FilterDuplicates||java.lang.Boolean||Default(True) Flag indicating whether to filter out duplicates files based on previously persisted meta information. When value set to false no meta data is persisted hence same file could be processed over and over again.
//    false|FilterOnFilename||java.lang.Boolean||Default(True) Flag indicating whether to include file name when persisting meta information about processed file.
//    false|FilterOnLastModifiedDate||java.lang.Boolean||Default(True) Flag indicating whether to include last modified date of the file when persisting meta information about processed file and whether to use the last modified date for filtering. If filterOnFilename=true and filterOnLastModifiedDate=false any modifications to the files would not be detected and file wouldn’t be reprocessed.
//    false|FtpsIsImplicit||java.lang.Boolean||Optional only available on FTP consumer. Default(false) only applicable when FTPS=true.
//    false|FtpsKeyStoreFilePassword||java.lang.String||Optional only available on FTP consumer. Only applicable when FTPS=true.
//    false|FtpsKeyStoreFilePath||java.lang.String||Optional only available on FTP consumer. Only applicable when FTPS=true.
//    false|FtpsPort|ftp.consumer.FtpsPort|java.lang.Integer||Optional only available on FTP consumer. Default(21) only applicable when FTPS=true. The remote port of FTPs server where consumer needs to connect.
//    false|FtpsProtocol|ftp.consumer.FtpsProtocol|java.lang.String||Optional only available on FTP consumer. Default(‘SSL’) only applicable when FTPS=true. The protocol used for remote FTPs connection.
//    false|IsRecursive||java.lang.Boolean||Default(False) Flag indicating whether the sourceDirectory file read should be performed in recursive manner. The option can be useful if once consumes files from top level directory without knowing the lower lever dir structure.
//    false|MaxRetryAttempts|ftp.consumer.MaxRetryAttempts|java.lang.Integer||Default(3) internal (S)FTP connector retry count.
//    false|MaxRows||java.lang.Integer||Default(-1) file filter related option. Given that meta data of processed files is being collected on every successful file consumptions, the maxRows option relates to housekeeping of the meta information. On every successful file consumption as part of post commit process file (S)FTP consumer will attempt to delete maxRows records from file filter persistence table. The operation is skipped when maxRows=-1
//    false|MinAge||java.lang.Long||Default(120) file filter related option, expressed in seconds, used to indicate minimum age of the file on the remote filesystem before file can be processed. This setting is in place to prevent (S)FTP consumer from picking up file which is still being written to.
//    false|MoveOnSuccessNewPath|ftp.consumer.MoveOnSuccessNewPath|java.lang.String||Optional only applicable when moveOnSuccess=true, it provides new directory path when the processed file is moved to.
//    false|MoveOnSuccess||java.lang.Boolean||Default(False) Flag indicating whether to move the processed file after successful consumption to different location defined by moveOnSuccessNewPath configuration.
//    false|PasswordFilePath|ftp.consumer.PasswordFilePath|java.lang.String||Optional only available on FTP consumer. The path of the file that contains the password.
//    false|RenameOnSuccessExtension||java.lang.String||Optional only applicable when renameOnSuccess=true, renameOnSuccessExtension is suffixed to the processed fileName
//    false|RenameOnSuccess||java.lang.Boolean||Default(False) Flag indicating whether to rename the processed file after successful consumption
//    false|ScheduledJobGroupName||java.lang.String||Group for scheduled jobs within the internal Ikasan scheduler
//    false|ScheduledJobName||java.lang.String||Name to identify this job within the internal Ikasan scheduler
//    false|SocketTimeout||java.lang.Integer||Optional only available on FTP consumer. Default(300000) expressed in milliseconds. Internal FTP connector socket connection timeout value.

//    #false|SourceDirectoryURLFactory|ftp.consumer.SourceDirectoryURLFactory|org.ikasan.framework.factory.DirectoryURLFactory||Classname for source directories URLs factory. The factory provides more flexible way of defining source directory. Most common use case would be when source directory changes names for instance based on date
//    false|SystemKey|ftp.consumer.SystemKey|java.lang.String||Optional only available on FTP consumer.
//    #false|TransactionManager|ftp.consumer.TransactionManager|org.springframework.transaction.jta.JtaTransactionManager||The Spring transaction manager, this may be needed to indetrage into thrid party containers.