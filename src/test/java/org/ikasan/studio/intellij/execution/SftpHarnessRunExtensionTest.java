package org.ikasan.studio.intellij.execution;

import org.ikasan.studio.core.generator.HarnessOverridesTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class SftpHarnessRunExtensionTest {
    @TempDir Path project;
    @Test void refusesFactoriesWithoutSelectedComponentSupportBeforeLaunching() throws Exception {
        var target = new HarnessOverridesTemplate.LaunchTarget("flow.component", "SFTP",
                "org/ikasan/studio/boot/flow/orders/ComponentFactoryOrders.java", "Orders / Input");
        assertThrows(java.io.IOException.class, () ->
                SftpHarnessRunExtension.verifyHarnessSupport(project, List.of(target), Set.of(target.id())));
        Path properties = project.resolve(HarnessOverridesTemplate.PATH);
        Path adapter = project.resolve(HarnessOverridesTemplate.JAVA_PATH);
        Path factory = project.resolve("generated/src/main/java/").resolve(target.factory());
        Files.createDirectories(properties.getParent());
        Files.createDirectories(factory.getParent());
        Files.writeString(properties, "# generated harness inventory");
        Files.writeString(adapter, HarnessOverridesTemplate.javaSource());
        Files.writeString(factory, "return builderFactory.getComponentBuilder().sftpProducer().build();");
        assertThrows(java.io.IOException.class, () ->
                SftpHarnessRunExtension.verifyHarnessSupport(project, List.of(target), Set.of(target.id())));
        Files.writeString(factory, "return StudioHarnessOverrides.apply(environment, endpoint, \"flow.component\", \"SFTP\", false);");
        assertDoesNotThrow(() ->
                SftpHarnessRunExtension.verifyHarnessSupport(project, List.of(target), Set.of(target.id())));
    }
}
