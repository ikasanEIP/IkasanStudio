package org.ikasan.studio.ui.component.canvas;

import com.intellij.openapi.project.Project;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.loading.MetaPackResources;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowRoute;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.execution.HarnessOverrideService;
import org.ikasan.studio.intellij.execution.IndependentH2RunConfigurationService.State;
import org.ikasan.studio.intellij.execution.IndependentH2RunConfigurationService;
import org.ikasan.studio.intellij.settings.IkasanStudioSettings;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.actions.FileDuplicateHistoryDialog;
import org.ikasan.studio.ui.actions.SftpFilesDialog;
import org.ikasan.studio.ui.actions.SftpHarnessAction;
import org.ikasan.studio.ui.component.properties.ComponentPropertiesPanel;
import org.ikasan.studio.ui.state.StudioSessionOptions;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DesignerCanvasTest {

    @Test
    void reselectingDisplayedTargetRevealsPropertiesWithoutDiscardingEdits() throws Exception {
        Project project = mock(Project.class);
        UiContext context = mock(UiContext.class);
        ComponentPropertiesPanel properties = mock(ComponentPropertiesPanel.class);
        Module module = mock(Module.class);
        when(project.getService(UiContext.class)).thenReturn(context);
        when(context.getOptions()).thenReturn(new StudioSessionOptions());
        when(context.getPropertiesPanel()).thenReturn(properties);
        when(properties.isEditingComponent(module)).thenReturn(true);
        SwingUtilities.invokeAndWait(() -> {
            DesignerCanvas canvas = new DesignerCanvas(project);
            try {
                canvas.editComponent(module);
                Mockito.verify(context).setRightTabbedPaneFocus(UiContext.PROPERTIES_TAB_INDEX);
                Mockito.verify(properties, Mockito.never())
                        .confirmSelectionChangeWithPendingEdits(ArgumentMatchers.any());
                Mockito.verify(properties, Mockito.never())
                        .updateTargetComponent(ArgumentMatchers.any());
            } finally { canvas.disposeCanvas(); }
        });
    }

    @Test
    void snapshotChoicesRefreshFromPublishedMetadataWithoutChangingReleasedSelection() throws Exception {
        Project project = mock(Project.class);
        UiContext context = mock(UiContext.class);
        when(project.getService(UiContext.class)).thenReturn(context);
        when(context.getOptions()).thenReturn(new StudioSessionOptions());
        SwingUtilities.invokeAndWait(() -> {
            try (MockedStatic<IkasanStudioSettings> settings = Mockito.mockStatic(IkasanStudioSettings.class)) {
                DesignerCanvas canvas = new DesignerCanvas(project);
                try {
                    Field field = DesignerCanvas.class.getDeclaredField("metaDataVersionComboBox");
                    field.setAccessible(true);
                    JComboBox<?> choices = (JComboBox<?>) field.get(canvas);
                    canvas.setAvailableMetaPacks(List.of("V4.1.6", "Example-SNAPSHOT", "AcmePack"),
                            Set.of("Example-SNAPSHOT", "AcmePack"));
                    assertThat(choices.getItemCount()).isEqualTo(1);
                    assertThat(choices.getSelectedItem()).isEqualTo("V4.1.6");
                    settings.when(IkasanStudioSettings::areSnapshotMetaPacksAllowed).thenReturn(true);
                    canvas.refreshMetaPackChoices();
                    assertThat(choices.getItemCount()).isEqualTo(3);
                    assertThat(choices.getSelectedItem()).isEqualTo("V4.1.6");
                    canvas.setSelectedMetapackVersion("Example-SNAPSHOT");
                    settings.when(IkasanStudioSettings::areSnapshotMetaPacksAllowed).thenReturn(false);
                    canvas.refreshMetaPackChoices();
                    assertThat(choices.getItemCount()).isEqualTo(1);
                    assertThat(choices.getSelectedItem()).isEqualTo("V4.1.6");
                } catch (ReflectiveOperationException failure) {
                    throw new AssertionError(failure);
                } finally { canvas.disposeCanvas(); }
            }
        });
    }

    @Test
    void editorShellAndPublishedPackChoicesDoNotDiscoverResourcesOnTheEdt() throws Exception {
        Project project = mock(Project.class);
        UiContext context = mock(UiContext.class);
        when(project.getService(UiContext.class)).thenReturn(context);
        when(context.getOptions()).thenReturn(new StudioSessionOptions());
        SwingUtilities.invokeAndWait(() -> {
            try (MockedStatic<ComponentLibrary> libraries = Mockito.mockStatic(ComponentLibrary.class);
                 MockedStatic<MetaPackResources> resources = Mockito.mockStatic(MetaPackResources.class)) {
                DesignerCanvas canvas = new DesignerCanvas(project);
                try {
                    canvas.setAvailableMetaPacks(List.of("V4.1.6", "AcmePack"), Set.of());
                    canvas.refreshMetaPackChoices();
                    canvas.setSelectedMetapackVersion("AcmePack");
                    libraries.verifyNoInteractions();
                    resources.verifyNoInteractions();
                } finally { canvas.disposeCanvas(); }
            }
        });
    }

    @Test
    void moduleActionsAreConfinedToTheCanvasBackgroundMenu() {
        Project project = mock(Project.class);
        UiContext context = mock(UiContext.class);
        when(project.getService(UiContext.class)).thenReturn(context);
        IndependentH2RunConfigurationService h2 = mock(IndependentH2RunConfigurationService.class);
        when(project.getService(IndependentH2RunConfigurationService.class)).thenReturn(h2);
        when(h2.state()).thenReturn(State.STOPPED);
        when(context.getViewHandlerFactory()).thenReturn(mock(ViewHandlerCache.class));
        Module module = mock(Module.class);
        when(module.isInitialised()).thenReturn(true);
        JPopupMenu moduleMenu = DesignCanvasContextMenu.createCanvasMenu(project, null, module);
        List<String> moduleLabels = Arrays.stream(moduleMenu.getComponents())
                .filter(JMenuItem.class::isInstance).map(JMenuItem.class::cast).map(JMenuItem::getText).toList();
        assertThat(moduleLabels).contains(StudioBundle.message("button.ImportModelJson"),
                StudioBundle.message("menu.SaveImage"), StudioBundle.message("label.Load"),
                StudioBundle.message("action.IkasanStudio.MigrateVersion.text"));

        assertThat(moduleLabels).contains(StudioBundle.message("exclusions.action"));
        List<String> moduleOnlyLabels = moduleLabels.stream()
                .filter(label -> !label.equals(StudioBundle.message("exclusions.action"))
                        && !label.equals(StudioBundle.message("wiretapEvents.action"))
                        && !label.equals(StudioBundle.message("menu.DescribeComponent"))
                        && !label.equals(StudioBundle.message("menu.ComponentWebHelp"))).toList();
        Flow flow = mock(Flow.class);
        FlowElement component = mock(FlowElement.class);
        when(component.getComponentMeta()).thenReturn(mock(ComponentMeta.class));
        for (BasicElement element : List.of(flow, component)) {
            JPopupMenu menu = DesignCanvasContextMenu.createCanvasMenu(project, null, element);
            List<String> labels = Arrays.stream(menu.getComponents())
                    .filter(JMenuItem.class::isInstance).map(JMenuItem.class::cast).map(JMenuItem::getText).toList();
            assertThat(labels).contains(StudioBundle.message("menu.EditComponent"), StudioBundle.message("wiretapEvents.action"))
                    .doesNotContain(StudioBundle.message("menu.JumpToCode"), StudioBundle.message("menu.JumpToProperties"))
                    .doesNotContainAnyElementsOf(moduleOnlyLabels);
            if (element instanceof Flow) assertThat(labels).contains(StudioBundle.message("exclusions.action"));
            else assertThat(labels).doesNotContain(StudioBundle.message("exclusions.action"));
            assertThat(labels).endsWith(StudioBundle.message("menu.DescribeComponent"),
                    StudioBundle.message("menu.ComponentWebHelp"));
            assertThat(menu.getComponent(menu.getComponentCount() - 3)).isInstanceOf(JSeparator.class);
        }
    }


    @Test
    void sftpEndpointBrowserUsesItsOwningConsumerOrProducer() {
        Project project = mock(Project.class);
        FlowElement component = mock(FlowElement.class);
        ComponentMeta meta = mock(ComponentMeta.class);
        when(component.getComponentMeta()).thenReturn(meta);
        try (MockedStatic<SftpFilesDialog> dialog = Mockito.mockStatic(SftpFilesDialog.class)) {
            for (String endpoint : List.of("SFTP Endpoint", "FTP Endpoint", "JMS Endpoint")) {
                when(meta.getEndpointKey()).thenReturn(endpoint);
                for (boolean consumer : List.of(true, false)) {
                    when(meta.isConsumer()).thenReturn(consumer);
                    when(meta.isProducer()).thenReturn(!consumer);
                    when(meta.isTimeEventConsumer()).thenReturn(consumer);
                    when(meta.getTestPayloadAdapter()).thenReturn(
                            ComponentMeta.FILE_TRANSFER_TEST_PAYLOAD_ADAPTER);
                    JPopupMenu menu = DesignCanvasContextMenu.createEndpointMenu(project, component);
                    Optional<JMenuItem> item = Arrays.stream(menu.getComponents())
                            .filter(JMenuItem.class::isInstance).map(JMenuItem.class::cast)
                            .filter(i -> StudioBundle.message("sftpBrowser.action").equals(i.getText())).findFirst();
                    assertThat(item.isPresent()).isEqualTo(endpoint.equals("SFTP Endpoint"));
                    if (item.isPresent()) {
                        item.get().doClick();
                        dialog.verify(() -> SftpFilesDialog.open(project, component));
                        dialog.clearInvocations();
                        if (consumer) {
                            assertThat(menu.getComponent(0)).isInstanceOf(JMenuItem.class);
                            assertThat(((JMenuItem) menu.getComponent(0)).getText())
                                    .isEqualTo(StudioBundle.message("menu.TriggerLocalFileScan"));
                            assertThat(menu.getComponent(menu.getComponentCount() - 2))
                                    .isInstanceOf(JSeparator.class);
                        }
                    }
                }
            }
        }
    }

    @Test
    void fileHistoryIsAvailableOnlyForFileTransferConsumersAndUsesTheirClientId() {
        Project project = mock(Project.class);
        HarnessOverrideService harnessService = mock(HarnessOverrideService.class);
        when(project.getService(HarnessOverrideService.class)).thenReturn(harnessService);
        UiContext context = mock(UiContext.class);
        when(project.getService(UiContext.class)).thenReturn(context);
        when(context.getViewHandlerFactory()).thenReturn(mock(ViewHandlerCache.class));
        FlowElement component = mock(FlowElement.class);
        ComponentMeta meta = mock(ComponentMeta.class);
        when(component.getComponentMeta()).thenReturn(meta);
        when(component.getPropertyValueAsString("clientID")).thenReturn("orders-inbound");
        try (MockedStatic<FileDuplicateHistoryDialog> dialog = Mockito.mockStatic(FileDuplicateHistoryDialog.class)) {
            for (String endpoint : List.of("FTP Endpoint", "SFTP Endpoint", "JMS Endpoint")) {
                when(meta.getEndpointKey()).thenReturn(endpoint);
                for (boolean consumer : List.of(true, false)) {
                    when(meta.isConsumer()).thenReturn(consumer);
                    JPopupMenu menu = DesignCanvasContextMenu.createCanvasMenu(project, null, component);
                    Optional<JMenuItem> item = Arrays.stream(menu.getComponents())
                            .filter(JMenuItem.class::isInstance).map(JMenuItem.class::cast)
                            .filter(i -> StudioBundle.message("fileHistory.action").equals(i.getText())).findFirst();
                    List<String> labels = Arrays.stream(menu.getComponents())
                            .filter(JMenuItem.class::isInstance).map(JMenuItem.class::cast).map(JMenuItem::getText).toList();
                    assertThat(labels.contains(StudioBundle.message("sftpHarness.title"))).isEqualTo(endpoint.equals("SFTP Endpoint"));
                    assertThat(labels).doesNotContain(StudioBundle.message("sftpHarness.files"));
                    if (endpoint.equals("SFTP Endpoint")) {
                        when(harnessService.isEnabled(component)).thenReturn(true);
                        JPopupMenu localMenu = DesignCanvasContextMenu.createCanvasMenu(project, null, component);
                        assertThat(Arrays.stream(localMenu.getComponents())
                                .filter(JMenuItem.class::isInstance).map(JMenuItem.class::cast).map(JMenuItem::getText).toList())
                                .contains(StudioBundle.message("sftpHarness.files"));
                        when(harnessService.isEnabled(component)).thenReturn(false);
                    }
                    boolean supported = consumer && !endpoint.equals("JMS Endpoint");
                    assertThat(item.isPresent()).isEqualTo(supported);
                    if (supported) {
                        item.orElseThrow().doClick();
                        dialog.verify(() -> FileDuplicateHistoryDialog.open(project, "orders-inbound"));
                        dialog.clearInvocations();
                    }
                }
            }
        }
    }

    @Test
    void restartWarningIsVisibleInTheRenderedPayloadTooltip() throws Exception {
        String tooltip = DesignerCanvas.appendRestartWarning(
                "<html>Input: java.lang.String<br>Output: java.lang.String</html>");
        HTMLEditorKit html = new HTMLEditorKit();
        Document document = html.createDefaultDocument();
        html.read(new StringReader(tooltip), document, 0);

        assertThat(document.getText(0, document.getLength()))
                .contains("Input: java.lang.String", "Output: java.lang.String",
                        StudioBundle.message("tooltip.ModuleRestartRequiredForChange"));
    }


    @BeforeAll
    static void warmUpMetaPack() throws StudioBuildException {
        // Opening a packaged meta-pack lazily starts JVM filesystem threads. Do that before
        // IntelliJ's per-test thread-leak snapshot so the regression test stays deterministic.
        DesignerCanvas.createModuleDraft(null, BASE_META_PACK, "warm-up", "org.example");
    }

    @Test
    void moduleConfigurationDraftDoesNotMutateLiveModule() throws StudioBuildException {
        Module liveModule = Module.getDumbModuleVersion();

        Module draft = DesignerCanvas.createModuleDraft(
                liveModule, BASE_META_PACK, "my-project", "org.example.integration");

        assertThat(draft).isNotSameAs(liveModule);
        assertThat(draft.getIdentity()).isEqualTo("my-project");
        assertThat(draft.getApplicationPackageName()).isEqualTo("org.example.integration");
        assertThat(liveModule.getIdentity()).isNull();
        assertThat(liveModule.getApplicationPackageName()).isNull();
    }

    @Test
    void newModuleDefaultsFlowsToAutomaticStartup() throws StudioBuildException {
        Module placeholderModule = Module.getDumbModuleVersion();

        Module draft = DesignerCanvas.createModuleDraft(
                placeholderModule, BASE_META_PACK, "my-project", "org.example.integration");

        assertThat(draft.getPropertyValue("flowStartupType")).isEqualTo("AUTOMATIC");
        assertThat(placeholderModule.getPropertyValue("flowStartupType")).isNull();
    }


    @Test
    void gettingStartedHintProgressesWithTheFlowModel() {
        Module module = mock(Module.class);
        when(module.isInitialised()).thenReturn(true);
        when(module.getFlows()).thenReturn(List.of());
        assertThat(DesignerCanvas.getGettingStartedHint(module))
                .isEqualTo(DesignerCanvas.GettingStartedHint.NO_FLOWS);

        Flow flow = mock(Flow.class);
        FlowRoute route = mock(FlowRoute.class);
        when(module.getFlows()).thenReturn(List.of(flow));
        assertThat(DesignerCanvas.getGettingStartedHint(module))
                .isEqualTo(DesignerCanvas.GettingStartedHint.EMPTY_FLOW);

        when(flow.hasConsumer()).thenReturn(true);
        when(flow.getFlowRoute()).thenReturn(route);
        when(flow.getFlowIntegrityStatus()).thenReturn("The flow needs a producer.");
        assertThat(DesignerCanvas.getGettingStartedHint(module))
                .isEqualTo(DesignerCanvas.GettingStartedHint.ADD_COMPONENTS);

        when(flow.getFlowIntegrityStatus()).thenReturn("");
        assertThat(DesignerCanvas.getGettingStartedHint(module))
                .isEqualTo(DesignerCanvas.GettingStartedHint.READY_TO_RUN);
    }

    /**
     * Regression test: previously, with two flows both incomplete (even for different reasons), a single
     * global hint category was picked for the whole module and only the first matching flow ever got a
     * visible hint - the second flow's own issue was silently masked. getFlowHint() lets each flow be judged
     * independently, which is what fixed that.
     */
    @Test
    void flowHintIsJudgedIndependentlyPerFlow() {
        Flow needsConsumer = mock(Flow.class);
        when(needsConsumer.hasConsumer()).thenReturn(false);
        assertThat(DesignerCanvas.getFlowHint(needsConsumer)).isEqualTo(DesignerCanvas.GettingStartedHint.EMPTY_FLOW);

        Flow needsProducer = mock(Flow.class);
        when(needsProducer.hasConsumer()).thenReturn(true);
        when(needsProducer.getFlowIntegrityStatus()).thenReturn("The flow needs a producer.");
        assertThat(DesignerCanvas.getFlowHint(needsProducer)).isEqualTo(DesignerCanvas.GettingStartedHint.ADD_COMPONENTS);

        Flow complete = mock(Flow.class);
        when(complete.hasConsumer()).thenReturn(true);
        when(complete.getFlowIntegrityStatus()).thenReturn("");
        assertThat(DesignerCanvas.getFlowHint(complete)).isNull();

        assertThat(DesignerCanvas.getFlowHint(null)).isNull();
    }

    /**
     * Regression test for the multi-flow overlap bug: IkasanModuleViewHandler reserves vertical space below an
     * incomplete flow using this exact measurement (see gapAfterFlow there), so it must agree with what
     * drawHintBlock actually renders - a null hint needs no space, and a "detailed" (heading + instruction)
     * hint measures taller than heading-only, since the paint code draws both.
     */
    @Test
    void measureHintBlockHeightReflectsWhatWillActuallyBeDrawn() {
        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scratch.createGraphics();
        try {
            Font baseFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

            assertThat(DesignerCanvas.measureHintBlockHeight(g, baseFont, null, true)).isZero();

            int headingOnly = DesignerCanvas.measureHintBlockHeight(
                    g, baseFont, DesignerCanvas.GettingStartedHint.ADD_COMPONENTS, false);
            int headingAndInstruction = DesignerCanvas.measureHintBlockHeight(
                    g, baseFont, DesignerCanvas.GettingStartedHint.ADD_COMPONENTS, true);

            assertThat(headingOnly).isPositive();
            assertThat(headingAndInstruction).isGreaterThan(headingOnly);
        } finally {
            g.dispose();
        }
    }

    @Test
    void runningFtpHarnessContextMenuExposesDetailsFilesDirectoryWarningAndStop() throws Exception {
        Project project = mock(Project.class);
        FlowElement ftp = TestFixtures.getFtpProducer(BASE_META_PACK);

        JPopupMenu menu = DesignCanvasContextMenu.createTestFtpServerMenu(project, ftp);

        assertThat(((JMenuItem) menu.getComponent(0)).getText()).isEqualTo(StudioBundle.message("menu.ShowTestFtpServerDetails"));
        assertThat(((JMenuItem) menu.getComponent(1)).getText()).isEqualTo(StudioBundle.message("menu.ShowTestFtpOverwriteLimitation"));
        assertThat(menu.getComponent(1).getForeground()).isNotNull();
        assertThat(((JMenuItem) menu.getComponent(2)).getText()).isEqualTo(StudioBundle.message("menu.OpenTestFtpFile"));
        assertThat(((JMenuItem) menu.getComponent(3)).getText()).isEqualTo(StudioBundle.message("menu.ShowTestFtpDirectory"));
        assertThat(((JMenuItem) menu.getComponent(5)).getText()).isEqualTo(StudioBundle.message("menu.StopTestFtpServer"));
    }


    @Test
    void localSftpEndpointMenuOffersFilesModeAndStop() {
        Project project = mock(Project.class);
        JPopupMenu menu = DesignCanvasContextMenu.createTestSftpServerMenu(project);
        try (MockedStatic<SftpHarnessAction> actions = Mockito.mockStatic(SftpHarnessAction.class)) {
            assertThat(((JMenuItem) menu.getComponent(0)).getText()).isEqualTo(StudioBundle.message("sftpHarness.files"));
            ((JMenuItem) menu.getComponent(0)).doClick();
            actions.verify(() -> SftpHarnessAction.showFiles(project));
            ((JMenuItem) menu.getComponent(1)).doClick();
            actions.verify(() -> SftpHarnessAction.choose(project));
            assertThat(((JMenuItem) menu.getComponent(3)).getText()).isEqualTo(StudioBundle.message("sftpHarness.stop"));
            ((JMenuItem) menu.getComponent(3)).doClick();
            actions.verify(() -> SftpHarnessAction.stop(project));
        }
    }

    @Test
    void localSftpEndpointsSeparateConsumersAndProducersAndExcludeFtp() throws Exception {
        FlowElement consumer = TestFixtures.getSftpConsumer(BASE_META_PACK);
        FlowElement producer = TestFixtures.getSftpProducer(BASE_META_PACK);
        Flow flow = mock(Flow.class);
        when(flow.getFlowElementsNoExternalEndPoints()).thenReturn(List.of(
                consumer, producer, TestFixtures.getFtpProducer(BASE_META_PACK)));
        Module module = mock(Module.class);
        when(module.getFlows()).thenReturn(List.of(flow));
        assertThat(DesignerCanvas.sftpMembers(module, true)).containsExactly(consumer);
        assertThat(DesignerCanvas.sftpMembers(module, false)).containsExactly(producer);
        assertThat(DesignerCanvas.sftpMembers(null, true)).isEmpty();
    }

    @Test
    void runningMailHarnessContextMenuExposesDetailsAndStop() throws Exception {
        Project project = mock(Project.class);
        FlowElement mail = FlowElement.flowElementBuilder()
                .componentMeta(ComponentLibrary
                        .getIkasanComponentByKeyMandatory(BASE_META_PACK, "Email Producer"))
                .componentName("mail")
                .build();

        JPopupMenu menu = DesignCanvasContextMenu.createTestMailServerMenu(project, mail);

        assertThat(((JMenuItem) menu.getComponent(0)).getText()).isEqualTo(StudioBundle.message("menu.ShowTestMailServerDetails"));
        assertThat(((JMenuItem) menu.getComponent(2)).getText()).isEqualTo(StudioBundle.message("menu.StopTestMailServer"));
    }


    @Test
    void everyValidFlowDropSurfaceResolvesToTheSameOwningRoute() throws Exception {
        Flow flow = TestFixtures.getUnbuiltFlow(BASE_META_PACK).build();
        FlowElement consumer = TestFixtures.getEventGeneratingConsumer(BASE_META_PACK);
        FlowElement component = TestFixtures.getMessageFilter(BASE_META_PACK);
        flow.setConsumer(consumer);
        flow.getFlowRoute().getFlowElements().add(component);
        consumer.setContainingFlow(flow);
        consumer.setContainingFlowRoute(flow.getFlowRoute());
        component.setContainingFlow(flow);
        component.setContainingFlowRoute(flow.getFlowRoute());

        DesignerCanvas.DropContext expected = new DesignerCanvas.DropContext(flow, flow.getFlowRoute());
        assertThat(DesignerCanvas.resolveDropContext(flow)).isEqualTo(expected);
        assertThat(DesignerCanvas.resolveDropContext(flow.getFlowRoute())).isEqualTo(expected);
        assertThat(DesignerCanvas.resolveDropContext(consumer)).isEqualTo(expected);
        assertThat(DesignerCanvas.resolveDropContext(component)).isEqualTo(expected);
        assertThat(DesignerCanvas.resolveDropContext(Module.getDumbModuleVersion())).isNull();
    }
}
