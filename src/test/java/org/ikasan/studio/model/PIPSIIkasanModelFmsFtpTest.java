package org.ikasan.studio.model;

public class PIPSIIkasanModelFmsFtpTest extends PIPSIIkasanModelAbstractTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    // This callback will set the path to the correct data file used in the test
    public String getTestDataDir() {
        return "/ikasanStandardSampleApps/fmsFtp/";
    }

    // @TODO this will migrate to JSON test
    public void test_parse_of_FmsFtp_standard_module() {
        System.out.println("Suspended till migration is complete");
    }

//        Module ikasanModule = Context.getIkasanModule(TEST_PROJECT_KEY);
//        final PsiClass moduleConfigClass = myJavaFacade.findClass("com.ikasan.sample.spring.boot.ModuleConfig", ProjectScope.getAllScope(myProject));
//        Assert.assertThat(moduleConfigClass, is(notNullValue()));
//        pipsiIkasanModel.setModuleConfigClazz(moduleConfigClass);
////        pipsiIkasanModel.updateIkasanModuleFromSourceCode();
//
////
////        final PsiClass moduleConfigClass = myJavaFacade.findClass("com.ikasan.sample.spring.boot.ModuleConfig", ProjectScope.getAllScope(myProject));
////        moduleConfigPsiFile = moduleConfigClass.getContainingFile();
////
////        IkasanModule ikasanModule = pipsiIkasanModel.buildIkasanModule(moduleConfigPsiFile);
//        Assert.assertThat(ikasanModule.getComponentName(), is("fms-ftp"));
//        Assert.assertThat(ikasanModule.getDescription(), is("Ftp Jms Sample Module"));
//        Assert.assertThat(ikasanModule.getFlows().size(), is(2));
//        Assert.assertThat(ikasanModule.getViewHandler(), is(notNullValue()));
//
//        List<Flow> flows = ikasanModule.getFlows();
//        Flow flow1 = flows.get(0);
//
//        Assert.assertThat(flow1.getViewHandler(), is(notNullValue()));
//        Assert.assertThat(flow1.getDescription(), is("Ftp to Jms"));
//        Assert.assertThat(flow1.getComponentName(), is("sourceFlow"));
//
//        Assert.assertThat(flow1.getFlowComponentList().size(), is(3));
//
//        FlowElement ftpConsumer = flow1.getFlowComponentList().get(0);
//        Assert.assertThat(ftpConsumer.getComponentName(), is("Ftp Consumer"));
//        Assert.assertThat(getConfiguredPropertyValues(ftpConsumer.getConfiguredProperties()),
//                Matchers.is("AgeOfFiles->30,Chronological->true,Chunking->false,ClientID->null,ConfiguredResourceId->configuredResourceId,CronExpression->null," +
//                        "Destructive->false,FilenamePattern->null,FilterDuplicates->true,FilterOnLastModifiedDate->true,MinAge->1l,Name->Ftp Consumer,Password->null," +
//                        "RemoteHost->null,RemotePort->null,RenameOnSuccess->false,RenameOnSuccessExtension->.tmp,ScheduledJobGroupName->FtpToLogFlow,ScheduledJobName->FtpConsumer,SourceDirectory->null,Username->null"));
//
//        FlowElement payloadToMap = flow1.getFlowComponentList().get(1);
//        Assert.assertThat(payloadToMap.getComponentName(), is("Ftp Payload to Map Converter"));
//        Assert.assertThat(getConfiguredPropertyValues(payloadToMap.getConfiguredProperties()),
//                Matchers.is("Name->Ftp Payload to Map Converter"));
//
//        FlowElement jmsProducer = flow1.getFlowComponentList().get(2);
//        Assert.assertThat(jmsProducer.getComponentName(), is("Ftp Jms Producer"));
//        Assert.assertThat(getConfiguredPropertyValues(jmsProducer.getConfiguredProperties()),
//                Matchers.is("ConfiguredResourceId->ftpJmsProducer,ConnectionFactory->producerConnectionFactory,ConnectionFactoryJndiPropertyFactoryInitial->null," +
//                        "ConnectionFactoryName->null,ConnectionFactoryPassword->null,ConnectionFactoryUsername->null,DestinationJndiName->ftp.private.jms.queue,Name->Ftp Jms Producer"));
//
//        Flow flow2 = flows.get(1);
//        Assert.assertThat(flow2.getViewHandler(), is(notNullValue()));
//        Assert.assertThat(flow2.getComponentName(), is("targetFlow"));
//        Assert.assertThat(flow2.getDescription(), is("Receives Text Jms message and sends it to FTP as file"));
//
//        Assert.assertThat(flow2.getFlowComponentList().size(), is(3));
//
//        FlowElement jmsConsumer = flow2.getFlowComponentList().get(0);
//        Assert.assertThat(jmsConsumer.getComponentName(), is("Ftp Jms Consumer"));
//        Assert.assertThat(getConfiguredPropertyValues(jmsConsumer.getConfiguredProperties()),
//                Matchers.is("ConfiguredResourceId->ftpJmsConsumer,ConnectionFactory->consumerConnectionFactory,DestinationJndiName->ftp.private.jms.queue,Name->Ftp Jms Consumer"));
//
//        FlowElement mapToPayload = flow2.getFlowComponentList().get(1);
//        Assert.assertThat(mapToPayload.getComponentName(), is("MapMessage to FTP Payload Converter"));
//        Assert.assertThat(getConfiguredPropertyValues(mapToPayload.getConfiguredProperties()),
//                Matchers.is("Name->MapMessage to FTP Payload Converter"));
//
//        FlowElement ftpProdcuer = flow2.getFlowComponentList().get(2);
//        Assert.assertThat(ftpProdcuer.getComponentName(), is("Ftp Producer"));
//        Assert.assertThat(getConfiguredPropertyValues(ftpProdcuer.getConfiguredProperties()),
//                Matchers.is("ClientID->null,ConfiguredResourceId->ftpProducerConfiguration,Name->Ftp Producer,OutputDirectory->null,Overwrite->true,Password->null,RemoteHost->null,RemotePort->null,Username->null"));
//    }
}