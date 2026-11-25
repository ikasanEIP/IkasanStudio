package org.ikasan.studio.intellij.testing;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.ikasan.studio.core.generator.FlowTestScaffold;

import static org.mockito.Mockito.*;

public class FlowTestPropertiesBannerTest extends BasePlatformTestCase {
    public void testOpenRepairReopenAndDispose() throws Exception {
        Path root = Files.createTempDirectory("studio-banner-test");
        Project project = mock(Project.class);
        when(project.getBasePath()).thenReturn(root.toString());
        Path properties = root.resolve(FlowTestScaffold.TEST_PROPERTIES_PATH);
        Path application = root.resolve("generated/src/main/resources/application.properties");
        Files.createDirectories(properties.getParent());
        Files.createDirectories(application.getParent());
        Files.writeString(properties, "test.jms.broker-url=${jms.old.url}\n");
        Files.writeString(application, "jms.new.url=local\n");
        FlowTestPropertiesBanner banner = new FlowTestPropertiesBanner(project);
        try {
            awaitVisibility(banner, true); // Initial open discovers existing problems.
            Files.writeString(properties, "test.jms.broker-url=${jms.new.url}\n");
            banner.requestCheck();
            awaitVisibility(banner, false);
            Files.writeString(properties, "test.jms.broker-url=${jms.old.url}\n");
        } finally { Disposer.dispose(banner); }
        FlowTestPropertiesBanner reopened = new FlowTestPropertiesBanner(project);
        try { awaitVisibility(reopened, true); }
        finally {
            Disposer.dispose(reopened);
            reopened.requestCheck(); // Disposed editors cannot queue work.
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void awaitVisibility(FlowTestPropertiesBanner banner, boolean visible) {
        PlatformTestUtil.waitWithEventsDispatching("Banner visibility did not become " + visible,
                () -> banner.isVisible() == visible, 10);
        assertEquals(visible, banner.isVisible());
    }
}
