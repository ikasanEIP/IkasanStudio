package org.ikasan.studio.component.utils;

import junit.framework.TestCase;
import org.ikasan.studio.component.utils.fixtures.TestDeepCopyData;
import org.ikasan.studio.component.utils.fixtures.TestDeepCopyDataSerializable;
import org.ikasan.studio.component.utils.fixtures.TestDeepCopyDataWithDefaultConstructor;

public class DeepCopyUtilTest extends TestCase {

    public void testDeepCopyWithNull() {
        Object duplicate = DeepCopyUtil.deepCopy(null);
        assertNull(duplicate);
    }

    public void testDeepCopyWithSimpleClass() {
        TestDeepCopyData original = new TestDeepCopyData("str1", 2);

        Object duplicate = DeepCopyUtil.deepCopy(original);
        assertEquals(original, duplicate);
        original.setStrAttribute1("str2");

        // Simple non-serializable class cant be duplicated so it is the underlying class
        assertEquals(original, duplicate);
    }

    public void testDeepCopyWithSimpleClassHavingDefaultConstructor() {
        TestDeepCopyDataWithDefaultConstructor original = new TestDeepCopyDataWithDefaultConstructor("str1", 2);

        Object duplicate = DeepCopyUtil.deepCopy(original);
        assertEquals(original, duplicate);
        original.setStrAttribute1("str2");

        // The default constructor allows duplication via reflection
        assertFalse(original.equals(duplicate));
    }

    public void testDeepCopyWithSerializableClass() {
        TestDeepCopyDataSerializable original = new TestDeepCopyDataSerializable("str1", 2);

        Object duplicate = DeepCopyUtil.deepCopy(original);
        assertEquals(original, duplicate);
        original.setStrAttribute1("str2");

        //  the serializable class allows serialisation via serializing
        assertFalse(original.equals(duplicate));
    }
}