package org.ikasan.studio.intellij.project;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StudioImportedRootsTest {
    private VirtualFile root(String path) {
        VirtualFile root = mock(VirtualFile.class);
        when(root.isValid()).thenReturn(true);
        when(root.getPath()).thenReturn(path);
        when(root.toString()).thenReturn("Decorated VirtualFile[" + path + "]");
        return root;
    }

    @Test void usesVirtualFilePathsAndIgnoresAnotherProjectsGeneratedDirectory() {
        VirtualFile unrelated = root("/other/generated");
        VirtualFile generated = root("/example/generated");
        assertThat(StudioProjectFiles.findGeneratedContentRoot("/example", new VirtualFile[]{unrelated, generated}))
                .isSameAs(generated);
        when(generated.isValid()).thenReturn(false);
        assertThat(StudioProjectFiles.findGeneratedContentRoot("/example", new VirtualFile[]{unrelated, generated})).isNull();
    }

    @Test void acceptsSplitContentRootsBelowTheGeneratedModule() {
        VirtualFile source = root("/example/generated/src/main");
        VirtualFile generated = root("/example/generated");
        VirtualFileSystem fs = mock(VirtualFileSystem.class);
        when(source.getFileSystem()).thenReturn(fs);
        when(fs.findFileByPath("/example/generated")).thenReturn(generated);
        assertThat(StudioProjectFiles.findGeneratedContentRoot("/example", new VirtualFile[]{source})).isSameAs(generated);
    }

    @Test void aProjectRootAloneDoesNotPretendThatMavenHasImportedTheGeneratedModule() {
        assertThat(StudioProjectFiles.findGeneratedContentRoot("/example",
                new VirtualFile[]{root("/example"), root("/example/generated-other")})).isNull();
    }
}
