package org.ikasan.studio.model;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.JavaPsiFacadeEx;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.testFramework.JavaPsiTestCase;
import com.intellij.testFramework.PsiTestUtil;
import org.junit.Test;

public class StudioPsiStudioUtilsHeavyTests extends JavaPsiTestCase {
    private VirtualFile myTestProjectRoot;
    private String TEST_PROJECT = "testproject";
    private static String TEST_DATA_DIR = "/general/";

    /**
     * @return path to test data file directory relative to root of this module.
     */
    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String root = getTestDataPath() + TEST_DATA_DIR;
//        myTestProjectRoot = createTestProjectStructure(root);
        PsiTestUtil.removeAllRoots(myModule, IdeaTestUtil.getMockJdk18());
        // this will set the test root to be within the windows temp directory, it will also mean this is cleared down
        // between tests.
//        myTestProjectRoot = createTestProjectStructure();
        myTestProjectRoot = createTestProjectStructure(root);
//        myFixture.copyDirectoryToProject("general", "src/");
//        IdeaTestFixtureFactory factory = IdeaTestFixtureFactory.getFixtureFactory();
//        LightProjectDescriptor descriptor = new LightProjectDescriptor();
//        TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder = factory.createLightFixtureBuilder(descriptor);
//        IdeaProjectTestFixture fixture = fixtureBuilder.getFixture();
//        JavaCodeInsightTestFixture javaFixture =
//                JavaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture, new LightTempDirTestFixtureImpl(true));
//        javaFixture.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        myTestProjectRoot = null;
        super.tearDown();
    }

//    @Test
//    public void test_createPackage_StandardPackage() {
//        PsiDirectory baseDir = createPackageFixture("org.test");
//        PsiDirectory[] psiJavaDirectory = baseDir.getSubdirectories();
//        Assert.assertThat(psiJavaDirectory.length, is(2));
//        Assert.assertThat(psiJavaDirectory[0].getName(), is("org"));
//        Assert.assertThat(psiJavaDirectory[1].getName(), is("test"));
//    }

    private PsiDirectory createPackageFixture(String packageName) {
//        Project myProject = myFixture.getProject();
        VirtualFile sourceRoot = StudioPsiUtils.getSourceRootContaining(myProject, StudioPsiUtils.JAVA_CODE);
        PsiDirectory baseDir = PsiDirectoryFactory.getInstance(myProject).createDirectory(sourceRoot);
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
                    @Override
                    public void run() {
                        StudioPsiUtils.createPackage(baseDir, packageName);
                    }
                }, "Name of the command", "Undo Group ID", UndoConfirmationPolicy.REQUEST_CONFIRMATION);
            }
        });
        return baseDir;
    }

//    @Test
//    public void test_createPackage() {
//        VirtualFile sourceRoot = StudioPsiUtils.getSourceRootContaining(myProject,StudioPsiUtils.JAVA_CODE);
//        PsiDirectory baseDir = PsiDirectoryFactory.getInstance(myProject).createDirectory(sourceRoot);
//        ApplicationManager.getApplication().runWriteAction(new Runnable() {
//            @Override
//            public void run() {
//                CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
//                    @Override
//                    public void run() {
//                        StudioPsiUtils.createPackage(baseDir, "org.test");
//                    }
//                }, "Name of the command", "Undo Group ID", UndoConfirmationPolicy.REQUEST_CONFIRMATION);
//            }
//        });
//
//        PsiDirectory[] psiJavaDirectory = baseDir.getSubdirectories();
//        Assert.assertThat(psiJavaDirectory.length, is(3));
//        Assert.assertThat(psiJavaDirectory[0].getName(), is("com"));
//        Assert.assertThat(psiJavaDirectory[0].getName(), is("org"));
//        Assert.assertThat(psiJavaDirectory[0].getName(), is("test"));
//    }


    @Test
    public void test_createFile1() {
        StudioPsiUtils.getAllSourceRootsForProject(myProject);
        PsiFile myFile = StudioPsiUtils.createFile1("bob.java", "public class Bob {} ", myProject);
        System.out.println("My file was " + myFile.getText());
//        StudioPsiUtils.getAllSourceRootsForProject(myProject);
//        PsiDirectory.
    }

    public static void ideas(PsiFile psiFile, Project project) {
//        ******
        // We must use this to unit test utils that update the psiFile, the util below will ensure the update is correct and consistent.
        PsiTestUtil.checkFileStructure(psiFile);

        // Dont create individual whitespace nodes from test, intead use the formatter
        // Its normally done automatically at the end of every command, it can also be called explicitly using
        CodeStyleManager.getInstance(project).reformat(psiFile);
        // IMPORTS /// Instead of declaring imports, insert fully qualified names into the code then call shortenClassReferences()
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiFile);

        // Once finished altering the psi document, we need to call the postProcessing (formatting) and commit method
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        documentManager.doPostponedOperationsAndUnblockDocument(documentManager.getDocument(psiFile));


//        PsiDirectory baseDir = PsiDirectoryFactory.getInstance(myProject).createDirectory(project.getBaseDir());

        //JavaDirectoryService.createClass(PsiDirectory dir, String name) // allows you to create a java class in a a given directory.
        //JavaDirectoryService.createClass(PsiDirectory dir, String name, String templateName)  -- allows to create a java cla
    }



//    @Test
//    public void testGetMethodsByReturnType() {
//        // Technically, we could have found any one of the methods in ikasaSource/*
//        PsiMethod methodFound = StudioPsiUtils.findFirstMethodByReturnType(myProject, "org.ikasan.spec.module.Module");
//        Assert.assertThat(methodFound, is(notNullValue()));
//        Assert.assertThat(methodFound.getName(), is("getModule"));
//    }

    public void testSCR22368_1() {
        JavaPsiFacadeEx facade =myJavaFacade;
//        JavaPsiFacadeEx facade = JavaPsiFacadeEx.getInstanceEx(getProject());
        PsiElementFactory factory = facade.getElementFactory();
        PsiClass aClass = factory.createClass("X");
        PsiMethod methodFromText = factory.createMethodFromText("void method() {\n" +
                "    IntelliJIDEARulezz<\n" +
                "}", null);
        PsiMethod method = (PsiMethod)aClass.add(methodFromText);
        PsiCodeBlock body = method.getBody();
        assertNotNull(body);
        PsiDeclarationStatement declarationStatement = (PsiDeclarationStatement)body.getStatements()[0];
        PsiJavaCodeReferenceElement referenceElement = (PsiJavaCodeReferenceElement)declarationStatement.getFirstChild().getFirstChild();
        PsiClass javaUtilListClass = facade.findClass(CommonClassNames.JAVA_UTIL_LIST);

        assertNotNull(javaUtilListClass);
        PsiElement resultingElement = referenceElement.bindToElement(javaUtilListClass);
        assertEquals("List<", resultingElement.getText());
        assertEquals("void method() {\n" +
                "    List<\n" +
                "}", method.getText());
    }
}