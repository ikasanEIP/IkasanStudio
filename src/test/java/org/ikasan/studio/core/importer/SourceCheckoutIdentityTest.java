package org.ikasan.studio.core.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class SourceCheckoutIdentityTest {
    @TempDir Path root;
    private Path source() { return root.resolve("checkout"); }
    private Path target() { return root.resolve("destination"); }
    private void write(Path base, String file, String contents) throws Exception {
        Path path = base.resolve(file); Files.createDirectories(path.getParent()); Files.writeString(path, contents);
    }
    private void destination(String name) throws Exception {
        write(target(), "generated/src/main/model/model.json", new ObjectMapper().writeValueAsString(
                Map.of("modelFormat", "ikasan-studio-documents", "module", Map.of("name", name))));
        write(target(), "generated/src/main/java/studio/Factory.java", "package studio; class Factory { @javax.annotation.Resource org.customer.Custom custom; }");
        write(target(), "pom.xml", "<project><modelVersion>4.0.0</modelVersion><groupId>example</groupId><artifactId>destination</artifactId><version>1</version></project>");
    }
    private void module(String directory, String name) throws Exception {
        String prefix = directory.isEmpty() ? "" : directory + "/";
        write(source(), prefix + "pom.xml", "<project><modelVersion>4.0.0</modelVersion><groupId>example</groupId><artifactId>different-artifact</artifactId><version>1</version></project>");
        write(source(), prefix + "src/main/java/org/customer/Custom.java", "package org.customer; public class Custom { void existing() {} }");
        if (name != null) write(source(), prefix + "src/main/resources/application.properties", "module.name=" + name + "\n");
    }
    private SourceModuleImport.Analysis analyse() throws Exception { return SourceModuleImport.analyse(source(), target(), () -> { }); }

    @Test void exactModuleNameMatchesRegardlessOfDirectoryAndArtifactHints() throws Exception {
        destination("OrderModule"); module("", "OrderModule");
        var analysis = analyse();
        assertThat(analysis.identity().status()).isEqualTo(SourceCheckoutIdentity.Status.MATCH);
        assertThat(analysis.identity().matchedImplementations()).contains("org.customer.Custom");
        var report = SourceModuleImport.apply(analysis, Set.of("src/main/java/org/customer/Custom.java"), Set.of(), false);
        assertThat(Files.readString(report)).contains("Imported module: OrderModule", "Status: MATCH");
    }

    @Test void mismatchIsCaseSensitiveAndRequiresExplicitApprovalInCoreApply() throws Exception {
        destination("OrderModule"); module("", "ordermodule");
        var analysis = analyse();
        assertThat(analysis.identity().status()).isEqualTo(SourceCheckoutIdentity.Status.MISMATCH);
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of("src/main/java/org/customer/Custom.java"), Set.of(), false))
                .hasMessageContaining("Explicitly approve");
        assertThat(target().resolve("user")).doesNotExist();
        var report = SourceModuleImport.apply(analysis, Set.of("src/main/java/org/customer/Custom.java"), Set.of(), false, true);
        assertThat(Files.readString(report)).contains("explicitly approved", "ordermodule");
    }

    @Test void missingExternalAndConflictingProfileNamesStayUnverified() throws Exception {
        destination("Orders"); module("", null);
        assertThat(analyse().identity().status()).isEqualTo(SourceCheckoutIdentity.Status.UNVERIFIED);
        write(source(), "src/main/resources/application.properties", "module.name=${MODULE_NAME:Orders}\n");
        assertThat(analyse().identity().status()).isEqualTo(SourceCheckoutIdentity.Status.UNVERIFIED);
        write(source(), "src/main/resources/application.properties", "module.name=Orders\n");
        write(source(), "src/main/resources/application-int.properties", "module.name=OtherOrders\n");
        var check = analyse().identity();
        assertThat(check.status()).isEqualTo(SourceCheckoutIdentity.Status.UNVERIFIED);
        assertThat(check.names()).containsExactly("Orders", "OtherOrders");
        assertThatThrownBy(() -> SourceModuleImport.apply(analyse(), Set.of(), Set.of(), false)).hasMessageContaining("Explicitly approve");
    }

    @Test void escapedUnicodeAndSameProfileNamesMatchWithoutResolvingTheIdeEnvironment() throws Exception {
        destination("日本"); module("", "\\u65e5\\u672c");
        write(source(), "config/module.properties", "module.name=\\u65e5\\u672c\n");
        assertThat(analyse().identity().status()).isEqualTo(SourceCheckoutIdentity.Status.MATCH);
        write(source(), "config/external.properties", "module.name=@module.name@\n");
        assertThat(analyse().identity().status()).isEqualTo(SourceCheckoutIdentity.Status.UNVERIFIED);
    }

    @Test void studioGeneratedIdentityIsReadWithoutOfferingGeneratedSourceForImport() throws Exception {
        destination("StudioModule"); module("", null);
        Files.delete(source().resolve("src/main/java/org/customer/Custom.java"));
        write(source(), "user/pom.xml", "<project><modelVersion>4.0.0</modelVersion><groupId>example</groupId><artifactId>user</artifactId><version>1</version></project>");
        write(source(), "user/src/main/java/org/customer/Custom.java", "package org.customer; public class Custom { }");
        write(source(), "generated/src/main/resources/application.properties", "module.name=StudioModule\n");
        write(source(), "generated/src/main/model/model.json", "{\"name\":\"StudioModule\"}");
        write(source(), "generated/src/main/java/studio/OriginalFactory.java", "package studio; class OriginalFactory { }");
        var analysis = analyse();
        assertThat(analysis.identity().status()).isEqualTo(SourceCheckoutIdentity.Status.MATCH);
        assertThat(analysis.files()).extracting(SourceModuleImport.FileChoice::source).contains("user/src/main/java/org/customer/Custom.java")
                .noneMatch(path -> path.startsWith("generated/"));
    }

    @Test void reactorUniqueMatchScopesClassesPropertiesAndDependenciesAndKeepsTheVerifiedLocalParent() throws Exception {
        destination("Right"); module("right", "Right"); module("wrong", "Wrong");
        write(source(), "pom.xml", "<project><modelVersion>4.0.0</modelVersion><groupId>example</groupId><artifactId>parent</artifactId><version>1</version><packaging>pom</packaging><dependencies><dependency><groupId>customer</groupId><artifactId>parent-library</artifactId><version>1</version></dependency></dependencies></project>");
        write(source(), "right/pom.xml", "<project><modelVersion>4.0.0</modelVersion><parent><groupId>example</groupId><artifactId>parent</artifactId><version>1</version></parent><artifactId>right</artifactId></project>");
        write(source(), "wrong/pom.xml", "<project><modelVersion>4.0.0</modelVersion><groupId>example</groupId><artifactId>wrong</artifactId><version>1</version><dependencies><dependency><groupId>customer</groupId><artifactId>wrong-library</artifactId><version>1</version></dependency></dependencies></project>");
        var analysis = analyse();
        assertThat(analysis.identity().candidate().directory()).isEqualTo("right");
        assertThat(analysis.files()).allMatch(file -> file.source().startsWith("right/"));
        assertThat(analysis.properties()).allMatch(file -> file.source().startsWith("right/"));
        assertThat(analysis.dependencies()).extracting(d -> d.dependency().getArtifactId()).contains("parent-library").doesNotContain("wrong-library");
    }

    @Test void multiplePlausibleModulesRequireSelectionAndDoNotMergeSiblings() throws Exception {
        destination("Same"); module("one", "Same"); module("two", "Same");
        var inspection = SourceCheckoutIdentity.inspect(source(), target(), () -> { });
        assertThat(inspection.automaticCandidate()).isNull();
        assertThatThrownBy(this::analyse).hasMessageContaining("Choose the source module");
        var selected = SourceModuleImport.analyse(source(), target(), "two", () -> { });
        assertThat(selected.files()).allMatch(file -> file.source().startsWith("two/"));
        assertThat(selected.identity().status()).isEqualTo(SourceCheckoutIdentity.Status.MATCH);
        write(source(), "two/src/main/resources/application-int.properties", "module.name=OtherProfile\n");
        assertThat(SourceCheckoutIdentity.inspect(source(), target(), () -> { }).automaticCandidate()).isNull();
        assertThatThrownBy(this::analyse).hasMessageContaining("Choose the source module");
        assertThatThrownBy(() -> SourceModuleImport.analyse(source(), target(), "../outside", () -> { })).hasMessageContaining("Choose the source module");
    }

    @Test void identityChangesIncludingAddedProfilesAndDestinationRenameInvalidateReviewBeforeWrites() throws Exception {
        destination("Orders"); module("", "Orders");
        var analysis = analyse();
        write(source(), "src/main/resources/application-dev.properties", "module.name=Wrong\n");
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of(), Set.of(), false, true)).hasMessageContaining("changed since review");
        assertThat(target().resolve("user")).doesNotExist();
        Files.delete(source().resolve("src/main/resources/application-dev.properties"));
        var refreshed = analyse(); destination("Renamed");
        assertThatThrownBy(() -> SourceModuleImport.apply(refreshed, Set.of(), Set.of(), false, true)).hasMessageContaining("changed since review");
    }

    @Test void stagedMergesApplyOnlyReviewedLinesAndKeepOriginalAndStaleGuards() throws Exception {
        destination("Orders"); module("", "Orders");
        write(source(), "src/main/java/org/customer/Custom.java", "package org.customer; public class Custom { void chosenDebug() {} void skipDebug() {} }");
        String oldDestination = "package org.customer; public class Custom { void keep() {} }";
        String merged = "package org.customer; public class Custom { void keep() {} void chosenDebug() {} }";
        write(target(), "user/src/main/java/org/customer/Custom.java", oldDestination);
        var analysis = analyse(); String key = "src/main/java/org/customer/Custom.java";
        String original = Files.readString(source().resolve(key));
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of(key), Set.of(), false, false, Map.of(key, merged)))
                .hasMessageContaining("Approve replacement");
        var report = SourceModuleImport.apply(analysis, Set.of(key), Set.of(), true, false, Map.of(key, merged));
        assertThat(Files.readString(target().resolve("user/src/main/java/org/customer/Custom.java"))).isEqualTo(merged).doesNotContain("skipDebug");
        assertThat(Files.readString(source().resolve(key))).isEqualTo(original);
        assertThat(Files.readString(report)).contains("Staged Java merges applied");
        var refreshed = analyse(); write(target(), "user/src/main/java/org/customer/Custom.java", oldDestination);
        assertThatThrownBy(() -> SourceModuleImport.apply(refreshed, Set.of(key), Set.of(), true, false, Map.of(key, merged)))
                .hasMessageContaining("Destination changed");
        assertThatThrownBy(() -> SourceModuleImport.apply(refreshed, Set.of(), Set.of(), true, false, Map.of(key, merged)))
                .hasMessageContaining("selected Java source files");
        var sourceReview = analyse(); write(source(), key, "package org.customer; public class Custom { void changedAfterReview() {} }");
        assertThatThrownBy(() -> SourceModuleImport.apply(sourceReview, Set.of(key), Set.of(), true, false, Map.of(key, merged)))
                .hasMessageContaining("Original source changed");
    }
}
