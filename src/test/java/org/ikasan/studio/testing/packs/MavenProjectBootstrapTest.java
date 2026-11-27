package org.ikasan.studio.testing.packs;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicInteger;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.project.MavenProjectBootstrap;
import org.ikasan.studio.core.project.MavenProjectBootstrap.Options;
import org.ikasan.studio.core.project.MavenProjectBootstrap.Plan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class MavenProjectBootstrapTest {
    @TempDir Path root;
    private static final String POM = """
            <project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion><groupId>org.example</groupId><artifactId>orders</artifactId>
              <version>1.0-SNAPSHOT</version><properties><company.setting>keep</company.setting><maven.compiler.source>1.7</maven.compiler.source></properties>
              <dependencies><dependency><groupId>junit</groupId><artifactId>junit</artifactId><version>4.13.2</version><scope>test</scope></dependency></dependencies>
            </project>
            """;
    private void source(String path, byte[] bytes) throws Exception { Path file=root.resolve(path);Files.createDirectories(file.getParent());Files.write(file,bytes); }
    private void project() throws Exception {
        Files.writeString(root.resolve("pom.xml"),POM);
        source("src/main/java/org/example/App.java", "package org.example; public class App { public static int sum(int a, int b) { return a+b; } }\r\n".getBytes(StandardCharsets.UTF_8));
        source("src/test/resources/data.bin",new byte[]{0,1,2,(byte)255});
        source("src/test/java/org/example/AppTest.java", "package org.example; public class AppTest { @org.junit.Test public void preservesBusinessTest() { org.junit.Assert.assertEquals(3, App.sum(1,2)); } }\n".getBytes(StandardCharsets.UTF_8));
    }
    @ParameterizedTest @ValueSource(strings={"V3.3.9","V4.1.6","V5.1.0"})
    void plansWithoutWritingAndPreservesSourcesDependenciesAndInstructions(String pack) throws Exception {
        project(); Files.writeString(root.resolve("AGENTS.md"),"developer instructions");
        Plan plan=MavenProjectBootstrap.prepare(root,new Options(pack,"org.example",12450));
        assertThat(root.resolve("jar")).doesNotExist();
        byte[] java=Files.readAllBytes(root.resolve("src/main/java/org/example/App.java"));
        byte[] binary=Files.readAllBytes(root.resolve("src/test/resources/data.bin"));
        Path backup=MavenProjectBootstrap.apply(root,plan,()->{});
        assertThat(Files.readAllBytes(root.resolve("jar/user/src/main/java/org/example/App.java"))).isEqualTo(java);
        assertThat(Files.readAllBytes(root.resolve("jar/user/src/test/resources/data.bin"))).isEqualTo(binary);
        assertThat(root.resolve("src/main/java/org/example/App.java")).doesNotExist();
        assertThat(Files.readString(root.resolve("jar/user/pom.xml"))).contains("junit","4.13.2");
        assertThat(Files.readString(root.resolve("pom.xml"))).contains("<company.setting>keep</company.setting>","<module>jar</module>");
        assertThat(Files.readString(root.resolve("AGENTS.md"))).isEqualTo("developer instructions");
        assertThat(Files.readString(backup.resolve("original/pom.xml"))).isEqualTo(POM);
        assertThat(Files.readAllBytes(backup.resolve("original/src/test/resources/data.bin"))).isEqualTo(binary);
        assertThat(ComponentIO.validatePersistedModuleJson(Files.readString(root.resolve("jar/generated/src/main/model/model.json")),"setup",false).getVersion()).isEqualTo(pack);
        assertThat(MavenProjectBootstrap.inspect(root).eligible()).isFalse();
        String export = System.getProperty("studio.bootstrap.export");
        if (export != null) {
            Path destination = Path.of(export).resolve(pack);
            Files.createDirectories(destination.getParent()); Files.createDirectory(destination);
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path path : paths.toList()) {
                    Path relative = root.relativize(path);
                    if (relative.startsWith(".ikasan-studio")) continue;
                    Path target = destination.resolve(relative);
                    if (Files.isDirectory(path)) Files.createDirectories(target);
                    else Files.copy(path, target);
                }
            }
        }
    }
    @Test void staleSourcesAndNewFilesRejectBeforeAnyWrite() throws Exception {
        project(); Plan plan=MavenProjectBootstrap.prepare(root,new Options("V4.1.6","org.example",8080));
        source("src/main/resources/new.txt",new byte[]{1});
        assertThatThrownBy(()->MavenProjectBootstrap.apply(root,plan,()->{})).hasMessageContaining("changed since review");
        assertThat(Files.readString(root.resolve("pom.xml"))).isEqualTo(POM); assertThat(root.resolve("jar")).doesNotExist();
    }
    @Test void lateFailureRestoresOriginalFilesAndRetainsRecoveryBackup() throws Exception {
        project(); Plan plan=MavenProjectBootstrap.prepare(root,new Options("V4.1.6","org.example",8080));
        AtomicInteger steps=new AtomicInteger();
        assertThatThrownBy(()->MavenProjectBootstrap.apply(root,plan,()->{if(steps.incrementAndGet()==plan.changes().size())throw new IllegalStateException("cancelled");}))
                .hasMessageContaining("backed up");
        assertThat(Files.readString(root.resolve("pom.xml"))).isEqualTo(POM);
        assertThat(Files.readString(root.resolve("src/main/java/org/example/App.java"))).endsWith("\r\n");
        assertThat(root.resolve(".ikasan-studio-project")).doesNotExist(); assertThat(root.resolve("jar")).doesNotExist();
        assertThat(root.resolve(".ikasan-studio/setup")).isDirectory();
    }
    @Test void sourcePermissionsSurviveSetup() throws Exception {
        project();
        Path source = root.resolve("src/test/resources/data.bin");
        if (Files.getFileAttributeView(source, PosixFileAttributeView.class) == null) return;
        Files.setPosixFilePermissions(source, PosixFilePermissions.fromString("rwx------"));
        Plan plan = MavenProjectBootstrap.prepare(root, new Options("V4.1.6", "org.example", 8080));
        Path backup = MavenProjectBootstrap.apply(root, plan, () -> { });
        assertThat(Files.getPosixFilePermissions(root.resolve("jar/user/src/test/resources/data.bin")))
                .isEqualTo(PosixFilePermissions.fromString("rwx------"));
        assertThat(Files.getPosixFilePermissions(backup)).isEqualTo(PosixFilePermissions.fromString("rwx------"));
    }

    @Test void existingApplicationConfigurationRequiresImportReview() throws Exception {
        project(); source("src/main/resources/application.properties", "server.port=8099".getBytes(StandardCharsets.UTF_8));
        assertThat(MavenProjectBootstrap.inspect(root).eligible()).isFalse();
        assertThat(MavenProjectBootstrap.inspect(root).detail()).contains("configuration", "import review");
        assertThat(root.resolve("jar")).doesNotExist();
    }

    @Test void rejectsComplexAndConflictingProjectsWithoutOverwritingThem() throws Exception {
        Files.writeString(root.resolve("pom.xml"),POM.replace("</project>","<modules><module>other</module></modules></project>"));
        assertThat(MavenProjectBootstrap.inspect(root).eligible()).isFalse();
        Files.writeString(root.resolve("pom.xml"),POM);Files.createDirectory(root.resolve("jar"));
        assertThat(MavenProjectBootstrap.inspect(root).eligible()).isFalse();
        assertThatThrownBy(()->MavenProjectBootstrap.prepare(root,new Options("V4.1.6","org.example",8080))).hasMessageContaining("existing jar/");
    }
}
