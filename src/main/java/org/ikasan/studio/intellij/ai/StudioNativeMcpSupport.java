package org.ikasan.studio.intellij.ai;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.progress.ProcessCanceledException;

/** Checks the optional module without linking the main plugin to a newer SDK. */
public final class StudioNativeMcpSupport {
    private StudioNativeMcpSupport() { }

    public static boolean available() {
        if (ApplicationInfo.getInstance().getBuild().getBaselineVersion() < 262) return false;
        try {
            return ExtensionPointName.create("com.intellij.mcpServer.mcpToolset")
                    .getExtensionsIfPointIsRegistered().stream()
                    .anyMatch(toolset -> toolset.getClass().getName().equals(
                            "org.ikasan.studio.intellij.ai.StudioNativeMcpToolset"));
        } catch (ProcessCanceledException cancelled) {
            throw cancelled;
        } catch (RuntimeException | LinkageError failure) {
            Logger.getInstance(StudioNativeMcpSupport.class).warn("Native MCP integration unavailable; using Java adapter", failure);
            return false;
        }
    }
}
