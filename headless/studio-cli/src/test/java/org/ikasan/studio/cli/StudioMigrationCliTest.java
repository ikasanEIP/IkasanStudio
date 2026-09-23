package org.ikasan.studio.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ikasan.studio.core.migration.MigrationWorkspace;
import java.io.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class StudioMigrationCliTest {
    @TempDir Path project;
    private int run(String... args) { return StudioMigrationCli.run(args, System.out, System.err); }
    private Path setup() throws Exception {
        Files.createDirectories(project.resolve("generated/src/main/model"));
        Files.createDirectories(project.resolve("user/src"));
        Files.writeString(project.resolve("user/src/Keep.java"), "// developer implementation\n");
        Files.copy(Path.of("regression-tests/migration/baseline.json"), project.resolve("generated/src/main/model/model.json"));
        Files.copy(Path.of("regression-tests/migration/project/pom.xml"), project.resolve("pom.xml"));
        return project.resolve("preview.json");
    }
    private int preview(Path plan, String target) {
        return run("preview", "--project", project.toString(), "--to", target, "--plan", plan.toString());
    }
    @Test void previewDoesNotMutateAndApplyUsesRecoveryTransaction() throws Exception {
        Path plan=setup(); String original=Files.readString(project.resolve("pom.xml"));
        assertEquals(0, preview(plan, "V4.1.6"));
        assertEquals(original, Files.readString(project.resolve("pom.xml")));
        assertFalse(Files.exists(project.resolve(MigrationWorkspace.HISTORY)));
        assertEquals(0, run("apply", "--plan", plan.toString()));
        assertTrue(Files.readString(project.resolve("generated/src/main/model/model.json")).contains("V4.1.6"));
        assertEquals("// developer implementation\n", Files.readString(project.resolve("user/src/Keep.java")));
        assertTrue(MigrationWorkspace.latest(project).committed());
        assertEquals(1, run("apply", "--plan", plan.toString()));
    }
    @Test void rejectsStalePomAndLeavesModelUnchanged() throws Exception {
        Path plan=setup(); assertEquals(0, preview(plan,"V4.1.6"));
        Files.writeString(project.resolve("pom.xml"), "<!-- edited -->\n", StandardOpenOption.APPEND);
        assertEquals(1, run("apply", "--plan", plan.toString()));
        assertTrue(Files.readString(project.resolve("generated/src/main/model/model.json")).contains("V3.3.9"));
        assertFalse(Files.exists(project.resolve(MigrationWorkspace.HISTORY)));
    }
    @Test void rejectsChangedDeveloperFilesAndUnsupportedVersion() throws Exception {
        Path plan=setup(); assertEquals(2, preview(plan,"V5.0.0")); assertFalse(Files.exists(plan));
        assertEquals(0, preview(plan,"V4.1.6"));
        Files.writeString(project.resolve("user/src/Keep.java"), "// changed\n");
        assertEquals(1, run("apply", "--plan", plan.toString()));
    }
    @Test void allowsRenderedJsonKeyOrdering() throws Exception {
        Path plan=setup(); assertEquals(0, preview(plan,"V4.1.6"));
        var json=org.ikasan.studio.core.persistence.json.StudioJson.newObjectMapper();
        var tree=json.readTree(Files.readString(plan));
        for(var change:tree.path("changes")) {
            if (change.path("path").asText().endsWith("component-catalogue.json")) {
                String text=new String(java.util.Base64.getDecoder().decode(change.path("after").asText()), java.nio.charset.StandardCharsets.UTF_8);
                var sorted=json.writer().with(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                        .writeValueAsBytes(json.readValue(text, java.util.Map.class));
                ((com.fasterxml.jackson.databind.node.ObjectNode)change).put("after", java.util.Base64.getEncoder().encodeToString(sorted));
            }
        }
        Files.writeString(plan,json.writeValueAsString(tree));
        assertEquals(0,run("apply","--plan",plan.toString()));
    }
    @Test void rejectsTamperedPreviewAndExistingDestination() throws Exception {
        Path plan=setup(); assertEquals(0, preview(plan,"V4.1.6"));
        assertEquals(1, preview(plan,"V4.1.6"));
        Files.writeString(plan, Files.readString(plan).replace("generated/src/main/resources/application.properties", "user/src/Keep.java"));
        assertEquals(1, run("apply", "--plan", plan.toString()));
        assertEquals("// developer implementation\n", Files.readString(project.resolve("user/src/Keep.java")));
    }
}
