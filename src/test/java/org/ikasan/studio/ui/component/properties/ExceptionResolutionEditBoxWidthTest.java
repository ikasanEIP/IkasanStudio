package org.ikasan.studio.ui.component.properties;

import com.intellij.openapi.ui.ComboBox;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The "Add Exception" popup's Exceptions dropdown used to render its longer configured values (e.g.
 * "org.ikasan.spec.component.transformation.TransformationException.class") truncated, requiring the whole
 * PropertiesPopupDialogue to be dragged wider by hand before they were readable - reported by the user.
 * -
 * Root cause: unlike a plain Swing JComboBox, com.intellij.openapi.ui.ComboBox does not size itself to its
 * widest item - it defaults to a modest fixed width regardless of content. Verifies
 * ExceptionResolutionEditBox#widenToFitContents fixes that directly against the real IntelliJ ComboBox class,
 * without needing a full ExceptionResolutionEditBox/Project to construct.
 */
class ExceptionResolutionEditBoxWidthTest {

    private static final String LONGEST_REAL_EXCEPTION = "org.ikasan.spec.component.transformation.TransformationException.class";

    private static final String[] REAL_EXCEPTIONS_CAUGHT = {
            "javax.jms.JMSException.class",
            "javax.resource.ResourceException.class",
            "org.ikasan.spec.component.endpoint.EndpointException.class",
            "org.ikasan.spec.component.filter.FilterException.class",
            "org.ikasan.spec.component.routing.RouterException.class",
            "org.ikasan.spec.component.splitting.SplitterException.class",
            LONGEST_REAL_EXCEPTION,
    };

    @Test
    void sanityCheckIntelliJsComboBoxDoesNotSizeItselfToItsContentUnlikePlainSwing() {
        JComboBox<String> plainSwingComboBox = new JComboBox<>(REAL_EXCEPTIONS_CAUGHT);
        ComboBox<String> intellijComboBox = new ComboBox<>(REAL_EXCEPTIONS_CAUGHT);

        // The whole reason this fix is needed: IntelliJ's own ComboBox does not behave like a plain JComboBox
        // here, so this can't be left to "just work" the way it does for ordinary Swing components.
        assertThat(intellijComboBox.getPreferredSize().width)
                .isLessThan(plainSwingComboBox.getPreferredSize().width);
    }

    @Test
    void widenToFitContentsMakesTheComboBoxAtLeastAsWideAsItsLongestItem() {
        ComboBox<String> comboBox = new ComboBox<>(REAL_EXCEPTIONS_CAUGHT);
        int textWidth = comboBox.getFontMetrics(comboBox.getFont()).stringWidth(LONGEST_REAL_EXCEPTION);

        ExceptionResolutionEditBox.widenToFitContents(comboBox, REAL_EXCEPTIONS_CAUGHT);

        // Some positive padding beyond the raw text width for the dropdown arrow/borders/editor insets, but not
        // an excessive amount - this is a regression guard against "some padding" silently becoming "so much
        // padding it may as well not be measuring the content at all".
        assertThat(comboBox.getPreferredSize().width).isGreaterThan(textWidth);
        assertThat(comboBox.getPreferredSize().width).isLessThan(textWidth + 100);
    }

    @Test
    void widenToFitContentsNeverShrinksAnAlreadyWiderComboBox() {
        ComboBox<String> comboBox = new ComboBox<>(new String[]{"short"});
        int before = comboBox.getPreferredSize().width;
        comboBox.setPreferredSize(new java.awt.Dimension(before + 500, comboBox.getPreferredSize().height));

        ExceptionResolutionEditBox.widenToFitContents(comboBox, new String[]{"short"});

        assertThat(comboBox.getPreferredSize().width).isEqualTo(before + 500);
    }

    @Test
    void toleratesAnEmptyOrAllNullItemList() {
        ComboBox<String> comboBox = new ComboBox<>(new String[0]);
        int before = comboBox.getPreferredSize().width;

        ExceptionResolutionEditBox.widenToFitContents(comboBox, new String[0]);
        assertThat(comboBox.getPreferredSize().width).isEqualTo(before);

        ExceptionResolutionEditBox.widenToFitContents(comboBox, new String[]{null, null});
        assertThat(comboBox.getPreferredSize().width).isEqualTo(before);
    }
}
