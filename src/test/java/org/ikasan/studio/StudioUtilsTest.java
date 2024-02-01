package org.ikasan.studio;

import org.ikasan.studio.generator.TestUtils;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentPropertyMeta;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class StudioUtilsTest {
    @Test
    public void test_get_directories() throws URISyntaxException, IOException {
        String[] dirs = StudioUtils.getDirectories("studio/Vtest.x/components");
        assertThat(dirs.length, is(3));

    }
    @Test
    public void test_get_last_token_with_multiple_tokens() {
        String actual = StudioUtils.getLastToken("\\.", "this.is.dot.delim");
        assertThat(actual, is("delim"));

        String actual2 = StudioUtils.getLastToken("\\.", "delim");
        assertThat(actual2, is("delim"));
    }

    @Test
    public void test_get_all_but_last_token_with_multiple_tokens() {
        String actual = StudioUtils.getAllButLastToken("\\.", "this.is.dot.delim");
        assertThat(actual, is("this.is.dot"));

        String actual2 = StudioUtils.getAllButLastToken("\\.", "this.delim");
        assertThat(actual2, is("this"));

        String actual3 = StudioUtils.getAllButLastToken("\\.", "delim");
        assertThat(actual3, is(""));
    }

    @Test
    public void toJavaClassName() {
        assertThat(StudioUtils.toJavaClassName(""), is(""));
        assertThat(StudioUtils.toJavaClassName("a"), is("A"));
        assertThat(StudioUtils.toJavaClassName("A"), is("A"));
        assertThat(StudioUtils.toJavaClassName("AS"), is("AS"));
        assertThat(StudioUtils.toJavaClassName("AS D"), is("ASD"));
        assertThat(StudioUtils.toJavaClassName("as d"), is("AsD"));
        assertThat(StudioUtils.toJavaClassName("as d    c"), is("AsDC"));
        assertThat(StudioUtils.toJavaClassName("Some Text"), is("SomeText"));
        assertThat(StudioUtils.toJavaClassName("my.package.name"), is("MyPackageName"));
    }

    @Test
    public void testToJavaIdentifier() {
        assertThat(StudioUtils.toJavaIdentifier(""), is(""));
        assertThat(StudioUtils.toJavaIdentifier("a"), is("a"));
        assertThat(StudioUtils.toJavaIdentifier("A"), is("a"));
        assertThat(StudioUtils.toJavaIdentifier("AS"), is("aS"));
        assertThat(StudioUtils.toJavaIdentifier("AS D"), is("aSD"));
        assertThat(StudioUtils.toJavaIdentifier("as d"), is("asD"));
        assertThat(StudioUtils.toJavaIdentifier("as d    c"), is("asDC"));
        assertThat(StudioUtils.toJavaIdentifier("Some Text"), is("someText"));
        assertThat(StudioUtils.toJavaIdentifier("my.package.name"), is("myPackageName"));
    }

    @Test
    public void testToJavaPackageName() {
        assertThat(StudioUtils.toJavaPackageName(""), is(""));
        assertThat(StudioUtils.toJavaPackageName("a"), is("a"));
        assertThat(StudioUtils.toJavaPackageName("A"), is("a"));
        assertThat(StudioUtils.toJavaPackageName("AS"), is("as"));
        assertThat(StudioUtils.toJavaPackageName("AS D"), is("asd"));
        assertThat(StudioUtils.toJavaPackageName("as d"), is("asd"));
        assertThat(StudioUtils.toJavaPackageName("as d    c"), is("asdc"));
        assertThat(StudioUtils.toJavaPackageName("Some 1 Text"), is("some1text"));
        assertThat(StudioUtils.toJavaPackageName("1test"), is("_1test"));
    }

    @Test
    public void testToUrlString() {
        assertThat(StudioUtils.toUrlString(""), is(""));
        assertThat(StudioUtils.toUrlString("a"), is("a"));
        assertThat(StudioUtils.toUrlString("A"), is("a"));
        assertThat(StudioUtils.toUrlString("AS"), is("as"));
        assertThat(StudioUtils.toUrlString("AS D"), is("as-d"));
        assertThat(StudioUtils.toUrlString("as d"), is("as-d"));
        assertThat(StudioUtils.toUrlString("as d    c"), is("as-d-c"));
        assertThat(StudioUtils.toUrlString("Some 1 Text"), is("some-1-text"));
    }

    // @TODO suspend while this is being redeveloped
    //@Test
    public void testConfigReader() throws IOException {
        Map<String, IkasanComponentPropertyMeta>  properties = StudioUtils.readIkasanComponentProperties("BROKER");
        assertThat(properties.size(), is(5));
        IkasanComponentPropertyMeta additionalName = properties.get("AdditionalName");
        IkasanComponentPropertyMeta name = properties.get("Name");
        IkasanComponentPropertyMeta other = properties.get("Other");
        IkasanComponentPropertyMeta total = properties.get("Total");
        IkasanComponentPropertyMeta userImplementedClass = properties.get("UserImplementedClass");
        assertThat(additionalName.toString(), is(
            "IkasanComponentPropertyMeta{paramGroupNumber=1, causesUserCodeRegeneration=false, mandatory=true, userImplementedClass=false, userDefineResource=false, propertyName='AdditionalName', propertyConfigFileLabel='', propertyDataType=class java.lang.String, usageDataType=java.lang.String, validation=, validationMessage=null, validationPattern=null, defaultValue=MyDefault, helpText='The name of the component'}"));
        assertThat(name.toString(), is(
            "IkasanComponentPropertyMeta{paramGroupNumber=1, causesUserCodeRegeneration=false, mandatory=true, userImplementedClass=false, userDefineResource=false, propertyName='Name', propertyConfigFileLabel='', propertyDataType=class java.lang.String, usageDataType=java.lang.String, validation=, validationMessage=null, validationPattern=null, defaultValue=null, helpText='The name of the component as displayed on diagrams, space are encouraged, succinct is best. The name should be unique for the flow.'}"));
        assertThat(other.toString(), is(
            "IkasanComponentPropertyMeta{paramGroupNumber=1, causesUserCodeRegeneration=false, mandatory=false, userImplementedClass=false, userDefineResource=false, propertyName='Other', propertyConfigFileLabel='', propertyDataType=class java.lang.Integer, usageDataType=java.lang.Integer, validation=, validationMessage=null, validationPattern=null, defaultValue=null, helpText='Total description'}"));
        assertThat(total.toString(), is(
            "IkasanComponentPropertyMeta{paramGroupNumber=1, causesUserCodeRegeneration=false, mandatory=false, userImplementedClass=false, userDefineResource=false, propertyName='Total', propertyConfigFileLabel='my.test.total', propertyDataType=class java.lang.Integer, usageDataType=java.lang.Integer, validation=, validationMessage=null, validationPattern=null, defaultValue=2, helpText='Total description'}"));
        assertThat(userImplementedClass.toString(), is(
            "IkasanComponentPropertyMeta{paramGroupNumber=1, causesUserCodeRegeneration=false, mandatory=true, userImplementedClass=true, userDefineResource=true, propertyName='UserImplementedClass', propertyConfigFileLabel='', propertyDataType=class java.lang.Object, usageDataType=java.lang.Object, validation=, validationMessage=null, validationPattern=null, defaultValue=null, helpText='This type of class will be implemented by the user, typically implementing an Ikasan interface'}"));
    }

    // @TODO suspend while this is being redeveloped
    //@Test
    public void toJsonTest() throws IOException {
        assertThat(StudioUtils.toJson("bob"), is("\"bob\""));

        Module module = new Module();
        module.setVersion("1.3");
        module.setComponentName("");
        module.setDescription("The Description");
        assertThat(StudioUtils.toJson(module), is(TestUtils.getFileAsString("/org/ikasan/studio/module.json")));
    }
}