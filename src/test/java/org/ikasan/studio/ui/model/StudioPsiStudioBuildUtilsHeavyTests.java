package org.ikasan.studio.ui.model;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;
import org.ikasan.studio.ui.UiContext;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ikasan.studio.ui.model.StudioPsiUtils.GENERATED_CONTENT_ROOT;
import static org.ikasan.studio.ui.model.StudioPsiUtils.SRC_MAIN_JAVA_CODE;

/**
 * Heavy tests create a new project for each test, where possible use lightweight
 * <a href="https://plugins.jetbrains.com/docs/intellij/light-and-heavy-tests.html">See Jetbrains Documentation</a>
 * NOTE JavaPsiTestCase provided by Intellij is still JUNIT3 !
 */
public class StudioPsiStudioBuildUtilsHeavyTests  extends HeavyPlatformTestCase
{
    private VirtualFile myTestProjectRoot;
    private final static String TEST_DATA_DIR = "/ikasanStandardSampleApps/general/";

    /**
     * @return path to test data file directory relative to root of this module.
     */
    protected @NotNull String getTestDataPath() {
        return "src/test/testData";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String root = getTestDataPath() + TEST_DATA_DIR;
        myTestProjectRoot = createTestProjectStructure(root);
        UiContext.setProject(myProject.getName(), myProject);
    }

    @Override
    protected void tearDown() throws Exception {
        myTestProjectRoot = null;
        super.tearDown();
    }

    public void test_createPomXml() throws IOException {
        String testFile = "src/test/resources/org/ikasan/studio/pom.xml";
        String content = Files.readString(Paths.get(testFile));
        StudioPsiUtils.createPomFile(myProject.getName(), GENERATED_CONTENT_ROOT, "", content);
        String actualFile = StudioPsiUtils.readFileAsString(myProject.getName(), "generated/pom.xml");
        assertThat(actualFile, is(content));
    }

    public void test_updatePomXml() throws IOException {
        String testFile = "src/test/resources/org/ikasan/studio/pom.xml";
        String content = Files.readString(Paths.get(testFile));
        StudioPsiUtils.createPomFile(myProject.getName(), GENERATED_CONTENT_ROOT, "", content);
        StudioPsiUtils.createPomFile(myProject.getName(), GENERATED_CONTENT_ROOT, "", content);
        String actualFile = StudioPsiUtils.readFileAsString(myProject.getName(), "generated/pom.xml");
        assertThat(actualFile, is(content));
    }

    public void test_createPropertiesFile() throws IOException {
        String testFile = "src/test/resources/org/ikasan/studio/application.properties";
        String content = Files.readString(Paths.get(testFile));
        StudioPsiUtils.createPropertiesFile(myProject.getName(), content);
        String actualFile = StudioPsiUtils.readFileAsString(myProject.getName(), "generated/src/main/resources/application.properties");
        assertThat(actualFile, is(content));
    }

    public void test_updatePropertiesFile() throws IOException {
        String testFile = "src/test/resources/org/ikasan/studio/application.properties";
        String content = Files.readString(Paths.get(testFile));
        StudioPsiUtils.createPropertiesFile(myProject.getName(), content);
        StudioPsiUtils.createPropertiesFile(myProject.getName(), content);
        String actualFile = StudioPsiUtils.readFileAsString(myProject.getName(), "generated/src/main/resources/application.properties");
        assertThat(actualFile, is(content));
    }

    public void test_createJsonModelFile() throws IOException {
        String testFile = "src/test/resources/org/ikasan/studio/populated_flow.json";
        String content = Files.readString(Paths.get(testFile));
        StudioPsiUtils.createJsonModelFile(myProject.getName(), content);
        String actualFile = StudioPsiUtils.readFileAsString(myProject.getName(), "generated/src/main/model/model.json");
        assertThat(actualFile, is(content));
    }

    public void test_updateJsonModelFile() throws IOException {
        String testFile = "src/test/resources/org/ikasan/studio/populated_flow.json";
        String content = Files.readString(Paths.get(testFile));
        StudioPsiUtils.createJsonModelFile(myProject.getName(), content);
        StudioPsiUtils.createJsonModelFile(myProject.getName(), content);
        String actualFile = StudioPsiUtils.readFileAsString(myProject.getName(), "generated/src/main/model/model.json");
        assertThat(actualFile, is(content));
    }

    public void test_createJavaFile() throws IOException {
        String testFile = "src/test/resources/org/ikasan/studio/ComponentFactoryFlow1.java";
        String content = Files.readString(Paths.get(testFile));
        // Path shortening needs the pom file to be correct and part of the project
        StudioPsiUtils.createJavaSourceFile(
                myProject.getName(),
                GENERATED_CONTENT_ROOT,
                SRC_MAIN_JAVA_CODE,
                "org/ikasan/studio/boot/flow/flow1",
                "ComponentFactoryFlow1",
                content,
                false, true);
        String actualFile = StudioPsiUtils.readFileAsString(myProject.getName(), "generated/src/main/java/org/ikasan/studio/boot/flow/flow1/ComponentFactoryFlow1.java");
        assertThat(actualFile, is(content));
    }

    public void test_writePerformance() throws IOException {
        // This might seem a lot, but build servers like Travis are not highly powered.
        long TEN_SECONDS = 10000;
        String content = Files.readString(Paths.get("src/test/resources/performanceTest/largeFile.txt"));
        long time = System.currentTimeMillis();

        final String myFile = "src/main/resources/testfile.txt";
        int ii = 0;

        StudioPsiUtils.createFileWithDirectories(myProject, myFile, content + (ii++));
        String last10 = lastXChars(10, StudioPsiUtils.readFileAsString(myProject.getName(), myFile));
        assertThat(last10, is("000 Lines0"));
        StudioPsiUtils.createFileWithDirectories(myProject, myFile, content + (ii++));
        last10 = lastXChars(10, StudioPsiUtils.readFileAsString(myProject.getName(), myFile));
        assertThat(last10, is("000 Lines1"));

        long runningTime = System.currentTimeMillis() - time;
        if (runningTime > TEN_SECONDS) {
            Assert.fail("The test took longer than " + TEN_SECONDS + " milliseconds");
        }
    }

    private String lastXChars(int xx, String input) {
        return input.chars()
                .mapToObj(c -> (char) c)
                .skip(input.length() - xx)
                .map(String::valueOf)
                .collect(Collectors.joining());
    }
}