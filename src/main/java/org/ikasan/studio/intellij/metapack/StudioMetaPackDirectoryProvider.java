package org.ikasan.studio.intellij.metapack;

import com.intellij.openapi.application.PathManager;
import org.ikasan.studio.core.metapack.loading.MetaPackDirectoryProvider;
import java.nio.file.Path;

/** Keeps user-installed resources outside the plugin distribution so plugin updates preserve them. */
public final class StudioMetaPackDirectoryProvider implements MetaPackDirectoryProvider {
    @Override public Path directory() {
        return Path.of(PathManager.getConfigPath(), "ikasan-studio", "meta-packs");
    }
}
