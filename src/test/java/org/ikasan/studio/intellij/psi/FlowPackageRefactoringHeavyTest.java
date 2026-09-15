package org.ikasan.studio.intellij.psi;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.PsiTestUtil;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.GeneratorUtils;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.project.StudioProjectFiles;

import java.util.List;

public class FlowPackageRefactoringHeavyTest extends HeavyPlatformTestCase {
    private Module module;
    private Flow flow;
    private String oldPackage;
    private String newPackage;

    static {
        try { TestFixtures.getBroker(TestFixtures.BASE_META_PACK); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createTestProjectStructure("src/test/testData/ikasanStandardSampleApps/general/");
        flow = TestFixtures.getUnbuiltFlow(TestFixtures.BASE_META_PACK).name("Original Flow")
                .consumer(TestFixtures.getEventGeneratingConsumer(TestFixtures.BASE_META_PACK)).build();
        module = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, List.of(flow));
        oldPackage = GeneratorUtils.getUserImplementedClassesPackageName(module, flow);
        newPackage = module.getApplicationPackageName() + ".renamedflow";
        write(oldPackage, "Implementation", "package " + oldPackage + "; public class Implementation { public String run() { return new Helper().value(); } }");
        VirtualFile sourceRoot = file(oldPackage, "Implementation").getParent();
        for (String ignored : oldPackage.split("\\.")) sourceRoot = sourceRoot.getParent();
        PsiTestUtil.addContentRoot(myModule, sourceRoot);
        PsiTestUtil.addSourceRoot(myModule, sourceRoot);
        write(oldPackage, "Helper", "package " + oldPackage + "; public class Helper { public String value() { return \"USER_CODE_MARKER\"; } }");
        write("outside", "Caller", "package outside; import " + oldPackage + ".Implementation; public class Caller { Implementation value; }");
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();
    }

    public void testMovesHelpersAndUpdatesReferencesWithoutCommittingTheModelEarly() {
        var plan = plan();
        assertEquals(2, plan.files().size());
        assertTrue(FlowPackageRefactoring.execute(plan));
        assertNull(file(oldPackage, "Implementation"));
        assertNotNull(file(newPackage, "Implementation"));
        assertTrue(text(newPackage, "Helper").contains("USER_CODE_MARKER"));
        assertTrue(text("outside", "Caller").contains(newPackage + ".Implementation"));
        assertEquals("Original Flow", flow.getIdentity());
    }

