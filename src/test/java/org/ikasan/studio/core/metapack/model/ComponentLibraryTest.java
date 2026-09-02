package org.ikasan.studio.core.metapack.model;

import org.ikasan.studio.core.metapack.ComponentLibrary;

import org.ikasan.studio.ui.icons.ComponentIconProvider;

import org.ikasan.studio.core.StudioBuildException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.TreeSet;

import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.ikasan.studio.core.metapack.ComponentLibrary.getDeserialisationKey;
import static org.ikasan.studio.core.metapack.ComponentLibrary.getIkasanComponentByKey;
import static org.junit.jupiter.api.Assertions.*;

class ComponentLibraryTest {
    private static int expectedIconDimension(int normalDimension) {
        // IntelliJ intentionally returns a 1x1 placeholder for SVG icons when no graphical
        // environment is available (as on Travis). The non-ImageIcon assertions below still
        // verify that SVG loading was selected; graphical runs additionally verify its size.
        return GraphicsEnvironment.isHeadless() ? 1 : normalDimension;
    }

    @BeforeAll
    static void warmUpComponentLibrary() throws StudioBuildException {
        // Meta-pack classloader I/O lazily starts NIO/Java2D daemon threads. Start them before
        // IntelliJ's per-test ThreadLeakTracker captures its baseline.
        ComponentLibrary.refreshComponentLibrary(BASE_META_PACK);
    }

