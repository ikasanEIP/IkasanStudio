package org.ikasan.studio.core.io;

import com.fasterxml.jackson.core.type.TypeReference;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.ikasan.studio.core.StudioBuildException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PojoDeserializationTest {
    @Test
    public void testWhenValidJsonPresented_ThenJavaObjectIsPopulatedWithExpectedDefaults() throws StudioBuildException {
        MyGenericClass myGenericClass = PojoDeserialisation.deserializePojo("genericpojo.json",
                new TypeReference<>() {
                });

        MatcherAssert.assertThat(myGenericClass.field1String, is("This is expected"));
        MatcherAssert.assertThat(myGenericClass.field1Boolean, is(true));
        MatcherAssert.assertThat(myGenericClass.field1Integer, is(1));
        MatcherAssert.assertThat(myGenericClass.field1Long, is(2L));
        MatcherAssert.assertThat(myGenericClass.field1Double, is(1.1));
        MatcherAssert.assertThat(myGenericClass.field1Float, is(2.2f));
        MatcherAssert.assertThat(myGenericClass.getField1Subclass().getField1SubclassString(), is("This is expected"));

        Assertions.assertNull(myGenericClass.unsuppliedField1String);
        Assertions.assertNull(myGenericClass.unsuppliedField1Boolean);
        Assertions.assertNull(myGenericClass.unsuppliedField1Integer);
        Assertions.assertNull(myGenericClass.unsuppliedField1Long);
        Assertions.assertNull(myGenericClass.unsuppliedField1Double);
        Assertions.assertNull(myGenericClass.unsuppliedField1Float);
        Assertions.assertNull(myGenericClass.getUnsuppliedField1Subclass());

        // Note here the advantage of using a primitive class.
        MatcherAssert.assertThat(myGenericClass.unsuppliedPrimitiveField1Boolean, is(false));
        MatcherAssert.assertThat(myGenericClass.unsuppliedPrimitiveField1Int, is(0));
        MatcherAssert.assertThat(myGenericClass.unsuppliedPrimitiveField1Long, is(0L));
        MatcherAssert.assertThat(myGenericClass.unsuppliedPrimitiveField1Double, is(0.0));
        MatcherAssert.assertThat(myGenericClass.unsuppliedPrimitiveField1Float, is(0.0f));

        MatcherAssert.assertThat(myGenericClass.getField1Map().get("key1").getField1SubclassString(), is("value1"));
        MatcherAssert.assertThat(myGenericClass.getField1Map().get("key2").getField1SubclassString(), is("value2"));

    }

    @Test
    public void testWhenJsonDoesNotContainPayloadAttribute_ThenExceptionIsThrown() {
        Exception exception = assertThrows(StudioBuildException.class, () -> PojoDeserialisation.deserializePojo("empty.json",
                new TypeReference<GenericPojo<MyGenericClass>>() {
                }));

        MatcherAssert.assertThat(exception.getMessage(), CoreMatchers.containsString("The serialised data in [empty.json] did not contain the required 'payload' top level element"));
    }
    @Test
    public void testWhenIncorrectDataFileNameProvided_ThenExceptionIsThrown() {
        Exception exception = assertThrows(StudioBuildException.class, () -> PojoDeserialisation.deserializePojo("xx.json",
                new TypeReference<GenericPojo<MyGenericClass>>() {
                }));

        MatcherAssert.assertThat(exception.getMessage(), CoreMatchers.containsString("The serialised data in [xx.json] could not be loaded, check the path is correct"));
    }
}
