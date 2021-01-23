package org.ikasan.studio.model;

import com.intellij.psi.PsiClass;
import com.intellij.psi.search.ProjectScope;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.Ikasan.IkasanModule;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class PIPSIIkasanModelAnvilReconciliationTest extends PIPSIIkasanModelAbstractTest {
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
        return "/anvilReconciliation/";
    }

    @Test
    public void test_parse_of_anvil_reconciliation_module() {
        IkasanModule ikasanModule = Context.getIkasanModule(TEST_PROJECT_KEY);
        final PsiClass moduleConfigClass = myJavaFacade.findClass("com.mizuho.esb.mhsc.anvil.reconciliation.boot.ModuleConfig", ProjectScope.getAllScope(myProject));
        Assert.assertThat(moduleConfigClass, is(notNullValue()));

        pipsiIkasanModel.setModuleConfigClazz(moduleConfigClass);
        pipsiIkasanModel.updateIkasanModule();
//
//        final PsiClass moduleConfigClass = myJavaFacade.findClass("com.mizuho.esb.mhsc.anvil.reconciliation.boot.ModuleConfig", ProjectScope.getAllScope(myProject));
//        moduleConfigPsiFile = moduleConfigClass.getContainingFile();
//
//        IkasanModule ikasanModule = pipsiIkasanModel.buildIkasanModule(moduleConfigPsiFile);

        // Work in progress

        Assert.assertThat(ikasanModule.getName(), is("moduleName"));
        Assert.assertThat(ikasanModule.getDescription(), is("Anvil Reconciliation Module"));
//        Assert.assertThat(ikasanModule.getFlows().size(), is(6));
//        Assert.assertThat(ikasanModule.getViewHandler(), is(notNullValue()));
//
//        List<IkasanFlow> flows = ikasanModule.getFlows();
//        IkasanFlow flow1 = flows.get(0);
//
//        Assert.assertThat(flow1.getViewHandler(), is(notNullValue()));
//        Assert.assertThat(flow1.getName(), is("JMS to JMS Flow"));
//        Assert.assertThat(flow1.getDescription(), is("Flow demonstrates usage of JMS Concumer and JMS Producer"));
//        Assert.assertThat(flow1.getInput().getDescription(), is("source"));
//        Assert.assertThat(flow1.getOutput().getDescription(), is("target"));

//        //@todo add in exception reolver
//
//        Assert.assertThat(flow1.getFlowElementList().size(), is(3));
//
//        IkasanFlowElement jmsConsumer = flow1.getFlowElementList().get(0);
//        Assert.assertThat(jmsConsumer.getName(), is("JMS Consumer"));
//        Assert.assertThat(jmsConsumer.getProperties().size(), is(4));
//
//        Assert.assertThat(flow1.getFlowElementList().get(1).getName(), is("Exception Generating Broker"));
//        Assert.assertThat(flow1.getFlowElementList().get(2).getName(), is("JMS Producer"));
    }
}