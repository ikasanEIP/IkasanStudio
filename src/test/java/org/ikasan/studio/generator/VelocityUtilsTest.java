package org.ikasan.studio.generator;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VelocityUtilsTest extends LightJavaCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void test_generateFromTemplate() {
        Map<String, Object> configs = new HashMap<>();
        VelocityUtils.generateFromTemplate("Application.java", configs);
    }

    @Test
    public void test_generatePropertiesFromTemplate() {
        List<PropertyEntry> properties = new ArrayList<>();
        properties.add(new PropertyEntry("x","y"));

        Map<String, Object> configs = new HashMap<>();
        configs.put("properties", properties);
        VelocityUtils.generateFromTemplate("application.properties", configs);
    }
//    private void doTest(String before,
//                        String after,
//                        ConflictResolutionPolicy policy,
//                        TemplateResource template = findDefaultTemplate()) {
//        myFixture.configureByText('a.java', before)
//
//        PsiClass clazz = findClass()
//        Collection<PsiMember> members = collectMembers(clazz)
//        GenerateToStringWorker worker = buildWorker(clazz, policy)
//
//        WriteCommandAction.runWriteCommandAction(myFixture.project, "","", {
//                worker.execute(members, template, policy)
//        }, myFixture.file)
//
//        myFixture.checkResult(after)
//    }
//
//    private static TemplateResource findDefaultTemplate() {
//        Collection<TemplateResource> templates = ToStringTemplatesManager.getInstance().getAllTemplates();
//        def template = templates.find { it.fileName == "String concat (+)" }
//        assert template != null
//        template
//    }
//
//    private PsiClass findClass() {
//        PsiJavaFile file = (PsiFile)myFixture.getJavaFacade().get.getFile();
//        PsiClass[] classes = file.classes
//
//        assert classes.length > 0
//        classes[0]
//    }
}