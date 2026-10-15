package org.ikasan.studio.packs;

import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Live business-component scenarios, kept separate from catalogue compilation and external transports. */
class ProcessingFixtureTest {
    @TempDir Path temporary;
    private static final String PACK = DevelopmentFixtureTest.PACK;
    private static final String PACKAGE = "org.example.processing.processing";

    @Test void exportsProcessingScenarios() throws Exception {
        Path root = Path.of(System.getProperty("studio.v5.processing", temporary.toString()));
        var scenarios = List.of("single-router", "multi-router", "default-components", "xml");
        for (String scenario : scenarios) {
            for (var file : render(scenario).entrySet()) {
                Path target = root.resolve(scenario).resolve(file.getKey());
                Files.createDirectories(target.getParent()); Files.writeString(target, file.getValue());
            }
        }
        Files.writeString(root.resolve("pom.xml"), "<project><modelVersion>4.0.0</modelVersion>"
                + "<groupId>org.ikasan.studio.probe</groupId><artifactId>processing</artifactId>"
                + "<version>1</version><packaging>pom</packaging><modules>"
                + scenarios.stream().map(s -> "<module>" + s + "</module>").reduce("", String::concat)
                + "</modules></project>");
    }

    private Map<String,String> render(String scenario) throws Exception {
        boolean xml = scenario.equals("xml"), multi = scenario.equals("multi-router"), defaults = scenario.equals("default-components");
        var consumer = component("Generic Consumer", "Input", "InputFeed");
        var flow = Flow.flowBuilder().metapackVersion(PACK).name("Processing").consumer(consumer).build();
        var elements = new ArrayList<FlowElement>();
        if (xml) {
            elements.add(component("Converter", "Create order", "CreateOrder"));
            var converter = DevelopmentFixtureTest.element("Object To XML String Converter", "XML");
            converter.setPropertyValue("objectClass", PACKAGE + ".Order");
            elements.add(converter);
            elements.add(DevelopmentFixtureTest.element("Logging Producer", "Output"));
        } else {
            elements.add(component("Broker", "Lookup", "Lookup"));
            elements.add(component("Translator", "Enrich", "Enrich"));
            elements.add(component(defaults ? "Default Message Filter" : "Message Filter", "Gate", "Gate"));
            elements.add(defaults ? DevelopmentFixtureTest.element("Default List Splitter", "Split") : component("Splitter", "Split", "Split"));
            var router = component(multi ? "Multi Recipient Router" : "Single Recipient Router", "Route", "Route");
            router.setPropertyValue("routeNames", List.of("left", "right"));
            elements.add(router);
        }
        var route = FlowRoute.flowRouteBuilder().flow(flow).flowElements(elements).build();
        if (!xml) route.setChildRoutes(List.of(
                FlowRoute.flowRouteBuilder().flow(flow).routeName("left").flowElements(new ArrayList<>(List.of(
                        DevelopmentFixtureTest.element("Dev Null Producer", "Left")))).build(),
                FlowRoute.flowRouteBuilder().flow(flow).routeName("right").flowElements(new ArrayList<>(List.of(
                        defaults ? component("Generic Producer", "Right", "Sink") : DevelopmentFixtureTest.element("Logging Producer", "Right")))).build()));
        flow.setFlowRoute(route);
        var module = Module.moduleBuilder().version(PACK).name("ProcessingProbe")
                .applicationPackageName("org.example.processing").port("8080").h2PortNumber("8092")
                .h2WebPortNumber("8093").flows(List.of(flow)).build();
        module.setPropertyValue("flowStartupType", "MANUAL");
        var files = DevelopmentFixtureTest.render(module);
        files.put("pom.xml", files.get("pom.xml").replace("<artifactId>ikasan5-review</artifactId>", "<artifactId>processing-" + scenario + "</artifactId>"));
        DevelopmentFixtureTest.putJava(files, PACKAGE, "InputFeed", FlowsUserImplementedComponentTemplate.create(PACKAGE, module, flow, consumer));
        if (xml) {
            implementation(files, "CreateOrder", "org.ikasan.spec.component.transformation.Converter<String, Order>",
                    "public Order convert(String input) { var order = new Order(); order.reference = input; order.quantity = 3; return order; }");
            DevelopmentFixtureTest.putJava(files, PACKAGE, "Order", "package " + PACKAGE + ";\n"
                    + "@jakarta.xml.bind.annotation.XmlRootElement(name=\"order\")\n"
                    + "@jakarta.xml.bind.annotation.XmlAccessorType(jakarta.xml.bind.annotation.XmlAccessType.FIELD)\n"
                    + "public class Order { public String reference; public int quantity; }");
        } else {
            implementation(files, "Lookup", "org.ikasan.spec.component.endpoint.Broker<String, java.util.List<String>>",
                    "public java.util.List<String> invoke(String input) { return new java.util.ArrayList<>(java.util.List.of(input)); }");
            implementation(files, "Enrich", "org.ikasan.spec.component.transformation.Translator<java.util.List<String>>",
                    "public void translate(java.util.List<String> input) { input.add(\"translated\"); }");
            implementation(files, "Gate", defaults ? "org.ikasan.spec.component.filter.FilterRule<java.util.List<String>>" : "org.ikasan.spec.component.filter.Filter<java.util.List<String>>",
                    defaults ? "public boolean accept(java.util.List<String> input) { return !input.get(0).equals(\"drop\"); }"
                            : "public java.util.List<String> filter(java.util.List<String> input) { return input.get(0).equals(\"drop\") ? null : input; }");
            if (!defaults) implementation(files, "Split", "org.ikasan.spec.component.splitting.Splitter<java.util.List<String>, String>",
                    "public java.util.List<String> split(java.util.List<String> input) { String prefix = String.join(\":\", input); return java.util.List.of(prefix + \":one\", prefix + \":two\"); }");
            implementation(files, "Route", "org.ikasan.spec.component.routing." + (multi ? "MultiRecipientRouter" : "SingleRecipientRouter") + "<String>",
                    multi ? "public java.util.List<String> route(String input) { return java.util.List.of(\"left\", \"right\"); }"
                            : defaults ? "public String route(String input) { return input.equals(\"translated\") ? \"right\" : \"left\"; }"
                            : "public String route(String input) { return input.endsWith(\":one\") ? \"left\" : \"right\"; }");
        }
        if (defaults) implementation(files, "Sink", "org.ikasan.spec.component.endpoint.Producer<String>",
                "public final java.util.List<String> received = new java.util.ArrayList<>(); public void invoke(String input) { received.add(input); }");
        files.keySet().removeIf(p -> p.startsWith("src/test/java/org/ikasan/studio/flowtests/") && !p.contains("/support/"));
        files.put("src/test/java/org/ikasan/studio/flowtests/ProcessingTest.java", resource(xml ? "xml.java.txt" : "routing.java.txt")
                .replace("@MULTI@", Boolean.toString(multi)).replace("@DEFAULT@", Boolean.toString(defaults))
                .replace("@RECEIVER_CHECK@", defaults ? ", (context, number, batch) -> org.junit.Assert.assertEquals(number < 3 ? List.of(\"translated\") : List.of(\"translated\", \"translated\"), context.getBean(" + PACKAGE + ".Sink.class).received)" : ""));
        return files;
    }

    private FlowElement component(String kind, String name, String implementation) throws Exception {
        var component = DevelopmentFixtureTest.element(kind, name);
        component.setPropertyValue("userImplementedClassName", implementation);
        return component;
    }
    private void implementation(Map<String,String> files, String name, String contract, String body) {
        DevelopmentFixtureTest.putJava(files, PACKAGE, name, "package " + PACKAGE + ";\n"
                + "@org.springframework.stereotype.Component(\"" + PACKAGE + "." + name + "\")\n"
                + "public class " + name + " implements " + contract + " { " + body + " }\n");
    }
    private String resource(String name) throws Exception {
        try (var input = Objects.requireNonNull(getClass().getResourceAsStream("/processing/" + name))) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
