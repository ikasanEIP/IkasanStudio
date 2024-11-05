package org.ikasan.studio.component;

import junit.framework.TestCase;
import org.ikasan.studio.component.utils.fixtures.TestDeepCopyDataSerializable;
import org.ikasan.studio.component.utils.fixtures.TestDeepCopyDataWithDefaultConstructor;

public class DebugTransitionComponentTest extends TestCase {
    DebugTransitionComponent debugTransitionComponent = new DebugTransitionComponent() {
        @Override
        public void debug(Object message) {
            message = null;
        }
    } ;

    public void testDebugCantAlterSimplePayloads() {
        String name = "bob";
        debugTransitionComponent.debug(name);
        assertEquals(name, "bob");

        TestDeepCopyDataSerializable testDeepCopyDataSerializable = new TestDeepCopyDataSerializable("bob", 1);
        debugTransitionComponent.debug(testDeepCopyDataSerializable);
        assertEquals(testDeepCopyDataSerializable.getStrAttribute1(), "bob");
        assertEquals(testDeepCopyDataSerializable.getIntAttribute1(), Integer.valueOf(1));

        TestDeepCopyDataWithDefaultConstructor testDeepCopyDataWithDefaultConstructor = new TestDeepCopyDataWithDefaultConstructor("bob", 1);
        debugTransitionComponent.debug(testDeepCopyDataWithDefaultConstructor);
        assertEquals(testDeepCopyDataWithDefaultConstructor.getStrAttribute1(), "bob");
        assertEquals(testDeepCopyDataWithDefaultConstructor.getIntAttribute1(), Integer.valueOf(1));
    }
}