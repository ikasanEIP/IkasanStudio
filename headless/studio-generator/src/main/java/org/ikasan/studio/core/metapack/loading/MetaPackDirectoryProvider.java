package org.ikasan.studio.core.metapack.loading;

import java.nio.file.Path;

/** Host-provided installation directory. Providers must not initialize the component library. */
public interface MetaPackDirectoryProvider {
    Path directory();

    /** Portable hosts must never activate IDE management operations. */
    default boolean activatePendingChanges() { return true; }
}
