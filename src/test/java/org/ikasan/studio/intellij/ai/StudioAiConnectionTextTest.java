package org.ikasan.studio.intellij.ai;

import java.awt.Font;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.text.StyleConstants;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StudioAiConnectionTextTest {
    @Test void initialWrappingDoesNotDemandScreenHeight() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (int fontSize : new int[]{13, 18}) {
                StudioAiConnectionText text = new StudioAiConnectionText("Open MCP settings and configure the client. ".repeat(15));
                text.setFont(new Font(Font.DIALOG, Font.PLAIN, fontSize));
                text.setLineWrap(true);
                text.setWrapStyleWord(true);
                assertThat(text.getWidth()).isZero();
                assertThat(text.getPreferredSize().height).isBetween(fontSize, 500);
                assertThat(text.getMinimumSize().height).isLessThan(40);
            }
        });
    }

    @Test void guidanceReflowsWhenNarrowedAndShrinksWhenTextChanges() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            StudioAiConnectionText text = new StudioAiConnectionText("Select Codex and paste the test prompt. ".repeat(15));
            text.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setSize(740, 200);
            int wideHeight = text.getPreferredSize().height;
            text.setSize(370, 200);
            assertThat(text.getPreferredSize().height).isGreaterThan(wideHeight);
            text.setText("Connection test passed.");
            assertThat(text.getPreferredSize().height).isLessThan(wideHeight);
        });
    }
    @Test void styledInstructionsStayCompactAndEmphasiseCheckboxLabel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            StudioAiConnectionInstructions instructions = new StudioAiConnectionInstructions(() -> { });
            int position = instructions.getText().indexOf("Enable MCP Server");
            assertThat(position).isGreaterThanOrEqualTo(0);
            assertThat(StyleConstants.isBold(instructions.getStyledDocument()
                    .getCharacterElement(position).getAttributes())).isTrue();
            assertThat(instructions.getPreferredSize().height).isBetween(50, 500);
            assertThat(instructions.getMinimumSize().height).isLessThan(40);
        });
    }
    @Test void inlineSettingsButtonInvokesSettingsAction() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicBoolean opened = new AtomicBoolean();
            StudioAiConnectionInstructions instructions = new StudioAiConnectionInstructions(() -> opened.set(true));
            // Keep the object replacement character visible as an escape.
            //noinspection UnnecessaryUnicodeEscape
            int position = instructions.getText().indexOf('\uFFFC');
            assertThat(position).isGreaterThanOrEqualTo(0);
            JButton button = (JButton) StyleConstants.getComponent(
                    instructions.getStyledDocument().getCharacterElement(position).getAttributes());
            assertThat(button.getText()).isEqualTo("Open IntelliJ MCP settings");
            assertThat(button.getFont().isBold()).isTrue();
            button.doClick();
            assertThat(opened).isTrue();
        });
    }

}
