package org.ikasan.studio.intellij.editor;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/** Supplies the Studio virtual file with the same squid icon used by the tool-window launcher. */
final class IkasanStudioFileType implements FileType {
    static final IkasanStudioFileType INSTANCE = new IkasanStudioFileType();
    private static final Icon ICON = IconLoader.getIcon(
            "/studio/icons/squid13x13.svg", IkasanStudioFileType.class);

    private IkasanStudioFileType() {
    }

    @Override
    public @NotNull @NonNls String getName() {
        return "IkasanStudioDesigner";
    }

    @Override
    public @NotNull @Nls String getDescription() {
        return "Ikasan Studio designer";
    }

    @Override
    public @NotNull @NonNls String getDefaultExtension() {
        return "";
    }

    @Override
    public Icon getIcon() {
        return ICON;
    }

    @Override
    public boolean isBinary() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }
}
