package org.ikasan.studio.core.persistence.json;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowRoute;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;

class FlowClipboardTest {
    private Flow copy(Flow flow) throws Exception {
        return FlowClipboard.decode(FlowClipboard.encode(FlowClipboard.capture(flow, BASE_META_PACK)), BASE_META_PACK);
    }

    @Test void filenameListsSurviveSavingReloadingAndCopyingWithoutDisplayBrackets() throws Exception {
        Flow source = TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerWithWiretapsFlow(BASE_META_PACK);
        var consumer = TestFixtures.getLocalFileConsumer(BASE_META_PACK);
        consumer.setComponentName(source.getConsumer().getIdentity());
        source.setConsumer(consumer);
        String expected = "myFile\\.txt,anotherFile\\.txt,[a-z]+[.]csv";
        consumer.setPropertyValue("filenames", List.of("myFile\\.txt", "anotherFile\\.txt", "[a-z]+[.]csv"));
        Module module = TestFixtures.getMyFirstModuleIkasanModule(BASE_META_PACK, new ArrayList<>(List.of(source)));
        for (int i = 0; i < 3; i++) {
            String json = org.ikasan.studio.core.io.ComponentIO.toJson(module);
            var tree = StudioJson.newObjectMapper().readTree(json);
            assertEquals(expected, tree.get("flows").get(0).get("consumer").get("filenames").asText());
            module = org.ikasan.studio.core.io.ComponentIO.validatePersistedModuleJson(json, "filename round trip", false);
            assertEquals(expected, module.getFlows().get(0).getConsumer().getPropertyValueAsString("filenames"));
        }
        Flow pasted = copy(source);
        assertEquals(expected, pasted.getConsumer().getPropertyValueAsString("filenames"));
        assertEquals(FlowClipboard.capture(source, BASE_META_PACK), FlowClipboard.capture(pasted, BASE_META_PACK));
    }

    @Test void preservesBranchesAndRebindsEveryParent() throws Exception {
        Flow source = TestFixtures.getEventGeneratingConsumerRouterFlow(BASE_META_PACK);
        source.setExceptionResolver(TestFixtures.getExceptionResolver(BASE_META_PACK));
        source.getExceptionResolver().getExceptionResolutionList().forEach(resolution -> {
            if ("ignore".equals(resolution.getTheAction())) resolution.setTheAction("ignoreException");
        });
        Flow pasted = copy(source);
        assertEquals(FlowClipboard.capture(source, BASE_META_PACK), FlowClipboard.capture(pasted, BASE_META_PACK));
        assertNotSame(source, pasted);
        assertSame(pasted, pasted.getConsumer().getContainingFlow());
        assertSame(pasted.getFlowRoute(), pasted.getConsumer().getContainingFlowRoute());
        assertSame(pasted, pasted.getExceptionResolver().getContainingFlow());
        assertParents(pasted, pasted.getFlowRoute());
        pasted.getFlowRoute().getChildRoutes().get(0).getFlowElements().get(0).setComponentName("Changed");
        pasted.getExceptionResolver().getIkasanExceptionResolutionMap().clear();
        assertNotEquals("Changed", source.getFlowRoute().getChildRoutes().get(0).getFlowElements().get(0).getIdentity());
        assertFalse(source.getExceptionResolver().getIkasanExceptionResolutionMap().isEmpty());
    }

