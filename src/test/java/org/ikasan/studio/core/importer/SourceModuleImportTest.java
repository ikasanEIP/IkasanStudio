package org.ikasan.studio.core.importer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.StringReader;
import java.nio.file.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.ikasan.studio.core.importer.SourceModuleImport.Analysis;
import org.ikasan.studio.core.importer.SourceModuleImport.DependencyChoice;
import org.ikasan.studio.core.importer.SourceModuleImport.DependencyComparison;
import org.ikasan.studio.core.importer.SourceModuleImport.FileChoice;
import org.ikasan.studio.core.importer.SourceModuleImport.ManagedFile;
import org.ikasan.studio.core.importer.SourceModuleImport.ReviewAction;
import org.ikasan.studio.core.importer.SourceModuleImport.SpringRegistration;
import org.ikasan.studio.core.model.ikasan.instance.Module;

class SourceModuleImportTest {
    @TempDir Path temporary;
    private Path source() { return temporary.resolve("original"); }
    private Path target() { return temporary.resolve("studio"); }
    private void write(Path root, String relative, String text) throws Exception {
        Path file = root.resolve(relative); Files.createDirectories(file.getParent()); Files.writeString(file, text);
    }
    private void fixture() throws Exception {
        write(target(), "generated/src/main/model/model.json", "{\"version\":\"V4.1.6\",\"name\":\"Example\",\"flows\":[]}");
        write(source(), "src/main/resources/application.properties", "module.name=Example\n");
        write(target(), "pom.xml", "<project><modelVersion>4.0.0</modelVersion><groupId>example</groupId><artifactId>studio</artifactId><version>1</version></project>");
        write(target(), "generated/src/main/java/studio/ComponentFactory.java", """
                package studio;
                class ComponentFactory {
                    @jakarta.annotation.Resource org.customer.Bespoke renamedBean;
                    @javax.annotation.Resource(name="explicitBean") org.customer.Other other;
                    @javax.annotation.Resource org.ikasan.builder.BuilderFactory builderFactory;
                }
                """);
        write(source(), "src/main/java/org/customer/Bespoke.java", "package org.customer;\r\n@org.springframework.context.annotation.PropertySource(\"classpath:bespoke.properties\")\r\npublic class Bespoke { Helper helper; }\r\n");
        write(source(), "src/main/java/org/customer/Helper.java", "package org.customer; public class Helper { }");
        write(source(), "src/main/java/org/customer/Other.java", "package org.customer; public class Other { }");
        write(source(), "src/main/java/org/customer/Unrelated.java", "package org.customer; public class Unrelated { }");
        write(source(), "src/main/java/org/customer/OriginalConfig.java", """
                package org.customer;
                @org.springframework.context.annotation.Configuration
                class OriginalConfig {
                    @org.springframework.context.annotation.Bean Bespoke oldBeanName() { return new Bespoke(); }
                }
                """);
        write(source(), "src/main/java/org/customer/Application.java", "package org.customer; @org.springframework.boot.autoconfigure.SpringBootApplication class Application { }");
        write(source(), "src/main/resources/application-dev.properties", "secret=${CUSTOM_SECRET}\n");
        write(source(), "src/main/resources/bespoke.properties", "custom.value=preserved\n");
        write(source(), "config/customer.properties", "custom.endpoint=https://customer\n");
        write(source(), "target/src/main/java/Fake.java", "not Java");
        write(source(), "src/test/resources/config/application-prod.properties", "test.only=should-not-import");
        write(source(), "pom.xml", """
                <project><modelVersion>4.0.0</modelVersion><groupId>example</groupId><artifactId>original</artifactId><version>1</version>
                <properties><custom.version>2.3.4</custom.version></properties><dependencies>
                <dependency><groupId>customer</groupId><artifactId>custom-client</artifactId><version>${custom.version}</version>
                  <exclusions><exclusion><groupId>old.xml</groupId><artifactId>parser</artifactId></exclusion></exclusions>
                </dependency>
                <dependency><groupId>customer</groupId><artifactId>inherited</artifactId></dependency>
                <dependency><groupId>org.ikasan</groupId><artifactId>ikasan-parent</artifactId><version>old</version></dependency>
                </dependencies></project>
                """);
    }
    private SourceModuleImport.Analysis analyse() throws Exception { return SourceModuleImport.analyse(source(), target(), () -> { }); }

    private static final String CONTEXT_BINARY = "org.ikasan.studio.boot.flow.orders.ComponentFactoryOrders$TrustedOrderConsumerContextFactory";
    private static final String CONTEXT_SOURCE = "generated/src/main/java/org/ikasan/studio/boot/flow/orders/ComponentFactoryOrders.java";

    private void trustedContextFixture() throws Exception {
        fixture();
        write(target(), "generated/src/main/resources/application.properties",
                "consumer.connectionFactoryJndiProperties={\"java.naming.factory.initial\":\"" + CONTEXT_BINARY + "\"}\n");
        write(target(), CONTEXT_SOURCE, "package org.ikasan.studio.boot.flow.orders; public class ComponentFactoryOrders { }");
        write(source(), CONTEXT_SOURCE, """
                package org.ikasan.studio.boot.flow.orders;
                import org.apache.activemq.jndi.ActiveMQInitialContextFactory;
                import java.util.List;
                @org.springframework.context.annotation.Configuration
                public class ComponentFactoryOrders {
                    public static class TrustedOrderConsumerContextFactory extends ActiveMQInitialContextFactory {
                        public static String initialFactoryName(String name) { return TrustedOrderConsumerContextFactory.class.getName(); }
                        public static List<String> configure() { return List.of("org.example.cat.domain"); }
                        public void createConnectionFactory() { }
                    }
                }
                """);
    }

