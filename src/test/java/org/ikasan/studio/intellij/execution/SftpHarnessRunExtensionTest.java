package org.ikasan.studio.intellij.execution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class SftpHarnessRunExtensionTest {
    @TempDir Path project;
    @Test void refusesOldSftpFactoriesBeforeLaunchingWithLocalMode() throws Exception {
        Path source = project.resolve("generated/src/main/java/ComponentFactoryOrders.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "return builderFactory.getComponentBuilder().sftpProducer().build();");
        assertThrows(java.io.IOException.class, () -> SftpHarnessRunExtension.verifyGeneratedSupport(project));
        Files.writeString(source, "return studioLocalSftp(builderFactory.getComponentBuilder().sftpProducer().build());");
        assertDoesNotThrow(() -> SftpHarnessRunExtension.verifyGeneratedSupport(project));
    }
}
