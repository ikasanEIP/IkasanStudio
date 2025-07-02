package org.ikasan.studio.core.model.ikasan.meta;

import org.ikasan.studio.core.StudioBuildException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeSet;

import static org.ikasan.studio.core.TestFixtures.BASE_META_PACK;
import static org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary.getDeserialisationKey;
import static org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary.getIkasanComponentByKey;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IkasanComponentLibraryTest {
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
            () -> assertEquals(5, flow.getProperties().size())
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
            () -> assertEquals(15, module.getProperties().size())
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
                () -> assertEquals(0, exceptionResolver.getProperties().size()),
                () -> assertEquals(5, exceptionResolver.getActionList().size())
        );
    }


}