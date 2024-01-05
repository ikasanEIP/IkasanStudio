package org.ikasan.studio.ui.model;

import org.ikasan.studio.model.ikasan.meta.IkasanComponentType;

import javax.swing.*;
import java.io.Serializable;

/**
 * Focuses on the presentation / UI specific features for the ikasan component.
 *
 * The technical details of a component are encapsulated in org.ikasan.studio.model.ikasan.meta.IkasanComponentType
 */
public class IkasanFlowUIComponent implements Serializable {
    String title;
    String helpText;
    String webHelpURL;
    IkasanComponentType ikasanComponentType;
    ImageIcon smallIcon;
    ImageIcon canvasIcon;

    public IkasanFlowUIComponent(String title) {
        this.title = title;
    }
    public IkasanFlowUIComponent(String title, String helpText, String webHelpURL, IkasanComponentType ikasanComponentType
            , ImageIcon smallIcon, ImageIcon canvasIcon) {
        this.title = title;
        this.helpText = helpText;
        this.webHelpURL = webHelpURL;
        this.ikasanComponentType = ikasanComponentType;
        this.smallIcon = smallIcon;
        this.canvasIcon = canvasIcon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public String getWebHelpURL() {
        return webHelpURL;
    }

    public IkasanComponentType getIkasanComponentType() {
        return ikasanComponentType;
    }

    public void setIkasanComponentType(IkasanComponentType ikasanComponentType) {
        this.ikasanComponentType = ikasanComponentType;
    }

    public ImageIcon getSmallIcon() {
        return smallIcon;
    }

    public void setSmallIcon(ImageIcon smallIcon) {
        this.smallIcon = smallIcon;
    }

    public ImageIcon getCanvasIcon() {
        return canvasIcon;
    }

    public void setCanvasIcon(ImageIcon canvasIcon) {
        this.canvasIcon = canvasIcon;
    }

    @Override
    public String toString() {
        return "IkasanFlowUIComponent{" +
                "text='" + title + '\'' +
                ", helpText='" + helpText + '\'' +
                ", ikasanComponentType=" + ikasanComponentType +
                ", smallIcon=" + smallIcon +
                ", canvasIcon=" + canvasIcon +
                '}';
    }
}
