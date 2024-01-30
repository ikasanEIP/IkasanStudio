package org.ikasan.studio.model.ikasan.meta;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.is;

class IkasanComponentLibraryTest {

    @Test
    void testThatDeserialisationPopulatesTheIcasanComponentLibrary() {
        IkasanComponentLibrary.refreshComponentLibrary(IkasanComponentLibrary.TEST_IKASAN_PACK);

        MatcherAssert.assertThat(
                IkasanComponentLibrary.getNumberOfComponents(IkasanComponentLibrary.TEST_IKASAN_PACK),
                is(3));

        MatcherAssert.assertThat(
                new TreeSet<>(IkasanComponentLibrary.getIkasanComponentList(IkasanComponentLibrary.TEST_IKASAN_PACK)),
                is(new TreeSet<>(Arrays.asList("EXCEPTION_RESOLVER", "FLOW", "MODULE"))));

    }
}