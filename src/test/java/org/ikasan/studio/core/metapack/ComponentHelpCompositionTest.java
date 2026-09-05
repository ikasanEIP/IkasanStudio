package org.ikasan.studio.core.metapack;

import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentHelpCompositionTest {
    @ParameterizedTest
    @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void categoryHelpPrecedesSpecificHelpForEveryComponentType(String version) throws Exception {
        ComponentLibrary.refreshComponentLibrary(version);

        for (ComponentMeta component : ComponentLibrary.getIkasanComponents(version).values()) {
            String categoryHelp = component.getComponentTypeMeta().getHelpText();
            if (categoryHelp == null || categoryHelp.isBlank()) {
                continue;
            }
            String componentHelp = component.getSpecificHelpText();
            if (componentHelp != null && !componentHelp.isBlank()) {
                assertThat(componentHelp.strip())
                        .as("component help should not duplicate category help for %s in %s", component.getName(), version)
                        .isNotEqualTo(categoryHelp.strip());
            }
            assertThat(component.getHelpText())
                    .as("category help should be first for %s in %s", component.getName(), version)
                    .startsWith(categoryHelp.strip());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void translatorShowsCategoryThenSpecificGuidance(String version) throws Exception {
        ComponentMeta translator = ComponentLibrary.getIkasanComponentByKeyMandatory(version, "Translator");
        String help = translator.getHelpText();

        assertThat(help.indexOf("A <b>Translator</b> modifies"))
                .isLessThan(help.indexOf("Implement the required in-place transformation"));
        assertThat(help).contains("does not replace it, so its Java type remains the same");
        assertThat(help).contains("</p><p>");
    }
}
