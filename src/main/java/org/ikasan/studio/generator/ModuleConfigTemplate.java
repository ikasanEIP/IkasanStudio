package org.ikasan.studio.generator;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

public class ModuleConfigTemplate extends Generator {
    private static String MODULE_CLASS = "ModuleConfig";
    private static String MODULE_CLASS_IMPL =
            "@org.springframework.context.annotation.Configuration(\"ModuleFactory\")" +
                    "@org.springframework.context.annotation.ImportResource({\"classpath:h2-datasource-conf.xml\"})" +
                    "public class ModuleConfig {}";
    public static void createBasicModule(final AnActionEvent ae, final Project project) {
        createTemplateFile(ae, project, DEFAULT_STUDIO_PACKAGE, MODULE_CLASS, MODULE_CLASS_IMPL, true);
    }
}
