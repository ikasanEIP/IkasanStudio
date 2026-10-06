package org.ikasan.studio.intellij.testing;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import java.util.List;

public class FlowTestRenameRefactoringHeavyTest extends HeavyPlatformTestCase {
    private Flow flow;
    private Module module;
    private static final String TEST = "user-flow-tests/src/test/java/org/ikasan/studio/flowtests/BusinessTest.java";
    private static final String RESOURCES = "user-flow-tests/src/test/resources/";
    @Override protected void setUp() throws Exception {
        super.setUp();
        createTestProjectStructure("src/test/testData/ikasanStandardSampleApps/general/");
        flow = TestFixtures.getUnbuiltFlow(TestFixtures.BASE_META_PACK).name("Original Flow")
                .consumer(TestFixtures.getEventGeneratingConsumer(TestFixtures.BASE_META_PACK)).build();
        flow.getConsumer().setName("Old Consumer");
        flow.getConsumer().setContainingFlow(flow);
        module = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, List.of(flow));
        write(TEST, """
                package org.ikasan.studio.flowtests;
                class BusinessTest {
                    private static final boolean TEST_REVIEWED = true;
                    private static final String FLOW_NAME = "Original Flow";
                    private static final String CONSUMER_NAME = "Old Consumer";
                    private static final String FIRST_BATCH_INPUT_FILENAME = "/Original_Flow/Old_Consumer/first.txt";
                    private static final String EXPECTED = "Original Flow";
                    String getFlowName() { return FLOW_NAME; }
                    void defineExpectedPath(Harness harness) { harness.consumer("Old Consumer"); }
                    void customAssertion() { assertEquals("Old Consumer", "Old Consumer"); }
                }
                """);
        write(RESOURCES + "Original_Flow/Old_Consumer/first.txt", "BUSINESS DATA");
        VirtualFile root = file("user-flow-tests/src/test/java");
        com.intellij.testFramework.PsiTestUtil.addContentRoot(myModule, root);
        com.intellij.testFramework.PsiTestUtil.addSourceRoot(myModule, root, true);
    }

    public void testFlowRenamePreservesBusinessValuesAndMovesFixtures() {
        var plan = inspect(null, "Renamed Flow");
        WriteCommandAction.runWriteCommandAction(myProject, () -> plan.apply(() -> {}));
        assertTrue(text().contains("FLOW_NAME = \"Renamed Flow\""));
        assertTrue(text().contains("EXPECTED = \"Original Flow\""));
        assertTrue(text().contains("TEST_REVIEWED = true"));
        assertTrue(text().contains("/Renamed_Flow/Old_Consumer/first.txt"));
        assertNotNull(file(RESOURCES + "Renamed_Flow/Old_Consumer/first.txt"));
        assertNull(file(RESOURCES + "Original_Flow"));
        assertEquals("BUSINESS DATA", readText(RESOURCES + "Renamed_Flow/Old_Consumer/first.txt"));
        assertTrue(plan.reviewFiles().contains("BusinessTest.java"));
    }

    public void testComponentRenameUpdatesOnlyRecognisedUsages() {
        var plan = inspect(flow.getConsumer(), "New Consumer");
        WriteCommandAction.runWriteCommandAction(myProject, () -> plan.apply(() -> {}));
        assertTrue(text().contains("CONSUMER_NAME = \"New Consumer\""));
        assertTrue(text().contains("harness.consumer(\"New Consumer\")"));
        assertTrue(text().contains("assertEquals(\"Old Consumer\", \"Old Consumer\")"));
        assertTrue(text().contains("/Original_Flow/New_Consumer/first.txt"));
        assertNotNull(file(RESOURCES + "Original_Flow/New_Consumer/first.txt"));
    }

    public void testPersistenceFailureRestoresReferencesAndResources() {
        String before = text();
        var plan = inspect(null, "Renamed Flow");
        try {
            WriteCommandAction.runWriteCommandAction(myProject, () -> plan.apply(() -> { throw new IllegalStateException("save failed"); }));
            fail("Expected failure");
        } catch (IllegalStateException expected) { assertEquals("save failed", expected.getMessage()); }
        assertEquals(before, text());
        assertNotNull(file(RESOURCES + "Original_Flow/Old_Consumer/first.txt"));
        assertNull(file(RESOURCES + "Renamed_Flow"));
    }

    public void testDestinationConflictStopsBeforeEdits() {
        write(RESOURCES + "Renamed_Flow/existing.txt", "KEEP");
        String before = text();
        try { inspect(null, "Renamed Flow"); fail("Expected conflict"); }
        catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("conflict")); }
        assertEquals(before, text());
        assertNotNull(file(RESOURCES + "Original_Flow/Old_Consumer/first.txt"));
    }

    public void testUnsavedScenarioIsNotOverwritten() {
        var document = FileDocumentManager.getInstance().getDocument(file(TEST));
        assertNotNull(document);
        WriteCommandAction.runWriteCommandAction(myProject, () -> document.insertString(0, "// unsaved edit\n"));
        try { inspect(null, "Renamed Flow"); fail("Expected unsaved-file guard"); }
        catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("Save")); }
        assertTrue(text().startsWith("// unsaved edit"));
        assertNotNull(file(RESOURCES + "Original_Flow/Old_Consumer/first.txt"));
    }

    public void testComponentRenamePersistsNameAndTestTogether() {
        myProject.getService(org.ikasan.studio.ui.UiContext.class).setIkasanModule(module);
        var previous = com.intellij.openapi.ui.TestDialogManager.setTestDialog(com.intellij.openapi.ui.TestDialog.OK);
        try { FlowTestRenameRefactoring.renameComponentAndSave(myProject, module, flow.getConsumer(), "New Consumer"); }
        finally { com.intellij.openapi.ui.TestDialogManager.setTestDialog(previous); }
        assertEquals("New Consumer", flow.getConsumer().getIdentity());
        assertTrue(text().contains("CONSUMER_NAME = \"New Consumer\""));
        assertTrue(readText("generated/src/main/model/model.json").contains("New Consumer"));
    }

    public void testPropertyLookupsAndOverridesFollowFlowRename() throws Exception {
        flow.setConsumer(TestFixtures.getLocalFileConsumer(TestFixtures.BASE_META_PACK));
        String oldKey = "originalflow.file.consumer.filenames";
        String newKey = "renamedflow.file.consumer.filenames";
        write(TEST, text().replace("void customAssertion()", "String directory(Context context) { return context.getEnvironment().getRequiredProperty(\""
                + oldKey + "\"); } void customAssertion()"));
        String properties = RESOURCES + "module-test.properties";
        write(properties, oldKey + "=custom/input\ntest.input=${" + oldKey + "}\nunrelated=keep\n");
        var plan = inspect(null, "Renamed Flow");
        WriteCommandAction.runWriteCommandAction(myProject, () -> plan.apply(() -> {}));
        assertTrue(text().contains("getRequiredProperty(\"" + newKey + "\")"));
        assertEquals(newKey + "=custom/input\ntest.input=${" + newKey + "}\nunrelated=keep\n",
                readText(properties));
    }

    public void testConflictingPropertyOverridesBlockRename() throws Exception {
        flow.setConsumer(TestFixtures.getLocalFileConsumer(TestFixtures.BASE_META_PACK));
        write(RESOURCES + "module-test.properties", "originalflow.file.consumer.filenames=old\nrenamedflow.file.consumer.filenames=new\n");
        String before = text();
        try { inspect(null, "Renamed Flow"); fail("Expected property conflict"); }
        catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("property keys")); }
        assertEquals(before, text());
    }

    public void testCustomComputedIdentityIsReportedWithoutEditingAssertions() {
        write(TEST, text().replace("private static final String FLOW_NAME = \"Original Flow\";",
                "private static final String CUSTOM_FLOW = \"Original Flow\";"));
        String before = text();
        var plan = inspect(null, "Renamed Flow");
        assertTrue(plan.reviewFiles().contains("BusinessTest.java"));
        WriteCommandAction.runWriteCommandAction(myProject, () -> plan.apply(() -> {}));
        assertEquals(before, text());
    }

    public void testVersion4FlowRenameUsesTheSameTestContract() throws Exception {
        flow = TestFixtures.getUnbuiltFlow("V4.1.6").name("Original Flow")
                .consumer(TestFixtures.getEventGeneratingConsumer("V4.1.6")).build();
        module = TestFixtures.getMyFirstModuleIkasanModule("V4.1.6", List.of(flow));
        var plan = inspect(null, "Renamed Flow");
        WriteCommandAction.runWriteCommandAction(myProject, () -> plan.apply(() -> {}));
        assertTrue(text().contains("FLOW_NAME = \"Renamed Flow\""));
        assertTrue(text().contains("EXPECTED = \"Original Flow\""));
        assertNotNull(file(RESOURCES + "Renamed_Flow/Old_Consumer/first.txt"));
    }

    private static final String GENERATED_TEST = "user-flow-tests/src/test/java/org/ikasan/studio/flowtests/OriginalFlowFlowTest.java";
    private static final String RENAMED_TEST = "user-flow-tests/src/test/java/org/ikasan/studio/flowtests/RenamedFlowFlowTest.java";

    private void generatedTest() {
        write(GENERATED_TEST, """
                package org.ikasan.studio.flowtests;
                public class OriginalFlowFlowTest {
                    private static final String FLOW_NAME = "Original Flow";
                    public OriginalFlowFlowTest() { }
                    OriginalFlowFlowTest self() { return this; }
                }
                """);
    }

    public void testGeneratedClassAndConstructorAreRenamed() {
        generatedTest();
        String callerPath = "user-flow-tests/src/test/java/Caller.java";
        write(callerPath, "import org.ikasan.studio.flowtests.OriginalFlowFlowTest; class Caller { OriginalFlowFlowTest value; }");
        var plan = inspect(null, "Renamed Flow");
        WriteCommandAction.runWriteCommandAction(myProject, () -> plan.apply(() -> {}));
        assertNull(file(GENERATED_TEST));
        String renamed = readText(RENAMED_TEST);
        assertTrue(renamed, renamed.contains("class RenamedFlowFlowTest"));
        assertTrue(renamed, renamed.contains("public RenamedFlowFlowTest()"));
        assertTrue(renamed, renamed.contains("RenamedFlowFlowTest self()"));
        assertTrue(renamed, renamed.contains("FLOW_NAME = \"Renamed Flow\""));
        assertNotNull(file(TEST)); // Custom class names are retained.
        String caller = readText(callerPath);
        assertFalse(caller, caller.contains("OriginalFlowFlowTest"));
        assertTrue(caller, caller.contains("RenamedFlowFlowTest"));
    }

    public void testGeneratedClassRenameRollsBackWithModelSave() {
        generatedTest();
        String original = readText(GENERATED_TEST);
        String callerPath = "user-flow-tests/src/test/java/Caller.java";
        String caller = "import org.ikasan.studio.flowtests.OriginalFlowFlowTest; class Caller { OriginalFlowFlowTest value; }";
        write(callerPath, caller);
        String originalCaller = readText(callerPath);
        var plan = inspect(null, "Renamed Flow");
        try {
            WriteCommandAction.runWriteCommandAction(myProject, () -> plan.apply(() -> { throw new IllegalStateException("save failed"); }));
            fail("Expected save failure");
        } catch (IllegalStateException expected) { assertEquals("save failed", expected.getMessage()); }
        assertNull(file(RENAMED_TEST));
        assertEquals(originalCaller, readText(callerPath));
        assertEquals(original, readText(GENERATED_TEST));
        assertTrue(text().contains("FLOW_NAME = \"Original Flow\""));
        assertNotNull(file(RESOURCES + "Original_Flow/Old_Consumer/first.txt"));
    }

    public void testGeneratedClassDestinationConflictBlocksRename() {
        generatedTest();
        write(RENAMED_TEST, "class RenamedFlowFlowTest { /* keep */ }");
        try { inspect(null, "Renamed Flow"); fail("Expected class conflict"); }
        catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("conflict")); }
        assertNotNull(file(GENERATED_TEST));
        assertTrue(readText(RENAMED_TEST).contains("keep"));
    }

    public void testComponentRenameDoesNotRenameGeneratedTestClass() {
        generatedTest();
        var plan = inspect(flow.getConsumer(), "New Consumer");
        WriteCommandAction.runWriteCommandAction(myProject, () -> plan.apply(() -> {}));
        assertNotNull(file(GENERATED_TEST));
    }

    public void testRunConfigurationFollowsSuccessfulRenameOnly() {
        generatedTest();
        var manager = com.intellij.execution.RunManager.getInstance(myProject);
        var settings = manager.createConfiguration("Test runner",
                com.intellij.execution.application.ApplicationConfigurationType.getInstance().getConfigurationFactories()[0]);
        var config = (com.intellij.execution.application.ApplicationConfiguration) settings.getConfiguration();
        config.setModule(myModule);
        config.setMainClassName("org.ikasan.studio.flowtests.OriginalFlowFlowTest");
        manager.addConfiguration(settings);
        var failedPlan = inspect(null, "Renamed Flow");
        try {
            WriteCommandAction.runWriteCommandAction(myProject, () -> failedPlan.apply(() -> { throw new IllegalStateException("save failed"); }));
            fail("Expected failure");
        } catch (IllegalStateException expected) { assertEquals("save failed", expected.getMessage()); }
        assertEquals("org.ikasan.studio.flowtests.OriginalFlowFlowTest", config.getMainClassName());
        var plan = inspect(null, "Renamed Flow");
        WriteCommandAction.runWriteCommandAction(myProject, () -> plan.apply(() -> {}));
        assertEquals("org.ikasan.studio.flowtests.RenamedFlowFlowTest", config.getMainClassName());
    }

    private FlowTestRenameRefactoring.Plan inspect(org.ikasan.studio.core.model.ikasan.instance.FlowElement component, String name) {
        return ReadAction.compute(() -> FlowTestRenameRefactoring.inspect(myProject, module, flow, component, name));
    }
    private void write(String path, String content) {
        StudioProjectFiles.createFileWithDirectories(myProject, "/" + path, content, null);
    }
    private VirtualFile file(String path) {
        var base = StudioProjectFiles.getProjectBaseDir(myProject);
        assertNotNull(base);
        return base.findFileByRelativePath(path);
    }
    private String readText(String path) {
        var file = file(path);
        assertNotNull(file);
        String contents = StudioProjectFiles.readVirtualFileAsString(file);
        assertNotNull(contents);
        return contents;
    }
    private String text() {
        var file = file(TEST);
        assertNotNull(file);
        var document = FileDocumentManager.getInstance().getDocument(file);
        assertNotNull(document);
        return document.getText();
    }
}
