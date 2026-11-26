package org.ikasan.studio.core.project;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.ikasan.studio.core.maven.IkasanPomModel;
import org.ikasan.studio.core.metapack.model.MetaPackManifest;

/** Maintains required Maven settings; scripts and assembly customisations are seeded only when missing. */
public final class DistributionScaffolding {
    private DistributionScaffolding() { }
    public record FileChange(String path, String previous, String content) { }
    private record PomInput(String source, String canonical, Model model) { }

    public static List<FileChange> prepare(Path workspace, MetaPackManifest pack) throws Exception {
        if (!StudioProjectLayout.isNested(workspace)) return List.of();
        if (Files.isSymbolicLink(workspace.resolve("jar")))
            throw new IllegalArgumentException("The Studio application reactor jar/ must not be a symbolic link.");
        Map<String, FileChange> changes = new LinkedHashMap<>();
        PomInput rootInput = read(workspace.resolve("pom.xml"));
        Model root = rootInput.model();
        new IkasanPomModel(root).applyBuildContract(pack);
        if (!root.getModules().contains("jar")) root.addModule("jar");
        if (!root.getModules().contains("distribution")) root.addModule("distribution");
        Model application = read(workspace.resolve("jar/generated/pom.xml")).model();
        String group = root.getGroupId() != null ? root.getGroupId() : root.getParent() == null ? null : root.getParent().getGroupId();
        String version = root.getVersion() != null ? root.getVersion() : root.getParent() == null ? null : root.getParent().getVersion();
        if (group == null || version == null || application.getArtifactId() == null)
            throw new IllegalArgumentException("The project must declare or inherit Maven groupId/version and declare the application artifactId.");
        root.getProperties().setProperty("studio.application.groupId", application.getGroupId() != null
                ? application.getGroupId() : group);
        root.getProperties().setProperty("studio.application.artifactId", application.getArtifactId());
        root.getProperties().setProperty("studio.application.version", application.getVersion() != null
                ? application.getVersion() : version);
        putPom(changes, workspace, "pom.xml", rootInput);

        PomInput distributionInput = read(workspace.resolve("distribution/pom.xml"));
        Model distribution = distributionInput.model();
        if (distribution.getDependencies().stream().noneMatch(d -> "${studio.application.artifactId}".equals(d.getArtifactId()))) {
            Dependency dependency = new Dependency();
            dependency.setGroupId("${studio.application.groupId}");
            dependency.setArtifactId("${studio.application.artifactId}");
            dependency.setVersion("${studio.application.version}");
            distribution.addDependency(dependency);
        }
        if (distribution.getBuild() == null) distribution.setBuild(new Build());
        Plugin assembly = distribution.getBuild().getPlugins().stream()
                .filter(p -> "maven-assembly-plugin".equals(p.getArtifactId())
                        && (p.getGroupId() == null || "org.apache.maven.plugins".equals(p.getGroupId())))
                .findFirst().orElse(null);
        if (assembly == null) {
            assembly = new Plugin();
            assembly.setGroupId("org.apache.maven.plugins");
            assembly.setArtifactId("maven-assembly-plugin");
            assembly.setVersion("3.7.1");
            distribution.getBuild().addPlugin(assembly);
        }
        if (assembly.getExecutions().stream().noneMatch(e -> e.getGoals().contains("single"))) {
            PluginExecution execution = new PluginExecution();
            execution.setId("studio-distribution");
            execution.setPhase("package");
            execution.addGoal("single");
            Xpp3Dom configuration = new Xpp3Dom("configuration");
            Xpp3Dom descriptors = new Xpp3Dom("descriptors");
            Xpp3Dom descriptor = new Xpp3Dom("descriptor");
            descriptor.setValue("${project.basedir}/distribution.xml");
            descriptors.addChild(descriptor);
            configuration.addChild(descriptors);
            execution.setConfiguration(configuration);
            assembly.addExecution(execution);
        }
        putPom(changes, workspace, "distribution/pom.xml", distributionInput);
        seed(changes, workspace, "distribution/distribution.xml", assembly());
        seed(changes, workspace, "bin/ikasan.sh", launcher());
        seed(changes, workspace, "bin/simple-env.sh", "#!/bin/sh\n# Developer-owned JVM options. Use JAVA_HOME to select the module JDK.\n# JAVA_OPTS=\"-Xms256m -Xmx1g\"\n");
        seed(changes, workspace, "bin/ikasan.bat", windowsLauncher());
        seed(changes, workspace, "bin/simple-env.bat", "@echo off\r\nrem Developer-owned JVM options; JAVA_HOME selects the module JDK.\r\nrem set JAVA_OPTS=-Xms256m -Xmx1g\r\n");
        return List.copyOf(changes.values());
    }

