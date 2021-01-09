package org.ikasan.studio.ui.model;

import org.ikasan.studio.model.Ikasan.IkasanFlowElementCategory;
import org.ikasan.studio.model.Ikasan.IkasanFlowElementType;

import javax.swing.*;
import java.io.Serializable;

public class IkasanFlowUIComponent implements Serializable {
    String text ;
    String helpText;
    String webHelpURL;
    IkasanFlowElementCategory ikasanFlowElementCategory;
    IkasanFlowElementType ikasanFlowElementType;
    ImageIcon smallIcon;
    ImageIcon canvasIcon;

    public IkasanFlowUIComponent(String text, String helpText, String webHelpURL, IkasanFlowElementType ikasanFlowElementType
            , ImageIcon smallIcon, ImageIcon canvasIcon) {
        this.text = text;
        this.helpText = helpText;
        this.webHelpURL = webHelpURL;
        this.ikasanFlowElementCategory = ikasanFlowElementType.getElementCategory();
        this.ikasanFlowElementType = ikasanFlowElementType;
        this.smallIcon = smallIcon;
        this.canvasIcon = canvasIcon;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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

    public IkasanFlowElementCategory getIkasanFlowElementCategory() {
        return ikasanFlowElementCategory;
    }

    public void setIkasanFlowElementCategory(IkasanFlowElementCategory ikasanFlowElementCategory) {
        this.ikasanFlowElementCategory = ikasanFlowElementCategory;
    }

    public IkasanFlowElementType getIkasanFlowElementType() {
        return ikasanFlowElementType;
    }

    public void setIkasanFlowElementType(IkasanFlowElementType ikasanFlowElementType) {
        this.ikasanFlowElementType = ikasanFlowElementType;
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
                "text='" + text + '\'' +
                ", helpText='" + helpText + '\'' +
                ", ikasanFlowElementCategory=" + ikasanFlowElementCategory +
                ", ikasanFlowElementType=" + ikasanFlowElementType +
                ", smallIcon=" + smallIcon +
                ", canvasIcon=" + canvasIcon +
                '}';
    }
}