    @Test
    void generalIconsPreferSvgWhenVectorAssetExists() {
        Icon helpIcon = ComponentIconProvider.getGeneralIcon("help.png", "Help");
        Icon sendTestMessageIcon = ComponentIconProvider.getGeneralIcon(
                "send-test-message.png", "Send Test Message");
        Icon wiretapIcon = ComponentIconProvider.getGeneralIcon("wiretap.png", "Wiretap");
        Icon logWiretapIcon = ComponentIconProvider.getGeneralIcon("log-wiretap.png", "Log Wiretap");
        Icon searchIcon = ComponentIconProvider.getGeneralIcon("search-icon.png", "Search");
        Icon replayIcon = ComponentIconProvider.getGeneralIcon("replay-small-icon.png", "Replay");
        Icon resubmitIcon = ComponentIconProvider.getGeneralIcon("resubmit-icon.png", "Resubmit");
        Icon ignoreIcon = ComponentIconProvider.getGeneralIcon("ignore-icon.png", "Ignore");
        Icon errorIcon = ComponentIconProvider.getGeneralIcon("error-service.png", "Error");
        Icon mappingIcon = ComponentIconProvider.getGeneralIcon("mapping-service.png", "Mapping");
        Icon wiretapServiceIcon = ComponentIconProvider.getGeneralIcon(
                "wiretap-service.png", "Wiretap Service");
        Icon configurationServiceIcon = ComponentIconProvider.getGeneralIcon(
                "configuration-service.png", "Configuration Service");
        Icon hospitalServiceIcon = ComponentIconProvider.getGeneralIcon(
                "hospital-service.png", "Hospital Service");
        Icon replayServiceIcon = ComponentIconProvider.getGeneralIcon(
                "replay-service.png", "Replay Service");
        Icon requestReplyIcon = ComponentIconProvider.getGeneralIcon(
                "request-reply.png", "Request Reply");
        Icon deadEndPointIcon = ComponentIconProvider.getGeneralIcon(
                "dead-end-point.png", "Dead End Point");
        Icon deadLetterChannelIcon = ComponentIconProvider.getGeneralIcon(
                "dead-letter-channel.png", "Dead Letter Channel");
        Icon fileLocationIcon = ComponentIconProvider.getGeneralIcon(
                "file-location.png", "File Location");
        Icon ftpLocationIcon = ComponentIconProvider.getGeneralIcon(
                "ftp-location.png", "FTP Location");
        Icon computerIcon = ComponentIconProvider.getGeneralIcon(
                "computer.png", "Computer");
        Icon emptyControlIcon = ComponentIconProvider.getGeneralIcon(
                "empty-control-small.png", "Empty Control");
        Icon largeWiretapIcon = ComponentIconProvider.getGeneralIcon(
                "lrg-wiretap.png", "Large Wiretap");
        Icon largeLogWiretapIcon = ComponentIconProvider.getGeneralIcon(
                "lrg-log-wiretap.png", "Large Log Wiretap");
        Icon help30Icon = ComponentIconProvider.getGeneralIcon(
                "help-30-30.png", "Help 30");
        Icon help40Icon = ComponentIconProvider.getGeneralIcon(
                "help-40-40.png", "Help 40");
        Icon studioIcon = ComponentIconProvider.getGeneralIcon(
                "icon.png", "Studio Icon");
        Icon squid13Icon = ComponentIconProvider.getGeneralIcon(
                "squid13x13.png", "Squid 13");
        Icon squidHeadIcon = ComponentIconProvider.getGeneralIcon(
                "mr-squid-head.png", "Mr Squid Head");

        assertAll(
                () -> assertEquals(expectedIconDimension(24), helpIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(24), helpIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(39), sendTestMessageIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(22), sendTestMessageIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(22), wiretapIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(22), wiretapIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(22), logWiretapIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(22), logWiretapIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(22), searchIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(22), searchIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(22), replayIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(22), replayIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(22), resubmitIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(22), resubmitIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(22), ignoreIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(22), ignoreIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(22), errorIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(22), errorIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(22), mappingIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(22), mappingIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(22), wiretapServiceIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(22), wiretapServiceIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(22), configurationServiceIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(22), configurationServiceIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(22), hospitalServiceIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(22), hospitalServiceIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(22), replayServiceIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(22), replayServiceIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(33), requestReplyIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(22), requestReplyIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(33), deadEndPointIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(22), deadEndPointIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(33), deadLetterChannelIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(22), deadLetterChannelIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(33), fileLocationIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(22), fileLocationIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(33), ftpLocationIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(22), ftpLocationIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(25), computerIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(22), computerIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(22), emptyControlIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(22), emptyControlIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(32), largeWiretapIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(32), largeWiretapIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(32), largeLogWiretapIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(32), largeLogWiretapIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(30), help30Icon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(30), help30Icon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(40), help40Icon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(40), help40Icon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(512), studioIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(512), studioIcon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(13), squid13Icon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(13), squid13Icon.getIconHeight()),
                () -> assertEquals(expectedIconDimension(378), squidHeadIcon.getIconWidth()),
                () -> assertEquals(expectedIconDimension(496), squidHeadIcon.getIconHeight()),
                () -> org.junit.jupiter.api.Assertions.assertFalse(helpIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(sendTestMessageIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(wiretapIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(logWiretapIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(searchIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(replayIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(resubmitIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(ignoreIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(errorIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(mappingIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(wiretapServiceIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(configurationServiceIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(hospitalServiceIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(replayServiceIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(requestReplyIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(deadEndPointIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(deadLetterChannelIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(fileLocationIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(ftpLocationIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(computerIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(emptyControlIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(largeWiretapIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(largeLogWiretapIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(help30Icon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(help40Icon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(studioIcon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(squid13Icon instanceof ImageIcon),
                () -> org.junit.jupiter.api.Assertions.assertFalse(squidHeadIcon instanceof ImageIcon)
        );
    }

    @Test
    void testDescerialisationKey() throws StudioBuildException {
        String key1 = getDeserialisationKey(getIkasanComponentByKey(BASE_META_PACK, "DB Endpoint"));
        assertEquals("DBEndpoint-Endpoint-", key1);
        String key2 = getDeserialisationKey(getIkasanComponentByKey(BASE_META_PACK, "Module"));
        assertEquals("org.ikasan.spec.module.Module-", key2);
        String key3 = getDeserialisationKey(getIkasanComponentByKey(BASE_META_PACK, "FTP Consumer"));
        assertEquals("org.ikasan.component.endpoint.quartz.consumer.ScheduledConsumer-org.ikasan.spec.component.endpoint.Consumer-FTP Consumer", key3);
    }

    @Test
    void testThatDeserializationPopulatesTheComponentLibrary() throws StudioBuildException {
        ComponentLibrary.refreshComponentLibrary(BASE_META_PACK);

        assertAll(
            "Check the module contains the expected values",
            () -> assertEquals(41, ComponentLibrary.getNumberOfComponents(BASE_META_PACK)),
            () -> assertEquals(
                    "[Basic AMQ JMS Producer, Basic AMQ Spring JMS Consumer, Broker, Channel Endpoint, Converter, DB Endpoint, Debug Transition, Default List Splitter, Default Message Filter, Dev Null Producer, Email Converter, Email Endpoint, Email Producer, Event Generating Consumer, Exception Resolver, FTP Consumer, FTP Endpoint, FTP Producer, File Endpoint, Flow, Generic Consumer, Generic Endpoint, Generic Producer, JMS Object Message To Object Converter, JMS Producer, Local File Consumer, Logging Producer, Message Filter, Module, Multi Recipient Router, Object To XML String Converter, Router Endpoint, SFTP Consumer, SFTP Endpoint, SFTP Producer, Scheduled Consumer, Scheduler Endpoint, Single Recipient Router, Splitter, Spring JMS Consumer, Translator]",
//                new TreeSet<>(Arrays.asList("Custom Converter", ComponentMeta.EXCEPTION_RESOLVER_TYPE, "Event Generating Consumer", ComponentMeta.FLOW_TYPE, ComponentMeta.MODULE_TYPE, "Dev Null Producer")),
                new TreeSet<>(ComponentLibrary.getIkasanComponentNames(BASE_META_PACK)).toString())
        );

        Map<String, ComponentMeta> componentMetaList = ComponentLibrary.getIkasanComponents(BASE_META_PACK);
        verifyDefaultModuleMeta(componentMetaList.get(ComponentMeta.MODULE_TYPE));
        verifyDefaultFlowMeta(componentMetaList.get(ComponentMeta.FLOW_TYPE));
        verifyDefaultExceptionResolverMeta((ExceptionResolverMeta)componentMetaList.get(ComponentMeta.EXCEPTION_RESOLVER_TYPE));
        verifyComponentUsesSvg(componentMetaList.get("Converter"));
        verifyComponentUsesSvg(componentMetaList.get("Channel Endpoint"));
        verifyComponentUsesSvg(componentMetaList.get("JMS Producer"));
        verifyComponentUsesSvg(componentMetaList.get("Basic AMQ JMS Producer"));
        verifyComponentUsesSvg(componentMetaList.get("JMS Object Message To Object Converter"));
        verifyComponentUsesSvg(componentMetaList.get("Object To XML String Converter"));
        verifyComponentUsesSvg(componentMetaList.get("Translator"));
        verifyComponentUsesSvg(componentMetaList.get("Email Producer"));
        verifyComponentUsesSvg(componentMetaList.get("Dev Null Producer"));
        verifyComponentUsesSvg(componentMetaList.get("FTP Producer"));
        verifyComponentUsesSvg(componentMetaList.get("Generic Producer"));
        verifyComponentUsesSvg(componentMetaList.get("Logging Producer"));
        verifyComponentUsesSvg(componentMetaList.get("SFTP Producer"));
        verifyComponentUsesSvg(componentMetaList.get("Event Generating Consumer"));
        verifyComponentUsesSvg(componentMetaList.get("Basic AMQ Spring JMS Consumer"));
        verifyComponentUsesSvg(componentMetaList.get("Spring JMS Consumer"));
        verifyComponentUsesSvg(componentMetaList.get("Local File Consumer"));
        verifyComponentUsesSvg(componentMetaList.get("Generic Consumer"));
        verifyComponentUsesSvg(componentMetaList.get("FTP Consumer"));
        verifyComponentUsesSvg(componentMetaList.get("SFTP Consumer"));
        verifyComponentUsesSvg(componentMetaList.get("Scheduled Consumer"));
        verifyComponentUsesSvg(componentMetaList.get("Exception Resolver"), 40, 27, 22, 22);
        verifyComponentUsesSvg(componentMetaList.get("DB Endpoint"));
        verifyComponentUsesSvg(componentMetaList.get("File Endpoint"));
        verifyComponentUsesSvg(componentMetaList.get("FTP Endpoint"));
        verifyComponentUsesSvg(componentMetaList.get("SFTP Endpoint"));
        verifyComponentUsesSvg(componentMetaList.get("Email Endpoint"));
        verifyComponentUsesSvg(componentMetaList.get("Email Converter"));
        verifyComponentUsesSvg(componentMetaList.get("Generic Endpoint"));
        verifyComponentUsesSvg(componentMetaList.get("Scheduler Endpoint"));
        verifyComponentUsesSvg(componentMetaList.get("Router Endpoint"), 40, 27, 15, 60);
        verifyComponentUsesSvg(componentMetaList.get("Broker"));
        verifyComponentUsesSvg(componentMetaList.get("Debug Transition"), 27, 27, 40, 60);
        verifyComponentUsesSvg(componentMetaList.get("Message Filter"));
        verifyComponentUsesSvg(componentMetaList.get("Default Message Filter"));
        verifyComponentUsesSvg(componentMetaList.get("Flow"));
        verifyComponentUsesSvg(componentMetaList.get("Module"), 378, 496, 378, 496);
        verifyComponentUsesSvg(componentMetaList.get("Multi Recipient Router"));
        verifyComponentUsesSvg(componentMetaList.get("Single Recipient Router"));
        verifyComponentUsesSvg(componentMetaList.get("Splitter"));
        verifyComponentUsesSvg(componentMetaList.get("Default List Splitter"));
    }

    @Test
    void emailProducerResolvesToAnEmailEndpointLabelledWithItsToRecipient() throws Exception {
        org.ikasan.studio.core.model.ikasan.instance.FlowElement emailProducer =
                org.ikasan.studio.core.TestFixtures.getEmailProducer(BASE_META_PACK);

        org.ikasan.studio.core.model.ikasan.instance.FlowElement endpoint =
                ComponentLibrary.getEndpointForGivenComponent(BASE_META_PACK, emailProducer);

        assertAll(
                "Email Producer must resolve to its own Email Endpoint, labelled with its toRecipient",
                () -> assertEquals("Email Endpoint", emailProducer.getComponentMeta().getEndpointKey()),
                () -> assertNotNull(endpoint, "Email Producer must resolve to a real endpoint, not silently return null"),
                () -> assertEquals("Email Endpoint", endpoint.getComponentMeta().getName()),
                () -> assertEquals(emailProducer.getPropertyValueAsString("toRecipient"), endpoint.getComponentName())
        );
    }

    private void verifyComponentUsesSvg(ComponentMeta component) {
        verifyComponentUsesSvg(component, 40, 27, 90, 60);
    }

    private void verifyComponentUsesSvg(ComponentMeta component, int smallWidth, int smallHeight,
                                        int canvasWidth, int canvasHeight) {
        assertAll(
                component.getName() + " should use native SVG icons",
                () -> assertEquals(expectedIconDimension(smallWidth), ComponentIconProvider.getSmallIcon(component).getIconWidth()),
                () -> assertEquals(expectedIconDimension(smallHeight), ComponentIconProvider.getSmallIcon(component).getIconHeight()),
                () -> assertEquals(expectedIconDimension(canvasWidth), ComponentIconProvider.getCanvasIcon(component).getIconWidth()),
                () -> assertEquals(expectedIconDimension(canvasHeight), ComponentIconProvider.getCanvasIcon(component).getIconHeight()),
                () -> assertFalse(ComponentIconProvider.getSmallIcon(component) instanceof ImageIcon),
                () -> assertFalse(ComponentIconProvider.getCanvasIcon(component) instanceof ImageIcon)
        );
    }

    protected void verifyDefaultFlowMeta(ComponentMeta flow) {
        assertAll(
            "Check the flow contains the expected values",
            () -> assertEquals(ComponentMeta.FLOW_TYPE, flow.getName()),
            () -> assertEquals("<p>The flow is the container for components and generally represents an atomic action.</p>", flow.getHelpText()),
            () -> assertEquals("org.ikasan.spec.flow.Flow", flow.getComponentType()),
            () -> assertEquals("https://github.com/ikasanEIP/ikasan/blob/3.1.x/ikasaneip/component/Readme.md", flow.getWebHelpURL()),
            () -> assertFalse(ComponentIconProvider.getSmallIcon(flow) instanceof ImageIcon),
            () -> assertFalse(ComponentIconProvider.getCanvasIcon(flow) instanceof ImageIcon),
            () -> assertEquals(7, flow.getAllowableProperties().size()),
            () -> assertTrue(flow.getAllowableProperties().containsKey("isRecording")),
            () -> assertTrue(flow.getAllowableProperties().containsKey("recordedEventTimeToLive")),
            () -> assertTrue(flow.getAllowableProperties().containsKey("invokeContextListeners"))
        );
    }
    protected void verifyDefaultModuleMeta(ComponentMeta module) {
        assertAll(
            "Check the module contains the expected values",
            () -> assertEquals(ComponentMeta.MODULE_TYPE, module.getName()),
            () -> assertEquals("<p>The module is the container for all flows</p>", module.getHelpText()),
            () -> assertEquals("org.ikasan.spec.module.Module", module.getComponentType()),
            () -> assertEquals("Readme.md", module.getWebHelpURL()),
            () -> assertFalse(ComponentIconProvider.getSmallIcon(module) instanceof ImageIcon),
            () -> assertFalse(ComponentIconProvider.getCanvasIcon(module) instanceof ImageIcon),
            () -> assertEquals(11, module.getAllowableProperties().size())
        );
    }

    protected void verifyDefaultExceptionResolverMeta(ExceptionResolverMeta exceptionResolver) {
        assertAll(
                "Check the Exception Resolver contains the expected values",
                () -> assertEquals(ComponentMeta.EXCEPTION_RESOLVER_TYPE, exceptionResolver.getName()),
                () -> assertEquals("org.ikasan.exceptionResolver.ExceptionResolver", exceptionResolver.getComponentType()),
                () -> assertEquals("<p>Exception Resolvers determine what action to take when an error occurs e.g. retry, exclude and continue, halt the flow.</p>", exceptionResolver.getHelpText()),
                () -> assertEquals(0, exceptionResolver.getAllowableProperties().size()),
                () -> assertEquals(5, exceptionResolver.getActionList().size())
        );
    }


}
