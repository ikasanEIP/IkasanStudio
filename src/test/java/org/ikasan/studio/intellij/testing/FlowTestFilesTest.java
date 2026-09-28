package org.ikasan.studio.intellij.testing;

import org.ikasan.studio.core.generator.FlowTestScaffold;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FlowTestFilesTest {
    @TempDir Path root;
    private FlowTestScaffold.Scaffold scaffold() {
        return new FlowTestScaffold.Scaffold("updated", "user-flow-tests/src/test/java/ExampleTest.java",
                Map.of("user-flow-tests/src/test/java/ExampleTest.java", "test", "user-flow-tests/pom.xml", "generated pom"));
    }
    @Test void fixtureResourcesAreCreatedOnlyWhenMissingIncludingOnRegeneration() throws Exception {
        Files.writeString(root.resolve("pom.xml"), "original");
        String directory = "user-flow-tests/src/test/resources/flow4/my_local_consumer/";
        Path first = root.resolve(directory + "first.txt");
        Files.createDirectories(first.getParent());
        Files.writeString(first, "developer fixture");
        var files = new java.util.LinkedHashMap<>(scaffold().files());
        files.put(directory + "first.txt", "first expected payload");
        files.put(directory + "second.txt", "second expected payload");
        var plan = new FlowTestScaffold.Scaffold("updated", scaffold().testPath(), files);
        FlowTestFiles.write(root, "original", plan);
        assertEquals("developer fixture", Files.readString(first));
        Path second = root.resolve(directory + "second.txt");
        assertEquals("second expected payload", Files.readString(second));
        Files.writeString(second, "custom second fixture");
        var existing = assertThrows(FlowTestFiles.ExistingTestException.class,
                () -> FlowTestFiles.write(root, "updated", plan));
        FlowTestFiles.archiveAndWrite(root, "updated", plan, existing);
        assertEquals("developer fixture", Files.readString(first));
        assertEquals("custom second fixture", Files.readString(second));
    }

    @Test void missingSampleInputDefaultsAreBackedUpAndExplicitFalseIsPreserved() throws Exception {
        Path file = root.resolve(FlowTestScaffold.TEST_PROPERTIES_PATH);
        Files.createDirectories(file.getParent());
        String original = "custom=keep\nstudio.sample-consumer.example.A.fixture-input-enabled=false\n";
        Files.writeString(file, original);
        String generated = """
                studio.sample-consumer.example.A.fixture-input-enabled=true
                studio.sample-consumer.example.B.fixture-input-enabled=true
                other=ignored
                """;
        FlowTestFiles.addMissingFixtureInputDefaults(root, generated);
        String updated = Files.readString(file);
        assertTrue(updated.startsWith(original));
        assertTrue(updated.contains("studio.sample-consumer.example.B.fixture-input-enabled=true"));
        assertFalse(updated.contains("other=ignored"));
        assertFalse(updated.contains("example.A.fixture-input-enabled=true"));
        FlowTestFiles.addMissingFixtureInputDefaults(root, generated);
        assertEquals(updated, Files.readString(file));
        try (var paths = Files.list(file.getParent())) {
            var backups = paths.filter(p -> p.getFileName().toString().contains(".bak")).toList();
            assertEquals(1, backups.size());
            assertEquals(original, Files.readString(backups.get(0)));
        }
    }

    @Test void sftpDependenciesUsePackVersionsAndPreserveExistingDeclarations() throws Exception {
        String existing = "<project><modelVersion>4.0.0</modelVersion><dependencies>"
                + "<dependency><groupId>org.apache.sshd</groupId><artifactId>sshd-core</artifactId><version>custom</version></dependency>"
                + "</dependencies></project>";
        StringBuilder template = new StringBuilder("<project><modelVersion>4.0.0</modelVersion><dependencies>");
        for (String artifact : java.util.List.of("sshd-sftp", "sshd-core", "sshd-common"))
            template.append("<dependency><groupId>org.apache.sshd</groupId><artifactId>").append(artifact)
                    .append("</artifactId><version>2.19.0</version><scope>test</scope></dependency>");
        template.append("</dependencies></project>");
        var files = Map.of(
                "user-flow-tests/src/test/java/org/ikasan/studio/flowtests/support/LocalFtpTestServer.java", "ftp",
                "user-flow-tests/src/test/java/org/ikasan/studio/flowtests/support/LocalSftpTestServer.java", "sftp",
                "user-flow-tests/pom.xml", template.toString());
        String updated = FlowTestFiles.withFtpTestDependencies(existing, files);
        assertTrue(updated.contains("<artifactId>sshd-core</artifactId><version>custom</version>"));
        assertTrue(updated.contains("<artifactId>sshd-sftp</artifactId><version>2.19.0</version><scope>test</scope>"));
        assertTrue(updated.contains("<artifactId>sshd-common</artifactId><version>2.19.0</version><scope>test</scope>"));
        assertEquals(updated, FlowTestFiles.withFtpTestDependencies(updated, files));
    }

    @Test void skippingExistingSupportStillCreatesMissingFlowAndFixtures() throws Exception {
        Files.writeString(root.resolve("pom.xml"), "original");
        String supportPath = FlowTestScaffold.SUPPORT_PATH;
        String helperPath = "user-flow-tests/src/test/java/org/ikasan/studio/flowtests/support/LocalSftpTestServer.java";
        String testPath = "user-flow-tests/src/test/java/org/ikasan/studio/flowtests/Flow7FlowTest.java";
        String fixturePath = "user-flow-tests/src/test/resources/flow7/my_sftp_producer/first.txt";
        for (String path : java.util.List.of(supportPath, helperPath)) {
            Files.createDirectories(root.resolve(path).getParent());
            Files.writeString(root.resolve(path), "custom " + path);
        }
        var scenario = new FlowTestScaffold.Scaffold("updated", testPath,
                Map.of(testPath, "new scenario", supportPath, "new support", helperPath, "new helper",
                        fixturePath, "first expected payload"));
        var plans = java.util.List.of(scenario,
                new FlowTestScaffold.Scaffold("updated", supportPath, Map.of(supportPath, "new support")),
                new FlowTestScaffold.Scaffold("updated", helperPath, Map.of(helperPath, "new helper")));
        assertThrows(FlowTestFiles.ExistingTestsException.class, () -> FlowTestFiles.checkExisting(root, plans));
        assertEquals(java.util.List.of(root.resolve(testPath)), FlowTestFiles.writeAll(root, "original", plans));
        assertEquals("new scenario", Files.readString(root.resolve(testPath)));
        assertEquals("first expected payload", Files.readString(root.resolve(fixturePath)));
        assertEquals("custom " + supportPath, Files.readString(root.resolve(supportPath)));
        assertEquals("custom " + helperPath, Files.readString(root.resolve(helperPath)));
        try (var files = Files.walk(root)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().contains(".bak")));
        }
    }

    @Test void sftpChoicePreservesPropertiesAndIsIdempotent() throws Exception {
        Path properties = root.resolve(FlowTestScaffold.TEST_PROPERTIES_PATH);
        Files.createDirectories(properties.getParent());
        String original = "test.sftp.enabled=false\ntest.sftp.username=custom\nother=keep\n";
        Files.writeString(properties, original);
        FlowTestFiles.enableLocalSftp(root);
        String updated = Files.readString(properties);
        assertTrue(updated.startsWith(original));
        assertTrue(updated.endsWith("test.sftp.enabled=true\n"));
        FlowTestFiles.enableLocalSftp(root);
        assertEquals(updated, Files.readString(properties));
        try (var paths = Files.list(properties.getParent())) {
            assertEquals(1, paths.filter(p -> p.getFileName().toString().contains(".bak")).count());
        }
    }

    @Test void smtpChoiceArchivesPropertiesAndAddsPackDependency() throws Exception {
        Path properties = root.resolve(FlowTestScaffold.TEST_PROPERTIES_PATH);
        Files.createDirectories(properties.getParent());
        String original = "# keep\ntest.ftp.enabled=true\ntest.smtp.enabled=false\ncustom=value\n";
        Files.writeString(properties, original);
        FlowTestFiles.enableLocalSmtp(root);
        String updated = Files.readString(properties);
        assertTrue(updated.startsWith(original));
        assertTrue(updated.endsWith("test.smtp.enabled=true\n"));
        assertEquals(updated, FlowTestFiles.localSmtpProperties(updated));
        try (var paths = Files.list(properties.getParent())) {
            assertTrue(paths.anyMatch(p -> p.getFileName().toString().startsWith("module-test.properties.bak")));
        }
        String pom = "<project><modelVersion>4.0.0</modelVersion></project>";
        var files = java.util.Map.of(
                "user-flow-tests/src/test/java/org/ikasan/studio/flowtests/support/LocalFtpTestServer.java", "ftp",
                "user-flow-tests/src/test/java/org/ikasan/studio/flowtests/support/LocalSmtpTestServer.java", "smtp",
                "user-flow-tests/pom.xml", "<project><modelVersion>4.0.0</modelVersion><dependencies><dependency><groupId>com.icegreen</groupId><artifactId>greenmail</artifactId><version>1.6.15</version></dependency></dependencies></project>");
        String result = FlowTestFiles.withFtpTestDependencies(pom, files);
        assertTrue(result.contains("<artifactId>greenmail</artifactId><version>1.6.15</version>"));
        assertEquals(result, FlowTestFiles.withFtpTestDependencies(result, files));
    }

    @Test void localFtpChoicePreservesPropertiesAndArchivesOriginal() throws Exception {
        Path properties = root.resolve(FlowTestScaffold.TEST_PROPERTIES_PATH);
        Files.createDirectories(properties.getParent());
        String original = "# developer settings\nmail.host=localhost\ntest.ftp.enabled=false\ntest.ftp.username=custom\ntest.ftp.password=secret\n";
        Files.writeString(properties, original);
        FlowTestFiles.enableLocalFtp(root);
        String updated = Files.readString(properties);
        assertTrue(updated.startsWith(original));
        java.util.Properties loaded = new java.util.Properties();
        loaded.load(new java.io.StringReader(updated));
        assertEquals("true", loaded.getProperty("test.ftp.enabled"));
        assertEquals("custom", loaded.getProperty("test.ftp.username"));
        assertEquals("secret", loaded.getProperty("test.ftp.password"));
        assertEquals("localhost", loaded.getProperty("mail.host"));
        FlowTestFiles.enableLocalFtp(root);
        assertEquals(updated, Files.readString(properties));
        try (var paths = Files.list(properties.getParent())) {
            var backups = paths.filter(p -> p.getFileName().toString().contains(".bak")).toList();
            assertEquals(1, backups.size());
            assertEquals(original, Files.readString(backups.get(0)));
        }
    }

    @Test void localFtpChoiceSuppliesDefaultsWithoutChangingOtherValues() throws Exception {
        String updated = FlowTestFiles.localFtpProperties("# original\nother=value\n");
        java.util.Properties loaded = new java.util.Properties();
        loaded.load(new java.io.StringReader(updated));
        assertEquals("true", loaded.getProperty("test.ftp.enabled"));
        assertEquals("ikasan", loaded.getProperty("test.ftp.username"));
        assertEquals("ikasan", loaded.getProperty("test.ftp.password"));
        assertEquals("value", loaded.getProperty("other"));
        assertEquals(updated, FlowTestFiles.localFtpProperties(updated));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"", "<dependencies/>", "<dependencies><!-- keep direct dependencies --></dependencies>"})
    void createsTestWithNestedPomDependencies(String direct) throws Exception {
        String nested = """
                <dependencyManagement><dependencies><dependency><groupId>example</groupId><artifactId>managed</artifactId><version>1</version></dependency></dependencies></dependencyManagement>
                <build><plugins><plugin><groupId>example</groupId><artifactId>plugin</artifactId><dependencies><dependency><groupId>example</groupId><artifactId>plugin-dependency</artifactId><version>1</version></dependency></dependencies></plugin></plugins></build>
                <profiles><profile><id>custom</id><dependencies><dependency><groupId>example</groupId><artifactId>profile-dependency</artifactId><version>1</version></dependency></dependencies></profile></profiles>
                <!-- preserve literal </dependencies> and <dependencies/> in comments -->
                <properties><example><![CDATA[<dependencies>not XML</dependencies>]]></example></properties>
                """;
        String original = "<?xml version=\"1.0\"?><project xmlns=\"http://maven.apache.org/POM/4.0.0\"><modelVersion>4.0.0</modelVersion>"
                + nested + direct + "</project>";
        Files.writeString(root.resolve("pom.xml"), "original");
        Path pom = root.resolve("user-flow-tests/pom.xml");
        Files.createDirectories(pom.getParent());
        Files.writeString(pom, original);
        var files = new java.util.LinkedHashMap<>(scaffold().files());
        files.put("user-flow-tests/src/test/java/org/ikasan/studio/flowtests/support/LocalFtpTestServer.java", "helper");
        var plan = new FlowTestScaffold.Scaffold("updated", scaffold().testPath(), files);
        Path test = FlowTestFiles.write(root, "original", plan);
        assertEquals("test", Files.readString(test));
        String updated = Files.readString(pom);
        assertTrue(updated.contains(nested));
        var reader = new org.apache.maven.model.io.xpp3.MavenXpp3Reader();
        var model = reader.read(new java.io.StringReader(updated));
        assertEquals(java.util.List.of("ftpserver-core", "mina-core"), model.getDependencies().stream().map(org.apache.maven.model.Dependency::getArtifactId).toList());
        assertEquals(updated, FlowTestFiles.withFtpTestDependencies(updated, files));
        try (var paths = Files.list(pom.getParent())) {
            Path backup = paths.filter(p -> p.getFileName().toString().startsWith("pom.xml.bak")).findFirst().orElseThrow();
            assertEquals(original, Files.readString(backup));
        }
    }

    @Test void ftpDependencyUpgradePreservesSettingsAndBacksUpPom() throws Exception {
        Files.writeString(root.resolve("pom.xml"), "original");
        Path testPom = root.resolve("user-flow-tests/pom.xml");
        Files.createDirectories(testPom.getParent());
        String original = "<project><modelVersion>4.0.0</modelVersion><!-- my settings --><properties><custom>keep</custom></properties></project>";
        Files.writeString(testPom, original);
        var files = new java.util.LinkedHashMap<>(scaffold().files());
        files.put("user-flow-tests/src/test/java/org/ikasan/studio/flowtests/support/LocalFtpTestServer.java", "helper");
        FlowTestFiles.write(root, "original", new FlowTestScaffold.Scaffold("updated", scaffold().testPath(), files));
        String updated = Files.readString(testPom);
        assertTrue(updated.contains("<!-- my settings -->"));
        assertTrue(updated.contains("<custom>keep</custom>"));
        assertTrue(updated.contains("ftpserver-core"));
        assertTrue(updated.contains("mina-core"));
        assertEquals(updated, FlowTestFiles.withFtpTestDependencies(updated, files));
        try (var paths = Files.list(testPom.getParent())) {
            Path backup = paths.filter(p -> p.getFileName().toString().startsWith("pom.xml.bak")).findFirst().orElseThrow();
            assertEquals(original, Files.readString(backup));
        }
    }

    @Test void sharedSetupIsPreservedUnlessExplicitlySelectedAndArchived() throws Exception {
        Files.writeString(root.resolve("pom.xml"), "original");
        var first = scaffold();
        var files = new java.util.LinkedHashMap<>(first.files());
        files.put(FlowTestScaffold.SUPPORT_PATH, "fresh support");
        files.put(FlowTestScaffold.TEST_PROPERTIES_PATH, "# connection keys");
        var withSupport = new FlowTestScaffold.Scaffold(first.rootPom(), first.testPath(), files);
        FlowTestFiles.write(root, "original", withSupport);
        Path support = root.resolve(FlowTestScaffold.SUPPORT_PATH);
        Files.writeString(support, "developer connections");
        Path properties = root.resolve(FlowTestScaffold.TEST_PROPERTIES_PATH);
        Files.writeString(properties, "test.host=localhost\n");
        var secondFiles = new java.util.LinkedHashMap<>(files);
        secondFiles.put("user-flow-tests/SecondTest.java", "second");
        var second = new FlowTestScaffold.Scaffold("updated", "user-flow-tests/SecondTest.java", secondFiles);
        FlowTestFiles.write(root, "updated", second);
        assertEquals("developer connections", Files.readString(support));
        var supportScaffold = new FlowTestScaffold.Scaffold("updated", FlowTestScaffold.SUPPORT_PATH,
                Map.of(FlowTestScaffold.SUPPORT_PATH, "fresh support", FlowTestScaffold.TEST_PROPERTIES_PATH, "# updated keys"));
        var selected = java.util.List.of(supportScaffold);
        var approval = assertThrows(FlowTestFiles.ExistingTestsException.class,
                () -> FlowTestFiles.checkExisting(root, selected));
        FlowTestFiles.archiveAndWriteAll(root, "updated", selected, approval.tests());
        assertEquals("fresh support", Files.readString(support));
        assertEquals("test.host=localhost\n", Files.readString(properties));
        try (var backups = Files.list(support.getParent())) {
            Path backup = backups.filter(p -> p.getFileName().toString().startsWith("ModuleFlowTestSupport.java.bak")).findFirst().orElseThrow();
            assertEquals("developer connections", Files.readString(backup));
        }
    }

    @Test void createsTestButPreservesExistingModulePomAndRefusesRegeneration() throws Exception {
        Files.writeString(root.resolve("pom.xml"), "original");
        Files.createDirectory(root.resolve("user-flow-tests"));
        Files.writeString(root.resolve("user-flow-tests/pom.xml"), "developer pom");
        Path test = FlowTestFiles.write(root, "original", scaffold());
        assertEquals("developer pom", Files.readString(root.resolve("user-flow-tests/pom.xml")));
        assertEquals("updated", Files.readString(root.resolve("pom.xml")));
        Files.writeString(test, "developer assertions");
        assertThrows(IOException.class, () -> FlowTestFiles.write(root, "updated", scaffold()));
        assertEquals("developer assertions", Files.readString(test));
    }
    @Test void staleParentLeavesNoNewFiles() throws Exception {
        Files.writeString(root.resolve("pom.xml"), "edited");
        assertThrows(IOException.class, () -> FlowTestFiles.write(root, "original", scaffold()));
        assertFalse(Files.exists(root.resolve("user-flow-tests")));
        assertEquals("edited", Files.readString(root.resolve("pom.xml")));
    }
    @Test void failedCreationRemovesOnlyFilesCreatedByThisAttempt() throws Exception {
        Files.writeString(root.resolve("pom.xml"), "original");
        Files.createDirectory(root.resolve("user-flow-tests"));
        Files.writeString(root.resolve("user-flow-tests/blocked"), "developer file");
        var files = new java.util.LinkedHashMap<String, String>();
        files.put("user-flow-tests/FirstTest.java", "first");
        files.put("user-flow-tests/blocked/SecondTest.java", "second");
        var plan = new FlowTestScaffold.Scaffold("updated", "user-flow-tests/FirstTest.java", files);
        assertThrows(IOException.class, () -> FlowTestFiles.write(root, "original", plan));
        assertFalse(Files.exists(root.resolve("user-flow-tests/FirstTest.java")));
        assertEquals("developer file", Files.readString(root.resolve("user-flow-tests/blocked")));
        assertEquals("original", Files.readString(root.resolve("pom.xml")));
    }
    @Test void rejectsSymlinkDestination() throws Exception {
        Files.writeString(root.resolve("pom.xml"), "original");
        Path external = Files.createDirectory(root.resolve("elsewhere"));
        Files.createSymbolicLink(root.resolve("user-flow-tests"), external);
        assertThrows(IOException.class, () -> FlowTestFiles.write(root, "original", scaffold()));
        try (var files = Files.list(external)) { assertEquals(0, files.count()); }
    }
    private FlowTestScaffold.Scaffold scaffold(String name) {
        String path = "user-flow-tests/src/test/java/" + name + "Test.java";
        return new FlowTestScaffold.Scaffold("updated", path,
                Map.of(path, name, "user-flow-tests/pom.xml", "generated pom"));
    }
    @Test void batchCreatesSelectedTestsAndPreservesExistingTestsAndPom() throws Exception {
        Files.writeString(root.resolve("pom.xml"), "original");
        var existing = scaffold("Existing");
        Path test = root.resolve(existing.testPath());
        Files.createDirectories(test.getParent());
        Files.writeString(test, "developer assertions");
        Files.writeString(root.resolve("user-flow-tests/pom.xml"), "developer pom");
        var first = scaffold("First");
        var second = scaffold("Second");
        var result = FlowTestFiles.writeAll(root, "original", java.util.List.of(existing, first, second));
        assertEquals(java.util.List.of(root.resolve(first.testPath()), root.resolve(second.testPath())), result);
        assertEquals("First", Files.readString(result.get(0)));
        assertEquals("Second", Files.readString(result.get(1)));
        assertEquals("developer assertions", Files.readString(test));
        assertEquals("developer pom", Files.readString(root.resolve("user-flow-tests/pom.xml")));
        assertEquals("updated", Files.readString(root.resolve("pom.xml")));
        assertTrue(FlowTestFiles.writeAll(root, "updated", java.util.List.of(existing, first, second)).isEmpty());
    }
    @Test void batchRejectsCollidingClassNamesBeforeCreatingAnything() throws Exception {
        Files.writeString(root.resolve("pom.xml"), "original");
        assertThrows(IOException.class, () -> FlowTestFiles.writeAll(root, "original", java.util.List.of(scaffold("Same"), scaffold("Same"))));
        assertFalse(Files.exists(root.resolve("user-flow-tests")));
        assertEquals("original", Files.readString(root.resolve("pom.xml")));
    }
    @Test void batchFailureRollsBackNewTestsAndLeavesExistingFiles() throws Exception {
        Files.writeString(root.resolve("pom.xml"), "original");
        Files.createDirectory(root.resolve("user-flow-tests"));
        Files.writeString(root.resolve("user-flow-tests/blocked"), "developer file");
        var blocked = new FlowTestScaffold.Scaffold("updated", "user-flow-tests/blocked/LaterTest.java",
                Map.of("user-flow-tests/blocked/LaterTest.java", "later"));
        var first = scaffold("First");
        assertThrows(IOException.class, () -> FlowTestFiles.writeAll(root, "original", java.util.List.of(first, blocked)));
        assertFalse(Files.exists(root.resolve(first.testPath())));
        assertFalse(Files.exists(root.resolve("user-flow-tests/pom.xml")));
        assertEquals("developer file", Files.readString(root.resolve("user-flow-tests/blocked")));
        assertEquals("original", Files.readString(root.resolve("pom.xml")));
    }

    @Test void archivesExactOriginalBytesAndCreatesFreshScaffoldWithoutChangingModulePom() throws Exception {
        Files.writeString(root.resolve("pom.xml"), "original");
        Path test = root.resolve(scaffold().testPath());
        Files.createDirectories(test.getParent());
        byte[] original = "developer assertions\r\n日本語\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(test, original);
        Files.writeString(root.resolve("user-flow-tests/pom.xml"), "developer pom");
        for (int i = 0; i < 2; i++) {
            Files.write(test, original);
            String pom = Files.readString(root.resolve("pom.xml"));
            var existing = assertThrows(FlowTestFiles.ExistingTestException.class, () -> FlowTestFiles.write(root, pom, scaffold()));
            assertEquals(test, FlowTestFiles.archiveAndWrite(root, pom, scaffold(), existing));
            assertEquals("test", Files.readString(test));
        }
        try (var paths = Files.list(test.getParent())) {
            var backups = paths.filter(p -> p.getFileName().toString().startsWith("ExampleTest.java.bak")).toList();
            assertEquals(2, backups.size());
            for (var backup : backups) assertArrayEquals(original, Files.readAllBytes(backup));
        }
        assertEquals("developer pom", Files.readString(root.resolve("user-flow-tests/pom.xml")));
    }
    @Test void rejectsArchiveApprovalWhenOriginalChanges() throws Exception {
        Files.writeString(root.resolve("pom.xml"), "original");
        Path test = root.resolve(scaffold().testPath());
        Files.createDirectories(test.getParent());
        Files.writeString(test, "old assertions");
        var existing = assertThrows(FlowTestFiles.ExistingTestException.class, () -> FlowTestFiles.write(root, "original", scaffold()));
        Files.writeString(test, "new assertions");
        assertThrows(IOException.class, () -> FlowTestFiles.archiveAndWrite(root, "original", scaffold(), existing));
        assertEquals("new assertions", Files.readString(test));
        try (var files = Files.list(test.getParent())) { assertEquals(1, files.count()); }
    }
    @Test void failedRegenerationRestoresOriginalAndRetainsBackup() throws Exception {
        Files.writeString(root.resolve("pom.xml"), "original");
        Path test = root.resolve(scaffold().testPath());
        Files.createDirectories(test.getParent());
        Files.writeString(test, "developer assertions");
        var existing = assertThrows(FlowTestFiles.ExistingTestException.class, () -> FlowTestFiles.write(root, "original", scaffold()));
        Files.writeString(root.resolve("user-flow-tests/blocked"), "blocked");
        var files = new java.util.LinkedHashMap<String, String>();
        files.put(scaffold().testPath(), "new scaffold");
        files.put("user-flow-tests/blocked/later.txt", "cannot create");
        var broken = new FlowTestScaffold.Scaffold("updated", scaffold().testPath(), files);
        assertThrows(IOException.class, () -> FlowTestFiles.archiveAndWrite(root, "original", broken, existing));
        assertEquals("developer assertions", Files.readString(test));
        assertEquals("original", Files.readString(root.resolve("pom.xml")));
        try (var paths = Files.list(test.getParent())) {
            var backup = paths.filter(p -> p.toString().contains(".bak")).findFirst().orElseThrow();
            assertEquals("developer assertions", Files.readString(backup));
        }
    }

    @Test void batchArchiveRecreatesOnlySelectedTests() throws Exception {
        Files.writeString(root.resolve("pom.xml"), "original");
        var first = scaffold("First");
        var second = scaffold("Second");
        var untouched = scaffold("Untouched");
        for (var item : java.util.List.of(first, second, untouched)) {
            Files.createDirectories(root.resolve(item.testPath()).getParent());
            Files.writeString(root.resolve(item.testPath()), "original " + item.testPath());
        }
        var plans = java.util.List.of(first, second, scaffold("New"));
        var prompt = assertThrows(FlowTestFiles.ExistingTestsException.class, () -> FlowTestFiles.checkExisting(root, plans));
        assertEquals(2, prompt.tests().size());
        assertEquals(3, FlowTestFiles.archiveAndWriteAll(root, "original", plans, prompt.tests()).size());
        assertEquals("First", Files.readString(root.resolve(first.testPath())));
        assertEquals("Second", Files.readString(root.resolve(second.testPath())));
        assertEquals("original " + untouched.testPath(), Files.readString(root.resolve(untouched.testPath())));
        for (var item : java.util.List.of(first, second)) {
            Path file = root.resolve(item.testPath());
            try (var paths = Files.list(file.getParent())) {
                var backup = paths.filter(p -> p.getFileName().toString().startsWith(file.getFileName() + ".bak")).findFirst().orElseThrow();
                assertEquals("original " + item.testPath(), Files.readString(backup));
            }
        }
    }
    @Test void failedBatchArchiveRestoresAllOriginals() throws Exception {
        Files.writeString(root.resolve("pom.xml"), "original");
        var first = scaffold("First");
        var second = scaffold("Second");
        for (var item : java.util.List.of(first, second)) {
            Files.createDirectories(root.resolve(item.testPath()).getParent());
            Files.writeString(root.resolve(item.testPath()), "original " + item.testPath());
        }
        Files.writeString(root.resolve("user-flow-tests/blocked"), "blocked");
        var broken = new FlowTestScaffold.Scaffold("updated", "user-flow-tests/blocked/Test.java",
                Map.of("user-flow-tests/blocked/Test.java", "test"));
        var plans = java.util.List.of(first, second, broken);
        var prompt = assertThrows(FlowTestFiles.ExistingTestsException.class, () -> FlowTestFiles.checkExisting(root, plans));
        assertThrows(IOException.class, () -> FlowTestFiles.archiveAndWriteAll(root, "original", plans, prompt.tests()));
        for (var item : java.util.List.of(first, second))
            assertEquals("original " + item.testPath(), Files.readString(root.resolve(item.testPath())));
        assertEquals("original", Files.readString(root.resolve("pom.xml")));
    }

}
