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
 * The idea is to keep the ikasan Module clean of any Initellij specific details, this module will inpect the
 * code to generate the ikasan Module and update the code to reflect changes to the ikasan Module.
 */
public class PIPSIIkasanModel {
    private static final Logger LOG = Logger.getInstance("#PIPSIIkasanModel");
    private final String projectKey ;

    /**
     * Plugin PSI (Program Structure Interface) Iksanan Model builder
     * @param projectKey is the key to the cache shared by all components of the model. Note that Intellij shares the
     *                   memory for multiple open projects, so each plugin IkasanModule virtualisation needs to be keyed
     *                   by the project name. Hence projectKey is passed around most classes.
     */
    public PIPSIIkasanModel(final String projectKey) {
        this.projectKey = projectKey;

    }

    protected Project getProject() {
        return Context.getProject(projectKey);
    }

    /**
     * An update has been made to the diagram so we need to reverse this into the code.
     */
    public void generateSourceFromModel() {
        generateSourceFromModel(null);
    }

    /**
     * An update has been made to the diagram, so we need to reverse this into the code.
     */
    public void generateSourceFromModel(List<Dependency> newDependencies) {
        Project project = Context.getProject(projectKey);
        CommandProcessor.getInstance().executeCommand(
                project,
                () -> ApplicationManager.getApplication().runWriteAction(
                        () -> {
                            LOG.info("Start ApplicationManager.getApplication().runWriteAction - source from model");
                            StudioPsiUtils.pomAddDependancies(projectKey, newDependencies);
                            //@todo start making below conditional on state changed.
                            ApplicationTemplate.create(project);
//                        generateBespokeComponents(project);
                            FlowTemplate.create(project);
                            ModuleConfigTemplate.create(project);
                            PropertiesTemplate.create(project);
                            LOG.info("End ApplicationManager.getApplication().runWriteAction - source from model");
                        }),
                "Generate Source from Flow Diagram",
                "Undo group ID");

        // Above is asynch, need to execute below as a second command in the same undo group,
        // Above command is executed then when done, next command in command group is done.
//            CommandProcessor.getInstance().executeCommand(
//                    project,
////                    () -> ApplicationManager.getApplication().runWriteAction(
//                    () -> ApplicationManager.getApplication().runReadAction(
//                            () -> {
//                                IkasanModule ikasanModule = Context.getIkasanModule(project.getName());
//                                moduleConfigClazz = ikasanModule.getViewHandler().getClassToNavigateTo();
//                                // reloadProject needed to re-read POM, must not be done till addDependancies
//                                // fully complete, hence in next executeCommand block
//                                if (newDependencies != null && !newDependencies.isEmpty()) {
//                                    ProjectManager.getInstance().reloadProject(project);
//                                }
//                            }),
//                    "Refresh POM",
//                    "Undo group ID");
//        ReadAction.nonBlocking(() -> {
//            IkasanModule ikasanModule = Context.getIkasanModule(project.getName());
//            moduleConfigClazz = ikasanModule.getViewHandler().getClassToNavigateTo();
//            // reloadProject needed to re-read POM, must not be done till addDependancies
//            // fully complete, hence in next executeCommand block
//            if (newDependencies != null && !newDependencies.isEmpty()) {
//                ProjectManager.getInstance().reloadProject(project);
//            }
//        }).inSmartMode(project);
        ApplicationManager.getApplication().runReadAction(
                () -> {
                    LOG.info("ApplicationManager.getApplication().runReadAction");
//                    Module ikasanModule = Context.getIkasanModule(project.getName());
                    // reloadProject needed to re-read POM, must not be done till addDependancies
                    // fully complete, hence in next executeCommand block
                    if (newDependencies != null && !newDependencies.isEmpty() && Context.getOptions(projectKey).isAutoReloadMavenEnabled()) {
                        ProjectManager.getInstance().reloadProject(project);
                    }
                    LOG.info("End ApplicationManager.getApplication().runReadAction");
                });
    }

    public void updateJsonModel() {
        Project project = Context.getProject(projectKey);
        CommandProcessor.getInstance().executeCommand(
                project,
                () -> ApplicationManager.getApplication().runWriteAction(
                        () -> {
                            LOG.info("Start ApplicationManager.getApplication().runWriteAction - model from source");
                            ModelTemplate.create(project);
                            LOG.info("End ApplicationManager.getApplication().runWriteAction - model from source");
                        }),
                "Generate JSON from Flow Diagram",
                "Undo group ID");

//        ApplicationManager.getApplication().runReadAction(
//                () -> {
//                    Module ikasanModule = Context.getIkasanModule(project.getName());
//                    // reloadProject needed to re-read POM, must not be done till addDependancies
//                    // fully complete, hence in next executeCommand block
//                    if (newDependencies != null && !newDependencies.isEmpty() && Context.getOptions(projectKey).isAutoReloadMavenEnabled()) {
//                        ProjectManager.getInstance().reloadProject(project);
//                    }
//                });
    }


