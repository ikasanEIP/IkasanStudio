package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;

class ModulePortDefaultsTest {
    ComponentPropertyEditRow row(String key, String value) {
        ComponentPropertyMeta meta = ComponentPropertyMeta.builder().propertyName(key).propertyDataType(String.class)
                .usageDataType("java.lang.String").mandatory(true).defaultValue("12451").build();
        return new ComponentPropertyEditRow(null, new ComponentProperty(meta, value), false, () -> {}, new HashMap<>());
    }
    @Test void initializationTracksHttpUntilH2IsEdited() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ComponentPropertyEditRow http = row("port", "12410"); ComponentPropertyEditRow database = row("h2DbPortNumber", null); ComponentPropertyEditRow web = row("h2WebPortNumber", null);
            database.configureModulePortDefault(http, 1, true); web.configureModulePortDefault(http, 2, true);
            assertEquals("12411", database.getValue()); assertEquals("12412", web.getValue());
            http.getOverridingInputField().setText("12500");
            assertEquals("12501", database.getValue()); assertEquals("12502", web.getValue());
            database.getOverridingInputField().setText("23000"); http.getOverridingInputField().setText("12600");
            assertEquals("23000", database.getValue()); assertEquals("12602", web.getValue());
        });
    }
    @Test void automaticValuesFilledBeforeTheCreationDialogStillTrackHttp() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ComponentPropertyEditRow http = row("port", "12450"); ComponentPropertyEditRow database = row("h2DbPortNumber", "12451");
            database.configureModulePortDefault(http, 1, true, true);
            http.getOverridingInputField().setText("12410"); assertEquals("12411", database.getValue());
        });
    }
    @Test void configuredValuesStayFixedUntilDefaultIsExplicitlyRequested() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ComponentPropertyEditRow http = row("port", "12410"); ComponentPropertyEditRow database = row("h2DbPortNumber", "23000");
            database.prepareModulePortDefaultButton(1); database.configureModulePortDefault(http, 1, false);
            http.getOverridingInputField().setText("12500"); assertEquals("23000", database.getValue());
            database.getDefaultValueButton().doClick(); assertEquals("12501", database.getValue());
            http.getOverridingInputField().setText("12600"); assertEquals("12501", database.getValue());
        });
    }
    @Test void validatesPortRangesAndDuplicateServicePorts() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Map<String,ComponentPropertyEditRow> rows = Map.of("port", row("port", "12410"), "h2DbPortNumber", row("h2DbPortNumber", "12411"), "h2WebPortNumber", row("h2WebPortNumber", "12412"));
            assertTrue(ComponentPropertiesPanel.validateModulePorts(rows).isEmpty());
            rows.get("h2DbPortNumber").getOverridingInputField().setText("12410");
            assertEquals(1, ComponentPropertiesPanel.validateModulePorts(rows).size());
            rows.get("h2DbPortNumber").getOverridingInputField().setText("65536");
            assertEquals(1, ComponentPropertiesPanel.validateModulePorts(rows).size());
        });
    }
}
