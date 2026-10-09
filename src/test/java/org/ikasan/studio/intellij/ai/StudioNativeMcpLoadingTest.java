package org.ikasan.studio.intellij.ai;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.testFramework.LightPlatformTestCase;

/** Run with testIdeCompatibility to exercise the actual optional-module class loader. */
public class StudioNativeMcpLoadingTest extends LightPlatformTestCase {
    public void testOptionalModuleCanReachTheContainingPlugin() throws Exception {
        if (ApplicationInfo.getInstance().getBuild().getBaselineVersion() < 262) {
            assertFalse(StudioNativeMcpSupport.available());
            return;
        }
        assertTrue("The bundled MCP implementation must load Studio's optional toolset", StudioNativeMcpSupport.available());
        Object toolset = ExtensionPointName.create("com.intellij.mcpServer.mcpToolset")
                .getExtensionsIfPointIsRegistered().stream()
                .filter(extension -> extension.getClass().getName().equals(
                        "org.ikasan.studio.intellij.ai.StudioNativeMcpToolset"))
                .findFirst().orElseThrow();
        ClassLoader optionalLoader = toolset.getClass().getClassLoader();
        assertSame("Both modules must share Studio's project service class, not load duplicate copies",
                StudioAiService.class, optionalLoader.loadClass(StudioAiService.class.getName()));
    }
}
