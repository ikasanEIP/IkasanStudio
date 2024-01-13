package org.ikasan.studio.model;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.testFramework.JavaPsiTestCase;
import com.intellij.testFramework.PsiTestUtil;
import org.apache.maven.model.Dependency;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.IkasanPomModel;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;

/**
 * Heavy tests create a new project for each test, where possible use lightwieght
 * <a href="https://plugins.jetbrains.com/docs/intellij/light-and-heavy-tests.html">See Jetbrains Documentation</a>
 *
 */
public class StudioPsiStudioUtilsHeavyTests extends JavaPsiTestCase {
    private VirtualFile myTestProjectRoot;
    private final String TEST_PROJECT = "testproject";
    protected final static String TEST_PROJECT_KEY = "testproject";
    private final static String TEST_DATA_DIR = "/ikasanStandardSampleApps/general/";

    /**
     * @return path to test data file directory relative to root of this module.
     */
    @Override
    protected @NotNull String getTestDataPath() {
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
//        Assert.assertThat(psiJavaDirectory[0].getComponentName(), is("org"));
//        Assert.assertThat(psiJavaDirectory[1].getComponentName(), is("test"));
//    }

    private PsiDirectory createPackageFixture(String packageName) {
//        Project myProject = myFixture.getProject();
        VirtualFile sourceRoot = StudioPsiUtils.getSourceRootContaining(myProject, StudioPsiUtils.JAVA_CODE);
        PsiDirectory baseDir = PsiDirectoryFactory.getInstance(myProject).createDirectory(sourceRoot);
        ApplicationManager.getApplication().runWriteAction(() -> CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
            @Override
            public void run() {
                StudioPsiUtils.createPackage(baseDir, packageName);
            }
        }, "Name of the Command", "Undo Group ID", UndoConfirmationPolicy.REQUEST_CONFIRMATION));
        return baseDir;
    }

//    @Test
//    public void test_createPackage() {
//        VirtualFile sourceRoot = StudioPsiUtils.getSourceRootContaining(myProject,StudioPsiUtils.JAVA_CODE);
//        PsiDirectory baseDir = PsiDirectoryFactory.getElement(myProject).createDirectory(sourceRoot);
//        ApplicationManager.getApplication().runWriteAction(new Runnable() {
//            @Override
//            public void run() {
//                CommandProcessor.getElement().executeCommand(myProject, new Runnable() {
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
//        Assert.assertThat(psiJavaDirectory[0].getComponentName(), is("com"));
//        Assert.assertThat(psiJavaDirectory[0].getComponentName(), is("org"));
//        Assert.assertThat(psiJavaDirectory[0].getComponentName(), is("test"));
//    }


    //public static void addDependancies(String projectKey, Map<String, Dependency> newDependencies) {
    public void test_addDependancies_adds_provided_dependancies_and_default_dependencies() {

        // The test fixtures set the project name to be the test method name
        String testProjectKey = myProject.getName();

        StudioPsiUtils.getAllSourceRootsForProject(myProject);
        IkasanPomModel ikasanPomModel = StudioPsiUtils.pomLoad(myProject) ;
        Context.setProject(testProjectKey, myProject);
        Context.setPom(testProjectKey, ikasanPomModel);

        Dependency dependency = new Dependency();
        dependency.setType("jar");
        dependency.setArtifactId("ikasan-connector-base");
        dependency.setGroupId("org.ikasan");
        dependency.setVersion("3.1.0");

        Assert.assertThat(ikasanPomModel.hasDependency(dependency), is(false));

        Map<String, Dependency> newDependencies = new HashMap<>();
        newDependencies.put(dependency.getManagementKey(), dependency);

        WriteCommandAction.runWriteCommandAction(
                myProject,
                () -> {
                    StudioPsiUtils.pomAddDependancies(testProjectKey, newDependencies);
                }
        );

        IkasanPomModel updatedPom = StudioPsiUtils.pomLoad(myProject) ;
        Assert.assertThat(updatedPom.hasDependency(dependency), is(true));
        Assert.assertThat(updatedPom.getProperty(IkasanPomModel.MAVEN_COMPILER_SOURCE), is("1.8"));
        Assert.assertThat(updatedPom.getProperty(IkasanPomModel.MAVEN_COMPILER_TARGET), is("1.8"));


//        IkasanPomModel.getModel().getProperties();
//        String taget = properties.getProperty("maven.compiler.target");
//        String target = properties.getProperty("maven.compiler.source");
    }

    @Test
    public void test_findFile() {
        StudioPsiUtils.getAllSourceRootsForProject(myProject);
        String applicationProperties = StudioPsiUtils.findFile(myProject, "application.properties") ;
        System.out.println(applicationProperties);
        String pom = StudioPsiUtils.findFile(myProject, "pom.xml") ;
        System.out.println(pom);
    }


    @Test
    public void test_createFile1() {
        StudioPsiUtils.getAllSourceRootsForProject(myProject);
        PsiFile myFile = StudioPsiUtils.createFile1("bob.java", "public class Bob {} ", myProject);
//        System.out.println("My file was " + myFile.getText());
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


//        PsiDirectory baseDir = PsiDirectoryFactory.getElement(myProject).createDirectory(project.getBaseDir());

        //JavaDirectoryService.createClass(PsiDirectory dir, String name) // allows you to create a java class in a a given directory.
        //JavaDirectoryService.createClass(PsiDirectory dir, String name, String templateName)  -- allows to create a java cla
    }

}