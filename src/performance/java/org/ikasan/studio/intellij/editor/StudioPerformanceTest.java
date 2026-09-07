package org.ikasan.studio.intellij.editor;

import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.openapi.util.Disposer;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.persistence.json.ProtectedModelFileWriter;
import org.ikasan.studio.intellij.project.StudioProjectInitialisationService;
import org.ikasan.studio.integration.ikasan.ModuleControlClient;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Real platform widgets and core IO; opt-in, with no machine-dependent timing assertions. */
public class StudioPerformanceTest extends HeavyPlatformTestCase {
    private final Map<String, Object> report = new LinkedHashMap<>();
    private final Map<String, List<Double>> timings = new LinkedHashMap<>();
    private int samples, warmups;
    private Path output;
    private Path exportedProject;
    private static volatile Object blackhole;

    public void testLargeProject() throws Exception {
        createTestProjectStructure("src/test/testData/ikasanStandardSampleApps/general/");
        String fixturePom = Files.readString(Path.of("src/test/testData/ikasanStandardSampleApps/general/pom.xml"));
        com.intellij.openapi.application.WriteAction.run(() -> {
            var root = org.ikasan.studio.intellij.project.StudioProjectFiles.getProjectBaseDir(myProject);
            assertNotNull(root);
            var pom = root.findOrCreateChildData(this, "pom.xml");
            com.intellij.openapi.vfs.VfsUtil.saveText(pom, fixturePom);
        });
        samples = option("Samples", 7); warmups = option("Warmups", 2);
        int flows = option("Flows", 40), components = option("Components", 12);
        if (flows < 2 || components < 3 || samples < 1 || warmups < 0) throw new IllegalArgumentException("Invalid benchmark dimensions");
        output = Path.of(System.getProperty("studio.performance.output"));
        Files.createDirectories(output);
        exportedProject = output.resolve("project-" + flows + "x" + components + "-" + System.currentTimeMillis());
        report.put("projectDirectory", exportedProject.getFileName().toString());
        report.put("timestampUtc", java.time.Instant.now().toString());
        report.put("environment", Map.of("java", System.getProperty("java.version"), "os", System.getProperty("os.name"),
                "arch", System.getProperty("os.arch"), "processors", Runtime.getRuntime().availableProcessors(),
                "maxHeapBytes", Runtime.getRuntime().maxMemory(), "vm", System.getProperty("java.vm.name")));
        report.put("fixture", Map.of("flows", flows, "componentsPerFlow", components, "components", flows * components,
                "metaPack", TestFixtures.BASE_META_PACK, "warmups", warmups, "samples", samples));
        Module module = background(() -> fixture(flows, components));
        String json = background(() -> ComponentIO.toJson(module));
        Path model = exportedProject.resolve("generated/src/main/model/model.json");
        Files.createDirectories(model.getParent()); Files.writeString(model, json);
        report.put("modelBytes", Files.size(model));
        report.put("viewport", Map.of("width", 1440, "height", 900, "uiScale", com.intellij.util.ui.JBUI.scale(100) / 100.0));
        var context = myProject.getService(UiContext.class);
        context.setIkasanModule(module);
        myProject.getService(StudioProjectInitialisationService.class).markReady();
        try {
            measure("model.load", () -> background(() -> ComponentIO.deserializeModuleInstanceString(Files.readString(model), "performance")));
            measure("model.save.atomicWithBackup", () -> background(() -> {
                ProtectedModelFileWriter.write(model, ComponentIO.toJson(module), value -> ComponentIO.deserializeModuleInstanceString(value, "performance save"));
                return null;
            }));
            measure("generation.templates", () -> background(() -> render(module)));
            measure("editor.open.readyProject", () -> {
                var editor = new IkasanStudioFileEditor(myProject, new IkasanStudioVirtualFile());
                editor.getComponent().setSize(1440, 900); editor.getComponent().doLayout();
                return editor;
            }, result -> Disposer.dispose((IkasanStudioFileEditor) result));
            measureCanvas(module);
            measureGenerationCommit(module);
            measurePolling(module);
            blackhole = null;
            var references = closeEditorAndReleaseReferences();
            collect();
            report.put("memory.afterClose", Map.of("usedHeapBytes", usedHeap(),
                    "editorReachable", references.get(0).get() != null, "canvasReachable", references.get(1).get() != null,
                    "propertiesReachable", references.get(2).get() != null,
                    "contextCanvasCleared", context.getDesignerCanvas() == null,
                    "modelRetainedByDesign", context.getIkasanModule() == module));
        } finally { writeReport(); }
    }

