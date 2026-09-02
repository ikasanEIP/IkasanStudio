package org.ikasan.studio.core.metapack;

import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentHelpCompositionTest {
    @ParameterizedTest
    @ValueSource(strings = {"V3.3.8", "V4.0.x"})
    void categoryHelpPrecedesSpecificHelpForEveryComponentType(String version) throws Exception {
        ComponentLibrary.refreshComponentLibrary(version);

        for (ComponentMeta component : ComponentLibrary.getIkasanComponents(version).values()) {
            String categoryHelp = component.getComponentTypeMeta().getHelpText();
            if (categoryHelp == null || categoryHelp.isBlank()) {
                continue;
            }
            assertThat(component.getHelpText())
                    .as("category help should be first for %s in %s", component.getName(), version)
                    .startsWith(categoryHelp.strip());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"V3.3.8", "V4.0.x"})
    void translatorShowsCategoryThenSpecificGuidance(String version) throws Exception {
        ComponentMeta translator = ComponentLibrary.getIkasanComponentByKeyMandatory(version, "Translator");
        String help = translator.getHelpText();

        assertThat(help.indexOf("The main responsibility of a translator"))
                .isLessThan(help.indexOf("Use a Translator"));
        assertThat(help).contains("</p><p>");
    }
}
