package org.ikasan.studio.generator;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

public class ApplicationTemplate extends Generator {
    private static String APPLICATION_CLASS = "Application";
    private static String APPLICATION_CLASS_IMP = "@org.springframework.boot.autoconfigure.SpringBootApplication " +
            "public class Application { public static void main(String[] args) {" +
            "org.springframework.boot.SpringApplication.run(Application.class, args);}}";

    public static void createBasicAppication(final AnActionEvent ae, final Project project) {
        createTemplateFile(ae, project, DEFAULT_STUDIO_PACKAGE, APPLICATION_CLASS, APPLICATION_CLASS_IMP, false);
    }
}

