package org.ikasan.studio.model.ikasan;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.Matchers.is;

public class IkasanLookupTest {

    @Test
    public void test_lookup_is_populated() {
        Map<String, String> lookup = IkasanLookup.EXCEPTION_RESOLVER_STD_EXCEPTIONS.getDisplayAndValuePairs();
        Assert.assertThat(lookup.size(), is(7));
        Assert.assertThat(lookup.toString(), is("{EndpointException=org.ikasan.spec.component.endpoint.EndpointException.class, FilterException=org.ikasan.spec.component.filter.FilterException.class, JMSException=javax.jms.JMSException.class, ResourceException=javax.resource.ResourceException.class, RouterException=org.ikasan.spec.component.routing.RouterException.class, SplitterException=org.ikasan.spec.component.splittingSplitterException.class, TransformationException=org.ikasan.spec.component.transformation.TransformationException.class}"));
    }
}