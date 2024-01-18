package org.ikasan.studio.model;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.JavaPsiTestCase;
import org.ikasan.studio.model.psi.PIPSIIkasanModelx;

public abstract class PIPSIIkasanModelAbstractTest extends JavaPsiTestCase {
    private String testDataDir = "";
    protected VirtualFile myTestProjectRoot;
    protected static String TEST_PROJECT_KEY = "testproject";
    protected PsiFile moduleConfigPsiFile;
    protected PIPSIIkasanModelx pipsiIkasanModel;

    public String getTestDataDir() {
        return testDataDir;
    }
    /**
     * @return path to test data file directory relative to root of this module.
     */
    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }

    @Override
    protected void setUp() throws Exception {
//        super.setUp();
//        if (getTestDataDir() == "") {
//            System.out.println("TEST_DATA_DIR should have been over-ridden, this may cause you issues");
//        }
//        IkasanComponentLibrary.refreshComponentLibrary();
//
//        String root = getTestDataPath() + getTestDataDir();
//        PsiTestUtil.removeAllRoots(myModule, IdeaTestUtil.getMockJdk18());
//        myTestProjectRoot = createTestProjectStructure(root);
//        Context.setProject(TEST_PROJECT_KEY, myProject);
//        Context.setIkasanModule(TEST_PROJECT_KEY, new Module());
//
//        pipsiIkasanModel = new PIPSIIkasanModelx(TEST_PROJECT_KEY);
//        Context.setPipsiIkasanModel(TEST_PROJECT_KEY, pipsiIkasanModel);
    }

    @Override
    protected void tearDown() throws Exception {
//        myTestProjectRoot = null;
//        super.tearDown();
    }
}