package org.ikasan.studio.intellij.project;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.GeneratorUtils;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.persistence.json.FlowClipboard;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class FlowSourceTransferHeavyTest extends HeavyPlatformTestCase {
    static {
        try { TestFixtures.getBroker(TestFixtures.BASE_META_PACK); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }

    @Override protected void setUp() throws Exception {
        super.setUp();
        createTestProjectStructure("src/test/testData/ikasanStandardSampleApps/general/");
    }

    // This test runs local javac against local fixture files and a local temporary output directory.
    @SuppressWarnings("UseOptimizedEelFunctions")
    public void testCopiesUnsavedImplementationAndSubpackageHelpersToDifferentPackageAndCompiles() throws Exception {
        write("example.source.orders", "Convert", "package example.source.orders; public class Convert { public String run() { return \"SAVED\"; } }");
        write("example.source.orders.helpers", "Helper", "package example.source.orders.helpers; public class Helper { public static String value() { return \"HAND_WRITTEN\"; } }");
        var document = FileDocumentManager.getInstance().getDocument(file("example.source.orders", "Convert"));
        assertNotNull(document);
        String unsaved = "package example.source.orders; import example.source.orders.helpers.Helper; public class Convert { public String run() { return Helper.value() + example.source.orders.helpers.Helper.value(); } }";
        WriteCommandAction.runWriteCommandAction(myProject, () -> document.setText(unsaved));
        var copied = ReadAction.compute(() -> FlowSourceTransfer.capture(myProject, "example.source.orders"));
        assertEquals(2, copied.files().size());
        assertEquals(unsaved, copied.files().get("Convert.java"));
        var relocated = ReadAction.compute(() -> FlowSourceTransfer.relocate(myProject, copied, "another.project.orderscopy"));
        FlowSourceTransfer.install(myProject, relocated);
        assertEquals(unsaved, document.getText());
        assertTrue(text("another.project.orderscopy", "Convert").contains("import another.project.orderscopy.helpers.Helper;"));
        assertTrue(text("another.project.orderscopy", "Convert").contains("another.project.orderscopy.helpers.Helper.value()"));
        assertTrue(text("another.project.orderscopy.helpers", "Helper").contains("HAND_WRITTEN"));
        var output = Files.createTempDirectory("flow-copy-compile");
        try {
            var compilerLog = output.resolve("javac.txt");
            Process compiler = new ProcessBuilder("javac", "-d", output.toString(),
                    file("another.project.orderscopy", "Convert").getPath(), file("another.project.orderscopy.helpers", "Helper").getPath())
                    .redirectErrorStream(true).redirectOutput(compilerLog.toFile()).start();
            if (!compiler.waitFor(20, java.util.concurrent.TimeUnit.SECONDS)) {
                compiler.destroyForcibly();
                fail("javac timed out");
            }
            assertEquals(Files.readString(compilerLog), 0, compiler.exitValue());
        } finally { com.intellij.openapi.util.io.NioFiles.deleteRecursively(output); }
    }

    public void testUpdatesSpringBeanReferencesWithoutRewritingOrdinaryStringsOrOtherPackages() throws Exception {
        String source = "package example.flow; import example.flow.Helper; import example.flowextra.Other; "
                + "@org.springframework.stereotype.Component(\"example.flow.Implementation\") class Implementation { "
                + "@javax.annotation.Resource(name=\"example.flow.Helper\") Object helper; "
                + "@org.springframework.beans.factory.annotation.Qualifier(\"custom-bean\") Object custom; "
                + "String text = \"example.flow.Helper\"; /* example.flow.Helper */ }";
        var copied = new FlowClipboard.Sources("example.flow", Map.of("Implementation.java", source));
        var relocated = ReadAction.compute(() -> FlowSourceTransfer.relocate(myProject, copied, "dest.renamed"));
        String result = relocated.files().get("Implementation.java");
        assertTrue(result.contains("package dest.renamed;"));
        assertTrue(result.contains("import dest.renamed.Helper;"));
        assertTrue(result.contains("import example.flowextra.Other;"));
        assertTrue(result.contains("Component(\"dest.renamed.Implementation\")"));
        assertTrue(result.contains("name=\"dest.renamed.Helper\""));
        assertTrue(result.contains("Qualifier(\"custom-bean\")"));
        assertTrue(result.contains("String text = \"example.flow.Helper\""));
        assertTrue(result.contains("/* example.flow.Helper */"));
    }

    public void testCollisionAndFailedWriteDoNotReplaceOrLeavePartialSources() throws Exception {
        write("dest.existing", "Implementation", "package dest.existing; class Implementation { /* KEEP */ }");
        var conflict = new FlowClipboard.Sources("dest.existing", Map.of("Implementation.java", "replacement"));
        try { FlowSourceTransfer.install(myProject, conflict); fail("Expected collision"); }
        catch (FlowSourceTransfer.DestinationExists expected) { assertEquals("dest.existing", expected.packageName); }
        assertTrue(text("dest.existing", "Implementation").contains("KEEP"));
        var sources = new FlowClipboard.Sources("dest.failed", Map.of("A.java", "package dest.failed; class A {}", "B.java", "package dest.failed; class B {}"));
        try {
            FlowSourceTransfer.install(myProject, sources, path -> { if (path.equals("B.java")) throw new IOException("Injected failure"); });
            fail("Expected write failure");
        } catch (IOException expected) { assertEquals("Injected failure", expected.getMessage()); }
        assertNull(file("dest.failed", "A"));
        var base = StudioProjectFiles.getProjectBaseDir(myProject);
        assertNotNull(base);
        assertNull(base.findFileByRelativePath("user/src/main/java/dest/failed"));
        var badPackage = new FlowClipboard.Sources("example.flow", Map.of("Wrong.java", "package unrelated; class Wrong {}"));
        try { ReadAction.compute(() -> FlowSourceTransfer.relocate(myProject, badPackage, "dest.newflow")); fail("Expected package mismatch"); }
        catch (IOException expected) { assertTrue(expected.getMessage().contains("package")); }
    }

    public void testFirstAndRepeatedGenerationPreservePastedCustomImplementation() throws Exception {
        String version = TestFixtures.BASE_META_PACK;
        Flow original = TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerWithWiretapsFlow(version);
        Flow pasted = FlowClipboard.decode(FlowClipboard.encode(FlowClipboard.capture(original, version)), version);
        pasted.setName("Copied Flow");
        Module module = TestFixtures.getMyFirstModuleIkasanModule(version, List.of(pasted));
        String pkg = GeneratorUtils.getUserImplementedClassesPackageName(module, pasted);
        var converter = pasted.getFlowElementsNoExternalEndPoints().stream()
                .filter(component -> component.getPropertyValue("userImplementedClassName") instanceof String).findFirst().orElseThrow();
        String name = converter.getPropertyValue("userImplementedClassName").toString();
        String code = "package " + pkg + "; public class " + name + " { public String convert() { return \"BESPOKE_BODY\"; } }";
        FlowSourceTransfer.install(myProject, new FlowClipboard.Sources(pkg, Map.of(name + ".java", code)));
        UiContext context = myProject.getService(UiContext.class);
        context.setIkasanModule(module);
        context.setViewHandlerFactory(new ViewHandlerCache(myProject));
        var generate = GeneratedProjectSynchronizer.class.getDeclaredMethod("generateAndSaveUserImplementClassStubsForFlow", com.intellij.openapi.project.Project.class, Module.class, Flow.class);
        generate.setAccessible(true);
        for (int attempt = 0; attempt < 2; attempt++) {
            GenerationTransactionManager.begin();
            try {
                generate.invoke(new GeneratedProjectSynchronizer(myProject), myProject, module, pasted);
                GenerationTransactionManager.commit(myProject);
            } finally { GenerationTransactionManager.abort(); }
            assertEquals(code, text(pkg, name));
        }
    }

    private void write(String pkg, String name, String source) {
        StudioProjectFiles.createJavaSourceFile(myProject, StudioProjectFiles.USER_CONTENT_ROOT,
                StudioProjectFiles.SRC_MAIN_JAVA_CODE, pkg, name, source, null);
    }
    private VirtualFile file(String pkg, String name) { return StudioProjectFiles.getUserImplementedClassFile(myProject, pkg, name); }
    private String text(String pkg, String name) throws IOException {
        return new String(file(pkg, name).contentsToByteArray(), java.nio.charset.StandardCharsets.UTF_8);
    }
}
