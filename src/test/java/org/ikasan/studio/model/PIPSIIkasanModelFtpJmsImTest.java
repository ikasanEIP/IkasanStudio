package org.ikasan.studio.model;

import com.intellij.psi.PsiClass;
import com.intellij.psi.search.ProjectScope;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.Module;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class PIPSIIkasanModelFtpJmsImTest extends PIPSIIkasanModelAbstractTest {


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
        return "/ikasanStandardSampleApps/ftpJmsIm/";
    }
    @Test
    public void test_parse_of_FmsJmsIm_standard_module() {
        Module ikasanModule = Context.getIkasanModule(TEST_PROJECT_KEY);
        final PsiClass moduleConfigClass = myJavaFacade.findClass("com.ikasan.sample.spring.boot.ModuleConfig", ProjectScope.getAllScope(myProject));
        Assert.assertThat(moduleConfigClass, is(notNullValue()));

        pipsiIkasanModel.setModuleConfigClazz(moduleConfigClass);
//        pipsiIkasanModel.updateIkasanModuleFromSourceCode();

//        IkasanModule ikasanModule = Context.getIkasanModule(TEST_PROJECT_KEY);
//        final PsiClass moduleConfigClass = myJavaFacade.findClass("com.ikasan.sample.spring.boot.ModuleConfig", ProjectScope.getAllScope(myProject));
//        moduleConfigPsiFile = moduleConfigClass.getContainingFile();
//
//        IkasanModule ikasanModule = pipsiIkasanModel.buildIkasanModule(moduleConfigPsiFile);
        Assert.assertThat(ikasanModule.getComponentName(), is("My Integration Module"));
//        Assert.assertThat(ikasanModule.getDescription(), is("Ftp Jms Sample Module"));
//        Assert.assertThat(ikasanModule.getFlows().size(), is(2));
//        Assert.assertThat(ikasanModule.getViewHandler(), is(notNullValue()));
//
//        List<IkasanFlow> flows = ikasanModule.getFlows();
//        IkasanFlow flow1 = flows.get(0);
//
//        Assert.assertThat(flow1.getViewHandler(), is(notNullValue()));
//        Assert.assertThat(flow1.getComponentName(), is("FTP to JMS Flow"));
//        Assert.assertThat(flow1.getDescription(), is("Ftp to Jms"));
//        Assert.assertThat(flow1.getInput().getDescription(), is("ftpConsumerRemoteHost"));
//        Assert.assertThat(flow1.getOutput().getDescription(), is("ftp.private.jms.queue"));
//        Assert.assertThat(flow1.getFlowComponentList().size(), is(3));
//
//        IkasanFlowComponent ftpConsumer = flow1.getFlowComponentList().get(0);
//        Assert.assertThat(ftpConsumer.getComponentName(), is("Ftp Consumer"));
//        Assert.assertThat(ftpConsumer.getProperties().size(), is(20));
//
//        Assert.assertThat(flow1.getFlowComponentList().get(1).getComponentName(), is("Ftp Payload to Map Converter"));
//        Assert.assertThat(flow1.getFlowComponentList().get(2).getComponentName(), is("Ftp Jms Producer"));
//
//        IkasanFlow flow2 = flows.get(1);
//        Assert.assertThat(flow2.getViewHandler(), is(notNullValue()));
//        Assert.assertThat(flow2.getComponentName(), is("JMS To FTP Flow"));
//        Assert.assertThat(flow2.getDescription(), is("Receives Text Jms message and sends it to FTP as file"));
//        Assert.assertThat(flow2.getInput().getDescription(), is("ftp.private.jms.queue"));
//        Assert.assertThat(flow2.getOutput().getDescription(), is("ftpProducerRemoteHost"));
//        Assert.assertThat(flow2.getFlowComponentList().size(), is(3));
//
//        IkasanFlowComponent jmsConsumer = flow2.getFlowComponentList().get(0);
//        Assert.assertThat(jmsConsumer.getComponentName(), is("Ftp Jms Consumer"));
//        Assert.assertThat(jmsConsumer.getProperties().size(), is(3));
//
//        Assert.assertThat(flow2.getFlowComponentList().get(1).getComponentName(), is("MapMessage to FTP Payload Converter"));
//
//        IkasanFlowComponent ftpProdcuer = flow2.getFlowComponentList().get(2);
//        Assert.assertThat(ftpProdcuer.getComponentName(), is("Ftp Producer"));
//        Assert.assertThat(ftpProdcuer.getProperties().size(), is(8));
    }
}