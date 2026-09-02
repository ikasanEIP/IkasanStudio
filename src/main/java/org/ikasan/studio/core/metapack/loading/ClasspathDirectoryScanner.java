package org.ikasan.studio.core.metapack.loading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/** Discovers immediate child directories in exploded resources and plugin JARs. */
public final class ClasspathDirectoryScanner {
    private static final Logger LOG = LoggerFactory.getLogger(ClasspathDirectoryScanner.class);

    private ClasspathDirectoryScanner() {
    }

    /**
     * The JAR filesystem deliberately remains open for the IDE process lifetime. Closing it invalidates paths
     * retained by other plugin resource consumers.
     */
    @SuppressWarnings("resource")
    public static String[] getDirectories(String directory) throws URISyntaxException, IOException {
        URL url = ClasspathDirectoryScanner.class.getClassLoader().getResource(directory);
        if (url == null) {
            LOG.warn("STUDIO: Could not find classpath directory {}", directory);
            return new String[0];
        }

        URI uri = url.toURI();
        if ("jar".equals(uri.getScheme())) {
            FileSystem fileSystem;
            try {
                fileSystem = FileSystems.getFileSystem(uri);
            } catch (FileSystemNotFoundException ignored) {
                fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
            }
            Path path = fileSystem.getPath(directory);
            Set<String> directories = Files.walk(path, 1)
                    .filter(Files::isDirectory)
                    .map(Path::toString)
                    .filter(candidate -> !candidate.endsWith(directory))
                    .collect(Collectors.toSet());
            return directories.toArray(String[]::new);
        }

        String[] children = new File(uri).list((current, name) -> new File(current, name).isDirectory());
        if (children == null) {
            return new String[0];
        }
        return Arrays.stream(children).map(child -> directory + "/" + child).toArray(String[]::new);
    }
}
