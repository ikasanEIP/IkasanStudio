package org.ikasan.studio.model.psi;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.apache.maven.model.Dependency;
import org.ikasan.studio.Context;
import org.ikasan.studio.generator.*;
import org.ikasan.studio.model.StudioPsiUtils;

import java.util.List;

/**
 * Encapsulates the Intellij representation of the ikasan Module
 * The idea is to keep the ikasan Module clean of any Initellij specific details, this module will inspect the
 * code to generate the ikasan Module and update the code to reflect changes to the ikasan Module.
 */
public class PIPSIIkasanModel {
    private static final Logger LOG = Logger.getInstance("#PIPSIIkasanModel");
    private final String projectKey ;

    /**
     * Plugin PSI (Program Structure Interface) Iksanan Model builder
     * @param projectKey is the key to the cache shared by all components of the model. Note that Intellij shares the
     *                   memory for multiple open projects, so each plugin IkasanModule virtualization needs to be keyed
     *                   by the project name. Hence, projectKey is passed around most classes.
     */
    public PIPSIIkasanModel(final String projectKey) {
        this.projectKey = projectKey;

    }

    protected Project getProject() {
        return Context.getProject(projectKey);
    }

    /**
     * An update has been made to the diagram, so we need to reverse this into the code.
     */
    public void generateSourceFromModelInstance() {
        generateSourceFromModelInstance(null);
    }

    /**
     * An update has been made to the diagram, so we need to reverse this into the code.
     */
    public void generateSourceFromModelInstance(List<Dependency> newDependencies) {
        Project project = Context.getProject(projectKey);
        CommandProcessor.getInstance().executeCommand(
                project,
                () -> ApplicationManager.getApplication().runWriteAction(
                        () -> {
                            LOG.info("Start ApplicationManager.getApplication().runWriteAction - source from model" + Context.getIkasanModule(projectKey));
                            StudioPsiUtils.pomAddDependencies(projectKey, newDependencies);
                            //@todo start making below conditional on state changed.
                            ApplicationTemplate.create(project);
                            FlowTemplate.create(project);
                            ModuleConfigTemplate.create(project);
                            PropertiesTemplate.create(project);
                            LOG.info("End ApplicationManager.getApplication().runWriteAction - source from model");
                        }),
                "Generate Source from Flow Diagram",
                "Undo group ID");
        ApplicationManager.getApplication().runReadAction(
                () -> {
                    LOG.info("ApplicationManager.getApplication().runReadAction");
                    // reloadProject needed to re-read POM, must not be done till addDependancies
                    // fully complete, hence in next executeCommand block
                    if (newDependencies != null && !newDependencies.isEmpty() && Context.getOptions(projectKey).isAutoReloadMavenEnabled()) {
                        ProjectManager.getInstance().reloadProject(project);
                    }
                    LOG.info("End ApplicationManager.getApplication().runReadAction");
                });
    }

    public void generateJsonFromModelInstance() {
        Project project = Context.getProject(projectKey);
        CommandProcessor.getInstance().executeCommand(
                project,
                () -> ApplicationManager.getApplication().runWriteAction(
                        () -> {
                            LOG.info("Start ApplicationManager.getApplication().runWriteAction - json from model");
                            ModelTemplate.create(project);
                            LOG.info("End ApplicationManager.getApplication().runWriteAction - json from model");
                            LOG.info("model now" + Context.getIkasanModule(projectKey));
                        }),
                "Generate JSON from Flow Diagram",
                "Undo group ID");
    }
}
