package org.ikasan.studio.core.generation;

import org.ikasan.studio.core.StudioBuildUtils;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaSourceNamesLocaleTest {
    @Test
    void machineLocaleDoesNotChangeGeneratedPackagesOrContextPaths() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("invoiceflow", JavaSourceNames.toPackageName("INVOICE Flow"));
            assertEquals("invoice-flow", StudioBuildUtils.toUrlString("INVOICE Flow"));
        } finally {
            Locale.setDefault(original);
        }
    }
}