    @Test void snapshotAndRepeatedPastesAreIndependentIncludingDecorators() throws Exception {
        Flow source = TestFixtures.getEventGeneratingConsumerCustomConverterDevNullProducerWithWiretapsFlow(BASE_META_PACK);
        var snapshot = FlowClipboard.capture(source, BASE_META_PACK);
        String originalName = source.getConsumer().getIdentity();
        source.getConsumer().setComponentName("Source edited after copy");
        String clipboard = FlowClipboard.encode(snapshot);
        Flow first = FlowClipboard.decode(clipboard, BASE_META_PACK);
        Flow second = FlowClipboard.decode(clipboard, BASE_META_PACK);
        assertEquals(originalName, first.getConsumer().getIdentity());
        first.getConsumer().setComponentName("Pasted edit");
        assertEquals(originalName, second.getConsumer().getIdentity());
        assertEquals(snapshot, FlowClipboard.capture(second, BASE_META_PACK));
        var firstDecorated = first.getFlowRoute().getFlowElements().stream()
                .filter(element -> element.getDecorators() != null && !element.getDecorators().isEmpty()).findFirst().orElseThrow();
        var secondDecorated = second.getFlowRoute().getFlowElements().stream()
                .filter(element -> element.getDecorators() != null && !element.getDecorators().isEmpty()).findFirst().orElseThrow();
        assertNotSame(firstDecorated.getDecorators().get(0), secondDecorated.getDecorators().get(0));
    }

    @Test void preservesUnfinishedNestedAndEmptyRoutesWithoutConsumer() throws Exception {
        Flow source = TestFixtures.getEventGeneratingConsumerRouterFlow(BASE_META_PACK);
        source.setConsumer(null);
        source.getFlowRoute().getFlowElements().remove(0); // A lone router, with branches but no transitions into it.
        FlowRoute child = source.getFlowRoute().getChildRoutes().get(0);
        child.getChildRoutes().add(FlowRoute.flowRouteBuilder().flow(source).routeName("unfinished").build());
        source.getFlowRoute().getChildRoutes().get(1).getFlowElements().clear();
        assertEquals(FlowClipboard.capture(source, BASE_META_PACK), FlowClipboard.capture(copy(source), BASE_META_PACK));
        Flow empty = new Flow(BASE_META_PACK);
        empty.setName("Empty");
        assertEquals(FlowClipboard.capture(empty, BASE_META_PACK), FlowClipboard.capture(copy(empty), BASE_META_PACK));
    }

    @Test void refusesOtherVersionsAndMalformedOrUnsupportedData() throws Exception {
        Flow source = TestFixtures.getEventGeneratingConsumerRouterFlow(BASE_META_PACK);
        String clipboard = FlowClipboard.encode(FlowClipboard.capture(source, BASE_META_PACK));
        assertThrows(FlowClipboard.VersionMismatch.class, () -> FlowClipboard.decode(clipboard, "V4.1.6"));
        assertThrows(IOException.class, () -> FlowClipboard.decode("ordinary text", BASE_META_PACK));
        assertThrows(IOException.class, () -> FlowClipboard.decode(FlowClipboard.PREFIX + "{}", BASE_META_PACK));
        var mapper = StudioJson.newObjectMapper();
        var root = mapper.readTree(clipboard.substring(FlowClipboard.PREFIX.length()));
        ((com.fasterxml.jackson.databind.node.ObjectNode) root.path("consumer")).put("implementingClass", "missing.Component");
        assertThrows(IOException.class, () -> FlowClipboard.decode(FlowClipboard.PREFIX + mapper.writeValueAsString(root), BASE_META_PACK));
    }

    @Test void avoidsDisplayAndGeneratedNameConflictsWithoutMutatingDestination() throws Exception {
        Flow existing = new Flow(BASE_META_PACK);
        existing.setName("Order Flow");
        Module destination = TestFixtures.getMyFirstModuleIkasanModule(BASE_META_PACK, new ArrayList<>(List.of(existing)));
        assertFalse(FlowClipboard.nameAvailable(destination, "Order Flow"));
        assertFalse(FlowClipboard.nameAvailable(destination, "order flow"));
        assertEquals("Order Flow Copy", FlowClipboard.availableName(destination, "Order Flow"));
        Flow another = new Flow(BASE_META_PACK);
        another.setName("Order Flow Copy");
        destination.getFlows().add(another);
        assertEquals("Order Flow Copy 2", FlowClipboard.availableName(destination, "Order Flow"));
        assertEquals("Fresh Flow", FlowClipboard.availableName(destination, "Fresh Flow"));
        assertEquals(2, destination.getFlows().size());
    }