    private static void seed(Map<String, FileChange> changes, Path root, String path, String content) {
        if (!Files.exists(root.resolve(path))) put(changes, root, path, null, content);
    }
    private static void putPom(Map<String, FileChange> changes, Path root, String path, PomInput input) throws Exception {
        String rendered = xml(input.model());
        if (!rendered.equals(input.canonical())) put(changes, root, path, input.source(), rendered);
    }
    private static void put(Map<String, FileChange> changes, Path root, String path, String previous, String content) {
        Path file = root.resolve(path);
        for (Path part = file; part != null && part.startsWith(root); part = part.getParent())
            if (Files.isSymbolicLink(part)) throw new IllegalArgumentException("Scaffolding does not follow symbolic links: " + part);
        if (!content.equals(previous)) changes.put(path, new FileChange(path, previous, content));
    }
    private static PomInput read(Path path) throws Exception {
        if (Files.isSymbolicLink(path)) throw new IllegalArgumentException("Scaffolding does not follow symbolic links: " + path);
        String source = Files.readString(path);
        Model model = new MavenXpp3Reader().read(new StringReader(source));
        return new PomInput(source, xml(model), model);
    }
    private static String xml(Model model) throws Exception {
        StringWriter text = new StringWriter();
        new MavenXpp3Writer().write(text, model);
        return text.toString();
    }
    private static String assembly() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <assembly xmlns="http://maven.apache.org/ASSEMBLY/2.2.0">
                  <id>dist</id>
                  <formats><format>zip</format></formats>
                  <baseDirectory>${project.parent.artifactId}-${project.version}</baseDirectory>
                  <fileSets>
                    <fileSet><directory>../bin</directory><outputDirectory>bin</outputDirectory><fileMode>0755</fileMode></fileSet>
                    <fileSet><directory>../jar/generated/src/main/resources</directory><outputDirectory>config</outputDirectory>
                      <includes><include>application.properties</include><include>logback-spring.xml</include></includes>
                    </fileSet>
                  </fileSets>
                  <dependencySets>
                    <dependencySet><outputDirectory>lib</outputDirectory><useProjectArtifact>false</useProjectArtifact>
                      <scope>runtime</scope><unpack>false</unpack>
                    </dependencySet>
                  </dependencySets>
                </assembly>
                """;
    }
    private static String launcher() {
        return """
                #!/bin/sh
                # Developer-owned launcher. Runs in the foreground; service managers own process lifecycle.
                set -eu
                APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
                cd "$APP_HOME"
                if [ -f "$APP_HOME/bin/simple-env.sh" ]; then . "$APP_HOME/bin/simple-env.sh"; fi
                JAVA=java
                if [ -n "${JAVA_HOME:-}" ]; then JAVA="$JAVA_HOME/bin/java"; fi
                # JAVA_OPTS intentionally accepts separate JVM arguments; application arguments remain quoted.
                exec "$JAVA" ${JAVA_OPTS:-} -cp "$APP_HOME/config:$APP_HOME/lib/*" org.ikasan.studio.boot.Application "$@"
                """;
    }

    private static String windowsLauncher() {
        return String.join("\r\n",
                "@echo off", "setlocal", "set \"APP_HOME=%~dp0..\"",
                "cd /d \"%APP_HOME%\" || exit /b 1",
                "if exist \"%APP_HOME%\\bin\\simple-env.bat\" call \"%APP_HOME%\\bin\\simple-env.bat\"",
                "set \"JAVA_EXE=java\"",
                "if defined JAVA_HOME set \"JAVA_EXE=%JAVA_HOME%\\bin\\java.exe\"",
                "\"%JAVA_EXE%\" %JAVA_OPTS% -cp \"%APP_HOME%\\config;%APP_HOME%\\lib\\*\" org.ikasan.studio.boot.Application %*",
                "exit /b %ERRORLEVEL%", "");
    }
}
