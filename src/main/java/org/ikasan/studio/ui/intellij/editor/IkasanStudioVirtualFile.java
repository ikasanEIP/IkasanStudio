package org.ikasan.studio.ui.intellij.editor;

import com.intellij.testFramework.LightVirtualFile;

final class IkasanStudioVirtualFile extends LightVirtualFile {
    IkasanStudioVirtualFile() {
        super(IkasanStudioEditorService.EDITOR_NAME, IkasanStudioFileType.INSTANCE, "");
        setWritable(false);
    }
}
