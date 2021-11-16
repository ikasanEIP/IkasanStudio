package org.ikasan.studio.model;

import com.intellij.psi.PsiClass;
import com.intellij.psi.search.ProjectScope;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.IkasanModule;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * This test class is intended to be used to cover features not explicitly covered by one of the Ikasan Blueprint tests.
 */
public class PIPSIIkasanModelOtherTest extends PIPSIIkasanModelAbstractTest {
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
        return "/other/";
    }


    @Test
    public void test_parse_of_other_standard_module() {
        IkasanModule ikasanModule = Context.getIkasanModule(TEST_PROJECT_KEY);
        final PsiClass moduleConfigClass = myJavaFacade.findClass("com.ikasan.sample.spring.boot.ModuleFactory", ProjectScope.getAllScope(myProject));
        Assert.assertThat(moduleConfigClass, is(notNullValue()));
        pipsiIkasanModel.setModuleConfigClazz(moduleConfigClass);
        pipsiIkasanModel.updateIkasanModule();

        Assert.assertThat(ikasanModule.getName(), is("myIntegrationModule"));
        Assert.assertThat(ikasanModule.getDescription(), is("My test module."));
        Assert.assertThat(ikasanModule.getFlows().size(), is(1));
        Assert.assertThat(ikasanModule.getFlows().get(0).getName(), is("myTestFlow"));
        Assert.assertThat(ikasanModule.getFlows().get(0).getFlowComponentList().size(), is(1));

    }

}