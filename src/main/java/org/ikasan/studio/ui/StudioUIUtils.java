package org.ikasan.studio.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.ui.viewmodel.AbstractViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandlerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class StudioUIUtils {

    // Private constructor emphasizes that this is a utils class, not to be instantiated.
    private StudioUIUtils() {}

    public static final Color IKASAN_GREY = Gray._231;
    public static final Color IKASAN_ORANGE = new JBColor(new Color(241, 90, 35), new Color(241, 90, 35));
    private static final Logger LOG = Logger.getInstance("#StudioUIUtils");

    public static void setLine(Graphics g, float width) {
        if (g instanceof Graphics2D g2d) {
            g2d.setStroke(new BasicStroke(width));
        }
    }

    public static AbstractViewHandler getViewHandler(BasicElement basicElement) {
        if (basicElement.getViewHandler() == null) {
            basicElement.setViewHandler(ViewHandlerFactory.getInstance(basicElement));
        }
        return (AbstractViewHandler)basicElement.getViewHandler();
    }


    public static Font getBoldFont(Graphics g) {
        return new Font(g.getFont().getName(), Font.BOLD, g.getFont().getSize());
    }

    public static void displayErrorMessage(String projectKey, String message) {
        JTextArea canvasTextArea = Context.getCanvasTextArea(projectKey);
        canvasTextArea.setForeground(JBColor.RED);
        canvasTextArea.setText(message);
    }

    public static void displayMessage(String projectKey, String message) {
        JTextArea canvasTextArea = Context.getCanvasTextArea(projectKey);
        canvasTextArea.setForeground(JBColor.BLACK);
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
        g.drawString(text, leftX, topY + stringHeight);
        g.setFont(origFont);
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
     * //@todo turn all strings into components to support better x,y,width,height
     */
    public static int drawCenteredStringFromTopCentre(Graphics g, PaintMode paintMode, String text, int centerX, int topY, int maxWidth, Font font) {
        if (maxWidth <= 0) {
            LOG.warn("Call to drawCenteredStringFromTopCentre with non-positive width, was [" + maxWidth + "]");
            maxWidth = 1;
        }
        Font origFont = g.getFont();
        if (font != null) {
            g.setFont(font);
        }
        if (text == null) {
            text = "";
        }
        int stringHeight = StudioUIUtils.getTextHeight(g);
        int stringWidth = StudioUIUtils.getTextWidth(g, text);
        List<String> textToDisplay = StudioUIUtils.splitStringIntoMultipleRows(text, (stringWidth/maxWidth)+1);
        int textY = topY + stringHeight;  // remember the y co-ord for drawstring is the baseline, not the top of the string.
        int numberOfLines = 0;  // debugging
        for (String subString : textToDisplay) {
            int textX = centerX - (StudioUIUtils.getTextWidth(g, subString) / 2);
            if (paintMode.equals(PaintMode.PAINT)) {
                g.drawString(subString, textX, textY);
            }
            numberOfLines ++;
            textY+= stringHeight;
        }
        g.setFont(origFont);
        return numberOfLines > 0 ? textY - stringHeight : topY;
    }

    /**
     * Draw the text string, center the first line at centerX and CenterY
     * If the string is bigger then maxWidth, split it over multiple substrings
     * @param g the graphics object
     * @param text to display
     * @param centerX for the text
     * @param centerY for the text
     * @param maxWidth for the text
     * @param font for the text
     * @return the bottom y value of the last string (se we know how far down we went)
     */
    public static int drawCenteredStringFromMiddleCentre(Graphics g, PaintMode paintMode, String text, int centerX, int centerY, int maxWidth, Font font) {
        int stringHeight = StudioUIUtils.getTextHeight(g);
        int initialY = centerY - (stringHeight / 2);
        return drawCenteredStringFromTopCentre(g, paintMode, text, centerX, initialY, maxWidth, font);
    }

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

    public static void paintGrid(Graphics g, int width, int height) {
        Graphics2D g2d = (Graphics2D) g.create();
        Stroke dashed = new BasicStroke(1);
        g2d.setStroke(dashed);

        for (int yy = 0 ; yy < width ; yy += 100) {
            g2d.drawLine(0, yy, width, yy);
        }

        for (int xx = 0 ; xx < height ; xx += 100) {
            g2d.drawLine(xx, 0, xx, height);
        }
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
            g.setColor(JBColor.LIGHT_GRAY);
            g.fillRect(popupX, popupY, width, height);

            // Border
            g.setColor(JBColor.RED);
            g.drawRect(popupX, popupY, width, height);

            // Text
            g.setColor(JBColor.BLACK);
            StudioUIUtils.drawStringLeftAlignedFromTopLeft(g, text, popupX + 3, popupY + 3, font);
            g.setColor(oldColor);
        }
    }
}
