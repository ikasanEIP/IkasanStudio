package org.ikasan.studio.model.ikasan;

import org.ikasan.studio.StudioUtils;
import org.junit.jupiter.api.Test;

class ModuleTest {


    @Test
    void testJsonSerialisation() {
        Module module = new Module();
        System.out.println(StudioUtils.toJson(module));

    }
}