package org.ikasan.studio.ui.icons;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.IconLoader;
import org.apache.commons.io.FilenameUtils;
import org.ikasan.studio.core.metapack.model.ComponentMeta;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Resolves Swing/IntelliJ icons from the resource identifiers held by core component metadata. */
public final class ComponentIconProvider {
    private static final Logger LOG = Logger.getInstance(ComponentIconProvider.class);
    private static final String GENERAL_ICONS_DIR = "studio/icons/";
    private static final String UNKNOWN_ICONS_DIR = GENERAL_ICONS_DIR + "unknown/";
    private static final Map<String, Icon> ICON_CACHE = new ConcurrentHashMap<>();

    private static final Icon WIRETAP_ICON = general("wiretap.png", "Wiretap");
    private static final Icon LOG_WIRETAP_ICON = general("log-wiretap.png", "Log Wiretap");
    private static final Icon SEND_TEST_MESSAGE_ICON = general("send-test-message.png", "Send Test Message");
    private static final Icon SEND_TEST_MESSAGE_FILE_ICON = general("send-test-message-file.png", "Send Test Message (File)");
    private static final Icon TRIGGER_ICON = general("trigger.png", "Trigger Now");
    private static final Icon REPLAY_SERVICE_ICON = general("replay-service.png", "Flow recording enabled");
    private static final Icon MAIL_SERVER_ICON = general("mailserver.png", "Test Mail Server");

    private ComponentIconProvider() {
    }

    public static Icon getSmallIcon(ComponentMeta meta) {
        return component(meta, "small.png", "Small " + meta.getName() + " icon");
    }

    public static Icon getCanvasIcon(ComponentMeta meta) {
        return component(meta, "normal.png", "Medium " + meta.getName() + " icon");
    }

    public static Icon getWiretapIcon() { return WIRETAP_ICON; }
    public static Icon getLogWiretapIcon() { return LOG_WIRETAP_ICON; }
    public static Icon getSendTestMessageIcon() { return SEND_TEST_MESSAGE_ICON; }
    public static Icon getSendTestMessageFileIcon() { return SEND_TEST_MESSAGE_FILE_ICON; }
    public static Icon getTriggerIcon() { return TRIGGER_ICON; }
    public static Icon getReplayServiceIcon() { return REPLAY_SERVICE_ICON; }
    public static Icon getMailServerIcon() { return MAIL_SERVER_ICON; }

    public static int getDecoratorHeight() {
        return Math.max(WIRETAP_ICON.getIconHeight(), LOG_WIRETAP_ICON.getIconHeight());
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
        return ICON_CACHE.computeIfAbsent(pngPath, ignored -> loadUncached(pngPath, fallbackPngPath, description));
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
