package org.ikasan.studio.core.model.ikasan;

import org.ikasan.studio.core.model.ikasan.meta.IkasanLookup;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class IkasanLookupTest {

    @Test
    public void test_lookup_is_populated() {
        Map<String, String> lookup = IkasanLookup.EXCEPTION_RESOLVER_STD_EXCEPTIONS.getDisplayAndValuePairs();
        assertThat(lookup.size(), is(7));
        assertThat(lookup.toString(), is("{javax.jms.JMSException.class=javax.jms.JMSException.class, javax.resource.ResourceException.class=javax.resource.ResourceException.class, org.ikasan.spec.component.endpoint.EndpointException.class=org.ikasan.spec.component.endpoint.EndpointException.class, org.ikasan.spec.component.filter.FilterException.class=org.ikasan.spec.component.filter.FilterException.class, org.ikasan.spec.component.routing.RouterException.class=org.ikasan.spec.component.routing.RouterException.class, org.ikasan.spec.component.splittingSplitterException.class=org.ikasan.spec.component.splittingSplitterException.class, org.ikasan.spec.component.transformation.TransformationException.class=org.ikasan.spec.component.transformation.TransformationException.class}"));
    }
}