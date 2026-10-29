package org.ikasan.metapack;

import java.nio.file.Path;
import org.ikasan.studio.core.metapack.loading.MetaPackDirectoryProvider;

/** Test host: same directory SPI used by IntelliJ, without an IntelliJ dependency. */
public class ExternalPackDirectoryProvider implements MetaPackDirectoryProvider {
    @Override public Path directory() {
        return Path.of(System.getProperty("demo.pack.directory", "target/not-installed"));
    }
}
