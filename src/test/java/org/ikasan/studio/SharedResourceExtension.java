package org.ikasan.studio;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class SharedResourceExtension implements BeforeAllCallback, AfterAllCallback {
//    private static ExpensiveResource resource;

//    public static ExpensiveResource getResource() {
//        return resource;
//    }

    @Override
    public void beforeAll(ExtensionContext context) throws StudioBuildException {
        // This will force the initilaisation and population of the component library that can e used by all tests
        // This component is designed to be thread safe so having concurrent tests executing against it is useful
        IkasanComponentLibrary.getIkasanComponentByKey("TestV1", "X Producer");
        IkasanComponentLibrary.getIkasanComponentByKey("TestV2", "X Producer");
//        if (resource == null) {
//            resource = new ExpensiveResource();
//            resource.initialize();
//        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        // optional: clean up only once at the very end
    }
}
