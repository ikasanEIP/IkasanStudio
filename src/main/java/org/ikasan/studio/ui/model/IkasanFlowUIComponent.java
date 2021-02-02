package org.ikasan.studio.ui.model;

import org.ikasan.studio.model.Ikasan.IkasanFlowComponentType;

import javax.swing.*;

/**
 * Focuses on the presentation / UI specific features for the Ikasan component.
 *
 * The technical details of a component are encapsulated in org.ikasan.studio.model.Ikasan.IkasanFlowComponentType
 */
public class IkasanFlowUIComponent {
    String title;
    String helpText;
    String webHelpURL;
    IkasanFlowComponentType ikasanFlowComponentType;
    ImageIcon smallIcon;
    ImageIcon canvasIcon;

    public IkasanFlowUIComponent(String title) {
        this.title = title;
    }
    public IkasanFlowUIComponent(String title, String helpText, String webHelpURL, IkasanFlowComponentType ikasanFlowComponentType
            , ImageIcon smallIcon, ImageIcon canvasIcon) {
        this.title = title;
        this.helpText = helpText;
        this.webHelpURL = webHelpURL;
        this.ikasanFlowComponentType = ikasanFlowComponentType;
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

    public IkasanFlowComponentType getIkasanFlowComponentType() {
        return ikasanFlowComponentType;
    }

    public void setIkasanFlowComponentType(IkasanFlowComponentType ikasanFlowComponentType) {
        this.ikasanFlowComponentType = ikasanFlowComponentType;
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
                ", ikasanFlowComponentType=" + ikasanFlowComponentType +
                ", smallIcon=" + smallIcon +
                ", canvasIcon=" + canvasIcon +
                '}';
    }
}
