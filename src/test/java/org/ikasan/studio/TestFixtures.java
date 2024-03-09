package org.ikasan.studio;

import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.model.ikasan.instance.*;
import org.ikasan.studio.model.ikasan.meta.ComponentMeta;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentLibrary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.ikasan.studio.model.ikasan.meta.ComponentPropertyMeta.*;
/**
 * Some of these text fixtures will be exported to the Meta Pack project
 * Ideally the Ikasan Packs should be loosely coupled with the IDE so that most
 * of the knoweldge and detail of what each pack support comes from the pack, not the IDE.
 */
public class TestFixtures {
    public static final String DEFAULT_PACKAGE = "org.ikasan";
    public static final String TEST_IKASAN_PACK = "Vtest.x";    // Ideall we should use this test pack but for conveniance for now use V3_3_IKASAN_PACK
    public static final String V3_3_IKASAN_PACK = "V3.3.x";
    public static final String TEST_FLOW_NAME = "MyFlow1";
    public static final String TEST_FLOW_DESCRIPTION = "MyFlowDescription";
    public static final String TEST_CRON_EXPRESSION = "0 0/1 * * * ?";

    public static Module getMyFirstModuleIkasanModule(List<Flow> flows) {
        return Module.moduleBuilder()
                .version("1.3")
                .name("A to B convert")
                .description("My first module")
                .applicationPackageName("co.uk.test")
                .port("8091")
                .h2PortNumber("8092")
                .h2WebPortNumber("8093")
                .flows(flows)
                .build();
    }

    public static Flow.FlowBuilder getUnbuiltFlow() {
        return Flow.flowBuilder()
                .description(TEST_FLOW_DESCRIPTION)
                .name(TEST_FLOW_NAME);
    }

    // -------------------------- Consumers -------------------------
    public static FlowElement getEventGeneratingConsumer() {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(V3_3_IKASAN_PACK, "Event Generating Consumer");
        return FlowElement.flowElementBuilder()
                .componentMeta(meta)
                .componentName("My Event Generating Consumer")
                .build();
    }

    public static FlowElement getFtpConsumer() {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(V3_3_IKASAN_PACK, "FTP Consumer");
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
        flowElement.setPropertyValue("configuredResourceId", "myUniqueConfiguredResourceIdName");
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
        return flowElement;
    }

    public static FlowElement getLocalFileConsumer() {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(V3_3_IKASAN_PACK, "Local File Consumer");
        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(meta)
                .componentName("My Local File Consumer")
                .build();
        flowElement.setPropertyValue("configuration", "org.ikasan.myflow.configuration");
        flowElement.setPropertyValue("configuredResourceId", "bob");
        flowElement.setPropertyValue("criticalOnStartup", true);
        flowElement.setPropertyValue("cronExpression", TEST_CRON_EXPRESSION);
        flowElement.setPropertyValue("directoryDepth", 1);
        flowElement.setPropertyValue("eager", true);
        flowElement.setPropertyValue("encoding", "UTF-8");
        flowElement.setPropertyValue("eventFactory", "org.ikasan.myflow.myEventFactory");
        flowElement.setPropertyValue("filenames", "{'myFile','anotherFile'}");
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
        return flowElement;
    }

    public static FlowElement getScheduledConsumer() {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(V3_3_IKASAN_PACK, "Scheduled Consumer");
        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(meta)
                .componentName("My Scheduled Consumer")
                .build();
        flowElement.setPropertyValue("cronExpression", TEST_CRON_EXPRESSION);
        flowElement.setPropertyValue("configuration", "org.ikasan.myflow.configuration");
        flowElement.setPropertyValue("configuredResourceId", "bob");
        flowElement.setPropertyValue("criticalOnStartup", true);
        flowElement.setPropertyValue("eager", true);
        flowElement.setPropertyValue("eventFactory", "org.ikasan.myflow.myEventFactory");
        flowElement.setPropertyValue("ignoreMisfire", true);
        flowElement.setPropertyValue("managedEventIdentifierService", "org.ikasan.myflow.myEventService");
        flowElement.setPropertyValue("managedResourceRecoveryManager", "org.ikasan.myflow.myManagedResourceRecoveryManager");
        flowElement.setPropertyValue("maxEagerCallbacks", 10);
        flowElement.setPropertyValue("messageProvider", "org.ikasan.myflow.myMessageProvider");
        flowElement.setPropertyValue("timezone", "UTC");

        return flowElement;
    }

    // ------------------------- Converters -------------------------

    public static FlowElement getCustomConverter() {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(V3_3_IKASAN_PACK, "Custom Converter");
        FlowElement flowElement =  FlowElement.flowElementBuilder()
                .componentMeta(meta)
                .componentName("My Custom Converter")
                .build();
        flowElement.setPropertyValue(FROM_TYPE, "java.lang.String");
        flowElement.setPropertyValue(TO_TYPE, "java.lang.Integer");
        flowElement.setPropertyValue(BESPOKE_CLASS_NAME, "myConverter");
        return flowElement;
    }
    // ------------------------- Filters -------------------------

    public static FlowElement getMessageFilter() {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(V3_3_IKASAN_PACK, "Message Filter");
        FlowElement flowElement =  FlowElement.flowElementBuilder()
                .componentMeta(meta)
                .componentName("My Message Filter")
                .build();
        flowElement.setPropertyValue(FROM_TYPE, java.lang.String.class);
        flowElement.setPropertyValue(BESPOKE_CLASS_NAME, "myFilter");
        flowElement.setPropertyValue("configuredResourceId", "MyResourceID");
        flowElement.setPropertyValue("configuration", "MyConfigurationClass");
        flowElement.setPropertyValue("configuredResource", true);
        return flowElement;
    }

    // ------------------------- Producers ---------------------------
    public static FlowElement getDevNullProducer() {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(V3_3_IKASAN_PACK, "Dev Null Producer");
        return FlowElement.flowElementBuilder()
                .componentMeta(meta)
                .componentName("My DevNull Producer")
                .build();
    }

    public static FlowElement getLoggingProducer() {
        ComponentMeta meta = IkasanComponentLibrary.getIkasanComponentByKeyMandatory(V3_3_IKASAN_PACK, "Logging Producer");

        FlowElement flowElement = FlowElement.flowElementBuilder()
                .componentMeta(meta)
                .componentName("My Logging Producer")
                .build();
        flowElement.setPropertyValue("configuredResourceId", "MyResourceID");
        flowElement.setPropertyValue("regExpPattern", "this");
        flowElement.setPropertyValue("replacementText", "that");
        flowElement.setPropertyValue("logEveryNth", 2);
        return flowElement;
    }


    // ------------------------ Flow Combinations --------------------

    public static Flow getEventGeneratingConsumerCustomConverterDevNullProducerFlow() {
        FlowElement eventGeneratingConsumer = getEventGeneratingConsumer();
        FlowElement customConverter = getCustomConverter();
        FlowElement devNullProducer = TestFixtures.getDevNullProducer();
        Transition transition = Transition.builder()
                .from(customConverter.getComponentName())
                .to(devNullProducer.getComponentName())
                .name("My Transition")
                .build();
        return getUnbuiltFlow()
                .consumer(eventGeneratingConsumer)
                .flowElements(new ArrayList<>(Arrays.asList(customConverter,devNullProducer)))
                .transitions(Collections.singletonList(transition))
                .build();
    }
}
