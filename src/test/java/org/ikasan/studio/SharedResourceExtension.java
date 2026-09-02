package org.ikasan.studio;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class SharedResourceExtension implements BeforeAllCallback, AfterAllCallback {
//    private static ExpensiveResource resource;

//    public static ExpensiveResource getResource() {
//        return resource;
//    }

    // Deliberately not annotated to match JUnit's @NullMarked contract on ExtensionContext - this project has no
    // existing JSpecify convention (see the identical @SuppressWarnings("NullableProblems") reasoning elsewhere
    // in this codebase for supertype-nullability mismatches), and JUnit always passes a real, non-null context.
    @SuppressWarnings("NullableProblems")
    @Override
    public void beforeAll(ExtensionContext context) throws StudioBuildException {
        // This will force the initilaisation and population of the component library that can e used by all tests
        // This component is designed to be thread safe so having concurrent tests executing against it is useful
        ComponentLibrary.getIkasanComponentByKey("TestV1", "X Producer");
        ComponentLibrary.getIkasanComponentByKey("TestV2", "X Producer");
//        if (resource == null) {
//            resource = new ExpensiveResource();
//            resource.initialize();
//        }
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void afterAll(ExtensionContext context) {
        // optional: clean up only once at the very end
    }
}
