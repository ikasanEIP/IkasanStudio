package org.ikasan.studio.packs;

import java.nio.file.*;
import java.util.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Map;
import java.util.TreeSet;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.ikasan.studio.core.metapack.model.ExceptionActionMeta;
import org.ikasan.studio.core.metapack.model.ExceptionResolverMeta;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.instance.ExceptionResolution;
import org.ikasan.studio.core.model.ikasan.instance.ExceptionResolver;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowRoute;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/** Compile-only catalogue fixture. Endpoint wiring and placeholder business stubs are not runtime scenarios. */
class ComponentCoverageTest {
    @TempDir Path temporary;
    private static final String PACK = DevelopmentFixtureTest.PACK;
    private static final List<String> COMPONENTS = List.of(
            "Event Generating Consumer", "Generic Consumer", "Scheduled Consumer", "Local File Consumer",
            "Spring JMS Consumer", "FTP Consumer", "SFTP Consumer",
            "Dev Null Producer", "Generic Producer", "Logging Producer", "Spring JMS Producer",
            "Email Producer", "FTP Producer", "SFTP Producer",
            "Converter", "JMS Map Message To Payload Converter", "Payload To Map Converter", "Email Converter", "JMS Object Message To Object Converter", "Object To XML String Converter",
            "Broker", "Translator", "Message Filter", "Default Message Filter", "Splitter", "Default List Splitter",
            "Single Recipient Router", "Multi Recipient Router", "Debug Transition");

    @Test void coversEveryRuntimePaletteComponentAndExportsCompilableSources() throws Exception {
        TreeSet<String> catalogue = new TreeSet<>();
        for (ComponentMeta meta : ComponentLibrary.getIkasanComponents(PACK).values()) {
            if (!meta.isEndpoint() && !meta.isModule() && !meta.isFlow() && !meta.isExceptionResolver()) catalogue.add(meta.getName());
        }
        assertEquals(new TreeSet<>(COMPONENTS), catalogue, "New palette components need an explicit compilation fixture");
        ArrayList<Flow> flows = new ArrayList<>();
        for (String key : COMPONENTS) flows.add(flow(key));
        Module module = Module.moduleBuilder().version(PACK).name("V5 Component Coverage")
                .applicationPackageName("org.example.coverage").port("8080").h2PortNumber("8092")
                .h2WebPortNumber("8093").flows(flows).build();
        module.setPropertyValue("flowStartupType", "MANUAL");
        Map<String,String> files = DevelopmentFixtureTest.render(module);
        for (Flow flow : flows) {
            String pkg = GeneratorUtils.getUserImplementedClassesPackageName(module, flow);
            for (FlowElement component : flow.getFlowElementsNoExternalEndPoints()) {
                Object value = component.getPropertyValue("userImplementedClassName");
                if (value != null) {
                    String name = StudioBuildUtils.substitutePlaceholderInPascalCase(module, flow, component, value.toString());
                    DevelopmentFixtureTest.putJava(files, pkg, name,
                            FlowsUserImplementedComponentTemplate.create(pkg, module, flow, component));
                }
            }
        }
        DevelopmentFixtureTest.putJava(files, DebugTransitionComponentTemplate.DEBUG_TRANSITION_COMPONENT_PACKAGE,
                DebugTransitionComponentTemplate.DEBUG_TRANSITION_COMPONENT_CLASS_NAME, DebugTransitionComponentTemplate.create(module));
        DevelopmentFixtureTest.putJava(files, DeepCopyUtilTemplate.DEEP_COPY_UTIL_PACKAGE,
                DeepCopyUtilTemplate.DEEP_COPY_UTIL_CLASS_NAME, DeepCopyUtilTemplate.create(module));
        // Business TODOs and endpoint placeholders are intentional: compile these tests, never run them by default.
        files.put("pom.xml", files.get("pom.xml").replace("<properties>", "<properties><skipTests>true</skipTests>"));
        files.put("COVERAGE.txt", "COMPILE-ONLY: not runtime coverage; optional setters are not exhaustive.\n"
                + String.join("\n", COMPONENTS) + "\nModule\nFlow\nException Resolver (all five declared actions)\n");
        Path root = Path.of(System.getProperty("studio.v5.components", temporary.toString()));
        for (Entry<String,String> file : files.entrySet()) {
            Path target = root.resolve(file.getKey()); Files.createDirectories(target.getParent());
            Files.writeString(target, file.getValue());
        }
        assertEquals(29, flows.size());
        assertEquals(29, files.keySet().stream().filter(p -> p.startsWith("src/test/java/org/ikasan/studio/flowtests/")
                && !p.contains("/support/")).count());
    }

