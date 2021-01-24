package org.ikasan.studio.ui;

import org.apache.log4j.Logger;
import org.ikasan.studio.Context;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Studion UI StudioUtils.
 */
public class StudioUIUtils {
    public static final Color IKASAN_GREY = new Color(231, 231, 231);
    private static final Logger log = Logger.getLogger(StudioUIUtils.class);

    public static void setLine(Graphics g, float width) {
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D)g;
            g2d.setStroke(new BasicStroke(2f));
        }
    }

    public static Font getBoldFont(Graphics g) {
        return new Font(g.getFont().getName(), Font.BOLD, g.getFont().getSize());
    }

    public static void displayErrorMessage(String projectKey, String message) {
        JTextArea canvasTextArea = Context.getCanvasTextArea(projectKey);
        canvasTextArea.setForeground(Color.RED);
        canvasTextArea.setText(message);
    }

    public static void displayMessage(String projectKey, String message) {
        JTextArea canvasTextArea = Context.getCanvasTextArea(projectKey);
        canvasTextArea.setForeground(Color.BLACK);
        canvasTextArea.setText(message);
    }

    public static int getTextHeight(Graphics g) {
        FontMetrics metrics = g.getFontMetrics();
        return metrics.getHeight();
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
     * @param g
     * @param paintMode
     * @param text
     * @param centerX
     * @param topY
     * @param maxWidth
     * @param font
     * @return the bottom y value of the last string (se we know how far down we went)
     * //@todo turn all strings into components to support better x,y,width,height
     */
    public static int drawCenteredStringFromTopCentre(Graphics g, PaintMode paintMode, String text, int centerX, int topY, int maxWidth, Font font) {
        if (maxWidth <= 0) {
            log.error("Call to drawCenteredStringFromTopCentre with non-positive width, was [" + maxWidth + "]");
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
        int currentBottomY = numberOfLines > 0 ? textY - stringHeight : topY;
//        log.info("drawCenteredStringFromTopCentre : paintMode [" + paintMode + "] text [" + text + "] centerX [" + centerX + "] topy [" + topY + "] maxWidth [" + maxWidth + "] font [" + font + "] leavingBottomY [" + currentBottomY + "]");
        return currentBottomY;
    }

    /**
     * Draw the text string, center the first line at centerX and CenterY
     * If the string is bigger then maxWidth, split it over multiple substrings
     * @param g
     * @param text
     * @param centerX
     * @param centerY
     * @param maxWidth
     * @param font
     * @return the bottom y value of the last string (se we know how far down we went)
     */
    public static int drawCenteredStringFromMiddleCentre(Graphics g, PaintMode paintMode, String text, int centerX, int centerY, int maxWidth, Font font) {
        int stringHeight = StudioUIUtils.getTextHeight(g);
        int intialY = centerY - (stringHeight / 2);
        return drawCenteredStringFromTopCentre(g, paintMode, text, centerX, intialY, maxWidth, font);
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
            for (int index = 0; index < splitInput.length; index++) {
                subString.append(splitInput[index]).append(" ");
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

    public static void paintGrid(Graphics g, int x, int y, int width, int height) {
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
}
