package org.ikasan.studio.core.metapack.loading;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Collections;
import java.util.TreeSet;

/** Discovers immediate resource directories across engine, pack and extension JARs. */
public final class ClasspathDirectoryScanner {
    private ClasspathDirectoryScanner() { }

    public static String[] getDirectories(String directory) throws URISyntaxException, IOException {
        return getDirectories(directory, ClasspathDirectoryScanner.class.getClassLoader());
    }

    /** Explicit loader also allows pack tooling to inspect isolated classpaths. */
    public static String[] getDirectories(String directory, ClassLoader loader) throws URISyntaxException, IOException {
        var directories = new TreeSet<String>();
        for (var url : Collections.list(loader.getResources(directory))) {
            var uri = url.toURI();
            Path root;
            if ("jar".equals(uri.getScheme())) {
                FileSystem fileSystem;
                try {
                    fileSystem = FileSystems.getFileSystem(uri);
                } catch (FileSystemNotFoundException missing) {
                    try {
                        fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                    } catch (FileSystemAlreadyExistsException concurrentOpen) {
                        fileSystem = FileSystems.getFileSystem(uri);
                    }
                }
                // Keep the shared JAR filesystem open; closing it invalidates other resource consumers.
                root = fileSystem.getPath(directory);
            } else {
                root = Path.of(uri);
            }
            try (var children = Files.list(root)) {
                children.filter(Files::isDirectory)
                        .forEach(child -> directories.add(directory + "/" + child.getFileName()));
            }
        }
        return directories.toArray(String[]::new);
    }
}
