package org.ikasan.studio.testing.engine;

import org.ikasan.studio.core.generator.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.ikasan.studio.core.persistence.json.StudioJson;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@org.junit.jupiter.api.Tag("engine")
class AiProjectContractGeneratorTest {
    @Test
    void archetypeDiscoveryInstructionsMatchGeneratedInstructions() throws Exception {
        Path discoveryFile = Path.of("ikasan-studio-ancillary", "ikasan-studio-project-archetype",
                "src/main/resources/archetype-resources/AGENTS.md");

        assertThat(Files.readString(discoveryFile)).isEqualTo(AiProjectContractGenerator.agentsGuide());
    }

    @Test
    void localEnvironmentTemplateMatchesUnfilteredArchetypeAndKeepsVariableExamplesLiteral() throws Exception {
        Path resources = Path.of("ikasan-studio-ancillary/ikasan-studio-project-archetype/src/main/resources");
        String template = LocalTestEnvironmentTemplate.content();
        assertThat(Files.readString(resources.resolve("archetype-resources/LOCAL_TEST_ENVIRONMENT.md")))
                .isEqualTo(template);
        assertThat(template).contains("${IKASAN_TEST_FTP_PASSWORD}", "env:IKASAN_TEST_SFTP_KEY_FILE",
                "sftp.remoteHost=", "sftp.directory=", "ftp.password=",
                "smtp.toRecipient=", "jms.destinationJndiName=");
        var doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(resources.resolve("META-INF/maven/archetype-metadata.xml").toFile());
        var xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
        String filtered = xpath.evaluate("//fileSet[includes/include='LOCAL_TEST_ENVIRONMENT.md']/@filtered", doc);
        assertThat(filtered).isNotEqualTo("true");
        assertThat(template).contains("root `.gitignore`");
    }

    @Test
    void projectSkillsMatchTheArchetypeAndKeepDiscoveryInsideTheProject() throws Exception {
        Path resources = Path.of("ikasan-studio-ancillary/ikasan-studio-project-archetype/src/main/resources");
        var files = StudioAiSkillTemplates.files();
        assertThat(files).hasSize(2);
        for (var entry : files.entrySet()) {
            assertThat(Files.readString(resources.resolve("archetype-resources").resolve(entry.getKey())))
                    .isEqualTo(entry.getValue());
            assertThat(Path.of(entry.getKey()).isAbsolute()).isFalse();
            assertThat(Path.of(entry.getKey()).normalize().startsWith("..")).isFalse();
        }
        assertThat(files.get(StudioAiSkillTemplates.CLAUDE_PATH)).contains(StudioAiSkillTemplates.WORKFLOW_PATH);
        var doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(resources.resolve("META-INF/maven/archetype-metadata.xml").toFile());
        var xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
        for (String directory : java.util.List.of(".agents/skills", ".claude/skills")) {
            assertThat(xpath.evaluate("//fileSet[directory='" + directory + "']/@filtered", doc)).isEqualTo("false");
        }
    }

    @Test
    void schemaAndGuidanceIdentifyTheProtectedSourceOfTruth() throws Exception {
        JsonNode schema = StudioJson.newObjectMapper().readTree(AiProjectContractGenerator.modelSchema());

        assertThat(schema.path("$defs").path("flow").isObject()).isTrue();
        assertThat(AiProjectContractGenerator.agentsGuide()).contains("model.json", "preserve unknown", "fields");
        assertThat(AiProjectContractGenerator.studioGuide("SyntheticGuide"))
                .contains("SyntheticGuide", "component-catalogue.json", "developer-owned",
                        "baseModelSha256", "renameComponent", "Import AI Proposal", "Keep Studio and IntelliJ open",
                        "ai-proposals/", "Review Latest AI Proposal", "temporary file", "notification")
                .doesNotContain("Python 3", "close Studio first", "renaming require Studio");
    }
    @Test
    void completionGuidanceSeparatesScaffoldsFromWorkingCodeAndPreservesOwnership() {
        String guide = AiProjectContractGenerator.studioGuide("V3.3.9");
        assertThat(guide).contains("aim for working behaviour by default", "explicitly requests that scope",
                "Continue", "Locate every required implementation and Spring bean", "A build with no tests", "UnsupportedOperationException",
                "List<File>", "individual File", "non-object message", "javax.jms", "jakarta.jms",
                "newly generated, unmodified stubs", "preserve existing logic", "external requirements",
                "representative success and failure payloads", "runtime behaviour remains unverified",
                "setExceptionResolution", "configureRoutes", "recipeConfigurations");
        assertThat(AiProjectContractGenerator.agentsGuide()).contains("Inspect the file and",
                "its diff first", "ask before replacing", "completion", "unfinished implementations");
    }

}
