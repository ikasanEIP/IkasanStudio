package org.ikasan.studio.intellij.migration;

import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MigrationJdkTest {
    @Test void requiresTheTargetMajorVersion() {
        Sdk sdk = mock(Sdk.class);
        when(sdk.getSdkType()).thenReturn(mock(JavaSdk.class));
        when(sdk.getVersionString()).thenReturn("openjdk version \"11.0.24\"");
        assertTrue(MigrationJdk.matches(sdk, 11));
        assertFalse(MigrationJdk.matches(sdk, 17));
        when(sdk.getVersionString()).thenReturn("openjdk version \"17.0.12\"");
        assertTrue(MigrationJdk.matches(sdk, 17));
        assertFalse(MigrationJdk.matches(sdk, 11));
    }

    @Test void rejectsUnknownJavaVersion() {
        Sdk sdk = mock(Sdk.class);
        when(sdk.getSdkType()).thenReturn(mock(JavaSdk.class));
        assertFalse(MigrationJdk.matches(sdk, 17));
        when(sdk.getVersionString()).thenReturn("unknown");
        assertFalse(MigrationJdk.matches(sdk, 17));
    }

    @Test void rejectsMissingSdk() {
        assertFalse(MigrationJdk.matches(null, 17));
    }

    @Test void onlyChangesStudioApplicationRunConfigurations() {
        assertTrue(MigrationJdk.isStudioApplication("org.ikasan.studio.boot.Application"));
        assertFalse(MigrationJdk.isStudioApplication("org.example.OtherApplication"));
        //noinspection ConstantValue -- intentionally verifying null input resolves to false, not NPE
        assertFalse(MigrationJdk.isStudioApplication(null));
    }
}
