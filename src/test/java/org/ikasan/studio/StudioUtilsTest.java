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
    public void test_get_all_but_last_token_with_multiple_tokens() {
        String actual = StudioUtils.getAllButLastToken("\\.", "this.is.dot.delim");
        Assert.assertThat(actual, is("this.is.dot"));

        String actual2 = StudioUtils.getAllButLastToken("\\.", "this.delim");
        Assert.assertThat(actual2, is("this"));

        String actual3 = StudioUtils.getAllButLastToken("\\.", "delim");
        Assert.assertThat(actual3, is(""));
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
        Assert.assertThat(StudioUtils.toJavaIdentifier("my.package.name"), is("myPackageName"));
    }

    @Test
    public void testToJavaPackageName() {
        Assert.assertThat(StudioUtils.toJavaPackageName(""), is(""));
        Assert.assertThat(StudioUtils.toJavaPackageName("a"), is("a"));
        Assert.assertThat(StudioUtils.toJavaPackageName("A"), is("a"));
        Assert.assertThat(StudioUtils.toJavaPackageName("AS"), is("as"));
        Assert.assertThat(StudioUtils.toJavaPackageName("AS D"), is("asd"));
        Assert.assertThat(StudioUtils.toJavaPackageName("as d"), is("asd"));
        Assert.assertThat(StudioUtils.toJavaPackageName("as d    c"), is("asdc"));
        Assert.assertThat(StudioUtils.toJavaPackageName("Some 1 Text"), is("some1text"));
        Assert.assertThat(StudioUtils.toJavaPackageName("1test"), is("_1test"));
    }


    @Test
    public void testConfigReader() throws IOException {
        Map<String, IkasanComponentPropertyMeta>  properties = StudioUtils.readIkasanComponentProperties("BROKER");
        Assert.assertThat(properties.size(), is(5));
        IkasanComponentPropertyMeta additionalName = properties.get("additionalName");
        IkasanComponentPropertyMeta name = properties.get("name");
        IkasanComponentPropertyMeta other = properties.get("other");
        IkasanComponentPropertyMeta total = properties.get("total");
        IkasanComponentPropertyMeta userImplementedClass = properties.get("userImplementedClass");
        Assert.assertThat(additionalName.toString(), is(
            "IkasanComponentPropertyMeta{mandatory=true, userImplementedClass=false, propertyName='additionalName', propertyConfigFileLabel='', dataType=class java.lang.String, defaultValue=MyDefault, helpText='The name of the component'}"));
        Assert.assertThat(name.toString(), is(
            "IkasanComponentPropertyMeta{mandatory=true, userImplementedClass=false, propertyName='name', propertyConfigFileLabel='null', dataType=class java.lang.String, defaultValue=, helpText='The name of the component as displayed on diagrams, also used for the variable name in the generated code.'}"));
        Assert.assertThat(other.toString(), is(
            "IkasanComponentPropertyMeta{mandatory=false, userImplementedClass=false, propertyName='other', propertyConfigFileLabel='', dataType=class java.lang.Integer, defaultValue=null, helpText='Total description'}"));
        Assert.assertThat(total.toString(), is(
            "IkasanComponentPropertyMeta{mandatory=false, userImplementedClass=false, propertyName='total', propertyConfigFileLabel='my.test.total', dataType=class java.lang.Integer, defaultValue=2, helpText='Total description'}"));
        Assert.assertThat(userImplementedClass.toString(), is(
            "IkasanComponentPropertyMeta{mandatory=true, userImplementedClass=true, propertyName='userImplementedClass', propertyConfigFileLabel='', dataType=class java.lang.Object, defaultValue=null, helpText='This type of class will be implemented by the user, typically implementing an Ikasan interface'}"));
    }
}