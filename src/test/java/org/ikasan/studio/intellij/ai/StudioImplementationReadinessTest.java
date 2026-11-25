package org.ikasan.studio.intellij.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.ikasan.studio.core.ai.ImplementationReadiness;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.core.persistence.json.StudioJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudioImplementationReadinessTest {
    @Test
    void resolvesExplicitTargetEvenWhenComponentIsNamedFlowAndRejectsStaleFindings() {
        Module module = mock(Module.class);
        Flow flow = mock(Flow.class);
        FlowElement component = mock(FlowElement.class);
        when(module.getFlows()).thenReturn(List.of(flow));
        when(flow.getIdentity()).thenReturn("Example");
        when(component.getIdentity()).thenReturn("Flow");
        when(flow.ftlGetConsumerAndFlowElements()).thenReturn(List.of(component));
        assertSame(component, StudioImplementationReadiness.resolveTarget(module, finding(ImplementationReadiness.Target.COMPONENT, "Flow")));
        assertSame(flow, StudioImplementationReadiness.resolveTarget(module, finding(ImplementationReadiness.Target.FLOW, "Flow")));
        assertSame(module, StudioImplementationReadiness.resolveTarget(module, finding(ImplementationReadiness.Target.MODULE, "Module")));
        assertNull(StudioImplementationReadiness.resolveTarget(module, finding(ImplementationReadiness.Target.COMPONENT, "Removed")));
        assertNull(StudioImplementationReadiness.resolveTarget(null, finding(ImplementationReadiness.Target.COMPONENT, "Flow")));
    }

    @Test
    void savesProjectEditsBeforeRescanningAndLeavesOtherProjectsAlone(@TempDir Path root) throws Exception {
        FileDocumentManager manager = mock(FileDocumentManager.class);
        Document edited = mock(Document.class);
        Document other = mock(Document.class);
        VirtualFile file = mock(VirtualFile.class);
        VirtualFile otherFile = mock(VirtualFile.class);
        Path source = root.resolve("user/src/main/java/org/example/demo/Producer.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "// TODO implement producer");
        JsonNode model = StudioJson.newObjectMapper().readTree("""
                {"applicationPackageName":"org.example","flows":[{"name":"Demo","flowElements":[
                {"componentName":"Output","userImplementedClassName":"Producer"}]}]}
                """);
        assertFalse(ImplementationReadiness.scan(root, model).findings().isEmpty());
        when(manager.getUnsavedDocuments()).thenReturn(new Document[]{edited, other});
        when(manager.getFile(edited)).thenReturn(file);
        when(manager.getFile(other)).thenReturn(otherFile);
        when(file.isInLocalFileSystem()).thenReturn(true);
        when(otherFile.isInLocalFileSystem()).thenReturn(true);
        when(file.getPath()).thenReturn(source.toString());
        when(otherFile.getPath()).thenReturn(root + "-other/Other.java");
        doAnswer(invocation -> { Files.writeString(source, "class Producer {}"); return null; })
                .when(manager).saveDocument(edited);
        StudioImplementationReadiness.saveProjectDocuments(manager, root);
        assertTrue(ImplementationReadiness.scan(root, model).findings().isEmpty());
        verify(manager, never()).saveDocument(other);
    }

    @Test
    void refusesToContinueWhenDocumentCannotBeSaved() {
        FileDocumentManager manager = mock(FileDocumentManager.class);
        Document document = mock(Document.class);
        VirtualFile file = mock(VirtualFile.class);
        when(manager.getUnsavedDocuments()).thenReturn(new Document[]{document});
        when(manager.getFile(document)).thenReturn(file);
        when(file.isInLocalFileSystem()).thenReturn(true);
        when(file.getPath()).thenReturn("/project/user/Example.java");
        when(manager.isDocumentUnsaved(document)).thenReturn(true);
        assertThrows(IllegalStateException.class, () ->
                StudioImplementationReadiness.saveProjectDocuments(manager, Path.of("/project")));
    }

    private ImplementationReadiness.Finding finding(ImplementationReadiness.Target target, String component) {
        return new ImplementationReadiness.Finding("Example", component, "generated/src/main/model/model.json", 1,
                "INVALID_PROPERTY", "Invalid recipient", target, "toRecipient", "bad.@example.com");
    }
}
