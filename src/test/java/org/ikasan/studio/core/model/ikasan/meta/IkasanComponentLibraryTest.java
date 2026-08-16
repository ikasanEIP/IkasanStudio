package org.ikasan.studio.core.model.ikasan.meta;

import org.ikasan.studio.core.StudioBuildException;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.Map;
import java.util.TreeSet;

import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary.getDeserialisationKey;
import static org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary.getIkasanComponentByKey;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IkasanComponentLibraryTest {
    @Test
    void generalIconsPreferSvgWhenVectorAssetExists() {
        Icon helpIcon = IkasanComponentLibrary.getGeneralIcon("help.png", "Help");
        Icon sendTestMessageIcon = IkasanComponentLibrary.getGeneralIcon(
                "send-test-message.png", "Send Test Message");
        Icon wiretapIcon = IkasanComponentLibrary.getGeneralIcon("wiretap.png", "Wiretap");
        Icon logWiretapIcon = IkasanComponentLibrary.getGeneralIcon("log-wiretap.png", "Log Wiretap");
        Icon searchIcon = IkasanComponentLibrary.getGeneralIcon("search-icon.png", "Search");
        Icon replayIcon = IkasanComponentLibrary.getGeneralIcon("replay-small-icon.png", "Replay");
        Icon resubmitIcon = IkasanComponentLibrary.getGeneralIcon("resubmit-icon.png", "Resubmit");
        Icon ignoreIcon = IkasanComponentLibrary.getGeneralIcon("ignore-icon.png", "Ignore");
        Icon errorIcon = IkasanComponentLibrary.getGeneralIcon("error-service.png", "Error");
        Icon mappingIcon = IkasanComponentLibrary.getGeneralIcon("mapping-service.png", "Mapping");
        Icon wiretapServiceIcon = IkasanComponentLibrary.getGeneralIcon(
                "wiretap-service.png", "Wiretap Service");
        Icon configurationServiceIcon = IkasanComponentLibrary.getGeneralIcon(
                "configuration-service.png", "Configuration Service");
        Icon hospitalServiceIcon = IkasanComponentLibrary.getGeneralIcon(
                "hospital-service.png", "Hospital Service");
        Icon replayServiceIcon = IkasanComponentLibrary.getGeneralIcon(
                "replay-service.png", "Replay Service");
        Icon requestReplyIcon = IkasanComponentLibrary.getGeneralIcon(
                "request-reply.png", "Request Reply");
        Icon deadEndPointIcon = IkasanComponentLibrary.getGeneralIcon(
                "dead-end-point.png", "Dead End Point");
        Icon deadLetterChannelIcon = IkasanComponentLibrary.getGeneralIcon(
                "dead-letter-channel.png", "Dead Letter Channel");
        Icon fileLocationIcon = IkasanComponentLibrary.getGeneralIcon(
                "file-location.png", "File Location");
        Icon ftpLocationIcon = IkasanComponentLibrary.getGeneralIcon(
                "ftp-location.png", "FTP Location");
        Icon computerIcon = IkasanComponentLibrary.getGeneralIcon(
                "computer.png", "Computer");
        Icon emptyControlIcon = IkasanComponentLibrary.getGeneralIcon(
                "empty-control-small.png", "Empty Control");
        Icon largeWiretapIcon = IkasanComponentLibrary.getGeneralIcon(
                "lrg-wiretap.png", "Large Wiretap");
        Icon largeLogWiretapIcon = IkasanComponentLibrary.getGeneralIcon(
                "lrg-log-wiretap.png", "Large Log Wiretap");
        Icon help30Icon = IkasanComponentLibrary.getGeneralIcon(
                "help-30-30.png", "Help 30");
        Icon help40Icon = IkasanComponentLibrary.getGeneralIcon(
                "help-40-40.png", "Help 40");
        Icon studioIcon = IkasanComponentLibrary.getGeneralIcon(
                "icon.png", "Studio Icon");
        Icon squid13Icon = IkasanComponentLibrary.getGeneralIcon(
                "squid13x13.png", "Squid 13");
        Icon squidHeadIcon = IkasanComponentLibrary.getGeneralIcon(
                "mr-squid-head.png", "Mr Squid Head");

        assertAll(
                () -> assertEquals(24, helpIcon.getIconWidth()),
                () -> assertEquals(24, helpIcon.getIconHeight()),
                () -> assertEquals(39, sendTestMessageIcon.getIconWidth()),
                () -> assertEquals(22, sendTestMessageIcon.getIconHeight()),
                () -> assertEquals(22, wiretapIcon.getIconWidth()),
                () -> assertEquals(22, wiretapIcon.getIconHeight()),
                () -> assertEquals(22, logWiretapIcon.getIconWidth()),
                () -> assertEquals(22, logWiretapIcon.getIconHeight()),
                () -> assertEquals(22, searchIcon.getIconWidth()),
                () -> assertEquals(22, searchIcon.getIconHeight()),
                () -> assertEquals(22, replayIcon.getIconWidth()),
                () -> assertEquals(22, replayIcon.getIconHeight()),
                () -> assertEquals(22, resubmitIcon.getIconWidth()),
                () -> assertEquals(22, resubmitIcon.getIconHeight()),
                () -> assertEquals(22, ignoreIcon.getIconWidth()),
                () -> assertEquals(22, ignoreIcon.getIconHeight()),
                () -> assertEquals(22, errorIcon.getIconWidth()),
                () -> assertEquals(22, errorIcon.getIconHeight()),
                () -> assertEquals(22, mappingIcon.getIconWidth()),
                () -> assertEquals(22, mappingIcon.getIconHeight()),
                () -> assertEquals(22, wiretapServiceIcon.getIconWidth()),
                () -> assertEquals(22, wiretapServiceIcon.getIconHeight()),
                () -> assertEquals(22, configurationServiceIcon.getIconWidth()),
                () -> assertEquals(22, configurationServiceIcon.getIconHeight()),
                () -> assertEquals(22, hospitalServiceIcon.getIconWidth()),
                () -> assertEquals(22, hospitalServiceIcon.getIconHeight()),
                () -> assertEquals(22, replayServiceIcon.getIconWidth()),
                () -> assertEquals(22, replayServiceIcon.getIconHeight()),
                () -> assertEquals(33, requestReplyIcon.getIconWidth()),
                () -> assertEquals(22, requestReplyIcon.getIconHeight()),
                () -> assertEquals(33, deadEndPointIcon.getIconWidth()),
                () -> assertEquals(22, deadEndPointIcon.getIconHeight()),
                () -> assertEquals(33, deadLetterChannelIcon.getIconWidth()),
                () -> assertEquals(22, deadLetterChannelIcon.getIconHeight()),
                () -> assertEquals(33, fileLocationIcon.getIconWidth()),
                () -> assertEquals(22, fileLocationIcon.getIconHeight()),
                () -> assertEquals(33, ftpLocationIcon.getIconWidth()),
                () -> assertEquals(22, ftpLocationIcon.getIconHeight()),
                () -> assertEquals(25, computerIcon.getIconWidth()),
                () -> assertEquals(22, computerIcon.getIconHeight()),
                () -> assertEquals(22, emptyControlIcon.getIconWidth()),
                () -> assertEquals(22, emptyControlIcon.getIconHeight()),
                () -> assertEquals(32, largeWiretapIcon.getIconWidth()),
                () -> assertEquals(32, largeWiretapIcon.getIconHeight()),
                () -> assertEquals(32, largeLogWiretapIcon.getIconWidth()),
                () -> assertEquals(32, largeLogWiretapIcon.getIconHeight()),
                () -> assertEquals(30, help30Icon.getIconWidth()),
                () -> assertEquals(30, help30Icon.getIconHeight()),
                () -> assertEquals(40, help40Icon.getIconWidth()),
                () -> assertEquals(40, help40Icon.getIconHeight()),
                () -> assertEquals(512, studioIcon.getIconWidth()),
                () -> assertEquals(512, studioIcon.getIconHeight()),
                () -> assertEquals(13, squid13Icon.getIconWidth()),
                () -> assertEquals(13, squid13Icon.getIconHeight()),
                () -> assertEquals(378, squidHeadIcon.getIconWidth()),
                () -> assertEquals(496, squidHeadIcon.getIconHeight()),
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
    void testThatDeserializationPopulatesTheIkasanComponentLibrary() throws StudioBuildException {
        IkasanComponentLibrary.refreshComponentLibrary(BASE_META_PACK);

        assertAll(
            "Check the module contains the expected values",
            () -> assertEquals(41, IkasanComponentLibrary.getNumberOfComponents(BASE_META_PACK)),
            () -> assertEquals(
                    "[Basic AMQ JMS Producer, Basic AMQ Spring JMS Consumer, Broker, Channel Endpoint, Custom Converter, Custom Message Filter, Custom Splitter, Custom Translator, DB Endpoint, Debug Transition, Default List Splitter, Default Message Filter, Dev Null Producer, Email Producer, Event Generating Consumer, Exception Resolver, FTP Consumer, FTP Endpoint, FTP Producer, File Endpoint, Flow, Generic Broker, Generic Consumer, Generic Converter, Generic Filter, Generic Producer, Generic Splitter, Generic Translator, JMS Producer, Local File Consumer, Logging Producer, Module, Multi Recipient Router, Object Message To Object Converter, Object Message To XML String Converter, Router Endpoint, SFTP Consumer, SFTP Producer, Scheduled Consumer, Single Recipient Router, Spring JMS Consumer]",
//                new TreeSet<>(Arrays.asList("Custom Converter", ComponentMeta.EXCEPTION_RESOLVER_TYPE, "Event Generating Consumer", ComponentMeta.FLOW_TYPE, ComponentMeta.MODULE_TYPE, "Dev Null Producer")),
                new TreeSet<>(IkasanComponentLibrary.getIkasanComponentNames(BASE_META_PACK)).toString())
        );

        Map<String, ComponentMeta> componentMetaList = IkasanComponentLibrary.getIkasanComponents(BASE_META_PACK);
        verifyDefaultModuleMeta(componentMetaList.get(ComponentMeta.MODULE_TYPE));
        verifyDefaultFlowMeta(componentMetaList.get(ComponentMeta.FLOW_TYPE));
        verifyDefaultExceptionResolverMeta((ExceptionResolverMeta)componentMetaList.get(ComponentMeta.EXCEPTION_RESOLVER_TYPE));
    }

    protected void verifyDefaultFlowMeta(ComponentMeta flow) {
        assertAll(
            "Check the flow contains the expected values",
            () -> assertEquals(ComponentMeta.FLOW_TYPE, flow.getName()),
            () -> assertEquals("<strong>Flow</strong><p>The flow is the container for components and generally represents an atomic action.</p>", flow.getHelpText()),
            () -> assertEquals("org.ikasan.spec.flow.Flow", flow.getComponentType()),
            () -> assertEquals("https://github.com/ikasanEIP/ikasan/blob/3.1.x/ikasaneip/component/Readme.md", flow.getWebHelpURL()),
            () -> assertEquals("Small Flow icon", flow.getSmallIcon().getDescription()),
            () -> assertEquals("Medium Flow icon", flow.getCanvasIcon().getDescription()),
            () -> assertEquals(4, flow.getAllowableProperties().size())
        );
    }
    protected void verifyDefaultModuleMeta(ComponentMeta module) {
        assertAll(
            "Check the module contains the expected values",
            () -> assertEquals(ComponentMeta.MODULE_TYPE, module.getName()),
            () -> assertEquals("The module is the container for all flows", module.getHelpText()),
            () -> assertEquals("org.ikasan.spec.module.Module", module.getComponentType()),
            () -> assertEquals("Readme.md", module.getWebHelpURL()),
            () -> assertEquals("Small Module icon", module.getSmallIcon().getDescription()),
            () -> assertEquals("Medium Module icon", module.getCanvasIcon().getDescription()),
            () -> assertEquals(11, module.getAllowableProperties().size())
        );
    }

    protected void verifyDefaultExceptionResolverMeta(ExceptionResolverMeta exceptionResolver) {
        assertAll(
                "Check the Exception Resolver contains the expected values",
                () -> assertEquals(ComponentMeta.EXCEPTION_RESOLVER_TYPE, exceptionResolver.getName()),
                () -> assertEquals("org.ikasan.exceptionResolver.ExceptionResolver", exceptionResolver.getComponentType()),
                () -> assertEquals("<strong>Exception Resolver</strong><p>Exception Resolvers determine what action to take when an error occurs e.g. retry, exclude and continue, halt the flow.</p>", exceptionResolver.getHelpText()),
                () -> assertEquals("Small Exception Resolver icon", exceptionResolver.getSmallIcon().getDescription()),
                () -> assertEquals("Medium Exception Resolver icon", exceptionResolver.getCanvasIcon().getDescription()),
                () -> assertEquals(0, exceptionResolver.getAllowableProperties().size()),
                () -> assertEquals(5, exceptionResolver.getActionList().size())
        );
    }


}
