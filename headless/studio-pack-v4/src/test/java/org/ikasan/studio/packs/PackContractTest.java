package org.ikasan.studio.packs;

import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.testkit.MetaPackContract;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PackContractTest extends MetaPackContract {
    @Override protected String packId() { return "V4.1.6"; }
    @Override protected Module sampleModule() throws Exception {
        return Module.moduleBuilder().version(packId()).name("Example")
                .applicationPackageName("org.example").port("8080").h2PortNumber("8092")
                .h2WebPortNumber("8093").flows(List.of()).build();
    }
    @Test void loadsIndependentlyWithoutOtherOfficialPacks() throws Exception {
        assertEquals(List.of(packId()), ComponentLibrary.getMetapackList());
        assertEquals(2, ComponentLibrary.getMetaPackManifest(packId()).schemaVersion());
        assertEquals(1, ComponentLibrary.getMetaPackManifest(packId()).generatorApiVersion());
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.intellij.openapi.application.ApplicationManager"));
    }
}
