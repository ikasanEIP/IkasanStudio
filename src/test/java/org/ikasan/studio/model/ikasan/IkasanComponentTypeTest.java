package org.ikasan.studio.model.ikasan;

import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class IkasanComponentTypeTest {

    //@todo remove ignore when all components have definitions files.
    @Test
    @Ignore
    public void test_number_of_property_meta() {
        for (IkasanComponentType ikasanComponentType : IkasanComponentType.values()) {
                assertThat("Incorrect number of metaValues for " + ikasanComponentType + "",  ikasanComponentType.metadataMap.values().size()> 0, is(true));
        }
    }
}