    //    /**
//     * The public entry point, builds the Pseudo ikasan Module used by the plugin.
//     */
//    // New Way
//    public void updateIkasanModuleFromSourceCode() {
//        PsiFile moduleConfigPsiFile = moduleConfigClazz.getContainingFile();
//        LOG.debug("Extracting ikasan model from file " + moduleConfigPsiFile.getText());
//        PsiClass moduleConfigClass = getClassFromPsiFile(moduleConfigPsiFile);
//        if (moduleConfigClass != null) {
//            PsiMethod getModuleMethod = StudioPsiUtils.findMethodFromClassByReturnType(moduleConfigClass, OLD_MODULE_BEAN_CLASS);
//            if (getModuleMethod != null) {
//                PsiStatement getModuleMethodReturnStatement = getReturnStatementFromMethod(getModuleMethod);
//                PsiReferenceExpressionImpl localVariableOfReturnStatement =  getLocalVariableFromReturnStatement(getModuleMethodReturnStatement);
//
//                if (localVariableOfReturnStatement == null) {
//                    // return ref contains the new statement
//                    PIPSIMethodList moduleMethodList = extractMethodCallsFromChain(getModuleMethodReturnStatement.getChildren(), new PIPSIMethodList());
//                    parseModuleStatementRHS(moduleMethodList, moduleConfigClass);
//                } else {
//                    // Need to refresh the Application properties
//                    Context.setApplicationProperties(projectKey, StudioPsiUtils.getApplicationProperties(getProject()));
//                    updateModuleWithProperties();
//
//                    PsiElement moduleLocalVariable = localVariableOfReturnStatement.resolve();
//                    if (moduleLocalVariable != null) {
//                        PIPSIMethodList moduleMethodList = extractMethodCallsFromChain(moduleLocalVariable.getChildren(), new PIPSIMethodList());
//
//                        // The base of the moduleMethodList is the moduleBulder e.g. mb.withDescription("Ftp Jms Sample Module")...
//                        // So get get it and resolve it back to the moduleBuilderDefintion
//                        PsiLocalVariable moduleBuilderLocalVariable = getModuleBuilderLocalVariable(moduleMethodList);
//                        // ikasan module is a member variable and built rather passed around to accommodate partial population / best endeavours
//                // @todo - could try to add the getModuleBuilder to the methodList then parse as above then wont need updateIkasanModuleWithModuleBuilder, just parseModuleStatementRHS
//                        updateIkasanModuleWithModuleBuilder(moduleBuilderLocalVariable);
//
//                        parseModuleStatementRHS(moduleMethodList, moduleConfigClass);
//                    } else {
//                        LOG.warn("Could not find moduleLocalVariable using moduleBeanMethod [" + getModuleMethod + "] and returnReference [" + localVariableOfReturnStatement + "]");
//                    }
//                }
//            } else {
//                LOG.warn("Could not load moduleBeanMethod using moduleConfigClass [" + moduleConfigClass + "]");
//            }
//        } else {
//            LOG.warn("Could not build moduleConfigClass from moduleConfigPsiFile [" + moduleConfigPsiFile + "]");
//        }
//    }

//
//    enum IkasanClazz {
//        MODULE_BUILDER("ModuleBuilder", "org.ikasan.builder.ModuleBuilder"),
//        FLOW_BUILDER("FlowBuilder", "org.ikasan.builder.FlowBuilder"),
//        COMPONENT_BUILDER("ComponentBuilder", "org.ikasan.builder.component.ComponentBuilder"),
//        EXCEPTION_RESOLVER_BUILDER("ExceptionResolverBuilder", "org.ikasan.builder.ExceptionResolverBuilder"),
//        BUILDER_FACTORY("BuilderFactory", "org.ikasan.builder.BuilderFactory"),
//        LOCAL_METHOD("", ""),
//        BESKPOKE_CLASS("", ""),
//        UNKNOWN("", "");
//
//        public final String clazzName;
//        public final String fullClazzName;
//
//        IkasanClazz(String clazzName, String fullClazzName) {
//            this.clazzName = clazzName;
//            this.fullClazzName = fullClazzName;
//        }
//
//        public static IkasanClazz parseClassType(String classType) {
//            for (IkasanClazz ikasaClazz : IkasanClazz.values()) {
//                if (ikasaClazz.clazzName.equals(classType) || ikasaClazz.fullClazzName.equals(classType)) {
//                    return ikasaClazz;
//                }
//            }
//            return BESKPOKE_CLASS;
//        }
//    }
//
//    /**
//     * Given we have built up all the flow, deduce the input and output end point for the flow.
//     * @param ikasanFlow that holds all the flow elements
//     */
//    protected Flow addInputOutputForFlow(final Flow ikasanFlow) {
//        List<FlowElement> flowElements = ikasanFlow.getFlowComponentList();
//        if (!flowElements.isEmpty()) {
//            FlowElement input = IkasanComponentType.getEndpointForFlowElement(flowElements.get(0), ikasanFlow);
//            if (input != null) {
//                ikasanFlow.setInput(input);
//            }
//            FlowElement output = IkasanComponentType.getEndpointForFlowElement(flowElements.get(flowElements.size()-1), ikasanFlow);
//            if (output != null) {
//                ikasanFlow.setOutput(output);
//            }
//        }
//        return ikasanFlow;
//    }
//

//
//    protected String getFullQualifiedClassName(String className) {
//        String bestGuess = className;
//        if (className != null) {
//            PsiClass fullyQualifiedExceptionClass = StudioPsiUtils.findFirstClass(Context.getProject(projectKey), className);   // For user defined exceptions
//            if (fullyQualifiedExceptionClass == null) {
//                fullyQualifiedExceptionClass = StudioPsiUtils.findFirstClass(Context.getProject(projectKey), className.replace(".class", ""));
//            }
//            if (fullyQualifiedExceptionClass != null) {
//                bestGuess = fullyQualifiedExceptionClass.getQualifiedName();
//            }
//        }
//        return bestGuess;
//    }
//
//    /**
//     * This is typically a local method that contains the flow defintion e.g.
//     * public Flow getFtpConsumerFlow(ModuleBuilder moduleBuilder, ComponentBuilder componentBuilder)
//     * @param myMethod to search
//     * @return flow local variable if it exists
//     */
//    protected PsiLocalVariable getFlowLocalVariableFromBespokeGetterMethod(final PsiMethod myMethod) {
//        PsiLocalVariable flowLocalVariable = null;
//        PsiReferenceExpression flowGetterReturnReference = getlocalVariableFromReturnReferenceForPSIMethod(myMethod);
//
//        if (flowGetterReturnReference != null && flowGetterReturnReference.resolve() != null) {
//            flowLocalVariable = (PsiLocalVariable)flowGetterReturnReference.resolve();
//        }
//        return flowLocalVariable;
//    }
//
//    /**
//     * The return statement for the supplied method will contain a reference to the returned value e.g. 'return ftpToJmsFlow;' is 'ftpToJmsFlow'
//     * @param myMethod we are looking through e.g. the getFtpConsumerFlow method
//     * @return the pointer to the returned value.
//     */
//    protected PsiReferenceExpressionImpl getlocalVariableFromReturnReferenceForPSIMethod(final PsiMethod myMethod) {
//        PsiStatement psiReturnStatement = getReturnStatementFromMethod(myMethod);
//        return getLocalVariableFromReturnStatement(psiReturnStatement);
//    }
//
//    /**
//     * The return statement for the supplied method will contain a reference to the returned value e.g. 'return ftpToJmsFlow;' is 'ftpToJmsFlow'
//     * @param psiReturnStatement we are looking through e.g. the getFtpConsumerFlow method
//     * @return the pointer to the returned value.
//     */
//    protected PsiReferenceExpressionImpl getLocalVariableFromReturnStatement(final PsiStatement psiReturnStatement) {
//        PsiReferenceExpressionImpl returnReference = null;
//        if (psiReturnStatement!= null) {
//            try {
//                returnReference = (PsiReferenceExpressionImpl) Arrays.stream(psiReturnStatement.getChildren())
//                        .filter(item -> item instanceof PsiReferenceExpressionImpl)
//                        .findFirst()
//                        .orElse(null);
//            } catch (NoSuchElementException nsee) {
//                LOG.warn("Could not find return statement reference from " + psiReturnStatement + ". Exception: " + nsee.getMessage(), nsee);
//            }
//        }
//        return returnReference;
//    }
//

}
