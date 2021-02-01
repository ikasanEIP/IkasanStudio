package org.ikasan.studio.model;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class StudioPsiStudioUtilsLightTests extends LightJavaCodeInsightFixtureTestCase {

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
    public void test_findFirstMethodByReturnType_finds_file() {
        // Note getTestDataPath() is overriding LightJavaCodeInsightFixtureTestCase
        myFixture.copyDirectoryToProject("general", "src/");

        PsiMethod methodFound = StudioPsiUtils.findFirstMethodByReturnType(myFixture.getProject(), "org.ikasan.spec.module.Module");

        Assert.assertThat(methodFound, is(notNullValue()));
        Assert.assertThat(methodFound.getName(), is("getModule"));

        PsiJavaFile javaFile = (PsiJavaFile) methodFound.getContainingFile();
        Assert.assertThat(javaFile.toString(), is("PsiJavaFile:ModuleConfig.java"));
        Assert.assertThat(javaFile.getPackageName(), is("com.ikasan.studio.example"));
        Assert.assertThat(javaFile.getFileType().getDescription(), is("Java"));
    }

    @Test
    public void test_createPackage_StandardPackage() {
        PsiDirectory baseDir = createPackageFixture("org.test");

        Assert.assertThat(baseDir, is(notNullValue()));
        PsiDirectory[] subDirs = baseDir.getSubdirectories();
        Assert.assertThat(subDirs.length, is(1));
        Assert.assertThat(subDirs[0].getName(), is("org"));
        PsiDirectory[] subSubDirs = subDirs[0].getSubdirectories();
        Assert.assertThat(subSubDirs.length, is(1));
        Assert.assertThat(subSubDirs[0].getName(), is("test"));

        Assert.assertThat(subSubDirs[0].getSubdirectories().length, is(0));
    }

    @Test
    public void test_createPackage_EmptyPackage() {
        PsiDirectory baseDir = createPackageFixture("");

        PsiDirectory[] psiJavaDirectory = baseDir.getSubdirectories();
        Assert.assertThat(psiJavaDirectory.length, is(0));
    }

    @Test
    public void test_createPackage_SingleElementInPackage() {
        PsiDirectory baseDir = createPackageFixture("com");
        PsiDirectory[] psiJavaDirectory = baseDir.getSubdirectories();
        Assert.assertThat(psiJavaDirectory.length, is(1));
        Assert.assertThat(psiJavaDirectory[0].getName(), is("com"));
    }

    private PsiDirectory createPackageFixture(String packageName) {
        Project myProject = myFixture.getProject();
        VirtualFile sourceRoot = StudioPsiUtils.getSourceRootContaining(myProject,"src");
        PsiDirectory baseDir = PsiDirectoryFactory.getInstance(myProject).createDirectory(sourceRoot);

        // This may be an asynch call but example tests in Intellij community indicate it is safe to assume it completes
        // before the end of the method i.e. we can assert the results.
        CommandProcessor.getInstance().executeCommand(
            getProject(),
            () -> ApplicationManager.getApplication().runWriteAction(
                () -> {
                    StudioPsiUtils.createPackage(baseDir, packageName);
                }),
                "Create package fixture",
                "Undo group ID");
        return baseDir;
    }
}
