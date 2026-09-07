package org.ikasan.studio.intellij.diagnostics;

import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.diagnostic.JetBrainsMarketplaceErrorReportSubmitter;

/** Resolves the real plugin registration without creating errors or submitting network reports. */
public class MarketplaceErrorReportingHeavyTest extends HeavyPlatformTestCase {
    public void testMarketplaceReporterBelongsToStudioAndProvidesConsentNotice() {
        var reporters = ExtensionPointName.<ErrorReportSubmitter>create("com.intellij.errorHandler")
                .getExtensionList().stream()
                .filter(reporter -> reporter.getPluginDescriptor() != null
                        && "com.github.ikasaneip.ikasanstudio".equals(reporter.getPluginDescriptor().getPluginId().getIdString()))
                .toList();
        assertEquals(1, reporters.size());
        var reporter = reporters.get(0);
        assertInstanceOf(reporter, JetBrainsMarketplaceErrorReportSubmitter.class);
        assertNotNull(reporter.getReportActionText());
        assertFalse(reporter.getReportActionText().isBlank());
        assertNotNull(reporter.getPrivacyNoticeText());
        assertFalse(reporter.getPrivacyNoticeText().isBlank());
    }
}
