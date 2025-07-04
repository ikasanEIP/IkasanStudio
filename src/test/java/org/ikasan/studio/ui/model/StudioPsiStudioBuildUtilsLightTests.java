package org.ikasan.studio.ui.model;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Test;

/**
 * Light tests reuse a project from the previous test run when possible, they are quicker and therefore advised
 * </p>
 * <a href="https://plugins.jetbrains.com/docs/intellij/light-and-heavy-tests.html">...</a>
 * </p>
 * Before executing each test, the project instance will be reused if the test case returns the same project descriptor
 * as the previous one or recreated if the descriptor is different (equals() = false).
 * Note - LightJavaCodeInsightFixtureTestCase forces Junit4
 */
public class StudioPsiStudioBuildUtilsLightTests extends BasePlatformTestCase {

    /**
     * @return path to test data file directory relative to root of this module.
     */
    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void test_createPackage_StandardPackage() {
    }
//
//        assertThat(baseDir, is(notNullValue()));
//        PsiDirectory[] subDirs = baseDir.getSubdirectories();
//        assertThat(subDirs.length, is(1));
//        assertThat(subDirs[0].getIdentity(), is("org"));
//        PsiDirectory[] subSubDirs = subDirs[0].getSubdirectories();
//        assertThat(subSubDirs.length, is(1));
//        assertThat(subSubDirs[0].getIdentity(), is("test"));
//
//        assertThat(subSubDirs[0].getSubdirectories().length, is(0));
//    }
//
//    @Test
//    public void test_createPackage_EmptyPackage() {
//        PsiDirectory baseDir = createPackageFixture("");
//
//        PsiDirectory[] psiJavaDirectory = baseDir.getSubdirectories();
//        assertThat(psiJavaDirectory.length, is(0));
//    }
//
//    @Test
//    public void test_createPackage_SingleElementInPackage() {
//        PsiDirectory baseDir = createPackageFixture("com");
//        PsiDirectory[] psiJavaDirectory = baseDir.getSubdirectories();
//        assertThat(psiJavaDirectory.length, is(1));
//        assertThat(psiJavaDirectory[0].getIdentity(), is("com"));
//    }
//
//    private PsiDirectory createPackageFixture(String packageName) {
//        Project myProject = myFixture.getProject();
//        VirtualFile sourceRoot = StudioPsiUtils.getExistingSourceDirectoryForContentRoot(myProject, "", TEMP_CONTENT_ROOT, "src");
////        PsiDirectory baseDir = PsiDirectoryFactory.getInstance(myProject).createDirectory(sourceRoot);
//
//        String subDir = packageName.replace('.', '/');
//        StudioPsiUtils.createSourceFile(myProject, TEMP_CONTENT_ROOT, "", subDir, "test.properties", "Test", false, true);
//        PsiFile applicationPropertiesVF = StudioPsiUtils.getPsiFileFromPath(myProject, sourceRoot, subDir+"/test.properties");
//        PsiDirectory directory = applicationPropertiesVF.getContainingDirectory();
//        return directory;
//    }
}