    @Test void extractsReviewedTrustedContextWithoutCopyingSpringFactoryAndLoadsItsBinaryName() throws Exception {
        trustedContextFixture();
        Analysis analysis = analyse();
        FileChoice helper = analysis.files().stream().filter(f -> f.source().equals(CONTEXT_SOURCE)).findFirst().orElseThrow();
        assertThat(helper.recommended()).isTrue();
        assertThat(helper.wiring()).isFalse();
        assertThat(SourceModuleImport.reviewActions(analysis, Set.of(), Set.of()))
                .anyMatch(a -> a.kind() == SourceModuleImport.ReviewKind.SOURCE && a.subject().equals(helper.destination()));
        SourceModuleImport.apply(analysis, Set.of(CONTEXT_SOURCE), Set.of(), false, true);
        String copied = Files.readString(target().resolve(helper.destination()));
        assertThat(copied).contains("org.example.cat.domain", "public class ComponentFactoryOrders$TrustedOrderConsumerContextFactory")
                .doesNotContain("@org.springframework.context.annotation.Configuration", "public class ComponentFactoryOrders {");
        // No broker is needed: prove javac and reflection accept the preserved binary name.
        Path stub = temporary.resolve("compile/org/apache/activemq/jndi/ActiveMQInitialContextFactory.java");
        Files.createDirectories(stub.getParent()); Files.writeString(stub, "package org.apache.activemq.jndi; public class ActiveMQInitialContextFactory { }");
        Path classes = temporary.resolve("classes"); Files.createDirectories(classes);
        Process compiler = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "javac").toString(),
                "--release", "17", "-d", classes.toString(), stub.toString(), target().resolve(helper.destination()).toString())
                .redirectErrorStream(true).start();
        String diagnostics = new String(compiler.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(compiler.waitFor()).as(diagnostics).isZero();
        try (URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[]{classes.toUri().toURL()}, null)) {
            Class<?> loaded = Class.forName(CONTEXT_BINARY, true, loader);
            assertThat(loaded.getMethod("initialFactoryName", String.class).invoke(null, "unused")).isEqualTo(CONTEXT_BINARY);
            assertThat(loaded.getMethod("configure").invoke(null)).isEqualTo(List.of("org.example.cat.domain"));
        }
        assertThat(analyse().files().stream().filter(f -> f.source().equals(CONTEXT_SOURCE)).findFirst().orElseThrow().identicalToDestination()).isTrue();
    }

    @Test void trustedContextExtractionRejectsStaleSourceAndPreservesExistingDestination() throws Exception {
        trustedContextFixture(); Analysis initial = analyse();
        FileChoice helper = initial.files().stream().filter(f -> f.source().equals(CONTEXT_SOURCE)).findFirst().orElseThrow();
        write(source(), CONTEXT_SOURCE, Files.readString(source().resolve(CONTEXT_SOURCE)) + "\n// changed");
        assertThatThrownBy(() -> SourceModuleImport.apply(initial, Set.of(CONTEXT_SOURCE), Set.of(), false, true))
                .hasMessageContaining("Original source changed");
        assertThat(target().resolve(helper.destination())).doesNotExist();
        Analysis analysis = analyse();
        write(target(), helper.destination(), "// developer-owned file");
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of(CONTEXT_SOURCE), Set.of(), false, true))
                .hasMessageContaining("Destination");
        assertThat(Files.readString(target().resolve(helper.destination()))).isEqualTo("// developer-owned file");
    }

    @Test void trustedContextRecoveryRejectsChangedRuntimeConfiguration() throws Exception {
        trustedContextFixture(); Analysis analysis = analyse();
        write(target(), "generated/src/main/resources/application.properties", "consumer.connectionFactoryJndiProperties={}\n");
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of(CONTEXT_SOURCE), Set.of(), false, true))
                .hasMessageContaining("Destination");
        assertThat(target().resolve("user/src/main/java/" + CONTEXT_BINARY.replace('.', '/') + ".java")).doesNotExist();
    }

    @Test void alreadyGeneratedTrustedContextDoesNotOfferADuplicate() throws Exception {
        trustedContextFixture();
        write(target(), CONTEXT_SOURCE, Files.readString(source().resolve(CONTEXT_SOURCE)));
        assertThat(analyse().files()).noneMatch(f -> f.destination().contains("$Trusted"));
    }

    @Test void customContextShapesAreLeftForManualReview() throws Exception {
        trustedContextFixture();
        write(source(), CONTEXT_SOURCE, Files.readString(source().resolve(CONTEXT_SOURCE))
                .replace("extends ActiveMQInitialContextFactory", "extends CustomContextFactory"));
        Analysis analysis = analyse();
        assertThat(analysis.files()).noneMatch(f -> f.destination().contains("$Trusted"));
        assertThat(SourceModuleImport.reviewActions(analysis, Set.of(), Set.of()))
                .anyMatch(a -> a.subject().contains("Missing runtime context factory: " + CONTEXT_BINARY));
    }

    @Test void missingTrustedContextSourceProducesAnActionableReview() throws Exception {
        trustedContextFixture(); Files.delete(source().resolve(CONTEXT_SOURCE));
        assertThat(SourceModuleImport.reviewActions(analyse(), Set.of(), Set.of()))
                .anyMatch(a -> a.kind() == SourceModuleImport.ReviewKind.SCAN && a.subject().contains("Missing runtime context factory: " + CONTEXT_BINARY));
    }

    @Test void uniqueSourceImplementationOfInjectedContractIsOfferedForRegistration() throws Exception {
        fixture();
        write(target(), "generated/src/main/java/studio/ComponentFactory.java", """
                package studio;
                class ComponentFactory { @javax.annotation.Resource customer.contract.Rule delegate; }
                """);
        write(source(), "src/main/java/org/customer/ConcreteRule.java", """
                package org.customer;
                import customer.contract.Rule;
                @org.springframework.stereotype.Component
                public class ConcreteRule implements Rule { }
                """);
        write(target(), "generated/src/main/java/org/ikasan/studio/boot/Application.java", """
                package org.ikasan.studio.boot;
                @org.springframework.context.annotation.ComponentScan("org.ikasan.studio.boot") class Application { }
                """);
        Analysis analysis = analyse();
        assertThat(analysis.beans()).extracting(SourceModuleImport.BeanRequirement::type).containsExactly("org.customer.ConcreteRule");
        Set<String> selected = Set.of("src/main/java/org/customer/ConcreteRule.java");
        assertThat(SourceModuleImport.reviewActions(analysis, Set.of(), Set.of()))
                .anyMatch(a -> a.kind() == SourceModuleImport.ReviewKind.SOURCE && a.subject().endsWith("ConcreteRule.java"));
        assertThat(SourceModuleImport.previewSpringRegistration(analysis, selected, Map.of()).components())
                .containsExactly("org.customer.ConcreteRule");
    }

    @Test void multipleSourceImplementationsAreNotChosenForAnInjectedContract() throws Exception {
        fixture();
        write(target(), "generated/src/main/java/studio/ComponentFactory.java", "package studio; class ComponentFactory { @javax.annotation.Resource customer.contract.Rule delegate; }");
        for (String name : List.of("FirstRule", "SecondRule")) write(source(), "src/main/java/org/customer/" + name + ".java",
                "package org.customer; @org.springframework.stereotype.Component public class " + name + " implements customer.contract.Rule { }");
        Analysis analysis = analyse();
        assertThat(analysis.beans()).extracting(SourceModuleImport.BeanRequirement::type).containsExactly("customer.contract.Rule");
        assertThat(SourceModuleImport.previewSpringRegistration(analysis,
                Set.of("src/main/java/org/customer/FirstRule.java", "src/main/java/org/customer/SecondRule.java"), Map.of())).isNull();
    }

    @Test void recommendsExactImplementationsAndHelpersButRequiresReviewOfOriginalWiring() throws Exception {
        fixture(); Analysis analysis = analyse();
        assertThat(analysis.files().stream().filter(SourceModuleImport.FileChoice::recommended).map(SourceModuleImport.FileChoice::destination))
                .containsExactlyInAnyOrder("user/src/main/java/org/customer/Bespoke.java", "user/src/main/java/org/customer/Helper.java",
                        "user/src/main/java/org/customer/Other.java", "user/src/main/java/org/customer/Unrelated.java", "user/src/main/resources/bespoke.properties");
        assertThat(analysis.files()).filteredOn(f -> f.source().endsWith("OriginalConfig.java"))
                .allMatch(f -> f.wiring() && !f.recommended());
        assertThat(analysis.files()).noneMatch(f -> f.source().endsWith("Application.java") || f.source().contains("target/"));
        assertThat(analysis.beans()).extracting(SourceModuleImport.BeanRequirement::name).containsExactly("renamedBean", "explicitBean");
        assertThat(analysis.beans().get(0).sourceDefinitions()).anyMatch(d -> d.contains("OriginalConfig.java:4") && d.contains("oldBeanName"));
        assertThat(analysis.properties()).extracting(SourceModuleImport.PropertyFile::environment).contains("dev", "").doesNotContain("prod");
        String report = SourceModuleImport.report(analysis, Set.of(), Set.of());
        assertThat(report).contains("renamedBean", "explicitBean", "first delivery", "later delivery")
                .doesNotContain("CUSTOM_SECRET", "https://customer", "custom.value=preserved",
                        "This is a transfer and review record");
        assertThat(target().resolve("user")).doesNotExist();
    }

    @Test void liveReviewContainsOnlyConcreteDecisionsAndUpdatesWithPendingCopies() throws Exception {
        fixture(); Analysis analysis = analyse();
        Set<String> dependencies = analysis.dependencies().stream().filter(SourceModuleImport.DependencyChoice::importable)
                .map(SourceModuleImport.DependencyChoice::id).collect(java.util.stream.Collectors.toSet());
        List<ReviewAction> actions = SourceModuleImport.reviewActions(analysis, Set.of(), dependencies);
        assertThat(actions).anyMatch(action -> action.kind() == SourceModuleImport.ReviewKind.BEAN && action.subject().contains("renamedBean"));
        assertThat(actions).anyMatch(action -> action.kind() == SourceModuleImport.ReviewKind.SOURCE && action.subject().endsWith("Bespoke.java"));
        assertThat(actions).anyMatch(action -> action.kind() == SourceModuleImport.ReviewKind.RESOURCE && action.subject().endsWith("bespoke.properties"));
        assertThat(actions).noneMatch(action -> action.subject().contains("Unrelated.java") || action.subject().contains("ikasan-parent")
                || action.kind() == SourceModuleImport.ReviewKind.DEPENDENCY || action.subject().contains("Module identity"));
        Set<String> files = analysis.files().stream().filter(SourceModuleImport.FileChoice::recommended)
                .map(SourceModuleImport.FileChoice::source).collect(java.util.stream.Collectors.toSet());
        assertThat(SourceModuleImport.reviewActions(analysis, files, dependencies)).noneMatch(action ->
                action.kind() == SourceModuleImport.ReviewKind.SOURCE || action.kind() == SourceModuleImport.ReviewKind.RESOURCE);
        files.remove("src/main/java/org/customer/Bespoke.java");
        assertThat(SourceModuleImport.reviewActions(analysis, files, dependencies)).anyMatch(action ->
                action.kind() == SourceModuleImport.ReviewKind.SOURCE && action.subject().endsWith("Bespoke.java"));
        assertThat(SourceModuleImport.report(analysis, files, dependencies)).contains("Module identity", "Selected files", "Generated bean contracts");
    }

    @Test void generatedBeansAreNotMisreportedAsMissingWiringButConditionalProvidersRemainReviewable() throws Exception {
        fixture();
        write(target(), "generated/src/main/java/studio/Beans.java", """
                package studio;
                @org.springframework.context.annotation.Configuration class Beans {
                    @org.springframework.context.annotation.Bean(name={"explicitBean", "alias"})
                    org.customer.Other provided() { return null; }
                    @org.springframework.context.annotation.Bean
                    @org.springframework.context.annotation.Profile("dev")
                    org.customer.Bespoke renamedBean() { return null; }
                }
                """);
        write(target(), "generated/src/main/java/studio/Flow.java", "package studio; @org.springframework.stereotype.Component(\"customName\") public class Flow {}");
        write(target(), "generated/src/main/java/studio/Internal.java", "package studio; class Internal { @jakarta.annotation.Resource Flow flow; }");
        Analysis analysis = analyse();
        assertThat(analysis.beans()).anyMatch(bean -> bean.name().equals("explicitBean") && bean.generatedProvider());
        assertThat(analysis.beans()).anyMatch(bean -> bean.name().equals("flow") && bean.type().equals("studio.Flow") && bean.generatedProvider());
        assertThat(analysis.beans()).anyMatch(bean -> bean.name().equals("renamedBean") && !bean.generatedProvider());
        assertThat(SourceModuleImport.reviewActions(analysis, Set.of(), Set.of()))
                .noneMatch(action -> action.kind() == SourceModuleImport.ReviewKind.BEAN && (action.subject().startsWith("flow :") || action.subject().startsWith("explicitBean :")))
                .anyMatch(action -> action.kind() == SourceModuleImport.ReviewKind.BEAN && action.subject().startsWith("renamedBean :"));
    }

    @Test void reviewRecognisesScannedDestinationAndStagedProvidersButKeepsMissingOrConditionalWiring() throws Exception {
        fixture();
        write(target(), "generated/src/main/java/studio/Application.java", """
                package studio;
                @org.springframework.boot.autoconfigure.SpringBootApplication
                @org.springframework.context.annotation.ComponentScan("org.customer") class Application {}
                """);
        String provider = "package org.customer; @org.springframework.stereotype.Component class Bespoke {}";
        write(source(), "src/main/java/org/customer/Bespoke.java", provider);
        Analysis analysis = analyse();
        String selected = "src/main/java/org/customer/Bespoke.java";
        assertThat(SourceModuleImport.reviewActions(analysis, Set.of(), Set.of())).anyMatch(action ->
                action.kind() == SourceModuleImport.ReviewKind.BEAN && action.subject().startsWith("renamedBean :"));
        assertThat(SourceModuleImport.reviewActions(analysis, Set.of(selected), Set.of())).noneMatch(action ->
                action.kind() == SourceModuleImport.ReviewKind.BEAN && action.subject().startsWith("renamedBean :"));
        assertThat(SourceModuleImport.reviewActions(analysis, Set.of(selected), Set.of(), Map.of(selected, "package org.customer; class Bespoke {}")))
                .anyMatch(action -> action.kind() == SourceModuleImport.ReviewKind.BEAN && action.subject().startsWith("renamedBean :"));
        assertThat(SourceModuleImport.reviewActions(analysis, Set.of(selected), Set.of(), Map.of(selected,
                "package org.customer; @org.springframework.context.annotation.Profile(\"dev\") @org.springframework.stereotype.Component class Bespoke {}")))
                .anyMatch(action -> action.kind() == SourceModuleImport.ReviewKind.BEAN && action.subject().startsWith("renamedBean :"));
        write(target(), "user/src/main/java/org/customer/Bespoke.java", provider);
        assertThat(SourceModuleImport.reviewActions(analyse(), Set.of(), Set.of())).noneMatch(action ->
                action.kind() == SourceModuleImport.ReviewKind.BEAN && action.subject().startsWith("renamedBean :"));
        write(target(), "generated/src/main/java/studio/Application.java", """
                package studio;
                @org.springframework.boot.autoconfigure.SpringBootApplication
                @org.springframework.context.annotation.ComponentScan("org.different") class Application {}
                """);
        Analysis unscanned = analyse();
        assertThat(SourceModuleImport.reviewActions(unscanned, Set.of(selected), Set.of())).anyMatch(action ->
                action.kind() == SourceModuleImport.ReviewKind.BEAN && action.subject().startsWith("renamedBean :"));
        assertThat(SourceModuleImport.reviewActions(unscanned, Set.of(selected), Set.of())).anyMatch(action ->
                action.kind() == SourceModuleImport.ReviewKind.SCAN && action.subject().contains("outside destination component scanning")
                        && action.subject().contains("org.customer.Bespoke"));
        assertThat(SourceModuleImport.report(unscanned, Set.of(selected), Set.of()))
                .contains("outside destination component scanning", "user-owned @Configuration");
        assertThat(SourceModuleImport.reviewActions(unscanned, Set.of(selected), Set.of(),
                Map.of(selected, "package org.customer; class Bespoke {}"))).noneMatch(action ->
                action.kind() == SourceModuleImport.ReviewKind.SCAN && action.subject().contains("outside destination component scanning"));
    }

    private void registrationFixture() throws Exception {
        fixture();
        write(target(), "generated/src/main/java/studio/Application.java", """
                package studio;
                @org.springframework.context.annotation.ComponentScan({"org.ikasan.studio.boot", "org.destination"})
                class Application {}
                """);
        write(source(), "src/main/java/org/customer/Bespoke.java",
                "package org.customer; @org.springframework.stereotype.Component public class Bespoke {}");
    }

    @Test void reviewedRegistrationIsCreatedWithTheCopiesAndResolvesFurtherReview() throws Exception {
        registrationFixture();
        Analysis analysis = analyse();
        Set<String> files = Set.of("src/main/java/org/customer/Bespoke.java");
        SpringRegistration registration = SourceModuleImport.previewSpringRegistration(analysis, files, Map.of());
        assertThat(registration).isNotNull();
        assertThat(registration.components()).containsExactly("org.customer.Bespoke");
        assertThat(registration.javaSource()).contains("@org.springframework.context.annotation.Import", "org.customer.Bespoke.class")
                .doesNotContain("org.customer.Other.class", "OriginalConfig.class");
        assertThat(Files.exists(target().resolve(registration.destination()))).isFalse();
        Path report = SourceModuleImport.apply(analysis, files, Set.of(), false, false, Map.of(), registration);
        assertThat(Files.readString(target().resolve(registration.destination()))).isEqualTo(registration.javaSource());
        assertThat(Files.readString(report)).contains("Reviewed Spring registration", registration.destination())
                .doesNotContain("Copied Spring provider is outside destination component scanning: org.customer.Bespoke");
        assertThat(SourceModuleImport.previewSpringRegistration(analyse(), Set.of(), Map.of())).isNull();
    }

    @Test void sourceCopyWithoutRegistrationApprovalDoesNotCreateWiring() throws Exception {
        registrationFixture();
        Analysis analysis = analyse();
        Set<String> files = Set.of("src/main/java/org/customer/Bespoke.java");
        SpringRegistration registration = SourceModuleImport.previewSpringRegistration(analysis, files, Map.of());
        assertNotNull(registration, "Expected a Spring registration preview for Bespoke");
        SourceModuleImport.apply(analysis, files, Set.of(), false);
        assertThat(Files.exists(target().resolve(registration.destination()))).isFalse();
    }

    @Test void registrationNeverOverwritesExistingUserFilesAndRejectsStaleOrForgedPlans() throws Exception {
        registrationFixture();
        String originalWiring = "user/src/main/java/org/ikasan/studio/boot/imported/ImportedSourceComponents.java";
        write(target(), originalWiring, "// Existing developer implementation\n");
        Analysis analysis = analyse();
        Set<String> files = Set.of("src/main/java/org/customer/Bespoke.java");
        SpringRegistration registration = SourceModuleImport.previewSpringRegistration(analysis, files, Map.of());
        assertNotNull(registration, "Expected a Spring registration preview for Bespoke");
        assertThat(registration.destination()).endsWith("ImportedSourceComponents2.java");
        assertThat(registration.javaSource()).contains("class ImportedSourceComponents2");
        SpringRegistration forged = new SourceModuleImport.SpringRegistration(registration.destination(), "arbitrary content", registration.components());
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, files, Set.of(), false, false, Map.of(), forged))
                .hasMessageContaining("registration review changed");
        write(target(), registration.destination(), "// Created while review was open\n");
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, files, Set.of(), false, false, Map.of(), registration))
                .hasMessageContaining("registration review changed");
        assertThat(Files.readString(target().resolve(originalWiring))).isEqualTo("// Existing developer implementation\n");
        assertThat(Files.readString(target().resolve(registration.destination()))).isEqualTo("// Created while review was open\n");
        assertThat(Files.exists(target().resolve("user/src/main/java/org/customer/Bespoke.java"))).isFalse();
    }

    @Test void registrationDoesNotInventExplicitAliasesOrAssumeCustomScanFilters() throws Exception {
        registrationFixture();
        write(target(), "generated/src/main/java/studio/ComponentFactory.java", """
                package studio;
                class ComponentFactory { @javax.annotation.Resource(name="wanted") org.customer.Bespoke component; }
                """);
        write(source(), "src/main/java/org/customer/Bespoke.java",
                "package org.customer; @org.springframework.stereotype.Component(\"different\") public class Bespoke {}");
        Set<String> files = Set.of("src/main/java/org/customer/Bespoke.java");
        assertThat(SourceModuleImport.previewSpringRegistration(analyse(), files, Map.of())).isNull();
        write(source(), "src/main/java/org/customer/Bespoke.java",
                "package org.customer; @org.springframework.stereotype.Component(\"wanted\") public class Bespoke {}");
        assertThat(SourceModuleImport.previewSpringRegistration(analyse(), files, Map.of())).isNotNull();
        write(target(), "generated/src/main/java/studio/Application.java", """
                package studio;
                @org.springframework.context.annotation.ComponentScan(basePackages="org.ikasan.studio.boot", useDefaultFilters=false)
                class Application {}
                """);
        assertThat(SourceModuleImport.previewSpringRegistration(analyse(), files, Map.of())).isNull();
    }

    @Test void registrationDoesNotCollapseDifferentBeanIdentitiesIntoOneComponent() throws Exception {
        registrationFixture();
        write(target(), "generated/src/main/java/studio/ComponentFactory.java", """
                package studio;
                class ComponentFactory {
                    @javax.annotation.Resource org.customer.Bespoke first;
                    @javax.annotation.Resource(name="second") org.customer.Bespoke second;
                }
                """);
        assertThat(SourceModuleImport.previewSpringRegistration(analyse(),
                Set.of("src/main/java/org/customer/Bespoke.java"), Map.of())).isNull();
    }

    @Test void registrationDoesNotConfuseScanDefaultNamesWithExplicitResourceNames() throws Exception {
        registrationFixture();
        Set<String> files = Set.of("src/main/java/org/customer/Bespoke.java");
        write(target(), "generated/src/main/java/studio/ComponentFactory.java", """
                package studio;
                class ComponentFactory { @javax.annotation.Resource(name="bespoke") org.customer.Bespoke component; }
                """);
        assertThat(SourceModuleImport.previewSpringRegistration(analyse(), files, Map.of())).isNull();
        write(target(), "generated/src/main/java/studio/ComponentFactory.java", """
                package studio;
                class ComponentFactory { @javax.annotation.Resource(name="org.customer.Bespoke") org.customer.Bespoke component; }
                """);
        assertThat(SourceModuleImport.previewSpringRegistration(analyse(), files, Map.of())).isNotNull();
    }

    @Test void registrationDoesNotInventBeansForPlainOrNonPublicClasses() throws Exception {
        registrationFixture();
        Set<String> files = Set.of("src/main/java/org/customer/Bespoke.java");
        write(source(), "src/main/java/org/customer/Bespoke.java", "package org.customer; public class Bespoke {}");
        assertThat(SourceModuleImport.previewSpringRegistration(analyse(), files, Map.of())).isNull();
        write(source(), "src/main/java/org/customer/Bespoke.java", "package org.customer; @org.springframework.stereotype.Component class Bespoke {}");
        assertThat(SourceModuleImport.previewSpringRegistration(analyse(), files, Map.of())).isNull();
    }

    @Test void explicitUserImportsResolveTheScanWarningAndAreProtectedAgainstStaleReview() throws Exception {
        fixture();
        write(target(), "generated/src/main/java/studio/Application.java", """
                package studio;
                @org.springframework.context.annotation.ComponentScan("org.different") class Application {}
                """);
        write(source(), "src/main/java/org/customer/Bespoke.java",
                "package org.customer; @org.springframework.stereotype.Component public class Bespoke {}");
        String wiring = "user/src/main/java/org/different/ImportedComponents.java";
        write(target(), wiring, """
                package org.different;
                @org.springframework.context.annotation.Configuration
                @org.springframework.context.annotation.Import(org.customer.Bespoke.class)
                public class ImportedComponents {}
                """);
        Analysis analysis = analyse();
        String selected = "src/main/java/org/customer/Bespoke.java";
        assertThat(SourceModuleImport.reviewActions(analysis, Set.of(selected), Set.of()))
                .noneMatch(action -> action.kind() == SourceModuleImport.ReviewKind.BEAN && action.subject().startsWith("renamedBean :"))
                .noneMatch(action -> action.kind() == SourceModuleImport.ReviewKind.SCAN && action.subject().contains("org.customer.Bespoke"));
        write(target(), wiring, "package org.different; public class ImportedComponents {}");
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of(selected), Set.of(), false))
                .hasMessageContaining("Spring wiring changed since review");
        write(target(), wiring, """
                package org.different;
                @org.springframework.context.annotation.Import(org.customer.Bespoke.class)
                public class ImportedComponents {}
                """);
        assertThat(SourceModuleImport.reviewActions(analyse(), Set.of(selected), Set.of())).anyMatch(action ->
                action.kind() == SourceModuleImport.ReviewKind.SCAN && action.subject().contains("org.customer.Bespoke"));
    }

    @Test void unchangedReferencedImplementationsAndSuppliedSupportHaveNoLiveReviewItems() throws Exception {
        supportFixture();
        Analysis analysis = analyse();
        List<FileChoice> unchangedFiles = analysis.files().stream().map(file -> new SourceModuleImport.FileChoice(file.source(), file.destination(),
                file.bytes(), file.bytes(), file.recommended(), file.wiring(), file.reason())).toList();
        Analysis quiet = new SourceModuleImport.Analysis(analysis.sourceRoot(), analysis.destinationRoot(), unchangedFiles, List.of(),
                analysis.properties(), List.of(), analysis.notes(), analysis.originalPom(), analysis.generatedInputs(),
                analysis.managedFiles(), analysis.supportInputs(), analysis.identity());
        assertThat(SourceModuleImport.reviewActions(quiet, Set.of(), Set.of())).isEmpty();
        String original = Files.readString(source().resolve(DEBUG)).replace("return payload;", "return null;");
        write(source(), DEBUG, original);
        analysis = analyse();
        assertThat(SourceModuleImport.reviewActions(analysis, Set.of(), Set.of()))
                .anyMatch(action -> action.kind() == SourceModuleImport.ReviewKind.SUPPORT_COMPARE);
        assertThat(SourceModuleImport.reviewActions(analysis, Set.of(DEBUG), Set.of()))
                .noneMatch(action -> action.kind() == SourceModuleImport.ReviewKind.SUPPORT_COMPARE);
    }

    @Test void transferPreservesOriginalBytesResourcesAndPackagesAndLeavesGeneratedAndOriginalUntouched() throws Exception {
        fixture(); Analysis analysis = analyse();
        byte[] before = Files.readAllBytes(source().resolve("src/main/java/org/customer/Bespoke.java"));
        String generated = Files.readString(target().resolve("generated/src/main/java/studio/ComponentFactory.java"));
        Set<String> files = new LinkedHashSet<>(); analysis.files().stream().filter(SourceModuleImport.FileChoice::recommended).forEach(f -> files.add(f.source()));
        Path report = SourceModuleImport.apply(analysis, files, Set.of(), false);
        assertThat(Files.readAllBytes(target().resolve("user/src/main/java/org/customer/Bespoke.java"))).isEqualTo(before);
        assertThat(Files.readAllBytes(source().resolve("src/main/java/org/customer/Bespoke.java"))).isEqualTo(before);
        assertThat(Files.readString(target().resolve("generated/src/main/java/studio/ComponentFactory.java"))).isEqualTo(generated);
        assertThat(target().resolve("user/src/main/resources/bespoke.properties")).exists();
        assertThat(target().resolve("user/src/main/java/org/customer/OriginalConfig.java")).doesNotExist();
        assertThat(report).exists();
    }

    @Test void dependenciesKeepPinnedVersionsAndExclusionsWithoutGuessingManagedOrPlatformVersions() throws Exception {
        fixture(); Analysis analysis = analyse();
        DependencyChoice custom = analysis.dependencies().stream().filter(d -> d.dependency().getArtifactId().equals("custom-client")).findFirst().orElseThrow();
        assertThat(custom.importable()).isTrue();
        assertThat(analysis.dependencies()).filteredOn(d -> d.dependency().getGroupId().equals("org.ikasan")).allMatch(d -> !d.importable());
        assertThat(analysis.dependencies()).filteredOn(d -> d.dependency().getArtifactId().equals("inherited")).allMatch(SourceModuleImport.DependencyChoice::importable);
        SourceModuleImport.apply(analysis, Set.of(), Set.of(custom.id()), false);
        Model model = new MavenXpp3Reader().read(new StringReader(Files.readString(target().resolve("pom.xml"))));
        assertThat(model.getDependencies()).hasSize(1);
        assertThat(model.getDependencies().get(0).getVersion()).isEqualTo("2.3.4");
        assertThat(model.getDependencies().get(0).getExclusions().get(0).getArtifactId()).isEqualTo("parser");
    }

    @Test void inheritedVersionsAndOptionalFlagsAreRetainedWithoutInventingVersions() throws Exception {
        fixture();
        String originalPom = Files.readString(source().resolve("pom.xml"));
        write(source(), "pom.xml", originalPom.replace("<artifactId>inherited</artifactId>", "<artifactId>inherited</artifactId><optional>true</optional><scope>runtime</scope>"));
        Analysis analysis = analyse();
        DependencyChoice inherited = analysis.dependencies().stream().filter(d -> d.dependency().getArtifactId().equals("inherited")).findFirst().orElseThrow();
        assertThat(inherited.importable()).isTrue();
        SourceModuleImport.apply(analysis, Set.of(), Set.of(inherited.id()), false);
        Model model = new MavenXpp3Reader().read(new StringReader(Files.readString(target().resolve("pom.xml"))));
        Dependency dependency = model.getDependencies().get(0);
        assertThat(dependency.getVersion()).isNull();
        assertThat(dependency.isOptional()).isTrue();
        assertThat(dependency.getScope()).isEqualTo("runtime");
    }

    @Test void directGeneratedConstructorReferencesAreRecognisedWithoutResourceFields() throws Exception {
        fixture();
        write(target(), "generated/src/main/java/studio/DirectFactory.java", "package studio; class DirectFactory { Object create() { return new org.customer.Unrelated(); } }");
        Analysis analysis = analyse();
        assertThat(analysis.files()).anySatisfy(file -> {
            assertThat(file.source()).endsWith("Unrelated.java");
            assertThat(file.recommended()).isTrue();
            assertThat(file.reason()).contains("Referenced by generated code");
        });
    }

    @Test void newImplementationsDefaultToImportButWiringDuplicatesAndGeneratedDebugSubclassesDoNot() throws Exception {
        fixture();
        write(source(), "src/main/java/org/customer/FlowDebug.java", "package org.customer; class FlowDebug extends org.ikasan.studio.component.DebugTransitionComponent { public void debug(Object value) {} }");
        Analysis analysis = analyse();
        assertThat(analysis.files()).anyMatch(f -> f.source().endsWith("Unrelated.java") && f.recommended() && f.previous() == null);
        assertThat(analysis.files()).anyMatch(f -> f.source().endsWith("FlowDebug.java") && !f.recommended());
        assertThat(analysis.files()).filteredOn(SourceModuleImport.FileChoice::wiring).noneMatch(SourceModuleImport.FileChoice::recommended);
    }

    @Test void dependencyComparisonIncludesUncheckedReviewOnlyDeclarationsWithoutApplyingThem() throws Exception {
        fixture(); Analysis analysis = analyse();
        for (DependencyChoice dependency : analysis.dependencies()) {
            DependencyComparison comparison = SourceModuleImport.compareDependency(analysis, dependency.id());
            assertThat(comparison.original()).contains("<artifactId>" + dependency.dependency().getArtifactId() + "</artifactId>");
            assertThat(comparison.destination()).isEmpty();
        }
        assertThat(target().resolve("user")).doesNotExist();
        assertThat(Files.readString(target().resolve("pom.xml"))).doesNotContain("custom-client");
    }

    @Test void existingUserCodeNeedsExplicitReplacementAndStaleSourceOrDestinationBlocksAllWrites() throws Exception {
        fixture();
        write(target(), "user/src/main/java/org/customer/Bespoke.java", "developer work");
        Analysis analysis = analyse();
        String bespoke = "src/main/java/org/customer/Bespoke.java";
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of(bespoke), Set.of(), false)).hasMessageContaining("Approve replacement");
        assertThat(Files.readString(target().resolve("user/src/main/java/org/customer/Bespoke.java"))).isEqualTo("developer work");
        write(target(), "user/src/main/java/org/customer/Bespoke.java", "new edits");
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of(bespoke), Set.of(), true)).hasMessageContaining("Destination changed");
        Analysis refreshed = analyse(); write(source(), bespoke, "changed source");
        assertThatThrownBy(() -> SourceModuleImport.apply(refreshed, Set.of(bespoke), Set.of(), true)).hasMessageContaining("Original source changed");
        assertThat(target().resolve("user/import-reviews")).doesNotExist();
    }

    @Test void ambiguousClassNamesAreNotAutomaticallyRecommendedAndDuplicatesAreRejected() throws Exception {
        fixture();
        write(source(), "another/src/main/java/org/customer/Bespoke.java", "package org.customer; public class Bespoke { }");
        Analysis analysis = analyse();
        assertThat(analysis.notes()).anyMatch(n -> n.contains("Ambiguous class"));
        assertThat(analysis.files()).filteredOn(f -> f.destination().endsWith("Bespoke.java")).noneMatch(SourceModuleImport.FileChoice::recommended);
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis,
                Set.of("another/src/main/java/org/customer/Bespoke.java", "src/main/java/org/customer/Bespoke.java"), Set.of(), false)).hasMessageContaining("Multiple sources");
        assertThat(target().resolve("user")).doesNotExist();
    }

    @Test void generatedContractChangesInvalidateReviewAndPartialWritesAreRolledBack() throws Exception {
        fixture(); Analysis analysis = analyse();
        String bespoke = "src/main/java/org/customer/Bespoke.java";
        write(target(), "generated/src/main/java/studio/NewFactory.java", "class NewFactory { }");
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of(bespoke), Set.of(), false)).hasMessageContaining("Generated code changed");
        Analysis refreshed = analyse();
        write(target(), "user/src/main/resources", "existing file blocks a resource directory");
        LinkedHashSet<String> selected = new LinkedHashSet<>(); selected.add(bespoke); selected.add("src/main/resources/bespoke.properties");
        assertThatThrownBy(() -> SourceModuleImport.apply(refreshed, selected, Set.of(), false)).isInstanceOf(java.io.IOException.class);
        assertThat(target().resolve("user/src/main/java/org/customer/Bespoke.java")).doesNotExist();
        assertThat(Files.readString(target().resolve("user/src/main/resources"))).isEqualTo("existing file blocks a resource directory");
        assertThat(target().resolve("user/import-reviews")).doesNotExist();
    }

    private static final String DEBUG = "user/src/main/java/org/ikasan/studio/component/DebugTransitionComponent.java";
    private static final String COPY = "user/src/main/java/org/ikasan/studio/component/utils/DeepCopyUtil.java";
    private static final String MODEL = "generated/src/main/model/model.json";

    private void supportFixture() throws Exception { supportFixture("V4.1.6"); }

    private void supportFixture(String pack) throws Exception {
        fixture();
        Module module = org.ikasan.studio.core.model.ikasan.instance.Module.moduleBuilder().version(pack)
                .name("Support example").applicationPackageName("org.example").port("8080")
                .h2PortNumber("8092").h2WebPortNumber("8093").flows(List.of()).build();
        write(target(), MODEL, org.ikasan.studio.core.generator.ModelTemplate.create(module));
        write(source(), "src/main/resources/application.properties", "module.name=Support example\n");
        String debug = org.ikasan.studio.core.generator.DebugTransitionComponentTemplate.create(module);
        String copy = org.ikasan.studio.core.generator.DeepCopyUtilTemplate.create(module);
        write(target(), DEBUG, debug); write(target(), COPY, copy);
        write(source(), DEBUG, debug); write(source(), COPY, copy);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"V3.3.9", "V4.1.6", "V5.1.0"})
    void supportVerificationUsesTheSavedPackAndIgnoresFormattingAndComments(String pack) throws Exception {
        supportFixture(pack);
        String original = "// Original checkout header\n" + Files.readString(source().resolve(DEBUG)).replace("    ", "\t");
        write(source(), DEBUG, original.replace("\n", "\r\n"));
        assertThat(analyse().managedFiles()).hasSize(2).allMatch(file -> file.status() == SourceModuleImport.SupportStatus.SUPPLIED)
                .anyMatch(file -> file.source().equals(DEBUG) && !file.identicalToDestination());
    }

    @Test void matchingSupportWithShortenedImportsIsIdenticalEvenWhenItDiffersFromTheRawTemplate() throws Exception {
        supportFixture();
        String original = Files.readString(source().resolve(COPY));
        String shortened = original.replace("package org.ikasan.studio.component.utils;", "package org.ikasan.studio.component.utils;\nimport java.io.Serializable;")
                .replace("java.io.Serializable", "Serializable");
        // Keep the import qualified while shortening the implementation reference.
        shortened = shortened.replace("import Serializable;", "import java.io.Serializable;");
        write(source(), COPY, shortened); write(target(), COPY, shortened);
        ManagedFile file = analyse().managedFiles().stream().filter(support -> support.source().equals(COPY)).findFirst().orElseThrow();
        assertThat(file.status()).isEqualTo(SourceModuleImport.SupportStatus.DESTINATION_DIFFERS);
        assertThat(file.identicalToDestination()).isTrue();
    }

    @Test void supportClassesAreSuppliedSeparatelyWhileCustomerNamesAndReferencesRemainImportable() throws Exception {
        supportFixture();
        write(source(), "src/main/java/org/customer/DeepCopyUtil.java", "package org.customer; public class DeepCopyUtil { }");
        write(source(), "src/main/java/org/customer/Bespoke.java", "package org.customer; public class Bespoke extends org.ikasan.studio.component.DebugTransitionComponent { Helper helper; }");
        Analysis analysis = analyse();
        assertThat(analysis.managedFiles()).hasSize(2).allMatch(f -> f.status() == SourceModuleImport.SupportStatus.SUPPLIED);
        assertThat(analysis.files()).noneMatch(f -> f.destination().equals(DEBUG) || f.destination().equals(COPY));
        assertThat(analysis.files()).anyMatch(f -> f.destination().equals("user/src/main/java/org/customer/DeepCopyUtil.java"));
        assertThat(analysis.files()).anyMatch(f -> f.source().endsWith("Helper.java") && f.recommended());
        String destination = Files.readString(target().resolve(DEBUG));
        Path report = SourceModuleImport.apply(analysis, Set.of("src/main/java/org/customer/Bespoke.java"), Set.of(), false);
        assertThat(Files.readString(target().resolve(DEBUG))).isEqualTo(destination);
        assertThat(Files.readString(report)).contains("Studio support files", "Supplied by Studio");
    }

    @Test void explicitSupportMergesRequireApprovalAndSurviveRegeneration() throws Exception {
        supportFixture();
        String destination = Files.readString(target().resolve(DEBUG));
        write(source(), DEBUG, "// original-only debug notes\n" + Files.readString(source().resolve(DEBUG)));
        Analysis analysis = analyse();
        String merged = "// reviewed debug note\n" + destination;
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of(DEBUG), Set.of(), false, false, Map.of(DEBUG, merged)))
                .hasMessageContaining("Approve replacement");
        assertThat(Files.exists(target().resolve(SourceModuleImport.SUPPORT_CUSTOMISATIONS))).isFalse();
        Path report = SourceModuleImport.apply(analysis, Set.of(DEBUG), Set.of(), true, false, Map.of(DEBUG, merged));
        assertThat(Files.readString(target().resolve(DEBUG))).isEqualTo(merged).doesNotContain("original-only");
        assertThat(Files.readString(source().resolve(DEBUG))).startsWith("// original-only");
        assertThat(SourceModuleImport.preserveSupportCustomisation(target(), "org.ikasan.studio.component", "DebugTransitionComponent")).isTrue();
        assertThat(SourceModuleImport.preserveSupportCustomisation(target(), "org.ikasan.studio.component.utils", "DeepCopyUtil")).isFalse();
        assertThat(Files.readString(report)).contains("CUSTOMISATION SELECTED", "Staged Java merges applied");
        Analysis next = analyse();
        Files.writeString(target().resolve(SourceModuleImport.SUPPORT_CUSTOMISATIONS), "# changed after review\n");
        assertThatThrownBy(() -> SourceModuleImport.apply(next, Set.of(DEBUG), Set.of(), true))
                .hasMessageContaining("Destination model changed since support review");
        assertThat(Files.readString(target().resolve(DEBUG))).isEqualTo(merged);
    }

    @Test void explicitWholeSupportCopyRetainsSourceBytesAndChecksStaleSources() throws Exception {
        supportFixture();
        String original = "// useful debug note\r\n" + Files.readString(source().resolve(DEBUG));
        write(source(), DEBUG, original);
        Analysis analysis = analyse();
        SourceModuleImport.apply(analysis, Set.of(DEBUG), Set.of(), true);
        assertThat(Files.readString(target().resolve(DEBUG))).isEqualTo(original);
        Analysis next = analyse();
        write(source(), DEBUG, original + "// changed later\n");
        assertThatThrownBy(() -> SourceModuleImport.apply(next, Set.of(DEBUG), Set.of(), true))
                .hasMessageContaining("Studio support file changed since review");
        assertThat(Files.readString(target().resolve(DEBUG))).isEqualTo(original);
    }

    @Test void differingOriginalsAreFlaggedWithoutReplacingGeneratedSupport() throws Exception {
        supportFixture();
        String original = Files.readString(source().resolve(DEBUG)).replace("return payload;", "return null;");
        write(source(), DEBUG, original);
        Analysis analysis = analyse();
        assertThat(analysis.managedFiles()).anyMatch(f -> f.source().equals(DEBUG) && f.status() == SourceModuleImport.SupportStatus.ORIGINAL_DIFFERS);
        SourceModuleImport.apply(analysis, Set.of(), Set.of(), true);
        assertThat(Files.readString(target().resolve(DEBUG))).contains("return payload;").doesNotContain("return null;");
        assertThat(Files.readString(source().resolve(DEBUG))).isEqualTo(original);
    }

    @Test void absentUnverifiedOrLocallyChangedDestinationSupportIsNotReportedAsSupplied() throws Exception {
        supportFixture();
        String generated = Files.readString(target().resolve(DEBUG));
        Files.delete(target().resolve(DEBUG));
        assertThat(analyse().managedFiles()).anyMatch(f -> f.source().equals(DEBUG) && f.status() == SourceModuleImport.SupportStatus.DESTINATION_MISSING);
        write(target(), DEBUG, generated.replace("return payload;", "return null;"));
        assertThat(analyse().managedFiles()).anyMatch(f -> f.source().equals(DEBUG) && f.status() == SourceModuleImport.SupportStatus.DESTINATION_DIFFERS);
        Files.delete(target().resolve(MODEL));
        assertThat(analyse().managedFiles()).anyMatch(f -> f.source().equals(DEBUG) && f.status() == SourceModuleImport.SupportStatus.UNVERIFIED);
    }

    @Test void sourceSupportDestinationSupportAndSavedModelChangesInvalidateReviewBeforeWrites() throws Exception {
        supportFixture();
        for (Path path : List.of(source().resolve(DEBUG), target().resolve(COPY), target().resolve(MODEL))) {
            Analysis analysis = analyse(); String before = Files.readString(path);
            Files.writeString(path, before + "\n");
            assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of("src/main/java/org/customer/Bespoke.java"), Set.of(), false))
                    .hasMessageContaining("changed since");
            assertThat(target().resolve("user/src/main/java/org/customer/Bespoke.java")).doesNotExist();
            assertThat(target().resolve("user/import-reviews")).doesNotExist();
            Files.writeString(path, before);
        }
    }

    @Test void rejectsDestinationSymlinksAndHonoursCancellation() throws Exception {
        fixture(); Analysis analysis = analyse();
        Files.createSymbolicLink(target().resolve("user"), source());
        assertThatThrownBy(() -> SourceModuleImport.apply(analysis, Set.of("src/main/java/org/customer/Bespoke.java"), Set.of(), false)).hasMessageContaining("symbolic links");
        assertThatThrownBy(() -> SourceModuleImport.analyse(source(), target(), () -> { throw new IllegalStateException("cancelled"); })).hasMessage("cancelled");
    }
}