    private void measureCanvas(Module module) throws Exception {
        var editor = new IkasanStudioFileEditor(myProject, new IkasanStudioVirtualFile());
        var context = myProject.getService(UiContext.class);
        DesignerCanvas canvas = context.getDesignerCanvas();
        canvas.setSize(1440, 900);
        var image = com.intellij.util.ui.UIUtil.createImage(1440, 900, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            measure("canvas.layoutAndPaint", () -> { canvas.setInitialiseAllDimensions(true); canvas.paint(graphics); return null; });
            measure("canvas.repaint.cachedLayout", () -> { canvas.paint(graphics); return null; });
            int[] index = {0};
            measure("properties.selectionChange", () -> {
                var flow = module.getFlows().get(index[0]++ % module.getFlows().size());
                context.getPropertiesTabPanel().updateTargetComponent(flow.getConsumer()); return null;
            });
            Flow last = module.getFlows().get(module.getFlows().size() - 1);
            var target = ViewHandlerCache.getFlowComponentViewHandler(myProject, last.getConsumer()).getCentrePoint();
            FlowElement candidate = TestFixtures.getDebugTransition(module.getMetaVersion());
            measure("drag.hoverValidation.lastFlow", () -> {
                blackhole = canvas.componentDraggedToFlowAction(target.x, target.y, candidate);
                canvas.resetContextSensitiveHighlighting(); return null;
            });
            var first = module.getFlows().get(0).getFlowRoute().getFlowElements();
            canvas.setSelectedComponent(first.get(0));
            measure("drag.moveGesture", () -> {
                var from = ViewHandlerCache.getFlowComponentViewHandler(myProject, first.get(0)).getCentrePoint();
                var to = ViewHandlerCache.getFlowComponentViewHandler(myProject, first.get(1)).getCentrePoint();
                long now = System.currentTimeMillis();
                canvas.dispatchEvent(new java.awt.event.MouseEvent(canvas, java.awt.event.MouseEvent.MOUSE_PRESSED, now, 0, from.x, from.y, 1, false, 1));
                canvas.dispatchEvent(new java.awt.event.MouseEvent(canvas, java.awt.event.MouseEvent.MOUSE_DRAGGED, now, java.awt.event.InputEvent.BUTTON1_DOWN_MASK, to.x, to.y, 0, false, 0));
                // Release at original location: a repeatable no-op drop, not a mutation benchmark.
                canvas.dispatchEvent(new java.awt.event.MouseEvent(canvas, java.awt.event.MouseEvent.MOUSE_RELEASED, now, 0, from.x, from.y, 1, false, 1));
                return null;
            });
        } finally { graphics.dispose(); Disposer.dispose(editor); }
    }

    private void measureGenerationCommit(Module module) throws Exception {
        var editor = new IkasanStudioFileEditor(myProject, new IkasanStudioVirtualFile());
        try {
            var synchronizer = new org.ikasan.studio.intellij.project.GeneratedProjectSynchronizer(myProject);
            long started = System.nanoTime();
            await(synchronizer.asynchGenerateSourceFromModelJsonInstanceAndSaveToDisk(
                    org.ikasan.studio.core.generation.GenerationRequest.full()));
            timings.put("generation.firstCommit", List.of((System.nanoTime() - started) / 1_000_000.0));
            measure("generation.regenerate.unchanged", () -> await(synchronizer.asynchGenerateSourceFromModelJsonInstanceAndSaveToDisk(
                    org.ikasan.studio.core.generation.GenerationRequest.full())));
            background(() -> { exportProject(module); return null; });
        } finally { Disposer.dispose(editor); }
    }

