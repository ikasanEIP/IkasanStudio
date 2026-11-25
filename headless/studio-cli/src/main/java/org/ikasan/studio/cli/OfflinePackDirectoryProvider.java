package org.ikasan.studio.cli;

import java.net.URISyntaxException;
import java.nio.file.Path;
import org.ikasan.studio.core.metapack.loading.MetaPackDirectoryProvider;
import org.ikasan.studio.core.metapack.loading.OfflinePackBundle;

/** Uses the exported distribution, never an IntelliJ installation or user-configured pack store. */
public final class OfflinePackDirectoryProvider implements MetaPackDirectoryProvider {
    static Path toolsHome() {
        Path location = location();
        return location.toString().endsWith(".jar") && location.getParent().getFileName().toString().equals("lib")
                ? location.getParent().getParent() : null;
    }
    private static Path location() {
        try { return Path.of(OfflinePackDirectoryProvider.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath(); }
        catch (URISyntaxException e) { throw new IllegalStateException("Cannot locate the offline tools distribution", e); }
    }
    @Override public Path directory() {
        Path home = toolsHome();
        return (home == null ? location() : home).resolve(OfflinePackBundle.DIRECTORY);
    }
    @Override public boolean activatePendingChanges() { return false; }
}
