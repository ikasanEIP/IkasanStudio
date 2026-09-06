package org.ikasan.studio.ui.icons;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.IconLoader;
import org.apache.commons.io.FilenameUtils;
import org.ikasan.studio.core.metapack.model.ComponentMeta;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.net.URL;

/** Resolves Swing/IntelliJ icons from the resource identifiers held by core component metadata. */
public final class ComponentIconProvider {
    private static final Logger LOG = Logger.getInstance(ComponentIconProvider.class);
    private static final String GENERAL_ICONS_DIR = "studio/icons/";
    private static final String UNKNOWN_ICONS_DIR = GENERAL_ICONS_DIR + "unknown/";

    private ComponentIconProvider() {
    }

    public static Icon getSmallIcon(ComponentMeta meta) {
        return component(meta, "small.png", "Small " + meta.getName() + " icon");
    }

    public static Icon getCanvasIcon(ComponentMeta meta) {
        return component(meta, "normal.png", "Medium " + meta.getName() + " icon");
    }

    public static Icon getWiretapIcon() { return general("wiretap.png", "Wiretap"); }
    public static Icon getLogWiretapIcon() { return general("log-wiretap.png", "Log Wiretap"); }
    public static Icon getSendTestMessageIcon() { return general("send-test-message.png", "Send Test Message"); }
    public static Icon getSendTestMessageFileIcon() { return general("send-test-message-file.png", "Send Test Message (File)"); }
    public static Icon getTriggerIcon() { return general("trigger.png", "Trigger Now"); }
    public static Icon getReplayServiceIcon() { return general("replay-service.png", "Flow recording enabled"); }
    public static Icon getMailServerIcon() { return general("mailserver.png", "Test Mail Server"); }
    public static Icon getFtpServerIcon() { return general("testftpserver.png", "Test FTP Server"); }

    public static int getDecoratorHeight() {
        return Math.max(getWiretapIcon().getIconHeight(), getLogWiretapIcon().getIconHeight());
    }

    public static Icon getGeneralIcon(String filename, String description) {
        return general(filename, description);
    }

    private static Icon component(ComponentMeta meta, String filename, String description) {
        String directory = meta.getIconResourceDirectory();
        String path = directory != null ? directory + "/" + filename : UNKNOWN_ICONS_DIR + filename;
        return load(path, UNKNOWN_ICONS_DIR + filename, description);
    }

    private static Icon general(String filename, String description) {
        return load(GENERAL_ICONS_DIR + filename, null, description);
    }

    private static Icon load(String pngPath, String fallbackPngPath, String description) {
        // IconLoader owns IDE-aware caching and theme/scale invalidation. A second plugin-level cache would
        // retain icons across look-and-feel changes and plugin reloads.
        return loadUncached(pngPath, fallbackPngPath, description);
    }

    private static Icon loadUncached(String pngPath, String fallbackPngPath, String description) {
        String svgPath = FilenameUtils.removeExtension(pngPath) + ".svg";
        URL svgUrl = ComponentIconProvider.class.getClassLoader().getResource(svgPath);
        if (svgUrl != null) {
            Icon svgIcon = IconLoader.findIcon(svgUrl);
            if (svgIcon != null) {
                return svgIcon;
            }
            LOG.warn("STUDIO: Could not load SVG icon " + svgPath + ", using PNG fallback");
        }
        URL pngUrl = ComponentIconProvider.class.getClassLoader().getResource(pngPath);
        if (pngUrl == null && fallbackPngPath != null) {
            LOG.warn("STUDIO: Could not load icon " + pngPath + ", using default");
            pngUrl = ComponentIconProvider.class.getClassLoader().getResource(fallbackPngPath);
        }
        return new ImageIcon(pngUrl, description);
    }
}
