package org.ikasan.studio.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Guards the lifecycle choices most likely to regress into IDE freezes or editor leaks. */
class ThreadingLifecycleBoundaryTest {

    @SuppressWarnings("UseOptimizedEelFunctions")
    @Test
    void uiContextGettersNeverPerformHiddenFilesystemOrPsiWork() throws Exception {
        String source = Files.readString(Path.of("src/main/java/org/ikasan/studio/ui/UiContext.java"));
        assertThat(method(source, "public IkasanPomModel getIkasanPomModel()", "public void setIkasanPomModel"))
                .doesNotContain("StudioProjectFiles", "ReadAction", "PsiManager", "Files.");
        assertThat(method(source, "public Map<String, String> getApplicationProperties()", "public void setApplicationProperties"))
                .doesNotContain("StudioProjectFiles", "ReadAction", "PsiManager", "Files.");
    }

    @SuppressWarnings("UseOptimizedEelFunctions")
    @Test
    void pollingServicesUsePooledAlarmsAndExplicitlyCancelRequests() throws Exception {
        for (String file : new String[]{"FlowErrorMonitorService.java", "TestMailServerSessionService.java"}) {
            String source = Files.readString(Path.of("src/main/java/org/ikasan/studio/intellij/runtime", file));
            assertThat(source).as(file)
                    .contains("Alarm.ThreadToUse.POOLED_THREAD", "implements Disposable", "alarm.cancelAllRequests()")
                    .doesNotContain("Alarm.ThreadToUse.SWING_THREAD");
        }
    }

    @SuppressWarnings("UseOptimizedEelFunctions")
    @Test
    void editorOwnedTimersAndCallbacksHaveDisposalGuards() throws Exception {
        String canvas = Files.readString(Path.of(
                "src/main/java/org/ikasan/studio/ui/component/canvas/DesignerCanvas.java"));
        String panel = Files.readString(Path.of(
                "src/main/java/org/ikasan/studio/ui/component/canvas/CanvasPanel.java"));
        String designer = Files.readString(Path.of("src/main/java/org/ikasan/studio/ui/DesignerUI.java"));

        assertThat(canvas).contains("void disposeCanvas()", "flowErrorFlashTimer.stop()", "boolean isDisposed()");
        assertThat(panel).contains("harnessRefreshTimer.stop()", "designerCanvas.disposeCanvas()");
        assertThat(designer).contains("disposed || project.isDisposed()", "disposed = true;");
    }

    @SuppressWarnings("UseOptimizedEelFunctions")
    @Test
    void sharedCanvasRedrawBoundaryReturnsToEdtAndRejectsDisposedCanvas() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/ikasan/studio/intellij/project/StudioProjectFiles.java"));
        String redraw = method(source, "public static void causeRedraw(Project project)",
                "public static VirtualFile getProjectBaseDir");
        assertThat(redraw).contains("isDispatchThread()", "invokeLater(redraw)",
                "project.isDisposed()", "canvas.isDisposed()");
    }

    private static String method(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from);
        assertThat(from).as("method start %s", start).isNotNegative();
        assertThat(to).as("method end %s", end).isGreaterThan(from);
        return source.substring(from, to);
    }
}
