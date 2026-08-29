package org.ikasan.studio.ui;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StudioUIUtils {
    public static final String NOTIFICATION_GROUP_ID = "Ikasan Studio";
    // Private constructor emphasizes that this is a utils class, not to be instantiated.
    private StudioUIUtils() {}

    //    public static final Color IKASAN_ORANGE = new JBColor(new Color(241, 90, 35), new Color(241, 90, 35));
    private static final Logger LOG = Logger.getInstance("#StudioUIUtils");

    /**
     * Escapes text so it renders literally inside an HTML fragment instead of being parsed as markup - e.g. a
     * generic Java type like "java.util.List&lt;java.io.File&gt;" would otherwise have its "&lt;java.io.File&gt;"
     * read as an (unrecognised) tag, which Swing's HTML renderer draws as a stray bordered box rather than
     * plain text. Callers building HTML strings from plain-text values (component names, type descriptions,
     * warning messages, ...) must run each one through this before concatenating it into markup.
     * @param text plain text that may contain HTML-significant characters
     * @return text with &amp;, &lt; and &gt; replaced by their HTML entities
     */
    public static String escapeHtml(String text) {
        if (text == null) {
            return null;
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * A compact "Input: X" / "Output: Y" HTML block, shared by the properties panel (a real component on the
     * canvas, showing its actual configured values) and the palette (no instance yet, showing what a
     * freshly-dropped one would default to - see ComponentMeta#getEffectiveInputTypeDescriptionPreview). Either
     * half is omitted (not replaced with a placeholder) when nothing can be said - e.g. no Output for a
     * terminal Producer, no Input for a flow-starting Consumer.
     * @param input from FlowElement#getEffectiveInputTypeDescription or ComponentMeta's preview equivalent
     * @param output from FlowElement#getEffectiveOutputTypeDescription or ComponentMeta's preview equivalent
     * @param isPreview true for the palette (values are defaults, not live) - appends a qualifier so it can't
     *                  be mistaken for a fixed guarantee
     * @return an HTML fragment, or null if neither half has anything to show
     */
    public static String buildInputOutputSummaryHtml(String input, String output, boolean isPreview) {
        if (input == null && output == null) {
            return null;
        }
        String suffix = isPreview ? " <i>(default)</i>" : "";
        StringBuilder summary = new StringBuilder("<p>");
        if (input != null) {
            summary.append("<b>Input:</b> ").append(escapeHtml(input)).append(suffix);
        }
        if (output != null) {
            if (input != null) {
                summary.append("<br>");
            }
            summary.append("<b>Output:</b> ").append(escapeHtml(output)).append(suffix);
        }
        summary.append("</p>");
        return summary.toString();
    }

    /**
     * The full help-panel summary block: the component's own name as a heading, always first (with the
     * underlying Ikasan class alongside it, where meaningful - see implementingClassName below), followed by
     * the "Input:/Output:" block from {@link #buildInputOutputSummaryHtml} (if either has anything to show).
     * The name comes from metadata (ComponentMeta#getName()) rather than relying on it already being
     * hand-written into the component's helpText - every component's helpText used to open with its own
     * hardcoded "&lt;strong&gt;Name&lt;/strong&gt;", which not only put the name in the wrong place relative to
     * this summary but meant every metapack author had to remember to keep it in sync with the component's
     * actual name by hand.
     * @param componentName the component's own name, e.g. "Local File Consumer"
     * @param implementingClassName the fully-qualified (or simple) Ikasan core class this component wires in
     *                              directly, or null/blank to omit - only pass this for components with no
     *                              user-implemented class of their own, where ComponentMeta#getImplementingClass()
     *                              names a real, concrete class rather than an interface used purely as a
     *                              deserialisation key (see ComponentMeta#isUseImplementingClassInFactory() -
     *                              that flag is precisely "implementingClass is real and gets 'new'd directly",
     *                              see componentFactory_en.ftl) - passing an interface/placeholder here would
     *                              mislead rather than help
     * @param input from FlowElement#getEffectiveInputTypeDescription or ComponentMeta's preview equivalent
     * @param output from FlowElement#getEffectiveOutputTypeDescription or ComponentMeta's preview equivalent
     * @param isPreview true for the palette (values are defaults, not live) - see buildInputOutputSummaryHtml
     * @return an HTML fragment - never null/empty as long as componentName is set
     */
    public static String buildComponentSummaryHtml(String componentName, String implementingClassName, String input, String output, boolean isPreview) {
        StringBuilder summary = new StringBuilder();
        if (componentName != null && !componentName.isBlank()) {
            summary.append("<p><b>").append(escapeHtml(componentName)).append("</b>");
            if (implementingClassName != null && !implementingClassName.isBlank()) {
                String simpleName = implementingClassName.contains(".")
                        ? implementingClassName.substring(implementingClassName.lastIndexOf('.') + 1) : implementingClassName;
                summary.append(" <font color=\"gray\">(Ikasan class: ").append(escapeHtml(simpleName)).append(")</font>");
            }
            summary.append("</p>");
        }
        String inputOutput = buildInputOutputSummaryHtml(input, output, isPreview);
        if (inputOutput != null) {
            summary.append(inputOutput);
        }
        return summary.toString();
    }

    /**
     * A "More info" link to a component's webHelpURL, shown at the end of its help text (see
     * ComponentPropertiesPanel#getDisplayedHelpTextForSelectedComponent / IkasanPaletteElementViewHandler#
     * getHelpText). Requires the containing panel to be non-editable with a HyperlinkListener wired up (see
     * HtmlScrollingDisplayPanel) - Swing only fires hyperlink events under those conditions, otherwise this
     * would render as blue, underlined text that does nothing when clicked.
     * @param webHelpURL from ComponentMeta#getWebHelpURL() - never null (defaults to a bundled Readme.md
     *                   reference), but check for blank defensively since a metapack could still set it that way
     * @return an HTML fragment, or null if webHelpURL is blank
     */
    public static String buildMoreInfoLinkHtml(String webHelpURL) {
        if (webHelpURL == null || webHelpURL.isBlank()) {
            return null;
        }
        return "<p><a href=\"" + escapeHtml(webHelpURL) + "\">More info</a></p>";
    }

    public static void setLine(Graphics g, float width) {
        if (g instanceof Graphics2D g2d) {
            g2d.setStroke(new BasicStroke(width));
        }
    }


    public static Font getBoldFont(Graphics g) {
        return StudioUIUtils.getMainFont();
    }

    public static void displayErrorMessage(Project project, String message) {
        JTextArea canvasTextArea = project.getService(UiContext.class).getCanvasTextArea();
        canvasTextArea.setForeground(Styling.IKASAN_RED);
        canvasTextArea.setText(message);
    }

    public static void displayMessage(Project project, String message) {
        JTextArea canvasTextArea = project.getService(UiContext.class).getCanvasTextArea();
        canvasTextArea.setForeground(Styling.IKASAN_BLACK);
        canvasTextArea.setText(message);
    }

    public static int getTextHeight(Graphics g, Font font) {
        Font oldFond = g.getFont();
        g.setFont(font);
        int height = getTextHeight(g);
        g.setFont(oldFond);
        return height;
    }

    public static int getTextHeight(Graphics g) {
        FontMetrics metrics = g.getFontMetrics();
        return metrics.getHeight();
    }

    public static int getTextWidth(Graphics g, String text, Font font) {
        Font oldFond = g.getFont();
        g.setFont(font);
        int width = getTextWidth(g, text);
        g.setFont(oldFond);
        return width;
    }

    public static int getTextWidth(Graphics g, String text) {
        if (text == null) {
            text = "";
        }
        FontMetrics metrics = g.getFontMetrics();
        return metrics.stringWidth(text);
    }

    /**
     * Draw the text string on the screen at the given co-ords
     * @param g graphics object
     * @param text to display
     * @param leftX for the text position
     * @param topY for the text position
     * @param font for the text
     */
    public static void drawStringLeftAlignedFromTopLeft(Graphics g, String text, int leftX, int topY, Font font) {
        Font origFont = g.getFont();
        if (font != null) {
            g.setFont(font);
        }
        if (text == null) {
            text = "";
        }
        int stringHeight = StudioUIUtils.getTextHeight(g);
        // remember the y co-ord for drawstring is the baseline, not the top of the string.
        drawAliasedText((Graphics2D)g, text, leftX, topY + stringHeight, font);
        g.setFont(origFont);
    }

    /**
     * Draw the text string on the screen at the given co-ords using anti-aliased fonts
     * @param g2d graphics object
     * @param text to display
     * @param leftX for the text position
     * @param topY for the text position
     * @param font for the text
     */
    private static void drawAliasedText(Graphics2D g2d, String text, int leftX, int topY, Font font) {
        Font origFont = g2d.getFont();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2d.setFont(font);
        g2d.drawString(text, leftX, topY);
        g2d.setFont(origFont);
    }

    /**
     * Draw the text string, center the first line at centerX but take the top Y as topY
     * If the string is bigger then maxWidth, split it over multiple substrings
     * @param g the graphic object
     * @param paintMode to use
     * @param text to display
     * @param centerX for the text
     * @param topY for the text
     * @param maxWidth for the text
     * @param font for the text
     * @return the bottom y value of the last string (se we know how far down we went)
     * //@todo turn all strings into components to support better TestV1,y,width,height
     */
    public static int drawCenteredStringFromTopCentre(Graphics g, PaintMode paintMode, String text, int centerX, int topY, int maxWidth, Font font) {
        int numberOfLines = 0;
        int stringHeight = 0;
        int textY = 0;
        if (text != null && !text.isEmpty() &&  !"null".equals(text)) {
            if (maxWidth <= 0) {
                LOG.warn("STUDIO: Call to drawCenteredStringFromTopCentre with non-positive width, was [" + maxWidth + "]");
                maxWidth = 1;
            }
            Font origFont = g.getFont();
            if (font != null) {
                g.setFont(font);
            }
             stringHeight = StudioUIUtils.getTextHeight(g);
            int stringWidth = StudioUIUtils.getTextWidth(g, text);
            List<String> textToDisplay = StudioUIUtils.splitStringIntoMultipleRows(text, (stringWidth/maxWidth)+1);
            textY = topY + stringHeight;  // remember the y co-ord for drawstring is the baseline, not the top of the string.

            for (String subString : textToDisplay) {
                int textX = centerX - (StudioUIUtils.getTextWidth(g, subString) / 2);
                if (paintMode.equals(PaintMode.PAINT)) {
                    drawAliasedText((Graphics2D)g, subString, textX, textY, font);
                }
                numberOfLines ++;
                textY+= stringHeight;
            }
            g.setFont(origFont);
        }

        return numberOfLines > 0 ? textY - stringHeight : topY;
    }

//    /**
//     * Draw the text string, center the first line at centerX and CenterY
//     * If the string is bigger then maxWidth, split it over multiple substrings
//     * @param g the graphics object
//     * @param text to display
//     * @param centerX for the text
//     * @param centerY for the text
//     * @param maxWidth for the text
//     * @param font for the text
//     * @return the bottom y value of the last string (se we know how far down we went)
//     */
//    public static int drawCenteredStringFromMiddleCentre(Graphics g, PaintMode paintMode, String text, int centerX, int centerY, int maxWidth, Font font) {
//        int stringHeight = StudioUIUtils.getTextHeight(g);
//        int initialY = centerY - (stringHeight / 2);
//        return drawCenteredStringFromTopCentre(g, paintMode, text, centerX, initialY, maxWidth, font);
//    }

    public static List<String> splitStringIntoMultipleRows(String text, int numberOfRows) {
        List<String> returnList = new ArrayList<>() ;
        if (numberOfRows == 0 ) {
            numberOfRows = 1;
        }
        if (text != null && numberOfRows > 0 && numberOfRows < text.length()) {
            int savedSpaces = numberOfRows - 1;
            int targetLength = (text.length() - savedSpaces) / numberOfRows;
            String[] splitInput = text.split("\\s+");
            StringBuilder subString = new StringBuilder();
            for (String s : splitInput) {
                subString.append(s).append(" ");
                if (subString.length() >= targetLength) {
                    returnList.add(subString.toString().trim());
                    // If we are on the last row, just absorb remaining words.
                    if (returnList.size() < numberOfRows) {
                        subString = new StringBuilder();
                    }
                }
            }
        }
        return returnList;
    }

    public static void paintWarningPopup(Graphics g, int x, int y, int maxX,int maxY, String text) {
        if (!text.isEmpty()) {
            Font font = StudioUIUtils.getBoldFont(g) ;

            int width = StudioUIUtils.getTextWidth(g, text, font) + 10;
            int height = StudioUIUtils.getTextHeight(g, font) + 10;

            int popupX = x + width < maxX ? x : x - width - 20;
            int popupY = y + height < maxY ? y : y -height -20;
            if (popupX < 0) {
                popupX = 0;
            }
            if (popupY < 0) {
                popupY = 0;
            }

            Color oldColor = g.getColor();
            // Central rectangle
            g.setColor(Styling.IKASAN_GREY);
            g.fillRect(popupX, popupY, width, height);

            // Border
            g.setColor(Styling.IKASAN_RED);
            g.drawRect(popupX, popupY, width, height);

            // Text
            g.setColor(Styling.IKASAN_BLACK);
            StudioUIUtils.drawStringLeftAlignedFromTopLeft(g, text, popupX + 3, popupY + 3, font);
            g.setColor(oldColor);
        }
    }

    public static void displayIdeaInfoMessage(Project project, String message) {
        NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(message, NotificationType.INFORMATION)
                .notify(project);
    }
    public static void displayIdeaWarnMessage(Project project, String message) {
        NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(message, NotificationType.WARNING)
                .notify(project);
    }
    public static void displayIdeaErrorMessage(Project project, String message) {
        NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(message, NotificationType.ERROR)
                .notify(project);
    }

    public static Color getLineColor() {
        return UIManager.getColor("Separator.separatorColor");
    }
    public static Font getMainFont() {
        Font uiFont = UIManager.getFont("TextArea.font");
        if (uiFont == null) {
            uiFont = UIManager.getFont("EditorPane.font");
        }
        return uiFont;
    }

    /**
     * Adds a right-click "Copy" context menu item to a plain, non-editable display component (e.g. a property's
     * JLabel) that copies the supplied text to the system clipboard. JLabel offers no built-in text selection, so
     * without this a user troubleshooting a specific property has no way to copy its exact name to paste into a
     * bug report / support message.
     * @param component the component to attach the popup trigger to (typically a JLabel).
     * @param textSupplier supplies the text to copy at click time, rather than a fixed String, so callers whose
     *                      label can change (e.g. a live cue appended to it) always copy the current text.
     */
    public static void makeCopyable(JComponent component, java.util.function.Supplier<String> textSupplier) {
        JPopupMenu copyMenu = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem(StudioBundle.message("button.Copy"));
        copyItem.addActionListener(e -> {
            String text = textSupplier.get();
            if (text != null) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(text), null);
            }
        });
        copyMenu.add(copyItem);
        component.addMouseListener(new java.awt.event.MouseAdapter() {
            private void maybeShowPopup(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    copyMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            @Override public void mousePressed(java.awt.event.MouseEvent e) { maybeShowPopup(e); }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) { maybeShowPopup(e); }
        });
    }

    /**
     * @return all validationIssues' messages joined one-per-line, or the empty string if there are none - shared
     * by every "OK"/"Update Code" button that surfaces its own disabled-reason as a tooltip (see
     * setAttentionPulse below), so the sidebar panel and the first-time popup dialogue present the same message.
     */
    public static String joinValidationMessages(List<ValidationInfo> validationIssues) {
        return validationIssues == null ? "" :
                validationIssues.stream().map(issue -> issue.message).collect(Collectors.joining("\n"));
    }

    private static final String ATTENTION_PULSE_TIMER_PROPERTY = "ikasan.studio.attentionPulseTimer";
    private static final String ATTENTION_PULSE_ORIGINAL_BORDER_PROPERTY = "ikasan.studio.attentionPulseOriginalBorder";
    // Ikasan brand orange (same values as ThemeAwareColors#getImportantBorderColor's own fallback) - deliberately
    // NOT sourced via that method (or ThemeAwareColors#getUrgentColor), since both look up UIManager keys like
    // "Component.borderColor"/"Separator.separatorColor" first, which are non-null in virtually every theme and
    // so silently shadow the intended orange/red fallback with a neutral, barely-visible border colour - exactly
    // why the original pulse "didn't stand out" in either light or dark mode. A slightly brighter shade is used
    // for dark themes, since the same fixed orange reads as muddier against a dark background.
    private static final JBColor ATTENTION_PULSE_COLOR = new JBColor(new Color(241, 90, 35), new Color(255, 140, 70));

    /**
     * Starts (or stops) a pulsating coloured border on the given button - used to draw the developer's eye to a
     * button that's gone disabled specifically because of a validation failure (as opposed to merely "nothing
     * has changed yet"), since a silently-disabled button is easy to miss, especially when the actual reason is
     * only discoverable by hovering over its tooltip. Idempotent and safe to call on every validation pass:
     * calling with active=true while already pulsing, or active=false while already stopped, is a no-op, so
     * callers never need to track pulsing state themselves.
     * -
     * State (the running Timer and the button's original border, to restore exactly) is stashed on the button
     * itself via client properties rather than in some external map, so it's inherently scoped to the button's
     * own lifecycle - no separate cleanup/disposal path is needed.
     * @param button the button to pulse - a no-op if null (e.g. a dialog whose buttons haven't been created yet).
     * @param active true to start pulsing (or keep pulsing), false to stop and restore the original border.
     */
    public static void setAttentionPulse(JButton button, boolean active) {
        if (button == null) {
            return;
        }
        Timer existingTimer = (Timer) button.getClientProperty(ATTENTION_PULSE_TIMER_PROPERTY);
        if (active) {
            if (existingTimer != null) {
                return;
            }
            Border originalBorder = button.getBorder();
            // Same outer inset (3px) on both the "on" and "off" borders - only the line's colour changes between
            // the vivid orange and fully transparent, so the button's size/position never jitters as it pulses.
            Border pulseOnBorder = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ATTENTION_PULSE_COLOR, 3), originalBorder);
            Border pulseOffBorder = BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(3, 3, 3, 3), originalBorder);
            boolean[] pulseOn = {false};
            Timer timer = new Timer(450, e -> {
                pulseOn[0] = !pulseOn[0];
                button.setBorder(pulseOn[0] ? pulseOnBorder : pulseOffBorder);
            });
            button.putClientProperty(ATTENTION_PULSE_ORIGINAL_BORDER_PROPERTY, originalBorder);
            button.putClientProperty(ATTENTION_PULSE_TIMER_PROPERTY, timer);
            timer.start();
        } else if (existingTimer != null) {
            existingTimer.stop();
            button.setBorder((Border) button.getClientProperty(ATTENTION_PULSE_ORIGINAL_BORDER_PROPERTY));
            button.putClientProperty(ATTENTION_PULSE_TIMER_PROPERTY, null);
            button.putClientProperty(ATTENTION_PULSE_ORIGINAL_BORDER_PROPERTY, null);
        }
    }
}
