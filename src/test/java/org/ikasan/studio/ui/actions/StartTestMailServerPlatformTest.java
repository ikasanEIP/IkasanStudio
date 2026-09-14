package org.ikasan.studio.ui.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StartTestMailServerPlatformTest {
    @Test
    void darwinUsesTheMacExecutableRatherThanMatchingWindows() throws Exception {
        assertEquals("MailHog_darwin_amd64", StartTestMailServerAction.mailHogAssetName("Darwin", "x86_64"));
        assertEquals("MailHog_darwin_amd64", StartTestMailServerAction.mailHogAssetName("Mac OS X", "aarch64"));
        assertEquals("MailHog_windows_amd64.exe", StartTestMailServerAction.mailHogAssetName("Windows 11", "amd64"));
        assertEquals("MailHog_linux_amd64", StartTestMailServerAction.mailHogAssetName("Linux", "amd64"));
    }
}
