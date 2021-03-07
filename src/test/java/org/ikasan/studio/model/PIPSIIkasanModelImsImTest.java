package org.ikasan.studio.model;

import com.intellij.psi.PsiClass;
import com.intellij.psi.search.ProjectScope;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.IkasanFlow;
import org.ikasan.studio.model.ikasan.IkasanFlowComponent;
import org.ikasan.studio.model.ikasan.IkasanModule;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class PIPSIIkasanModelImsImTest extends PIPSIIkasanModelAbstractTest {

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
        return "/jmsIm/";
    }
    @Test
    public void test_parse_of_jmsIm_standard_module() {
        IkasanModule ikasanModule = Context.getIkasanModule(TEST_PROJECT_KEY);
        final PsiClass moduleConfigClass = myJavaFacade.findClass("com.ikasan.sample.spring.boot.ModuleConfig", ProjectScope.getAllScope(myProject));
        Assert.assertThat(moduleConfigClass, is(notNullValue()));
        pipsiIkasanModel.setModuleConfigClazz(moduleConfigClass);
        pipsiIkasanModel.updateIkasanModule();

        Assert.assertThat(ikasanModule.getName(), is("jms-im"));
        Assert.assertThat(ikasanModule.getDescription(), is("Sample Module"));
        Assert.assertThat(ikasanModule.getFlows().size(), is(1));
        Assert.assertThat(ikasanModule.getViewHandler(), is(notNullValue()));

        List<IkasanFlow> flows = ikasanModule.getFlows();
        IkasanFlow flow1 = flows.get(0);

        Assert.assertThat(flow1.getViewHandler(), is(notNullValue()));
        Assert.assertThat(flow1.getName(), is("JMS to JMS Flow"));
        Assert.assertThat(flow1.getDescription(), is("Flow demonstrates usage of JMS Concumer and JMS Producer"));
//        Assert.assertThat(flow1.getInput().getDescription(), is("source"));
//        Assert.assertThat(flow1.getOutput().getDescription(), is("target"));

        //@todo add in exception reolver

        Assert.assertThat(flow1.getFlowComponentList().size(), is(3));

        IkasanFlowComponent jmsConsumer = flow1.getFlowComponentList().get(0);
        Assert.assertThat(jmsConsumer.getName(), is("JMS Consumer"));
        Assert.assertThat(jmsConsumer.getProperties().size(), is(5));

        Assert.assertThat(flow1.getFlowComponentList().get(1).getName(), is("Exception Generating Broker"));
        Assert.assertThat(flow1.getFlowComponentList().get(2).getName(), is("JMS Producer"));
    }
}