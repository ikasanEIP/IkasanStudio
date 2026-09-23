package org.ikasan.studio.intellij.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class OfflineToolsExporterTest {
    @TempDir Path parent;
    @Test void extractsBundledToolsAndPreservesExecutableLauncher() throws Exception {
        try(var archive=getClass().getResourceAsStream(OfflineToolsExporter.RESOURCE)) {
            assertNotNull(archive);
            Path exported=OfflineToolsExporter.export(archive,parent,()->{});
            assertTrue(Files.isRegularFile(exported.resolve("README.md")));
            assertTrue(Files.isRegularFile(exported.resolve("bin/studio_upgrade.py")));
            assertTrue(Files.isRegularFile(exported.resolve("bin/studio-cli.bat")));
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix"))
                assertTrue(Files.isExecutable(exported.resolve("bin/studio-cli")));
            String javaExecutable=Path.of(System.getProperty("java.home"), "bin",
                    System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
            Path output=parent.resolve("cli-help.txt");
            Process cli=new ProcessBuilder(javaExecutable, "-cp", exported.resolve("lib/*").toString(),
                    "org.ikasan.studio.cli.StudioMigrationCli", "--help")
                    .redirectErrorStream(true).redirectOutput(output.toFile()).start();
            try {
                assertTrue(cli.waitFor(30, java.util.concurrent.TimeUnit.SECONDS), "Standalone CLI timed out");
                assertEquals(0,cli.exitValue(),Files.readString(output));
                assertTrue(Files.readString(output).contains("preview --project"));
            } finally { if(cli.isAlive())cli.destroyForcibly(); }
            try(var libs=Files.list(exported.resolve("lib"))) {
                assertTrue(libs.anyMatch(p->p.getFileName().toString().startsWith("studio-generator-")));
            }
        }
    }
    @Test void refusesExistingDirectoryWithoutChangingItsFiles() throws Exception {
        Path existing=Files.createDirectory(parent.resolve(OfflineToolsExporter.DIRECTORY));
        Files.writeString(existing.resolve("keep.txt"),"keep");
        assertThrows(FileAlreadyExistsException.class,()->OfflineToolsExporter.export(new ByteArrayInputStream(new byte[0]),parent,()->{}));
        assertEquals("keep",Files.readString(existing.resolve("keep.txt")));
    }
    @Test void rejectsTraversalAndCleansPartialExtraction() throws Exception {
        try(var archive=archive("studio-cli-test/bin/ok", "studio-cli-test/../../outside")) {
            assertThrows(IOException.class,()->OfflineToolsExporter.export(archive,parent,()->{}));
        }
        try(var files=Files.list(parent)) { assertEquals(0,files.count()); }
    }
    @Test void cancellationCleansStagingAndDoesNotPublishDestination() throws Exception {
        try(var archive=archive("studio-cli-test/bin/ok")) {
            assertThrows(IllegalStateException.class,()->OfflineToolsExporter.export(archive,parent,()->{throw new IllegalStateException("cancelled");}));
        }
        try(var files=Files.list(parent)) { assertEquals(0,files.count()); }
    }
    @Test void rejectsIncompleteArchive() throws Exception {
        try(var archive=archive("studio-cli-test/bin/ok")) {
            assertThrows(IOException.class,()->OfflineToolsExporter.export(archive,parent,()->{}));
        }
        assertFalse(Files.exists(parent.resolve(OfflineToolsExporter.DIRECTORY)));
    }
    private InputStream archive(String... names) throws IOException {
        var buffer=new ByteArrayOutputStream();
        try(var zip=new ZipOutputStream(buffer)) {
            for(String name:names){zip.putNextEntry(new ZipEntry(name));zip.write("test".getBytes());zip.closeEntry();}
        }
        return new ByteArrayInputStream(buffer.toByteArray());
    }
}
