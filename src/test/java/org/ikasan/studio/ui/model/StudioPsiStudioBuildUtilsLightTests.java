package org.ikasan.studio.ui.model;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ikasan.studio.ui.model.StudioPsiUtils.TEMP_CONTENT_ROOT;

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
        PsiDirectory baseDir = createPackageFixture("org.test");

        assertThat(baseDir, is(notNullValue()));
        PsiDirectory[] subDirs = baseDir.getSubdirectories();
        assertThat(subDirs.length, is(1));
        assertThat(subDirs[0].getName(), is("org"));
        PsiDirectory[] subSubDirs = subDirs[0].getSubdirectories();
        assertThat(subSubDirs.length, is(1));
        assertThat(subSubDirs[0].getName(), is("test"));

        assertThat(subSubDirs[0].getSubdirectories().length, is(0));
    }

    @Test
    public void test_createPackage_EmptyPackage() {
        PsiDirectory baseDir = createPackageFixture("");

        PsiDirectory[] psiJavaDirectory = baseDir.getSubdirectories();
        assertThat(psiJavaDirectory.length, is(0));
    }

    @Test
    public void test_createPackage_SingleElementInPackage() {
        PsiDirectory baseDir = createPackageFixture("com");
        PsiDirectory[] psiJavaDirectory = baseDir.getSubdirectories();
        assertThat(psiJavaDirectory.length, is(1));
        assertThat(psiJavaDirectory[0].getName(), is("com"));
    }

    private PsiDirectory createPackageFixture(String packageName) {
        Project myProject = myFixture.getProject();
        VirtualFile sourceRoot = StudioPsiUtils.getSourceDirectoryForContentRoot(myProject, "", TEMP_CONTENT_ROOT, "src");
        PsiDirectory baseDir = PsiDirectoryFactory.getInstance(myProject).createDirectory(sourceRoot);

        // This may be an async call but example tests in Intellij community indicate it is safe to assume it completes
        // before the end of the method i.e. we can assert the results.
        CommandProcessor.getInstance().executeCommand(
            getProject(),
            () -> ApplicationManager.getApplication().runWriteAction(
                () -> {
                    StudioPsiUtils.createPackage(baseDir, packageName);
                }),
                "Create Package Fixture",
                "Undo group ID");
        return baseDir;
    }
}