    public void testDestinationConflictLeavesOriginalCodeAndModelUntouched() {
        write(newPackage, "Implementation", "package " + newPackage + "; public class Implementation {}");
        try {
            plan();
            fail("Expected destination conflict");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains(newPackage));
        }
        assertTrue(text(oldPackage, "Helper").contains("USER_CODE_MARKER"));
        assertEquals("Original Flow", flow.getIdentity());
    }

    public void testFailureAfterRefactoringRestoresPackageAndOutsideReferences() {
        String caller = text("outside", "Caller");
        try {
            FlowPackageRefactoring.execute(plan(), () -> { throw new IllegalStateException("injected failure"); });
            fail("Expected failure");
        } catch (IllegalStateException expected) {
            assertEquals("injected failure", expected.getMessage());
            assertEquals(0, expected.getSuppressed().length);
        }
        assertNotNull(file(oldPackage, "Implementation"));
        assertNull(file(newPackage, "Implementation"));
        assertEquals(caller, text("outside", "Caller"));
        assertTrue(text(oldPackage, "Helper").contains("USER_CODE_MARKER"));
    }

    public void testDefaultBeanNamesAndReferencesChangeButCustomNamesRemain() {
        write("org.springframework.stereotype", "Component", "package org.springframework.stereotype; public @interface Component { String value(); }");
        write("javax.annotation", "Resource", "package javax.annotation; public @interface Resource { String name(); }");
        write(oldPackage, "Implementation", "package " + oldPackage + "; @org.springframework.stereotype.Component(\"" + oldPackage
                + ".Implementation\") public class Implementation {}");
        write(oldPackage, "Helper", "package " + oldPackage + "; @org.springframework.stereotype.Component(\"custom-name\") public class Helper {}");
        write("outside", "Caller", "package outside; public class Caller { @javax.annotation.Resource(name=\""
                + oldPackage + ".Implementation\") Object value; }");
        assertTrue(FlowPackageRefactoring.execute(plan()));
        assertTrue(text(newPackage, "Implementation").contains("\"" + newPackage + ".Implementation\""));
        assertTrue(text("outside", "Caller").contains("\"" + newPackage + ".Implementation\""));
        assertTrue(text(newPackage, "Helper").contains("\"custom-name\""));
    }

    public void testExplicitExternalImplementationInFlowPackageBlocksRename() throws Exception {
        var broker = TestFixtures.getBroker(TestFixtures.BASE_META_PACK);
        broker.setContainingFlow(flow);
        flow.getFlowRoute().getFlowElements().add(broker);
        broker.setPropertyValue("requiresStub", false);
        broker.setPropertyValue("userImplementedClassName", oldPackage + ".Implementation");
        try { plan(); fail("Expected externally supplied implementation guard"); }
        catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains(oldPackage + ".Implementation")); }
        assertNotNull(file(oldPackage, "Implementation"));
    }

    public void testCancellationRestoresSourceAndKeepsOldIdentity() {
        try {
            FlowPackageRefactoring.execute(plan(), () -> { throw new com.intellij.openapi.progress.ProcessCanceledException(); });
        } catch (com.intellij.openapi.progress.ProcessCanceledException expected) {
            // The platform may propagate cancellation or consume it at its progress boundary.
        }
        assertNotNull(file(oldPackage, "Implementation"));
        assertNull(file(newPackage, "Implementation"));
        assertEquals("Original Flow", flow.getIdentity());
    }

    public void testRenamePersistsModelAndReloadNavigationFindsMovedImplementation() throws Exception {
        var broker = org.ikasan.studio.core.model.ikasan.instance.FlowElementFactory.createFlowElement(
                TestFixtures.BASE_META_PACK,
                org.ikasan.studio.core.metapack.ComponentLibrary.getIkasanComponentByKeyMandatory(TestFixtures.BASE_META_PACK, "Broker"),
                flow, flow.getFlowRoute(), "Implementation");
        broker.setPropertyValue("userImplementedClassName", "Implementation");
        flow.getFlowRoute().getFlowElements().add(broker);
        var context = myProject.getService(org.ikasan.studio.ui.UiContext.class);
        context.setIkasanModule(module);
        context.setViewHandlerFactory(new org.ikasan.studio.ui.viewmodel.ViewHandlerCache(myProject));
        assertTrue(FlowPackageRefactoring.renameAndSave(plan(), module, flow, "Renamed Flow"));
        assertEquals("Renamed Flow", flow.getIdentity());
        String flowPackage = org.ikasan.studio.core.generator.Generator.STUDIO_FLOW_PACKAGE + "." + flow.getJavaPackageName();
        StudioProjectFiles.createJavaSourceFile(myProject, StudioProjectFiles.GENERATED_CONTENT_ROOT,
                StudioProjectFiles.SRC_MAIN_JAVA_CODE, flowPackage, flow.getJavaClassName(),
                "package " + flowPackage + "; public class " + flow.getJavaClassName() + " {}", null);
        new org.ikasan.studio.intellij.project.GeneratedProjectSynchronizer(myProject).initialisePsiFileHandles();
        com.intellij.openapi.application.impl.NonBlockingReadActionImpl.waitForAsyncTaskCompletion();
        var target = org.ikasan.studio.ui.viewmodel.ViewHandlerCache.getFlowComponentViewHandler(myProject, broker).getCodeNavigationTarget();
        assertTrue(target.isPresent());
        assertEquals(file(newPackage, "Implementation"), target.psiFile().getVirtualFile());
        var base = StudioProjectFiles.getProjectBaseDir(myProject);
        assertNotNull(base);
        var modelFile = base.findFileByRelativePath("generated/src/main/model/model.json");
        assertNotNull(modelFile);
        String modelText = StudioProjectFiles.readVirtualFileAsString(modelFile);
        assertNotNull(modelText);
        assertTrue(modelText.contains("Renamed Flow"));
    }

    public void testModelSaveFailureRestoresCodeAndName() {
        var context = myProject.getService(org.ikasan.studio.ui.UiContext.class);
        context.setIkasanModule(module);
        StudioProjectFiles.createFileWithDirectories(myProject, "/generated/src/main/model/model.json", "{broken", null);
        try {
            FlowPackageRefactoring.renameAndSave(plan(), module, flow, "Renamed Flow");
            fail("Expected protected model writer to reject invalid existing model");
        } catch (RuntimeException expected) {
            assertEquals("Original Flow", flow.getIdentity());
            assertNotNull(file(oldPackage, "Implementation"));
            assertNull(file(newPackage, "Implementation"));
        }
    }

    public void testRenamingBackRestoresModelNameAndImplementationPackage() {
        var context = myProject.getService(org.ikasan.studio.ui.UiContext.class);
        context.setIkasanModule(module);
        assertTrue(FlowPackageRefactoring.renameAndSave(plan(), module, flow, "Renamed Flow"));
        var reverse = ReadAction.compute(() -> FlowPackageRefactoring.inspect(myProject, module, flow, "Original Flow"));
        assertTrue(FlowPackageRefactoring.renameAndSave(reverse, module, flow, "Original Flow"));
        assertEquals("Original Flow", flow.getIdentity());
        assertNotNull(file(oldPackage, "Implementation"));
        assertNull(file(newPackage, "Implementation"));
        assertTrue(text(oldPackage, "Helper").contains("USER_CODE_MARKER"));
    }

    private FlowPackageRefactoring.Plan plan() {
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();
        return ReadAction.compute(() -> FlowPackageRefactoring.inspect(myProject, module, flow, "Renamed Flow"));
    }

    private void write(String pkg, String name, String contents) {
        StudioProjectFiles.createJavaSourceFile(myProject, StudioProjectFiles.USER_CONTENT_ROOT,
                StudioProjectFiles.SRC_MAIN_JAVA_CODE, pkg, name, contents, null);
    }
    private VirtualFile file(String pkg, String name) { return StudioProjectFiles.getUserImplementedClassFile(myProject, pkg, name); }
    private String text(String pkg, String name) {
        VirtualFile file = file(pkg, name);
        var document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file);
        return document != null ? document.getText() : StudioProjectFiles.readVirtualFileAsString(file);
    }
}
