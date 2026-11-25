package org.ikasan.metapack;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import org.ikasan.studio.core.metapack.loading.MetaPackArchive;
import org.ikasan.studio.core.metapack.loading.MetaPackInstaller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class PackArchiveIT {
    @TempDir Path temporary;

    @Test void installAndRestartDiscoverTheExternalPackWithoutBundledResources() throws Exception {
        Path installed = temporary.resolve("installed");
        Files.createDirectories(installed);
        Path jar = Path.of("target/ikasan5-metapack-1.0.4.jar").toAbsolutePath();
        probe(installed, "install", jar.toString());
        probe(installed, "discover", Path.of("target/demo-render").toAbsolutePath().toString());
    }

    private void probe(Path directory, String mode, String argument) throws Exception {
        String classpath = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .filter(entry -> !Path.of(entry).toAbsolutePath().normalize().equals(Path.of("target/classes").toAbsolutePath().normalize()))
                .filter(entry -> !Path.of(entry).toAbsolutePath().normalize().equals(Path.of("target/ikasan5-metapack-1.0.4.jar").toAbsolutePath().normalize()))
                .collect(Collectors.joining(File.pathSeparator));
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Ddemo.pack.directory=" + directory.toAbsolutePath(), "-cp", classpath,
                ExternalPackProbe.class.getName(), mode, argument).redirectErrorStream(true)
                .redirectOutput(Path.of("target/probe-" + mode + ".log").toFile()).start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly(); fail("External pack probe timed out: " + mode);
        }
        assertEquals(0, process.exitValue(), Files.readString(Path.of("target/probe-" + mode + ".log")));
    }

    @Test void packagedArtifactIsAcceptedByTheStudioInstaller() throws Exception {
        Path jar = Path.of("target/ikasan5-metapack-1.0.4.jar");
        assertEquals("ExampleIkasan5", MetaPackInstaller.validate(MetaPackArchive.open(jar)).id());
        try (JarFile archive = new JarFile(jar.toFile())) {
            assertFalse(archive.stream().anyMatch(entry -> entry.getName().endsWith(".class")));
            assertNotNull(archive.getEntry("studio/metapack/ExampleIkasan5/templates/org/ikasan/studio/generator/applicationTemplate_en.ftl"));
        }
    }
}