    private static Flow flow(String key) throws Exception {
        FlowElement target = DevelopmentFixtureTest.element(key, "Subject");
        ComponentMeta meta = target.getComponentMeta();
        configure(target, key);
        FlowElement consumer = meta.isConsumer() ? target : DevelopmentFixtureTest.element("Event Generating Consumer", "Input");
        Flow flow = Flow.flowBuilder().metapackVersion(PACK).name(key + " Coverage").consumer(consumer).build();
        ArrayList<FlowElement> elements = new ArrayList<>();
        if (!meta.isConsumer()) elements.add(target);
        if (!meta.isProducer() && !meta.isRouter()) elements.add(DevelopmentFixtureTest.element("Dev Null Producer", "Output"));
        FlowRoute route = FlowRoute.flowRouteBuilder().flow(flow).flowElements(elements).build();
        if (meta.isRouter()) {
            target.setPropertyValue("routeNames", List.of("route1", "route2"));
            ArrayList<FlowRoute> children = new ArrayList<>();
            for (int i = 1; i <= 2; i++) children.add(FlowRoute.flowRouteBuilder().flow(flow).routeName("route" + i)
                    .flowElements(new ArrayList<>(List.of(DevelopmentFixtureTest.element("Dev Null Producer", "Output " + i)))).build());
            route.setChildRoutes(children);
        }
        flow.setFlowRoute(route);
        // Explicit Jakarta exception wiring is part of the compiled factory/flow surface.
        ExceptionResolverMeta resolverMeta = (ExceptionResolverMeta)
                ComponentLibrary.getIkasanComponentByKeyMandatory(PACK, "Exception Resolver");
        LinkedHashMap<String,ExceptionResolution> resolutions = new LinkedHashMap<>();
        int index = 0;
        for (ExceptionActionMeta action : resolverMeta.getActionList()) {
            LinkedHashMap<String,ComponentProperty> properties = new LinkedHashMap<>();
            for (Entry<String,ComponentPropertyMeta> entry : action.getActionProperties().entrySet()) {
                Object value = entry.getValue().getDefaultValue();
                properties.put(entry.getKey(), new ComponentProperty(entry.getValue(), value == null ? 3 : value));
            }
            String exception = resolverMeta.getExceptionsCaught().get(index++);
            resolutions.put(exception, ExceptionResolution.exceptionResolutionBuilder().metapackVersion(PACK)
                    .exceptionsCaught(exception).theAction(action.getActionName()).componentProperties(properties).build());
        }
        ExceptionResolver resolver = ExceptionResolver.exceptionResolverBuilder().metapackVersion(PACK)
                .ikasanExceptionResolutionMap(resolutions).build();
        flow.setExceptionResolver(resolver);
        return flow;
    }

    private static void configure(FlowElement target, String key) {
        // The canvas resolves class-name defaults while inserting components. This headless fixture
        // supplies the same explicit user choice instead of persisting UI placeholder expressions.
        if (target.getProperty("userImplementedClassName") != null)
            target.setPropertyValue("userImplementedClassName", "SubjectImplementation");
        switch (key) {
            case "Spring JMS Consumer", "Spring JMS Producer" -> target.setPropertyValue("destinationJndiName", "dynamicQueues/coverage");
            case "SFTP Consumer" -> {
                target.setPropertyValue("sourceDirectory", "/input"); target.setPropertyValue("filenamePattern", ".*\\.txt");
                target.setPropertyValue("username", "test");
            }
            case "SFTP Producer" -> target.setPropertyValue("username", "test");
            case "Object To XML String Converter" -> target.setPropertyValue("objectClass", "java.lang.String");
            case "Local File Consumer" -> target.setPropertyValue("filenames", "/tmp/studio-v5-compile-only/.*\\.txt");
            case "Email Producer" -> {
                target.setPropertyValue("toRecipients", List.of("one@example.test", "two@example.test"));
                target.setPropertyValue("extendedMailSessionProperties", Map.of("mail.smtp.host", "127.0.0.1"));
            }
            default -> { }
        }
    }
}
