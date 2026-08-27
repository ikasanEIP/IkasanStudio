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