    @Test void supportsBothPacksAndPreservesInternalBranchEndpoints() throws Exception {
        for (String version : List.of(BASE_META_PACK, TestFixtures.META_IKASAN_PACK_4_1_6)) {
            Flow source = new Flow(version);
            source.setName("Branches");
            var router = TestFixtures.getSingleRecipientRouter(version);
            router.setContainingFlow(source);
            router.setContainingFlowRoute(source.getFlowRoute());
            source.getFlowRoute().getFlowElements().add(router);
            router.setPropertyValue("routeNames", List.of("left", "right"));
            source.getFlowRoute().syncChildRoutesForRouter(version, router);
            assertFalse(source.getFlowRoute().getChildRoutes().isEmpty());
            var data = FlowClipboard.capture(source, version);
            Flow pasted = FlowClipboard.decode(FlowClipboard.encode(data), version);
            assertEquals(data, FlowClipboard.capture(pasted, version));
            assertParents(pasted, pasted.getFlowRoute());
        }
    }

    @Test void transfersSourcesWithoutChangingLegacyClipboardSupport() throws Exception {
        Flow flow = TestFixtures.getEventGeneratingConsumerRouterFlow(BASE_META_PACK);
        var snapshot = FlowClipboard.capture(flow, BASE_META_PACK);
        var sources = new FlowClipboard.Sources("example.flow", java.util.Map.of(
                "Custom.java", "package example.flow; class Custom {}",
                "helpers/Helper.java", "package example.flow.helpers; class Helper {}"));
        String text = FlowClipboard.encode(snapshot, sources);
        assertTrue(FlowClipboard.isFlow(text));
        var restored = FlowClipboard.decodeTransfer(text, BASE_META_PACK);
        assertEquals(sources, restored.sources());
        assertEquals(snapshot, FlowClipboard.capture(restored.flow(), BASE_META_PACK));
        assertThrows(FlowClipboard.VersionMismatch.class, () -> FlowClipboard.decodeTransfer(text, "V4.1.6"));
        assertTrue(FlowClipboard.decodeTransfer(FlowClipboard.encode(snapshot), BASE_META_PACK).sources().files().isEmpty());
        assertThrows(IOException.class, () -> FlowClipboard.decode(text, BASE_META_PACK));
    }

    @Test void rejectsUnsafeSourcePathsAndOversizedPayloads() throws Exception {
        var snapshot = FlowClipboard.capture(TestFixtures.getEventGeneratingConsumerRouterFlow(BASE_META_PACK), BASE_META_PACK);
        for (String path : List.of("../Outside.java", "/Outside.java", "nested/../../Outside.java", "nested//Outside.java", "pom.xml")) {
            var sources = new FlowClipboard.Sources("example.flow", java.util.Map.of(path, "text"));
            assertThrows(IOException.class, () -> FlowClipboard.encode(snapshot, sources), path);
        }
        String hostile = FlowClipboard.SOURCE_PREFIX + "{\"flow\":{},\"sources\":{\"packageName\":\"example.flow\",\"files\":{\"../Outside.java\":\"text\"}}}";
        assertThrows(IOException.class, () -> FlowClipboard.decodeTransfer(hostile, BASE_META_PACK));
        var oversized = new FlowClipboard.Sources("example.flow", java.util.Map.of("Large.java", "x".repeat(8 * 1024 * 1024)));
        assertThrows(IOException.class, () -> FlowClipboard.encode(snapshot, oversized));
    }

    private void assertParents(Flow flow, FlowRoute route) {
        assertSame(flow, route.getFlow());
        for (var element : route.getFlowElements()) {
            assertSame(flow, element.getContainingFlow());
            assertSame(route, element.getContainingFlowRoute());
        }
        route.getChildRoutes().forEach(child -> assertParents(flow, child));
    }
}
