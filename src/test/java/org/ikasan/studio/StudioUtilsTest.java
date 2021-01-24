package org.ikasan.studio;

import junit.framework.TestCase;
import org.ikasan.studio.model.Ikasan.IkasanComponentPropertyMeta;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;

public class StudioUtilsTest extends TestCase {

    @Test
    public void test_get_last_token_with_multiple_tokens() {
        String actual = StudioUtils.getLastToken("\\.", "this.is.dot.delim");
        Assert.assertThat(actual, is("delim"));

        String actual2 = StudioUtils.getLastToken("\\.", "delim");
        Assert.assertThat(actual2, is("delim"));
    }

    @Test
    public void testToJavaIdentifier() {
        Assert.assertThat(StudioUtils.toJavaIdentifier(""), is(""));
        Assert.assertThat(StudioUtils.toJavaIdentifier("a"), is("a"));
        Assert.assertThat(StudioUtils.toJavaIdentifier("A"), is("a"));
        Assert.assertThat(StudioUtils.toJavaIdentifier("AS"), is("as"));
        Assert.assertThat(StudioUtils.toJavaIdentifier("AS D"), is("asD"));
        Assert.assertThat(StudioUtils.toJavaIdentifier("as d"), is("asD"));
        Assert.assertThat(StudioUtils.toJavaIdentifier("as d    c"), is("asDC"));
        Assert.assertThat(StudioUtils.toJavaIdentifier("Some Text"), is("someText"));
    }

    @Test
    public void testConfigReader() throws IOException {
        Map<String, IkasanComponentPropertyMeta>  properties = StudioUtils.readIkasanComponentProperties("BROKER");
        Assert.assertThat(properties.size(), is(2));
        IkasanComponentPropertyMeta nameProperty = (IkasanComponentPropertyMeta)properties.get(IkasanComponentPropertyMeta.NAME);
        Assert.assertThat(nameProperty.getPropertyName(), is(IkasanComponentPropertyMeta.NAME));
        Assert.assertThat(nameProperty.getPropertyConfigFileLabel(), is(""));
        Assert.assertThat(nameProperty.getMandatory(), is(true));
        Assert.assertThat(nameProperty.getDataType().toString(), is("class java.lang.String"));

        IkasanComponentPropertyMeta totalProperty = (IkasanComponentPropertyMeta)properties.get("total");
        Assert.assertThat(totalProperty.getPropertyName(), is("total"));
        Assert.assertThat(totalProperty.getPropertyConfigFileLabel(), is("my.test.total"));
        Assert.assertThat(totalProperty.getMandatory(), is(false));
        Assert.assertThat(totalProperty.getDataType().toString(), is("class java.lang.Integer"));
    }
}