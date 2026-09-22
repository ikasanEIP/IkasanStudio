package org.ikasan.studio.testing.packs;

import org.ikasan.studio.SharedResourceExtension;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.ai.ImplementationReadiness;
import org.ikasan.studio.core.generator.FlowsUserImplementedComponentTemplate;
import org.ikasan.studio.core.generator.GeneratorUtils;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The readiness checker recognises unfinished scaffolds by their wording, so it silently misses any stub whose marker it
 * does not know. The generated Generic Consumer (demo poller), Translator and Email Converter stubs were all missed.
 * This generates every real stub in each pack, so a template that adds a marker the checker cannot see fails here.
 */
@ExtendWith(SharedResourceExtension.class)
@org.junit.jupiter.api.Tag("packs")
class ImplementationReadinessStubsTest {
    @TempDir Path root;

    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void everyGeneratedStubThatContainsATodoMarkerIsReported(String pack) throws Exception {
        Module live = TestFixtures.getMyFirstModuleIkasanModule(pack, new ArrayList<>());
        var all = ComponentLibrary.getIkasanComponents(pack);
        List<String> withMarker = new ArrayList<>();
        for (var entry : new TreeMap<>(all).entrySet()) {
            var meta = entry.getValue();
            if (!meta.isGeneratesUserImplementedClass() || meta.isModule() || meta.isFlow() || meta.isEndpoint()
                    || meta.isRouter() || meta.isExceptionResolver()) continue;
            Flow flow = new Flow(pack);
            flow.setName("Flow" + entry.getKey().replaceAll("[^A-Za-z0-9]", ""));
            live.getFlows().add(flow);
            var consumerMeta = meta.isConsumer() ? meta : all.get("Event Generating Consumer");
            FlowElement consumer = FlowElementFactory.createFlowElement(pack, consumerMeta, flow, flow.getFlowRoute(), meta.isConsumer() ? "Subject" : "Start");
            consumer.defaultUnsetMandatoryProperties();
            StudioBuildUtils.substituteAllPlaceholderInPascalCase(live, flow, consumer);
            flow.setConsumer(consumer);
            if (!meta.isConsumer()) {
                FlowElement element = FlowElementFactory.createFlowElement(pack, meta, flow, flow.getFlowRoute(), "Subject");
                element.defaultUnsetMandatoryProperties();
                StudioBuildUtils.substituteAllPlaceholderInPascalCase(live, flow, element);
                flow.getFlowRoute().insertFlowElement(0, element);
            }
        }
        Path model = root.resolve("generated/src/main/model/model.json");
        Files.createDirectories(model.getParent());
        Files.writeString(model, ComponentIO.toValidatedModuleJson(live));
        for (Flow flow : live.getFlows()) {
            String userPackage = GeneratorUtils.getUserImplementedClassesPackageName(live, flow);
            for (FlowElement component : flow.getFlowElementsNoExternalEndPoints()) {
                if (!(component instanceof FlowUserImplementedElement)) continue;
                String className = (String) component.getProperty("userImplementedClassName").getValue();
                String source = FlowsUserImplementedComponentTemplate.create(userPackage, live, flow, component);
                Path file = root.resolve("user/src/main/java").resolve(userPackage.replace('.', '/')).resolve(className + ".java");
                Files.createDirectories(file.getParent());
                Files.writeString(file, source);
                if (source.lines().anyMatch(line -> line.strip().matches("//\\s*@?TODO\\b.*"))) withMarker.add(flow.getIdentity());
            }
        }
        var flagged = ImplementationReadiness.scanProject(root).findings().stream()
                .map(ImplementationReadiness.Finding::flow).distinct().toList();

        assertThat(withMarker).as(pack + ": the stubs are expected to contain scaffold markers").isNotEmpty();
        assertThat(flagged).as(pack + ": every stub with a TODO marker must be reported").containsAll(withMarker);
    }
}
