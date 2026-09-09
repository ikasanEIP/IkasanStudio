package org.ikasan.studio.ui.harness;

import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowRoute;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.editor.ModuleDiagramAutoSaver;
import org.ikasan.studio.intellij.settings.IkasanStudioSettings;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.component.canvas.DesignerCanvas;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live coverage for the "auto-save the module diagram on project close" option
 * (IkasanStudioSettings#isAutoSaveModuleDiagramOnCloseEnabled / ModuleDiagramAutoSaver): the setting must gate
 * whether anything happens at all, the file must land in the same place (project root, "ModuleDiagram-
 * &lt;name&gt;.png") a manual "Save image" would use, and it must work whether or not the Studio editor tab -
 * and therefore DesignerCanvas/its view-handler cache - is still open when the project closes.
 */
// ComponentTestHarness bridges the IntelliJ fixture lifecycle to Jupiter and disables Vintage discovery.
@SuppressWarnings("JUnit4AnnotatedMethodInJUnit3TestCase")
public class ModuleDiagramAutoSaverHarnessTest extends ComponentTestHarness {

    @AfterEach
    void restoreSettingDefault() {
        // IkasanStudioSettings is an application-level service - reset explicitly so enabling it here can't
        // leak into other tests sharing the same test JVM.
        IkasanStudioSettings.setAutoSaveModuleDiagramOnClose(false);
    }

    /**
     * The light test-fixture project's getBasePath() isn't backed by a real directory on disk (unlike a real
     * IDE project, whose base directory always already exists) - create it so ImageIO can actually write
     * there, matching the guarantee a real project provides.
     */
    private static File ensureBaseDirExists(Project project) throws IOException {
        String basePath = project.getBasePath();
        assertThat(basePath).as("test project must have a base path").isNotNull();
        return Files.createDirectories(Path.of(basePath)).toFile();
    }

    private Module buildModuleWithOneFlow() throws Exception {
        String metapackVersion = TestFixtures.BASE_META_PACK;
        FlowElement consumer = TestFixtures.getEventGeneratingConsumer(metapackVersion);
        FlowElement producer = TestFixtures.getDevNullProducer(metapackVersion);
        Flow flow = TestFixtures.getUnbuiltFlow(metapackVersion).consumer(consumer).build();
        FlowRoute rootRoute = FlowRoute.flowRouteBuilder()
                .flow(flow)
                .flowElements(new ArrayList<>(List.of(producer)))
                .build();
        consumer.setContainingFlowRoute(rootRoute);
        producer.setContainingFlowRoute(rootRoute);
        flow.setFlowRoute(rootRoute);
        return TestFixtures.getMyFirstModuleIkasanModule(metapackVersion, new ArrayList<>(List.of(flow)));
    }

    @Test
    void disabledSettingSavesNothing() throws Exception {
        Module module = buildModuleWithOneFlow();
        Project project = getProject();
        UiContext uiContext = project.getService(UiContext.class);
        uiContext.setIkasanModule(module);
        IkasanStudioSettings.setAutoSaveModuleDiagramOnClose(false);

        File expected = new File(ensureBaseDirExists(project), DesignerCanvas.moduleDiagramFileName(module));
        Files.deleteIfExists(expected.toPath());

        ModuleDiagramAutoSaver.saveOnProjectClose(project);

        assertThat(expected).doesNotExist();
    }

    @Test
    void enabledSettingSavesADiagramEvenWithoutAnOpenCanvas() throws Exception {
        Module module = buildModuleWithOneFlow();
        Project project = getProject();
        UiContext uiContext = project.getService(UiContext.class);
        uiContext.setIkasanModule(module);
        // Simulate the Studio editor tab already being closed: no DesignerCanvas/view-handler cache
        // registered, only the module model itself - see UiContext#clearDesignerUI.
        uiContext.setDesignerCanvas(null);
        uiContext.setViewHandlerFactory(null);
        IkasanStudioSettings.setAutoSaveModuleDiagramOnClose(true);

        File expected = new File(ensureBaseDirExists(project), DesignerCanvas.moduleDiagramFileName(module));
        Files.deleteIfExists(expected.toPath());
        try {
            ModuleDiagramAutoSaver.saveOnProjectClose(project);

            assertThat(expected).exists();
            BufferedImage saved = ImageIO.read(expected);
            assertThat(saved).as("saved file should be a readable PNG").isNotNull();
            assertThat(saved.getWidth()).isPositive();
            assertThat(saved.getHeight()).isPositive();
            // Building throwaway rendering support for this one export must not leave it registered
            // afterward - nothing else should later find a designer where the tab was genuinely closed.
            assertThat(uiContext.getViewHandlerFactory()).isNull();
        } finally {
            Files.deleteIfExists(expected.toPath());
        }
    }

    @Test
    void enabledSettingReusesAnAlreadyOpenCanvas() throws Exception {
        Module module = buildModuleWithOneFlow();
        Project project = getProject();
        File expected = new File(ensureBaseDirExists(project), DesignerCanvas.moduleDiagramFileName(module));
        Files.deleteIfExists(expected.toPath());
        UiContext uiContext = project.getService(UiContext.class);
        uiContext.setViewHandlerFactory(new ViewHandlerCache(project));
        uiContext.setIkasanModule(module);
        DesignerCanvas liveCanvas = new DesignerCanvas(project);
        uiContext.setDesignerCanvas(liveCanvas);
        IkasanStudioSettings.setAutoSaveModuleDiagramOnClose(true);

        try {
            ModuleDiagramAutoSaver.saveOnProjectClose(project);

            assertThat(expected).exists();
            // The live canvas/view-handler cache registered above must still be exactly what's there
            // afterward - only the "tab already closed" path builds and then clears a throwaway one.
            assertThat(uiContext.getDesignerCanvas()).isSameAs(liveCanvas);
            assertThat(uiContext.getViewHandlerFactory()).isNotNull();
        } finally {
            try {
                Files.deleteIfExists(expected.toPath());
            } finally {
                liveCanvas.disposeCanvas();
            }
        }
    }
}
