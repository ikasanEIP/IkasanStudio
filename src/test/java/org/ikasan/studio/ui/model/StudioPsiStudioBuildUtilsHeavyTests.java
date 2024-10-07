package org.ikasan.studio.ui.model;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.annotations.NotNull;

import static org.ikasan.studio.ui.model.psi.PIPSIIkasanModel.MODULE_PROPERTIES_FILENAME_WITH_EXTENSION;

/**
 * Heavy tests create a new project for each test, where possible use lightweight
 * <a href="https://plugins.jetbrains.com/docs/intellij/light-and-heavy-tests.html">See Jetbrains Documentation</a>
 * NOTE JavaPsiTestCase provided by Intellij is still JUNIT3 !
 */
public class StudioPsiStudioBuildUtilsHeavyTests  extends HeavyPlatformTestCase
{
    private VirtualFile myTestProjectRoot;
    private final static String TEST_DATA_DIR = "/ikasanStandardSampleApps/general/";

    /**
     * @return path to test data file directory relative to root of this module.
     */
//    @Override
    protected @NotNull String getTestDataPath() {
        return "src/test/testData";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String root = getTestDataPath() + TEST_DATA_DIR;
//        PsiTestUtil.removeAllRoots(myModule, IdeaTestUtil.getMockJdk18());
        myTestProjectRoot = createTestProjectStructure(root);

    }

    @Override
    protected void tearDown() throws Exception {
        myTestProjectRoot = null;
        super.tearDown();
    }


    private PsiDirectory createPackageFixture(String packageName) {
        VirtualFile sourceRoot = StudioPsiUtils.getSourceDirectoryForContentRoot(myProject, "", StudioPsiUtils.GENERATED_CONTENT_ROOT, StudioPsiUtils.MAIN_JAVA);
        PsiDirectory baseDir = PsiDirectoryFactory.getInstance(myProject).createDirectory(sourceRoot);
        ApplicationManager.getApplication().runWriteAction(() -> CommandProcessor.getInstance().executeCommand(myProject,
                () -> StudioPsiUtils.createPackage(baseDir, packageName), "Name of the Command", "Undo Group ID", UndoConfirmationPolicy.REQUEST_CONFIRMATION));
        return baseDir;
    }

    //public static void addDependancies(String projectKey, Map<String, Dependency> newDependencies) {
//    public void test_addDependencies_adds_provided_dependencies_and_default_dependencies() {
//
//        // The test fixtures set the project name to be the test method name
//        String testProjectKey = myProject.getName();
//
//        StudioPsiUtils.getAllSourceRootsForProject(myProject);
////        IkasanPomModel ikasanPomModel = StudioPsiUtils.pomLoadFromVirtualDisk(myProject, this.getTempDir().toString()) ;
//        UiContext.setProject(testProjectKey, myProject);
////        UiContext.setIkasanPomModel(testProjectKey, ikasanPomModel);
//
//        Dependency dependency = new Dependency();
//        dependency.setType("jar");
//        dependency.setArtifactId("ikasan-connector-base");
//        dependency.setGroupId("org.ikasan");
//        dependency.setVersion("3.1.0");
//
////        assertThat(ikasanPomModel.hasDependency(dependency), is(false));
//
//        Set<Dependency> newDependencies = new HashSet<>();
//        newDependencies.add(dependency);
//        // Deliberatley add twice to ensure we de-duplicate
//        newDependencies.add(dependency);
//
//        WriteCommandAction.runWriteCommandAction(
//            myProject,
//            () -> StudioPsiUtils.checkForDependencyChangesAndSaveIfChanged(testProjectKey, newDependencies)
//        );
//
//        IkasanPomModel updatedPom = StudioPsiUtils.pomLoadFromVirtualDisk(myProject, this.getTempDir().toString()) ;
//        assertThat(updatedPom.hasDependency(dependency), is(true));
//        assertThat(updatedPom.getProperty(IkasanPomModel.MAVEN_COMPILER_SOURCE), is("1.8"));
//        assertThat(updatedPom.getProperty(IkasanPomModel.MAVEN_COMPILER_TARGET), is("1.8"));
//    }

    public void test_findFile() {
        StudioPsiUtils.getAllSourceRootsForProject(myProject);
        String applicationProperties = StudioPsiUtils.findFile(myProject, MODULE_PROPERTIES_FILENAME_WITH_EXTENSION) ;
        System.out.println(applicationProperties);
        String pom = StudioPsiUtils.findFile(myProject, "pom.xml") ;
        System.out.println(pom);
    }


    public void test_createFile1() {
        StudioPsiUtils.getAllSourceRootsForProject(myProject);
        PsiFile myFile = StudioPsiUtils.createFile1("bob.java", "public class Bob {} ", myProject);
    }

    public static void ideas(PsiFile psiFile, Project project) {
        // We must use this to unit test utils that update the psiFile, the util below will ensure the update is correct and consistent.
        PsiTestUtil.checkFileStructure(psiFile);

        // Dont create individual whitespace nodes from test, instead use the formatter
        // Its normally done automatically at the end of every command, it can also be called explicitly using
        CodeStyleManager.getInstance(project).reformat(psiFile);
        // IMPORTS /// Instead of declaring imports, insert fully qualified names into the code then call shortenClassReferences()
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiFile);

        // Once finished altering the psi document, we need to call the postProcessing (formatting) and commit method
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        if (documentManager != null) {
            documentManager.doPostponedOperationsAndUnblockDocument(documentManager.getDocument(psiFile));
        }
    }

}