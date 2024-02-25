package org.ikasan.studio.model;

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

    public void test_parse_of_FmsJmsIm_standard_module() {
        System.out.println("Suspended till migration is complete");
    }
//        // @TODO this will migrate to JSON test
////        Module ikasanModule = Context.getMyFirstModuleIkasanModule(TEST_PROJECT_KEY);
////        final PsiClass moduleConfigClass = myJavaFacade.findClass("com.ikasan.sample.spring.boot.ModuleConfig", ProjectScope.getAllScope(myProject));
////        Assert.assertThat(moduleConfigClass, is(notNullValue()));
//
//

}