package org.ikasan.studio.model.ikasan;

import org.ikasan.studio.StudioUtils;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentTypeMeta;
import org.junit.Test;

public class FlowElementTest {

//    @org.junit.Test
//    public void test_lookup_is_populated() {
//        Map<String, String> lookup = IkasanLookup.EXCEPTION_RESOLVER_STD_EXCEPTIONS.getDisplayAndValuePairs();
//        Assert.assertThat(lookup.size(), is(7));
//        Assert.assertThat(lookup.toString(), is("{javax.jms.JMSException.class=javax.jms.JMSException.class, javax.resource.ResourceException.class=javax.resource.ResourceException.class, org.ikasan.spec.component.endpoint.EndpointException.class=org.ikasan.spec.component.endpoint.EndpointException.class, org.ikasan.spec.component.filter.FilterException.class=org.ikasan.spec.component.filter.FilterException.class, org.ikasan.spec.component.routing.RouterException.class=org.ikasan.spec.component.routing.RouterException.class, org.ikasan.spec.component.splittingSplitterException.class=org.ikasan.spec.component.splittingSplitterException.class, org.ikasan.spec.component.transformation.TransformationException.class=org.ikasan.spec.component.transformation.TransformationException.class}"));
//    }
//
//    @BeforeEach
//    void setUp() {
//    }
//
//    @AfterEach
//    void tearDown() {
//    }

    @Test
    public void testJsonSerialisation() {
//        public FlowElement(IkasanComponentType type, Flow parent, String name, String description) {
        FlowElement flowElement = new FlowElement(IkasanComponentTypeMeta.DB_CONSUMER, null, "My Database Consumer", "My Database Consumer Description");
        System.out.println(StudioUtils.toJson(flowElement));
    }
}