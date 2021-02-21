package org.ikasan.studio.model.Ikasan;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;

public class IkasanComponentTest extends TestCase {

    IkasanComponent ikasanComponent;


    @Test
    public void testGetStandardProperties() {
        String newPropertyKey = "myProperty";
        String newPropertyValue = "myValue";
        ikasanComponent = new IkasanComponent();
        ikasanComponent.setName("MyName");
        ikasanComponent.setDescription("MyDescription");
        ikasanComponent.setPropertyValue(newPropertyKey,
                new IkasanComponentPropertyMeta(false, newPropertyKey, "configLabel", java.lang.String.class, null, ""),
                newPropertyValue);

        Map<String, IkasanComponentProperty> standardProperties = ikasanComponent.getStandardProperties();
        Assert.assertThat(standardProperties.size(), is(1));
        Assert.assertThat(((IkasanComponentProperty)standardProperties.get(newPropertyKey)).getValue(), is(newPropertyValue));
    }
}