    private void exportProject(Module module) throws Exception {
        var baseDir = org.ikasan.studio.intellij.project.StudioProjectFiles.getProjectBaseDir(myProject);
        assertNotNull(baseDir);
        Path root = Path.of(baseDir.getPath());
        Path exported = exportedProject;
        try (var files = Files.walk(root)) {
            for (Path source : files.filter(Files::isRegularFile).toList()) {
                Path relative = root.relativize(source);
                if (!relative.startsWith("generated") && !relative.startsWith("user") && !relative.toString().equals("AGENTS.md")) continue;
                Path target = exported.resolve(relative); Files.createDirectories(target.getParent());
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        var reader = new org.apache.maven.model.io.xpp3.MavenXpp3Reader();
        org.apache.maven.model.Model pom;
        try (var input = Files.newBufferedReader(root.resolve("pom.xml"))) { pom = reader.read(input); }
        pom.setGroupId("org.ikasan.studio.performance"); pom.setArtifactId("performance"); pom.setVersion("1.0-SNAPSHOT");
        pom.setPackaging("pom"); pom.setModules(List.of("user", "generated"));
        try (var writer = Files.newBufferedWriter(exported.resolve("pom.xml"))) {
            new org.apache.maven.model.io.xpp3.MavenXpp3Writer().write(writer, pom);
        }
        Path archetype = Path.of("ikasan-studio-ancillary/ikasan-studio-project-archetype/src/main/resources/archetype-resources");
        for (String child : List.of("user", "generated")) {
            String childPom = Files.readString(archetype.resolve(child + "/pom.xml"))
                    .replace("${groupId}", pom.getGroupId()).replace("${rootArtifactId}", pom.getArtifactId()).replace("${version}", pom.getVersion());
            Files.createDirectories(exported.resolve(child)); Files.writeString(exported.resolve(child + "/pom.xml"), childPom);
        }
        Files.writeString(exported.resolve("generated/src/main/model/model.json"), ComponentIO.toJson(module));
        Files.writeString(exported.resolve("PERFORMANCE.md"), """
                # Studio performance fixture
                Open pom.xml in an isolated IntelliJ project and open Ikasan Studio after Maven import.
                This deterministic model exercises FTP, JMS, local-file, event and email connectors.
                Connector settings are test data; importing and editing does not require those services.
                It is a designer/generation benchmark, not a configured runnable integration deployment.
                See docs/PerformanceTesting.md in the Studio repository for measurements and profiling.
                """);
    }

    private List<WeakReference<?>> closeEditorAndReleaseReferences() {
        collect(); report.put("memory.beforeOpen.usedHeapBytes", usedHeap());
        var editor = new IkasanStudioFileEditor(myProject, new IkasanStudioVirtualFile());
        var context = myProject.getService(UiContext.class);
        List<WeakReference<?>> refs = List.of(new WeakReference<>(editor), new WeakReference<>(context.getDesignerCanvas()),
                new WeakReference<>(context.getPropertiesPanel()));
        report.put("memory.open.usedHeapBytes", usedHeap());
        Disposer.dispose(editor);
        return refs;
    }

    private void measurePolling(Module module) throws Exception {
        int delay = option("PollDelayMs", 250);
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("localhost", 0), 0);
        byte[] response = new ObjectMapper().writeValueAsBytes(Map.of("flows", module.getFlows().stream()
                .map(flow -> Map.of("name", flow.getIdentity(), "state", "running")).toList()));
        server.createContext("/", exchange -> {
            try { Thread.sleep(delay); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            exchange.sendResponseHeaders(200, response.length);
            try (var out = exchange.getResponseBody()) { out.write(response); }
        });
        String oldPort = module.getPort();
        module.setPropertyValue("port", Integer.toString(server.getAddress().getPort()));
        server.start();
        try {
            report.put("polling.simulatedServerDelayMs", delay);
            measure("polling.slowServer.wall", () -> background(() -> ModuleControlClient.fetchFlowStates(module)));
            var future = CompletableFuture.supplyAsync(() -> {
                try { return ModuleControlClient.fetchFlowStates(module); }
                catch (Exception e) { throw new CompletionException(e); }
            });
            measure("polling.edtHeartbeat", () -> {
                var heartbeat = new CompletableFuture<Void>();
                javax.swing.SwingUtilities.invokeLater(() -> heartbeat.complete(null)); await(heartbeat); return null;
            });
            await(future);
        } finally { module.setPropertyValue("port", oldPort); server.stop(0); }
    }

    private static Module fixture(int count, int components) throws Exception {
        String pack = TestFixtures.BASE_META_PACK;
        List<Flow> flows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            FlowElement consumer = switch (i % 4) {
                case 0 -> TestFixtures.getFtpConsumer(pack);
                case 1 -> TestFixtures.getSpringJmsConsumer(pack);
                case 2 -> TestFixtures.getLocalFileConsumer(pack);
                default -> TestFixtures.getEventGeneratingConsumer(pack);
            };
            consumer.setComponentName("Consumer " + i);
            Flow flow = TestFixtures.getUnbuiltFlow(pack).name(String.format(java.util.Locale.ROOT, "Flow %04d", i)).consumer(consumer).build();
            consumer.setContainingFlow(flow);
            var elements = new ArrayList<FlowElement>();
            for (int j = 1; j < components; j++) {
                FlowElement element = j == components - 1
                        ? (i % 2 == 0 ? TestFixtures.getEmailProducer(pack) : TestFixtures.getJmsProducer(pack))
                        : TestFixtures.getDebugTransition(pack);
                element.setComponentName("Component " + i + " " + j);
                if (element.getComponentMeta().isDebug()) element.setPropertyValue("userImplementedClassName", "DebugStep" + j);
                element.setContainingFlow(flow);
                element.setContainingFlowRoute(flow.getFlowRoute());
                elements.add(element);
            }
            flow.getFlowRoute().setFlowElements(elements);
            flows.add(flow);
        }
        return TestFixtures.getMyFirstModuleIkasanModule(pack, flows);
    }

    private static Object render(Module module) throws Exception {
        long bytes = ModuleConfigTemplate.create(module).length() + PropertiesTemplate.create(module).length();
        for (Flow flow : module.getFlows()) {
            String pkg = "org.ikasan.studio.flow." + flow.getJavaPackageName();
            bytes += FlowTemplate.create(pkg, module, flow).length();
            bytes += FlowsComponentFactoryTemplate.create(pkg, module, flow).length();
        }
        return bytes;
    }
    private void measure(String name, Callable<?> work) throws Exception { measure(name, work, ignored -> { }); }
    private void measure(String name, Callable<?> work, java.util.function.Consumer<Object> cleanup) throws Exception {
        for (int i = 0; i < warmups; i++) { blackhole = work.call(); cleanup.accept(blackhole); }
        var values = new ArrayList<Double>(); timings.put(name, values);
        for (int i = 0; i < samples; i++) {
            long start = System.nanoTime(); blackhole = work.call(); values.add((System.nanoTime() - start) / 1_000_000.0);
            cleanup.accept(blackhole);
        }
        blackhole = null;
    }
    private static <T> T background(Callable<T> work) throws Exception {
        return await(CompletableFuture.supplyAsync(() -> {
            try { return work.call(); } catch (Exception e) { throw new CompletionException(e); }
        }));
    }
    private static <T> T await(CompletableFuture<T> future) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(120);
        while (!future.isDone()) {
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            if (System.nanoTime() > deadline) throw new TimeoutException("Benchmark operation exceeded 120 seconds");
            Thread.sleep(1);
        }
        return future.get();
    }
    private static void collect() {
        for (int i = 0; i < 3; i++) { PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue(); System.gc(); }
    }
    private static long usedHeap() { return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed(); }
    private static int option(String name, int fallback) { return Integer.getInteger("studio.performance." + name, fallback); }
    private void writeReport() throws Exception {
        Map<String, Object> metrics = new LinkedHashMap<>();
        timings.forEach((name, raw) -> {
            var sorted = raw.stream().sorted().toList();
            if (!sorted.isEmpty()) metrics.put(name, Map.of("samplesMs", raw, "p50Ms", sorted.get(sorted.size()/2),
                    "p95Ms", sorted.get(Math.min(sorted.size()-1, (int)Math.ceil(sorted.size()*.95)-1))));
        });
        report.put("timings", metrics);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.resolve("measurements.json").toFile(), report);
    }
}
