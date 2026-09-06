package org.ikasan.studio.ui.actions;

import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.command.undo.UndoableAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowUserImplementedElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.intellij.settings.IkasanStudioSettings;
import org.ikasan.studio.ui.UiContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.FROM_TYPE;
import static org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.TO_TYPE;
import static org.ikasan.studio.core.metapack.model.ComponentPropertyMeta.USER_IMPLEMENTED_CLASS_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Behavioural coverage for DeleteComponentAction's full actionPerformed flow, including the confirmation
 * dialog, undo registration and canvas/palette reset - not just the pure enum-mapping helper. IntelliJ's
 * CommandProcessor/UndoManager/MessageDialogBuilder are mocked statically (mockStatic, matching the existing
 * precedent in FlowElementTest.java) since production code calls them directly and there is otherwise no
 * running IDE application in a plain unit test to exercise them for real.
 * <p>
 * {@code MessageDialogBuilder<T extends MessageDialogBuilder<T>>} is self-bounded generic, and a wildcarded
 * class literal ({@code MessageDialogBuilder<?>.class}) isn't valid Java syntax - {@code MessageDialogBuilder
 * .class} is unavoidably raw, hence the class-wide suppression (matching the same accepted convention as e.g.
 * CanvasPanel's own {@code @SuppressWarnings("rawtypes")} for JBPanel).
 */
@SuppressWarnings("rawtypes")
class DeleteComponentActionTest {
    @Test
    void mapsEveryDialogExitToAnExplicitDeletionChoice() {
        assertThat(DeleteComponentAction.userCodeDeletionChoice(Messages.YES))
                .isEqualTo(DeleteComponentAction.UserCodeDeletionChoice.DELETE_COMPONENT_AND_CLASS);
        assertThat(DeleteComponentAction.userCodeDeletionChoice(Messages.NO))
                .isEqualTo(DeleteComponentAction.UserCodeDeletionChoice.DELETE_COMPONENT_ONLY);
        assertThat(DeleteComponentAction.userCodeDeletionChoice(Messages.CANCEL))
                .isEqualTo(DeleteComponentAction.UserCodeDeletionChoice.CANCEL);
        assertThat(DeleteComponentAction.userCodeDeletionChoice(-1))
                .isEqualTo(DeleteComponentAction.UserCodeDeletionChoice.CANCEL);
    }

    @Test
    void deletesComponentButRetainsItsUserClassWhenTheUserDeclines() throws Exception {
        FlowElement converter = customConverter("myConverter");
        Flow flow = flowWithConverters(converter);
        Module module = buildModule(flow);
        Project project = mock(Project.class);
        UiContext uiContext = stubUiContext(project, module);

        try (MockedStatic<CommandProcessor> commandProcessorStatic = mockStatic(CommandProcessor.class);
             MockedStatic<UndoManager> undoManagerStatic = mockStatic(UndoManager.class);
             MockedStatic<StudioProjectFiles> studioProjectFilesStatic = mockStatic(StudioProjectFiles.class);
             MockedStatic<IkasanStudioSettings> settingsStatic = mockStatic(IkasanStudioSettings.class);
             MockedStatic<MessageDialogBuilder> dialogStatic = mockStatic(MessageDialogBuilder.class)) {
            runCommandsSynchronously(commandProcessorStatic);
            stubUndoManager(undoManagerStatic, project);
            settingsStatic.when(IkasanStudioSettings::isPromptBeforeDeletingUserCode).thenReturn(true);
            studioProjectFilesStatic.when(() -> StudioProjectFiles.resolveUserImplementedClassPath(any(), any()))
                    .thenReturn("/generated/myConverter.java");
            stubUserCodeDialog(dialogStatic, project, Messages.NO);

            new DeleteComponentAction(project, converter).actionPerformed(null);

            assertThat(flow.getFlowRoute().getFlowElements()).doesNotContain(converter);
            studioProjectFilesStatic.verify(() -> StudioProjectFiles.deleteUserImplementedClassFile(any(), any()), never());
            verify(uiContext).resetSelectionAfterDeletion();
        }
    }

    @Test
    void deletesComponentAndConfirmsDeletionOfAnUnusedUserClass() throws Exception {
        FlowElement converter = customConverter("myConverter");
        Flow flow = flowWithConverters(converter);
        Module module = buildModule(flow);
        Project project = mock(Project.class);
        UiContext uiContext = stubUiContext(project, module);

        try (MockedStatic<CommandProcessor> commandProcessorStatic = mockStatic(CommandProcessor.class);
             MockedStatic<UndoManager> undoManagerStatic = mockStatic(UndoManager.class);
             MockedStatic<StudioProjectFiles> studioProjectFilesStatic = mockStatic(StudioProjectFiles.class);
             MockedStatic<IkasanStudioSettings> settingsStatic = mockStatic(IkasanStudioSettings.class);
             MockedStatic<MessageDialogBuilder> dialogStatic = mockStatic(MessageDialogBuilder.class)) {
            runCommandsSynchronously(commandProcessorStatic);
            stubUndoManager(undoManagerStatic, project);
            settingsStatic.when(IkasanStudioSettings::isPromptBeforeDeletingUserCode).thenReturn(true);
            studioProjectFilesStatic.when(() -> StudioProjectFiles.resolveUserImplementedClassPath(any(), any()))
                    .thenReturn("/generated/myConverter.java");
            stubUserCodeDialog(dialogStatic, project, Messages.YES);

            new DeleteComponentAction(project, converter).actionPerformed(null);

            assertThat(flow.getFlowRoute().getFlowElements()).doesNotContain(converter);
            studioProjectFilesStatic.verify(() -> StudioProjectFiles.deleteUserImplementedClassFile(
                    eq(project), eq(new org.ikasan.studio.core.model.command.UserClassReference(
                            module.getApplicationPackageName() + "." + flow.getJavaPackageName(), "myConverter"))));
            verify(uiContext).resetSelectionAfterDeletion();
        }
    }

    @Test
    void cancellingLeavesTheFlowCompletelyUnchanged() throws Exception {
        FlowElement converter = customConverter("myConverter");
        Flow flow = flowWithConverters(converter);
        Module module = buildModule(flow);
        Project project = mock(Project.class);
        UiContext uiContext = stubUiContext(project, module);

        try (MockedStatic<StudioProjectFiles> studioProjectFilesStatic = mockStatic(StudioProjectFiles.class);
             MockedStatic<IkasanStudioSettings> settingsStatic = mockStatic(IkasanStudioSettings.class);
             MockedStatic<MessageDialogBuilder> dialogStatic = mockStatic(MessageDialogBuilder.class)) {
            settingsStatic.when(IkasanStudioSettings::isPromptBeforeDeletingUserCode).thenReturn(true);
            studioProjectFilesStatic.when(() -> StudioProjectFiles.resolveUserImplementedClassPath(any(), any()))
                    .thenReturn("/generated/myConverter.java");
            stubUserCodeDialog(dialogStatic, project, Messages.CANCEL);

            new DeleteComponentAction(project, converter).actionPerformed(null);

            assertThat(flow.getFlowRoute().getFlowElements()).contains(converter);
            studioProjectFilesStatic.verify(() -> StudioProjectFiles.deleteUserImplementedClassFile(any(), any()), never());
            verify(uiContext, never()).resetSelectionAfterDeletion();
        }
    }

    @Test
    void sharedUserClassOnAComponentThatWasNotRemovedRemainsUntouched() throws Exception {
        FlowElement converterToRemove = customConverter("myConverter");
        FlowElement converterToKeep = customConverter("myOtherConverter");
        converterToKeep.setComponentName("My Other Converter");
        converterToKeep.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, "myOtherConverter");
        Flow flow = flowWithConverters(converterToRemove, converterToKeep);
        Module module = buildModule(flow);
        Project project = mock(Project.class);
        stubUiContext(project, module);

        try (MockedStatic<CommandProcessor> commandProcessorStatic = mockStatic(CommandProcessor.class);
             MockedStatic<UndoManager> undoManagerStatic = mockStatic(UndoManager.class);
             MockedStatic<StudioProjectFiles> studioProjectFilesStatic = mockStatic(StudioProjectFiles.class);
             MockedStatic<IkasanStudioSettings> settingsStatic = mockStatic(IkasanStudioSettings.class);
             MockedStatic<MessageDialogBuilder> dialogStatic = mockStatic(MessageDialogBuilder.class)) {
            runCommandsSynchronously(commandProcessorStatic);
            stubUndoManager(undoManagerStatic, project);
            settingsStatic.when(IkasanStudioSettings::isPromptBeforeDeletingUserCode).thenReturn(true);
            studioProjectFilesStatic.when(() -> StudioProjectFiles.resolveUserImplementedClassPath(any(), any()))
                    .thenReturn("/generated/myConverter.java");
            stubUserCodeDialog(dialogStatic, project, Messages.YES);

            new DeleteComponentAction(project, converterToRemove).actionPerformed(null);

            assertThat(flow.getFlowRoute().getFlowElements())
                    .doesNotContain(converterToRemove)
                    .contains(converterToKeep);
            studioProjectFilesStatic.verify(() -> StudioProjectFiles.deleteUserImplementedClassFile(
                    eq(project), eq(new org.ikasan.studio.core.model.command.UserClassReference(
                            module.getApplicationPackageName() + "." + flow.getJavaPackageName(), "myConverter"))));
            studioProjectFilesStatic.verify(() -> StudioProjectFiles.deleteUserImplementedClassFile(
                    eq(project), eq(new org.ikasan.studio.core.model.command.UserClassReference(
                            module.getApplicationPackageName() + "." + flow.getJavaPackageName(), "myOtherConverter"))), never());
        }
    }

    @Test
    void missingUserClassFileDoesNotFailTheComponentDeletion() throws Exception {
        FlowElement converter = customConverter("myConverter");
        Flow flow = flowWithConverters(converter);
        Module module = buildModule(flow);
        Project project = mock(Project.class);
        UiContext uiContext = stubUiContext(project, module);

        try (MockedStatic<CommandProcessor> commandProcessorStatic = mockStatic(CommandProcessor.class);
             MockedStatic<UndoManager> undoManagerStatic = mockStatic(UndoManager.class);
             MockedStatic<StudioProjectFiles> studioProjectFilesStatic = mockStatic(StudioProjectFiles.class)) {
            runCommandsSynchronously(commandProcessorStatic);
            stubUndoManager(undoManagerStatic, project);
            // No file on disk for this class - resolveUserImplementedClassPath (unstubbed) returns null, so
            // chooseUserCodeDeletion never even shows a dialog (no MessageDialogBuilder mock needed here).

            assertThatCode(() -> new DeleteComponentAction(project, converter).actionPerformed(null))
                    .doesNotThrowAnyException();

            assertThat(flow.getFlowRoute().getFlowElements()).doesNotContain(converter);
            studioProjectFilesStatic.verify(() -> StudioProjectFiles.deleteUserImplementedClassFile(any(), any()), never());
            verify(uiContext).resetSelectionAfterDeletion();
        }
    }

    @Test
    void undoRestoresTheRemovedComponentToTheFlowModel() throws Exception {
        // This covers the model-restoration half of "undo restores both model and source-file state". The
        // source-file half is IntelliJ's own VFS/Local-History undo tracking for the WriteCommandAction the
        // file deletion runs inside (see StudioProjectFiles#deleteFile) - not logic this class implements, and
        // not something a mocked unit test (no real VFS/undo engine present) can prove either way. Confirming
        // that end-to-end would need a @Tag("harness") test against a real IDE instance.
        FlowElement converter = customConverter("myConverter");
        Flow flow = flowWithConverters(converter);
        Module module = buildModule(flow);
        Project project = mock(Project.class);
        stubUiContext(project, module);

        try (MockedStatic<CommandProcessor> commandProcessorStatic = mockStatic(CommandProcessor.class);
             MockedStatic<UndoManager> undoManagerStatic = mockStatic(UndoManager.class);
             MockedStatic<StudioProjectFiles> studioProjectFilesStatic = mockStatic(StudioProjectFiles.class);
             MockedStatic<IkasanStudioSettings> settingsStatic = mockStatic(IkasanStudioSettings.class);
             MockedStatic<MessageDialogBuilder> dialogStatic = mockStatic(MessageDialogBuilder.class)) {
            runCommandsSynchronously(commandProcessorStatic);
            UndoManager undoManager = stubUndoManager(undoManagerStatic, project);
            settingsStatic.when(IkasanStudioSettings::isPromptBeforeDeletingUserCode).thenReturn(true);
            studioProjectFilesStatic.when(() -> StudioProjectFiles.resolveUserImplementedClassPath(any(), any()))
                    .thenReturn("/generated/myConverter.java");
            stubUserCodeDialog(dialogStatic, project, Messages.YES);

            new DeleteComponentAction(project, converter).actionPerformed(null);
            assertThat(flow.getFlowRoute().getFlowElements()).doesNotContain(converter);

            ArgumentCaptor<UndoableAction> undoableActionCaptor = ArgumentCaptor.forClass(UndoableAction.class);
            verify(undoManager).undoableActionPerformed(undoableActionCaptor.capture());
            UndoableAction registeredAction = undoableActionCaptor.getValue();

            registeredAction.undo();
            assertThat(flow.getFlowRoute().getFlowElements()).contains(converter);

            registeredAction.redo();
            assertThat(flow.getFlowRoute().getFlowElements()).doesNotContain(converter);
        }
    }

    @Test
    void propertiesSelectionReturnsToThePaletteAfterDeletion() throws Exception {
        FlowElement converter = customConverter("myConverter");
        Flow flow = flowWithConverters(converter);
        Module module = buildModule(flow);
        Project project = mock(Project.class);
        UiContext uiContext = stubUiContext(project, module);

        try (MockedStatic<CommandProcessor> commandProcessorStatic = mockStatic(CommandProcessor.class);
             MockedStatic<UndoManager> undoManagerStatic = mockStatic(UndoManager.class);
             MockedStatic<StudioProjectFiles> studioProjectFilesStatic = mockStatic(StudioProjectFiles.class);
             MockedStatic<IkasanStudioSettings> settingsStatic = mockStatic(IkasanStudioSettings.class);
             MockedStatic<MessageDialogBuilder> dialogStatic = mockStatic(MessageDialogBuilder.class)) {
            runCommandsSynchronously(commandProcessorStatic);
            stubUndoManager(undoManagerStatic, project);
            settingsStatic.when(IkasanStudioSettings::isPromptBeforeDeletingUserCode).thenReturn(true);
            studioProjectFilesStatic.when(() -> StudioProjectFiles.resolveUserImplementedClassPath(any(), any()))
                    .thenReturn("/generated/myConverter.java");
            stubUserCodeDialog(dialogStatic, project, Messages.YES);

            new DeleteComponentAction(project, converter).actionPerformed(null);

            verify(uiContext, times(1)).resetSelectionAfterDeletion();
        }
    }

    // ---- fixtures & mocking helpers ----

    /**
     * TestFixtures.getCustomConverter builds a plain FlowElement via FlowElement#flowElementBuilder, not a
     * FlowUserImplementedElement - fine for tests unrelated to generated-class handling, but wrong here: the
     * production code being tested (UserClassReference#forElement) specifically requires the
     * FlowUserImplementedElement subtype (see FlowElementFactory, which creates one whenever
     * ComponentMeta#isGeneratesUserImplementedClass() is true, e.g. for a real Converter). This builds one
     * directly via FlowUserImplementedElement's own builder instead.
     */
    private static FlowElement customConverter(String className) throws Exception {
        ComponentMeta meta = ComponentLibrary.getIkasanComponentByKeyMandatory(BASE_META_PACK, "Converter");
        FlowElement flowElement = FlowUserImplementedElement.flowUserImplementedElementBuilder()
                .componentMeta(meta)
                .componentName("My Custom Converter")
                .build();
        flowElement.setPropertyValue(FROM_TYPE, "java.lang.String");
        flowElement.setPropertyValue(TO_TYPE, "java.lang.Integer");
        flowElement.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, className);
        flowElement.defaultUnsetMandatoryProperties();
        return flowElement;
    }

    private static Flow flowWithConverters(FlowElement... converters) throws Exception {
        Flow flow = new Flow(BASE_META_PACK);
        FlowElement consumer = TestFixtures.getGenericConsumer(BASE_META_PACK);
        consumer.setContainingFlow(flow);
        consumer.setContainingFlowRoute(flow.getFlowRoute());
        flow.setConsumer(consumer);
        for (FlowElement converter : converters) {
            converter.setContainingFlow(flow);
            converter.setContainingFlowRoute(flow.getFlowRoute());
            flow.getFlowRoute().getFlowElements().add(converter);
        }
        return flow;
    }

    private static Module buildModule(Flow flow) throws Exception {
        return TestFixtures.getMyFirstModuleIkasanModule(BASE_META_PACK, List.of(flow));
    }

    private static UiContext stubUiContext(Project project, Module module) {
        UiContext uiContext = mock(UiContext.class);
        when(uiContext.getIkasanModule()).thenReturn(module);
        when(project.getService(UiContext.class)).thenReturn(uiContext);
        return uiContext;
    }

    private static void runCommandsSynchronously(MockedStatic<CommandProcessor> commandProcessorStatic) {
        CommandProcessor commandProcessor = mock(CommandProcessor.class);
        commandProcessorStatic.when(CommandProcessor::getInstance).thenReturn(commandProcessor);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return null;
        }).when(commandProcessor).executeCommand(any(), any(Runnable.class), any(), any());
    }

    private static UndoManager stubUndoManager(MockedStatic<UndoManager> undoManagerStatic, Project project) {
        UndoManager undoManager = mock(UndoManager.class);
        undoManagerStatic.when(() -> UndoManager.getInstance(project)).thenReturn(undoManager);
        return undoManager;
    }

    private static void stubUserCodeDialog(MockedStatic<MessageDialogBuilder> dialogStatic, Project project, int result) {
        MessageDialogBuilder.YesNoCancel yesNoCancel = mock(MessageDialogBuilder.YesNoCancel.class, RETURNS_SELF);
        when(yesNoCancel.show(project)).thenReturn(result);
        dialogStatic.when(() -> MessageDialogBuilder.yesNoCancel(any(), any())).thenReturn(yesNoCancel);
    }
}
