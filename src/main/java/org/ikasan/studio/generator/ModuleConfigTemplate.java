package org.ikasan.studio.generator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiJavaFile;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.Ikasan.IkasanModule;

import java.util.HashMap;
import java.util.Map;

/**
 * Currently both velocity and embedded strategy are being evaluated
 */
public class ModuleConfigTemplate extends Generator {
    public static String MODULE_CLASS_NAME = "ModuleConfig";
    private static String MODULE_VM = "ModuleConfigTemplate.vm";

    public static void create(final Project project) {
        IkasanModule ikasanModule = Context.getIkasanModule(project.getName());
        String templateString = generateContents(ikasanModule);
        PsiJavaFile newFile = createTemplateFile(project, STUDIO_BOOT_PACKAGE, MODULE_CLASS_NAME, templateString, true, true);
        ikasanModule.getViewHandler().setPsiJavaFile(newFile);
    }

    public static String generateContents(IkasanModule ikasanModule) {
        Map<String, Object> configs = getVelocityConfigs();
        configs.put(CLASS_NAME_TAG, MODULE_CLASS_NAME);
        configs.put(FLOWS_TAG, ikasanModule.getFlows());
        configs.put(MODULE_TAG, ikasanModule);
        String templateString = VelocityUtils.generateFromTemplate(MODULE_VM, configs);
        return templateString;
    }
}
