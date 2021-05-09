package org.ikasan.studio.model.psi;

import com.intellij.lang.jvm.JvmMethod;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.PsiJavaCodeReferenceElementImpl;
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl;
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl;
import org.apache.log4j.Logger;
import org.ikasan.studio.Context;
import org.ikasan.studio.StudioUtils;
import org.ikasan.studio.generator.ApplicationTemplate;
import org.ikasan.studio.generator.FlowTemplate;
import org.ikasan.studio.generator.ModuleConfigTemplate;
import org.ikasan.studio.generator.PropertiesTemplate;
import org.ikasan.studio.model.StudioPsiUtils;
import org.ikasan.studio.model.ikasan.*;

import java.util.*;

/**
 * Encapsulates the Intellij representation of the ikasan Module
 *
 * The idea is to keep the ikasan Module clean of any Initellij specific details, this module will inpect the
 * code to generate the ikasan Module and update the code to reflect changes to the ikasan Module.
 */
public class PIPSIIkasanModel {
    public static final String OLD_MODULE_BEAN_CLASS = "org.ikasan.spec.module.Module";

    private static final Logger log = Logger.getLogger(PIPSIIkasanModel.class);
    private static final String WITH_DESCRIPTION_METHOD_NAME = "withDescription";
    private static final String ADD_FLOW_METHOD_NAME = "addFlow";
    private static final String MODULE_BUILDER_SET_NAME_METHOD = "getModuleBuilder";
    private static final String FLOW_BUILDER_NAME_METHOD = "getFlowBuilder";
    private static final String COMPONENT_BUILDER_NAME_METHOD = "getComponentBuilder";

    private PsiElementFactory javaPsiFactory;
    private String projectKey ;
    private IkasanModule ikasanModule;
    private PsiClass moduleConfigClazz;
    private PsiJavaFile moduleConfigFile;
    private PsiJavaFile applicationClazz;
    private PsiFile resourceFile;

    /**
     * Plugin PSI (Program Structure Interface) Iksanan Model builder
     * @param projectKey is the key to the cache shared by all components of the model. Note that Intellij shares the
     *                   memory for multiple open projects, so each plugin IkasanModule virtualisation needs to be keyed
     *                   by the project name. Hence projectKey is passed around most classes.
     */
    public PIPSIIkasanModel(final String projectKey) {
        this.projectKey = projectKey;
        ikasanModule = Context.getIkasanModule(projectKey);
        javaPsiFactory = JavaPsiFacade.getInstance(getProject()).getElementFactory();
    }

    public void setModuleConfigClazz(PsiClass moduleConfigClazz) {
        this.moduleConfigClazz = moduleConfigClazz;
    }
    public PsiClass getModuleConfigClazz() {
        return  moduleConfigClazz;
    }

    protected Project getProject() {
        return Context.getProject(projectKey);
    }

    /**
     * An update has been made to the diagram so we need to reverse this into the code.
     */
    public void generateSourceFromModel() {
        Project project = Context.getProject(projectKey);
//        if (moduleConfigClazz == null) {
            CommandProcessor.getInstance().executeCommand(
                project,
                () -> ApplicationManager.getApplication().runWriteAction(
                    () -> {
                        ApplicationTemplate.create(project);
//                        generateBespokeConponents(project);
                        FlowTemplate.create(project);
                        ModuleConfigTemplate.create(project);
                        PropertiesTemplate.create(project);
                        // Basics done, now populate the source with the specifics of the model


                    }),
                "Generate source from flow diagram",
                "Undo group ID");

            // is the above asyn ??
            // @todo verify if above is asynch, if so maybe block or pass in this we we can update moduleConfigClazz.
            IkasanModule ikasanModule = Context.getIkasanModule(project.getName());
            moduleConfigClazz = ikasanModule.getViewHandler().getClassToNavigateTo();

//        }
    }

//    private void generateBespokeConponents(Project project) {
//        IkasanModule ikasanModule = Context.getIkasanModule(project.getName());
//        for(IkasanFlow flow : ikasanModule.getFlows()) {
//            for (IkasanFlowComponent component : flow.getFlowComponentList()) {
//                if (component.getType().isBespokeClass()) {
//                    BespokeClassTemplate.create(project, component);
//                }
//            }
//        }
//    }

    protected PsiLocalVariable getModuleBuilderLocalVariable(PIPSIMethodList moduleMethodList) {
        PsiLocalVariable moduleBuilderLocalVariable = null;
        PsiReferenceExpression moduleBuiderReference = moduleMethodList.getBaseMethodinstanceVariable();
        IkasanClazz ikasanClazz = getTypeOfReferenceExpression(moduleBuiderReference);

        if (IkasanClazz.MODULE_BUILDER.equals(ikasanClazz)) {
            // ModuleBuilder mb = builderFactory.getModuleBuilder("fms-ftp");
            PsiElement potentialModuleBuilderLocalVariable = moduleBuiderReference.resolve();
            if (potentialModuleBuilderLocalVariable instanceof PsiLocalVariable) {
                moduleBuilderLocalVariable = (PsiLocalVariable)potentialModuleBuilderLocalVariable;
            }

        }
        return moduleBuilderLocalVariable;
    }

    /**
     * Process the ModuleBuilder expression
     *
     * @param moduleBuilderLocalVariable points to the module name and the variable which is the root of all elements of the module
     * e.g. ModuleBuilder mb = builderFactory.getModuleBuilder("fms-ftp");
     */
    protected void updateIkasanModuleWithModuleBuilder(PsiLocalVariable moduleBuilderLocalVariable) {
        PIPSIMethodList moduleBuilderMethodList = extractMethodCallsFromChain(moduleBuilderLocalVariable.getChildren(), new PIPSIMethodList());
        PIPSIMethod moduleBuilderMethod = moduleBuilderMethodList.getFirstMethodByName(MODULE_BUILDER_SET_NAME_METHOD);
        ikasanModule.setName(getReferenceOrLiteral(moduleBuilderMethod,0));
        updateModuleWithProperties();
    }

    private void updateModuleWithProperties() {
        Properties properties = Context.getApplicationProperties(projectKey);
        ikasanModule.setApplicationPackageName((String)properties.get(IkasanComponentPropertyMeta.APPLICATION_PACKAGE_KEY));
        ikasanModule.setApplicationPortNumber((String)properties.get(IkasanComponentPropertyMeta.APPLICATION_PORT_NUMBER_KEY));
    }


    /**
     * From the given pipsiMethod and attribute, determine if the attribute is a property, if so return the value
     * otherwise assume the attribute was a literal and return that.
     * @param pipsiMethod to interrogate
     * @param attribute to interrogate
     * @return a String containing the Spring injected value or literal value
     */
    private String getReferenceOrLiteral(PIPSIMethod pipsiMethod, int attribute) {
        //                //@todo we should be able to get the type as well if we need to
        String parameter = "";
        String springValueKey = getSpringValueKey(pipsiMethod, attribute);
        if (springValueKey != null) {
            Properties properties = Context.getApplicationProperties(projectKey);
            parameter = properties.getProperty(springValueKey);
        } else {
            // A standard literal have been provided.
            parameter = pipsiMethod.getLiteralParameterAsString(attribute, true);
        }
        return parameter;
    }

    /**
     * The public entry point, builds the Pseudo ikasan Module used by the plugin.
     * @return A populated ikasan Module used by the plugin.
     */
    // New Way
    public void updateIkasanModule() {
        PsiFile moduleConfigPsiFile = moduleConfigClazz.getContainingFile();
        log.debug("Extracting ikasan model from file " + moduleConfigPsiFile.getText());
        PsiClass moduleConfigClass = getClassFromPsiFile(moduleConfigPsiFile);
        if (moduleConfigClass != null) {
            PsiMethod getModuleMethod = StudioPsiUtils.findMethodFromClassByReturnType(moduleConfigClass, OLD_MODULE_BEAN_CLASS);
            if (getModuleMethod != null) {
                PsiStatement getModuleMethodReturnStatement = getReturnStatementFromMethod(getModuleMethod);
                PsiReferenceExpressionImpl localVariableOfReturnStatement =  getLocalVariableFromReturnStatement(getModuleMethodReturnStatement);

                if (localVariableOfReturnStatement == null) {
                    // return ref contains the new statement
                    PIPSIMethodList moduleMethodList = extractMethodCallsFromChain(getModuleMethodReturnStatement.getChildren(), new PIPSIMethodList());
                    parseModuleStatementRHS(moduleMethodList, moduleConfigClass);
                } else {
                    // Need to refresh the Application properties
                    Context.setApplicationProperties(projectKey, StudioPsiUtils.getApplicationProperties(getProject()));
                    updateModuleWithProperties();

                    PsiElement moduleLocalVariable = localVariableOfReturnStatement.resolve();
                    if (moduleLocalVariable != null) {
                        PIPSIMethodList moduleMethodList = extractMethodCallsFromChain(moduleLocalVariable.getChildren(), new PIPSIMethodList());

                        // The base of the moduleMethodList is the moduleBulder e.g. mb.withDescription("Ftp Jms Sample Module")...
                        // So get get it and resolve it back to the moduleBuilderDefintion
                        PsiLocalVariable moduleBuilderLocalVariable = getModuleBuilderLocalVariable(moduleMethodList);
                        // ikasan module is a member variable and built rather passed around to accommodate partial population / best endeavours
                // @todo - could try to add the getModuleBuilder to the methodList then parse as above then wont need updateIkasanModuleWithModuleBuilder, just parseModuleStatementRHS
                        updateIkasanModuleWithModuleBuilder(moduleBuilderLocalVariable);

                        parseModuleStatementRHS(moduleMethodList, moduleConfigClass);
                    } else {
                        log.error("Could not find moduleLocalVariable using moduleBeanMethod [" + getModuleMethod + "] and returnReference [" + localVariableOfReturnStatement + "]");
                    }
                }
            } else {
                log.error("Could not load moduleBeanMethod using moduleConfigClass [" + moduleConfigClass + "]");
            }
        } else {
            log.error("Could not build moduleConfigClass from moduleConfigPsiFile [" + moduleConfigPsiFile + "]");
        }
    }

    /**
     * Given a Intellij virtual file, extract the virtual PSI class.
     * @param psiFile to examine
     * @return an Intellij 'Program Structure Interface' (PSI) virtual Class
     */
    protected PsiClass getClassFromPsiFile(final PsiFile psiFile) {
        PsiClass returnClass = null;
        if (psiFile instanceof  PsiJavaFile) {
            PsiJavaFile jfile = (PsiJavaFile)psiFile;
            PsiClass[] psiClasses = jfile.getClasses();
            if (psiClasses.length > 0) {
                returnClass = psiClasses[0];
            }
        }
        return returnClass;
    }

    /**
     * Parse the declaration of the module e.g.
     *
     * ... mb.withDescription("Ftp Jms Sample Module")
     *                 .addFlow(ftpToJmsFlow).addFlow(jmsToFtpFlow).build();
     *
     * extract description then process each flow.
     *
     * @param methodList of the methods called by the builder e.g. withDescription, addFlow
     */
    protected void parseModuleStatementRHS(final PIPSIMethodList methodList, final PsiClass moduleConfigClass) {
        // Set the module description
        PIPSIMethod withDescriptionMethod = methodList.getFirstMethodByName(WITH_DESCRIPTION_METHOD_NAME);
        ikasanModule.setDescription(withDescriptionMethod == null ? null :
                getReferenceOrLiteral(withDescriptionMethod, 0));
//                withDescriptionMethod.getLiteralParameterAsString(0, true));

        // Just in case we have it here also
        //@todo what it parameter was a reference or spring reference- chase !
        PIPSIMethod moduleBuilderMethod = methodList.getFirstMethodByName(MODULE_BUILDER_SET_NAME_METHOD);
        if (moduleBuilderMethod != null) {
//            ikasanModule.setName(moduleBuilderMethod.getLiteralParameterAsString(0, true));
            ikasanModule.setName(getReferenceOrLiteral(moduleBuilderMethod, 0));
        }

        // Expose the flows
        List<PIPSIMethod> pipsiFlowMethods = methodList.getMethodsByName(ADD_FLOW_METHOD_NAME);
        for (PIPSIMethod pipsiMethod :  pipsiFlowMethods) {
            PsiLocalVariable flowLocalVariable = null;
            PsiLocalVariable flowBuilderLocalVariable = null;
            // addFlow(param)
//            PsiReferenceExpression addFlowParameter = (PsiReferenceExpression)pipsiMethod.getParameter(0);

            PsiExpression addFlowParameter = pipsiMethod.getParameter(0);
            PsiElement addFlowParameterResolved = null ;
            PsiElement[] methodChainToCreateItem = null;
            if (addFlowParameter instanceof  PsiReferenceExpression) {
                addFlowParameterResolved = ((PsiReferenceExpression) addFlowParameter).resolve();  // e.g. local var ftpToJmsFlow   (pointing to Flow ftpToJmsFlow = getFtpConsumerFlow(mb,builderFactory.getComponentBuilder());
                methodChainToCreateItem = addFlowParameterResolved.getChildren();
//                String parameterType = getParameterType((PsiReferenceExpression) addFlowParameter);
            } else if (addFlowParameter instanceof  PsiMethodCallExpression) {
//                addFlowParameterResolved = ((PsiMethodCallExpressionImpl) addFlowParameter).resolveMethod();
                addFlowParameterResolved = addFlowParameter;
                methodChainToCreateItem = new PsiElement[] { addFlowParameter } ;
            } else {
                ; //@todo
            }
            // Trace flow param back to definition,
            //          .addFlow(sourceFlow) -> Flow sourceFlow = moduleBuilder.getFlowBuilder("dbToJMSFlow").with ...
            //          .addFlow(jmsToFtpFlow) -> Flow ftpToJmsFlow = getFtpConsumerFlow(mb,builderFactory.getComponentBuilder())
            //          .addFlow(get_NewFlow1(moduleBuilder, componentBuilder)) -> Flow get_NewFlow1(ModuleBuilder moduleBuilder, ComponentBuilder componentBuilder) {
            //          .addFlow(newflow1.getNewflow1()) -> @Resource  Newflow1 newflow1;  .. newFlow1.getFlow

            if (addFlowParameterResolved != null) {
                PIPSIMethodList expressionRHS = extractMethodCallsFromChain(methodChainToCreateItem, new PIPSIMethodList());
//                PIPSIMethodList expressionRHS = extractMethodCallsFromChain(addFlowParameterResolved.getChildren(), new PIPSIMethodList());
                // this could be another local method call, or it could be the flow definition.
                PsiReferenceExpression expressionStart = expressionRHS.baseMethodinstanceVariable;
                IkasanClazz expressionStartType = getTypeOfReferenceExpression(expressionStart);

                if (expressionStartType.equals(IkasanClazz.MODULE_BUILDER)) {
                    // Flow xx = moduleBuilder.getFlowBuilder()...
                    flowLocalVariable = (PsiLocalVariable) addFlowParameterResolved;
                    flowBuilderLocalVariable = (PsiLocalVariable) addFlowParameterResolved;
                } else if (expressionStartType.equals(IkasanClazz.FLOW_BUILDER)) {
                    // Flow flow = fb.withDescription("Flow demonstrates usage of JMS Concumer and JMS Producer")...
                    flowLocalVariable = (PsiLocalVariable) addFlowParameterResolved;
                    flowBuilderLocalVariable = (PsiLocalVariable) expressionStart.resolve();
                } else if (expressionStartType.equals(IkasanClazz.LOCAL_METHOD)){
                    flowLocalVariable = getFlowLocalVariableFromBespokeGetterMethod(expressionRHS.getMethodByIndex(0).getMethodDeclaration());
                    flowBuilderLocalVariable = getFlowBuilderLocalVariableFromFlowExpression(flowLocalVariable);
                } else if (expressionStartType.equals(IkasanClazz.BESKPOKE_CLASS)) {
                    // Assume its a bespoke class and try to look it up, assume there is a class get'ClassName'
                    PsiElement bespokeClassVariable = expressionStart.resolve();
                    if (bespokeClassVariable instanceof PsiVariable) {
                        String beskpokeClazzName = StudioPsiUtils.getTypeOfVariable((PsiVariable) bespokeClassVariable);
                        PsiClass bespokeClazz = StudioPsiUtils.findFirstClass(getProject(), beskpokeClazzName);
                        PsiMethod getFlowMethod = StudioPsiUtils.findMethodFromClassByReturnType(bespokeClazz, "org.ikasan.spec.flow.Flow");
                        flowLocalVariable = getFlowLocalVariableFromBespokeGetterMethod(getFlowMethod);
                        flowBuilderLocalVariable = getFlowBuilderLocalVariableFromFlowExpression(flowLocalVariable);
                    }

                } else {

                    log.error("Unable to parse methodList [" + methodList + "]");
                }

                if (flowBuilderLocalVariable != null || flowLocalVariable != null) {
                    IkasanFlow newFlow = updateIkasanFlowWithFlowBuilder(flowBuilderLocalVariable);
                    parseFlowMethod(flowLocalVariable, moduleConfigClass, newFlow);
                }
            }
        }
    }

    protected String getParameterType(PsiReferenceExpression parameter) {
        PsiElement parameterResolved = parameter.resolve();

        if (parameterResolved instanceof PsiField && parameterResolved.getText().startsWith("@Resouce")) {
            return "SpringResource";
        } else {
            return "Unknown";
        }
    }

    protected IkasanFlow updateIkasanFlowWithFlowBuilder(final PsiLocalVariable flowBuilderLocalVar) {
        IkasanFlow newFlow = new IkasanFlow();
        PIPSIMethodList flowBuilderMethodCalls = extractMethodCallsFromChain(flowBuilderLocalVar.getChildren(), new PIPSIMethodList());
        PIPSIMethod getFlowBuilderCall = flowBuilderMethodCalls.getFirstMethodByName(FLOW_BUILDER_NAME_METHOD);
//        String flowName = getFlowBuilderCall.getLiteralParameterAsString(0, true);
        String flowName = getReferenceOrLiteral(getFlowBuilderCall, 0);
        newFlow.setName(flowName);
        return newFlow;
    }


    /**
     * Get the flowBuilder method from the flow variable
     *
     * Flow ftpToJmsFlow = ftpToLogFlowBuilder.withDescription("Ftp to Jms")
     *
     * @param flowLocalVariable
     * @return
     */
    protected PsiLocalVariable getFlowBuilderLocalVariableFromFlowExpression(final PsiElement flowLocalVariable) {
        PsiLocalVariable flowBuilderLocalVariable = null;
        if (flowLocalVariable != null) {
            PIPSIMethodList expressionRHS = extractMethodCallsFromChain(flowLocalVariable.getChildren(), new PIPSIMethodList());
            PsiReferenceExpression expressionStart = expressionRHS.baseMethodinstanceVariable;
            IkasanClazz expressionStartType = getTypeOfReferenceExpression(expressionStart);
            if (IkasanClazz.FLOW_BUILDER.equals(expressionStartType)) {
                flowBuilderLocalVariable = (PsiLocalVariable)expressionStart.resolve();
            }
        }
        return flowBuilderLocalVariable;
    }

    /**
     * Parse the flow method e.g. getFtpConsumerFlow(..) { .. }. This works backwards from the return statement
     * @param flowLocalVar pointing to Flow definition
     * @param moduleConfigClass
     */
    protected void parseFlowMethod(final PsiLocalVariable flowLocalVar, final PsiClass moduleConfigClass, IkasanFlow newFlow) {

        PIPSIMethodList flowMethodCalls = extractMethodCallsFromChain(flowLocalVar.getChildren(), new PIPSIMethodList());
        for(PIPSIMethod pipsiMethod : flowMethodCalls.getPipsiMethods()) {

            if (IkasanComponentCategory.DESCRIPTION.associatedMethodName.equals(pipsiMethod.getName())) {
//                newFlow.setDescription(pipsiMethod.getLiteralParameterAsString(0, true));
                newFlow.setDescription(getReferenceOrLiteral(pipsiMethod, 0));
            }

            if (IkasanComponentCategory.BROKER.associatedMethodName.equals(pipsiMethod.getName()) ||
                IkasanComponentCategory.CONSUMER.associatedMethodName.equals(pipsiMethod.getName()) ||
                IkasanComponentCategory.CONVERTER.associatedMethodName.equals(pipsiMethod.getName()) ||
                IkasanComponentCategory.FILTER.associatedMethodName.equals(pipsiMethod.getName()) ||
                IkasanComponentCategory.PRODUCER.associatedMethodName.equals(pipsiMethod.getName()) ||
                IkasanComponentCategory.ROUTER.associatedMethodName.equals(pipsiMethod.getName()) ||
                IkasanComponentCategory.SPLITTER.associatedMethodName.equals(pipsiMethod.getName()) ||
                IkasanComponentCategory.TRANSLATER.associatedMethodName.equals(pipsiMethod.getName())) {
                String flowElementName = pipsiMethod.getLiteralParameterAsString(0, true);
                String flowElementDescription = pipsiMethod.getLiteralParameterAsString(0, true);
                IkasanFlowComponent ikasanFlowComponent = null;

//                        new IkasanFlowComponent(
//                        newFlow,
//                        pipsiMethod.getLiteralParameterAsString(0, true),
//                        pipsiMethod.getLiteralParameterAsString(0, true));
                // usually the component, or componentFactory.getXX(), or new PayloadToMapConverter()
                PsiExpression flowComponentParam2 = pipsiMethod.getParameter(1);

                if (flowComponentParam2 != null) {
                    // Simplest scenario e.g. new MapMessageToPayloadConverter()
                    if (flowComponentParam2 instanceof PsiNewExpression) {
                        PsiJavaCodeReferenceElement flowComponentConstructor = (PsiJavaCodeReferenceElement) Arrays.stream(flowComponentParam2.getChildren()).filter(x -> x instanceof PsiJavaCodeReferenceElement).findFirst().orElse(null);
                        if (flowComponentConstructor != null) {
                            IkasanComponentType ikasanComponentType = IkasanComponentType.parseMethodName(flowComponentConstructor.getText());
                            if (IkasanComponentType.UNKNOWN == ikasanComponentType) {
                                // This is probably a bespoke component, try to deduce the component from the type
                                String classInterface = null;
                                try {
                                    // @todo maybe cycle through and try all interfaces for an ikasan match in case clients play with multiple interfaces.
                                    classInterface = ((PsiClass)((PsiJavaCodeReferenceElementImpl) flowComponentConstructor).resolve()).getImplementsList().getReferencedTypes()[0].getClassName();
                                    ikasanComponentType = IkasanComponentType.parseMethodName(classInterface);
                                } catch (NullPointerException npe) {
                                    // Hate to catch NPE but until I can turn this into an element safe recursion, this will have to do.
                                    log.warn("Attempt to get interface type for " + flowComponentConstructor.getText()+ " failed. Component type will be set as unknown");
                                }
                            }
                            ikasanFlowComponent = IkasanFlowComponent.getInstance(ikasanComponentType, newFlow, flowElementName, flowElementDescription);
                        }
                    } else {
                    // More complex scenario, we need to traverse down the call stack until we find a component.
                        PIPSIMethodList pipsiMethodList = getComponentBuilderMethods(flowComponentParam2) ;
                        ikasanFlowComponent = createFlowElementWithProperties(newFlow, flowElementName, flowElementDescription, pipsiMethodList);
                    }
                }
                if (ikasanFlowComponent == null) {
                    ikasanFlowComponent = IkasanFlowComponent.getInstance(IkasanComponentType.UNKNOWN, newFlow, flowElementName, flowElementDescription);
                }

                // This is not the right long term class but will do for now.
                ikasanFlowComponent.getViewHandler().setClassToNavigateTo(moduleConfigClass);
                ikasanFlowComponent.getViewHandler().setOffsetInclassToNavigateTo(
                        pipsiMethod.getMethodDeclaration() != null ? pipsiMethod.getMethodDeclaration().getStartOffsetInParent() : 0);
                newFlow.addFlowComponent(ikasanFlowComponent);
            }
        }
        newFlow = addInputOutputForFlow(newFlow);
        ikasanModule.addFlow(newFlow);
    }


    // componentFactory.getXmlToObjectConverter()....
    // jmsProducer -> Producer jmsProducer = builderFactory.getComponentBuilder().jmsProducer()

    /**
     * Get all the methods (settings) applied to a specific ikasan component
     * @param expressionToBeSearched, this is usually the second parameter of the flows Builder's consumer or converter methods
     *  e.g.
     *      .consumer("JMS Consumer", jmsConsumer)
     *      .converter("XML to Person", componentFactory.getXmlToObjectConverter())
     *
     * @return
     */
    protected PIPSIMethodList getComponentBuilderMethods(PsiExpression expressionToBeSearched) {
        PIPSIMethodList returnPIPSIMethodList = null;
        if (expressionToBeSearched == null) {
            return returnPIPSIMethodList;
        } else {
            if (expressionToBeSearched instanceof PsiReferenceExpression) {
                // a reference AKA a local variable
                // e.g. jmsConsumer resolves to ... Consumer jmsConsumer = builderFactory.getComponentBuilder().jmsConsumer().setConnectionFactory(consumerConnectionFactory)
                PsiElement resolvedReference = ((PsiReferenceExpression) expressionToBeSearched).resolve();
                if (resolvedReference != null) {
                    PIPSIMethodList expressionRHS = extractMethodCallsFromChain(resolvedReference.getChildren(), new PIPSIMethodList());
                    if (containsConponentBuilder(expressionRHS)) {
                        returnPIPSIMethodList = expressionRHS;
                        // TODO, maybe need to continue, case style
                    } else {
                        log.error("getComponentBuilderMethods(1) failed for " + expressionToBeSearched.getText());
                    }
                } else {
                    log.error("getComponentBuilderMethods(2) failed to resolve local varaible " + expressionToBeSearched.getText());
                }
            } else if (expressionToBeSearched instanceof PsiMethodCallExpression) {
                // A call to a method that may eventually contain a reference to a component builder e.g.
                // componentFactory.getXmlToObjectConverter()
                PIPSIMethodList expressionRHS =  extractMethodCallsFromChain(expressionToBeSearched, new PIPSIMethodList());
                // The method chain directly calls componentBuilder
                if (containsConponentBuilder(expressionRHS)) {
                    returnPIPSIMethodList = expressionRHS;
                } else {
                    // The method chain calls a factory method e.g. componentFactory.getDBConsumer()
                    // We get the class of the factory e.g. ComponentFactory, then look up the getter on that factory
                    String factoryType = expressionRHS.baseMethodinstanceVariable.getType().getCanonicalText(true);
                    PsiClass factoryClass = StudioPsiUtils.findFirstClass(getProject(), factoryType);
                    String factoryClassGetterMethodName = expressionRHS.getMethodByIndex(0).getName();
                    PsiMethod[] factoryClassGetterMethods = factoryClass.findMethodsByName(factoryClassGetterMethodName, false);
                    // Assume its the first, a bit knackered if override, if ever get, need to use signature match.
                    JvmMethod factoryClassGetterMethod = factoryClassGetterMethods[0];
                   // Hopefully the method that creates the component
                    PsiStatement getterReturnStatement = getReturnStatementFromMethod((PsiMethod)factoryClassGetterMethod);
                    //// return statement
                    // The returnReference will contain our type of component .e.g Filter, Producer etc
                    PsiReferenceExpressionImpl returnReference = getLocalVariableFromReturnStatement(getterReturnStatement);
// XXXX
                    String getterReturnType = ((PsiType)factoryClassGetterMethod.getReturnType()).getCanonicalText();
                    String ikasanComponentType = null;
                    if (IkasanComponentCategory.isIkasanComponent(getterReturnType)) {
                        ikasanComponentType = getterReturnType;
                    }

                    if (returnReference != null) {
                        // the method returns a variable, that local variable is the component
                        PsiElement componentVariable = returnReference.resolve();
                        if (componentVariable != null) {
                            PIPSIMethodList  pipsiMethodList = new PIPSIMethodList();
                            pipsiMethodList.setBaseType(ikasanComponentType);
                            if (componentVariable instanceof PsiField &&
                                    ((PsiField) componentVariable).getSourceElement().getText().contains("@Resource")) {
                                // this is an injected bespoke class
                                String beskpokeClassName = ((PsiField) componentVariable).getType().getCanonicalText();
                                List<PIPSIMethod> additionalParameters = extractParametersFromBespokeIkasanComponent(beskpokeClassName);
                                pipsiMethodList.addAllPIPSIMethod(additionalParameters);

                            } else {
                                PsiCodeBlock containingBlock = getContainingCodeBlock(componentVariable);
                                if (containingBlock != null && componentVariable instanceof PsiLocalVariable) {
                                    PIPSIMethodList additionalMethodList = getAllMethodCallsForLocalVariableInCodeBlock(containingBlock, (PsiLocalVariable)componentVariable);
                                    pipsiMethodList.addAllPIPSIMethod(additionalMethodList.getPipsiMethods());
                                }
                            }

                            returnPIPSIMethodList =  pipsiMethodList;
                        } else {
                            // TODO ... otherwise ?
                            log.error("getComponentBuilderMethods(3) failed for " + expressionToBeSearched.getText());
                        }
                    } else {
                        // the return statement itself created the component e.g.
                        // return builderFactory.getComponentBuilder().scheduledConsumer().build()
                        PIPSIMethodList moduleMethodList = extractMethodCallsFromChain(getterReturnStatement.getChildren(), new PIPSIMethodList());
                        returnPIPSIMethodList =  moduleMethodList;
                    }
                }
            }
        }
        return returnPIPSIMethodList;
    }

    /**
     * We have resolved to a bespoke class that is implementing an Ikasan Component interface gete Converter<String, String>
     * This method will extract the appropriate custom properties for this special class type.
     * @param beskpokeClassName to be explored
     * @return A list of fake IkasanMethods that emulate the fluid interface set methods.
     */
    private List<PIPSIMethod> extractParametersFromBespokeIkasanComponent(String beskpokeClassName) {
        List<PIPSIMethod> additionalParameters = new ArrayList<>();
        // The Bespoke Ikasan Class
        PsiClass psiClass = StudioPsiUtils.findFirstClass(getProject(), beskpokeClassName);
        PIPSIMethod bespokeClassParam = createFakePIPSIMethod("set" + IkasanComponentPropertyMeta.BESPOKE_CLASS_NAME, psiClass.getMethods()[0], StudioUtils.getLastToken("\\.", beskpokeClassName));
        additionalParameters.add(bespokeClassParam);
        PsiClassType[] psiClassTypes = psiClass.getImplementsList().getReferencedTypes();

        if (psiClassTypes != null) {
            for (PsiClassType type : psiClassTypes) {
                PsiClass resolvedType = type.resolve();
                if (resolvedType != null) {
                    IkasanComponentCategory ikasanComponentCategory = IkasanComponentCategory.parseBaseClass(resolvedType.getQualifiedName());
                    if (ikasanComponentCategory == IkasanComponentCategory.CONVERTER) {
                        PsiType[] templateTypes = type.getParameters();
                        if (templateTypes.length > 0) {
                            PIPSIMethod fromType = createFakePIPSIMethod("set" + IkasanComponentPropertyMeta.FROM_TYPE, psiClass.getMethods()[0], templateTypes[0].getCanonicalText());
                            additionalParameters.add(fromType);
                        }
                        if (templateTypes.length > 1) {
                            PIPSIMethod toType = createFakePIPSIMethod("set" + IkasanComponentPropertyMeta.TO_TYPE, psiClass.getMethods()[0], templateTypes[1].getCanonicalText());
                            additionalParameters.add(toType);
                        }
                    }
                }
            }
        }
        return additionalParameters;
    }

    private PIPSIMethod createFakePIPSIMethod(String methodName, PsiMethod jumpToMethod, String literalValue) {
        PsiLiteralExpression stringLiteral = (PsiLiteralExpression) javaPsiFactory.createExpressionFromText("\""+literalValue+"\"", null);
        return new PIPSIMethod(methodName, jumpToMethod, new PsiExpression[] {stringLiteral});
    }

    /**
     * Given a local variable and the code block that contains it, go from the variable declaration and all statements after it,
     * gathering any method call either from the first declaration or subsequent method calls against the variable.
     * @param container code block that contains the local variable
     * @param localVaraible that we need to gather information about
     * @return the methods executed against the local variable.
     */
    protected PIPSIMethodList getAllMethodCallsForLocalVariableInCodeBlock(PsiCodeBlock container, PsiLocalVariable localVaraible) {
        PIPSIMethodList  pipsiMethodList = new PIPSIMethodList();
        boolean skip = true;
        String variableName = localVaraible.getName();
        String setterSignature = variableName+".set";
        for (PsiElement psiElement : container.getStatements()) {
            // keep skipping until the declaration of the local variable
            if (skip && psiElement instanceof PsiDeclarationStatement && psiElement.getFirstChild() == localVaraible) {
                skip = false;
                // e.g. MyFilter - not sure if this is needed yet, keep for now.
                PsiClassReferenceType type = (PsiClassReferenceType) ((PsiLocalVariable) psiElement.getFirstChild()).getType();
                PsiType[] superClasses = ((PsiLocalVariable) psiElement.getFirstChild()).getType().getSuperTypes();
                continue; // mybe we still need to check if this is A NEW statements or a chained method !
            }
            if (skip) {
                continue;
            } else if (psiElement.getText().contains(setterSignature)) {
                PIPSIMethodList  partialMethodList = extractMethodCallsFromChain(psiElement.getChildren(), new PIPSIMethodList());
                pipsiMethodList.addAllPIPSIMethod(partialMethodList.getPipsiMethods());
            }
        }
        return pipsiMethodList;
    }

    /**
     * Given a pisElement e.g. a local variable, return the code block that contains it.
     * @param psiElement of interest
     * @return the PSiCodeBlock that contains the psiElement
     */
    protected PsiCodeBlock getContainingCodeBlock(PsiElement psiElement) {
        PsiElement currentElement = psiElement;
        while (currentElement != null && !(currentElement instanceof PsiCodeBlock)) {
            currentElement = currentElement.getParent();
        }
        return (PsiCodeBlock)currentElement;
    }


    /**
     * Look through all the chained methods for 'getComponentBuilder', return true if it is found
     * @param expressionRHS to be searched
     * @return true if 'getComponentBuilder' is anywhere in the chained method calls.
     */
    protected boolean containsConponentBuilder(PIPSIMethodList expressionRHS) {
        boolean contains = false;

        PsiReferenceExpression expressionStart = expressionRHS.baseMethodinstanceVariable;
        IkasanClazz expressionStartType = getTypeOfReferenceExpression(expressionStart);
        if (IkasanClazz.COMPONENT_BUILDER.equals(expressionStartType)) {
            contains = true;
        }
        for (PIPSIMethod componentBuilderMethod: expressionRHS.getPipsiMethods()) {
            String methodName = componentBuilderMethod.getName();
            if (methodName.equals("getComponentBuilder")) {
                contains = true ;
                break;
            }
        }
        return contains;
    }

    IkasanClazz getTypeOfReferenceExpression(PsiReferenceExpression firstRHSToken) {
        IkasanClazz ikasanClazz = IkasanClazz.UNKNOWN;
        if (firstRHSToken == null) {
            log.warn("getTypeOfReferenceExpression called with null firstRHSToken");
        } else {
            PsiElement methodOrVariable = firstRHSToken.resolve();
            if (methodOrVariable instanceof PsiVariable) {
                ikasanClazz = IkasanClazz.parseClassType(StudioPsiUtils.getTypeOfVariable((PsiVariable) methodOrVariable));  // @todo mabe also inspec annotation for @Resource
            } else if (methodOrVariable instanceof PsiMethod) {
                ikasanClazz = IkasanClazz.LOCAL_METHOD;
            }
        }
        return ikasanClazz;
    }



    enum IkasanClazz {
        MODULE_BUILDER("ModuleBuilder", "org.ikasan.builder.ModuleBuilder"),
        FLOW_BUILDER("FlowBuilder", "org.ikasan.builder.FlowBuilder"),
        COMPONENT_BUILDER("ComponentBuilder", "org.ikasan.builder.component.ComponentBuilder"),
        LOCAL_METHOD("", ""),
        BESKPOKE_CLASS("", ""),
        UNKNOWN("", "");

        public final String clazzName;
        public final String fullClazzName;

        IkasanClazz(String clazzName, String fullClazzName) {
            this.clazzName = clazzName;
            this.fullClazzName = fullClazzName;
        }

        public static IkasanClazz parseClassType(String classType) {
            for (IkasanClazz ikasaClazz : IkasanClazz.values()) {
                if (ikasaClazz.clazzName.equals(classType) || ikasaClazz.fullClazzName.equals(classType)) {
                    return ikasaClazz;
                }
            }
            return BESKPOKE_CLASS;
        }
    }

    /**
     * Given we have built up all the flow, deduce the input and output end point for the flow.
     * @param ikasanFlow that holds all the flow elements
     */
    protected IkasanFlow addInputOutputForFlow(final IkasanFlow ikasanFlow) {
        List<IkasanFlowComponent> flowElements = ikasanFlow.getFlowComponentList();
        if (!flowElements.isEmpty()) {
            IkasanFlowComponent input = IkasanComponentType.getEndpointForFlowElement(flowElements.get(0), ikasanFlow);
            if (input != null) {
                ikasanFlow.setInput(input);
            }
            IkasanFlowComponent output = IkasanComponentType.getEndpointForFlowElement(flowElements.get(flowElements.size()-1), ikasanFlow);
            if (output != null) {
                ikasanFlow.setOutput(output);
            }
        }
        return ikasanFlow;
    }

    /**
     * Examine the provided pipsiMethod parameter to determine if it is a Spring @Value, if so, return the
     * ${key}, otherwise return null
     * @param pipsiMethod to examine
     * @return either a string representing the Spring @Value key, or null if not applicable
     */
    private String getSpringValueKey(PIPSIMethod pipsiMethod, int parameter) {
        String springValueKey = null;
        PsiExpression psiExpression = pipsiMethod.getParameter(0);
        if (psiExpression instanceof PsiReferenceExpression) {
            PsiElement springVariable = ((PsiReferenceExpression)psiExpression).resolve();
            if (springVariable instanceof  PsiField) {
                PsiAnnotation[] annotations = ((PsiField)springVariable).getAnnotations();
                if (annotations.length > 0) {
                    List annotationAttributes = annotations[0].getAttributes();
                    if (annotationAttributes != null && ! annotationAttributes.isEmpty()) {
                        springValueKey = ((PsiNameValuePair)annotationAttributes.get(0)).getLiteralValue();
                        if (springValueKey!=null && springValueKey.contains("${")) {
                            springValueKey = springValueKey.replace("$", "")
                                    .replace("{","")
                                    .replace("}","");
                        }
                    }

                }
            }
        }
        return springValueKey;
    }

    /**
     * create a new IkasanFlowComponent including any properties that may have been set.
     * @param parent flow for this element
     * @param name of the element
     * @param description for the element
     * @param componentBuilderMethodList e.g. .consumer("Ftp Consumer", componentBuilder.ftpConsumer()
     *      *                     .setCronExpression(ftpConsumerCronExpression)
     *      *                     .setClientID(ftpConsumerClientID)
     *      *                     ...
     *      *                         .build()
     * @return new new IkasanFlowComponent
     */
    protected IkasanFlowComponent createFlowElementWithProperties(final IkasanFlow parent, final String name, final String description, final PIPSIMethodList componentBuilderMethodList) {
        IkasanFlowComponent ikasanFlowComponent = null;
        Map<String, Object> flowElementProperties = new TreeMap<>();
        for (PIPSIMethod pipsiMethod: componentBuilderMethodList.getPipsiMethods()) {
            String methodName = pipsiMethod.getName();
            if  ("build".equals(methodName) || COMPONENT_BUILDER_NAME_METHOD.equals(methodName)) {
                // Ignore for now
            }
            else if (methodName.startsWith("set")) {
                String parameter =  getReferenceOrLiteral(pipsiMethod,0);
//                String propertyName = methodName.replaceFirst("set", "");
////                //@todo we should be able to get the type as well if we need to
//                String springValueKey = getSpringValueKey(componentBuilderMethod, 0);
//                String parameter ;
//                if (springValueKey != null) {
//                    Properties properties = Context.getApplicationProperties(projectKey);
//                    parameter = properties.getProperty(springValueKey);
//                } else {
//                    // A standard literal have been provided.
//                    parameter = componentBuilderMethod.getLiteralParameterAsString(0, true);
//                }

                // Only expect 1 param for the setter
                flowElementProperties.put(methodName.replaceFirst("set", ""), parameter);
            } else {
                // Must be the component type
                ikasanFlowComponent = IkasanFlowComponent.getInstance(IkasanComponentType.parseMethodName(methodName), parent, name, description);
//                ikasanFlowComponent.setTypeAndViewHandler(IkasanComponentType.parseMethodName(methodName));
            }
        }

        // In this case, the object was created before the setter chaining
        if (ikasanFlowComponent == null && componentBuilderMethodList.getBaseType() != null) {
            ikasanFlowComponent = IkasanFlowComponent.getInstance(IkasanComponentType.parseCategoryType(componentBuilderMethodList.getBaseType()), parent, name, description);
        }

        if (ikasanFlowComponent != null && ! flowElementProperties.isEmpty()) {
            // Now we know the component, we can match the properties to the known properties for that component
            IkasanComponentType componentType = ikasanFlowComponent.getType();
            for (Map.Entry<String, Object> entry : flowElementProperties.entrySet()) {
                IkasanComponentPropertyMeta metaData = componentType.getMetaDataForPropertyName(entry.getKey());
                if (metaData != null) {
                    ikasanFlowComponent.getProperties().put(entry.getKey(), new IkasanComponentProperty(metaData, entry.getValue()));
                } else {
                    ikasanFlowComponent.getProperties().put(entry.getKey(),
                        new IkasanComponentProperty(IkasanComponentPropertyMeta.getUnknownComponentMeta(entry.getKey()), entry.getValue()));
                }
            }
        }
        return ikasanFlowComponent;
    }

    /**
     * This is typically a local method that contains the flow defintion e.g.
     *
     * public Flow getFtpConsumerFlow(ModuleBuilder moduleBuilder, ComponentBuilder componentBuilder)
     *
     * @param myMethod
     * @return
     */
    protected PsiLocalVariable getFlowLocalVariableFromBespokeGetterMethod(final PsiMethod myMethod) {
        PsiLocalVariable flowLocalVariable = null;
        PsiReferenceExpression flowGetterReturnReference = getlocalVariableFromReturnReferenceForPSIMethod(myMethod);

        if (flowGetterReturnReference != null && flowGetterReturnReference.resolve() != null) {
            flowLocalVariable = (PsiLocalVariable)flowGetterReturnReference.resolve();
        }
        return flowLocalVariable;
    }

    /**
     * The return statement for the supplied method will contain a reference to the returned value e.g. 'return ftpToJmsFlow;' is 'ftpToJmsFlow'
     * @param myMethod we are looking through e.g. the getFtpConsumerFlow method
     * @return the pointer to the returned value.
     */
    protected PsiReferenceExpressionImpl getlocalVariableFromReturnReferenceForPSIMethod(final PsiMethod myMethod) {
        PsiStatement psiReturnStatement = getReturnStatementFromMethod(myMethod);
        return getLocalVariableFromReturnStatement(psiReturnStatement);
    }

    /**
     * The return statement for the supplied method will contain a reference to the returned value e.g. 'return ftpToJmsFlow;' is 'ftpToJmsFlow'
     * @param psiReturnStatement we are looking through e.g. the getFtpConsumerFlow method
     * @return the pointer to the returned value.
     */
    protected PsiReferenceExpressionImpl getLocalVariableFromReturnStatement(final PsiStatement psiReturnStatement) {
        PsiReferenceExpressionImpl returnReference = null;
        try {
            returnReference = (PsiReferenceExpressionImpl) Arrays.stream(psiReturnStatement.getChildren())
                    .filter(item -> item instanceof PsiReferenceExpressionImpl)
                    .findFirst()
                    .orElse(null);
        } catch (NoSuchElementException nsee) {
            log.warn("Could not find return statement reference from " + psiReturnStatement + ". Exception: " + nsee.getMessage(), nsee);
        }
        return returnReference;
    }

    protected PsiStatement getReturnStatementFromMethod(final PsiMethod flowMethod) {
        PsiStatement psiReturnStatement = null;
        if (flowMethod != null && flowMethod.getBody() != null) {
            PsiCodeBlock flowMethodCodeBlock = flowMethod.getBody();
            PsiStatement[] psiStatement = flowMethodCodeBlock.getStatements();
            if (psiStatement.length > 0) {
                psiReturnStatement = psiStatement[psiStatement.length - 1];
            }
        }
        return psiReturnStatement;
    }


    protected PIPSIMethodList extractMethodCallsFromChain(final PsiElement psiElement, final PIPSIMethodList pipsiMethodList) {
        PsiElement[] psiElements = { psiElement} ;
        return extractMethodCallsFromChain(psiElements, pipsiMethodList);
    }
    /**
     * Recursive - Given a statement consisting of chained method calls, extract all the method calls, remember this works BACKWARD from the last
     * element to the first
     * @param children the children at the current level
     * @param pipsiMethodList This list of methods being built up. e.g.
     *   builderFactory.getComponentBuilder().jmsProducer()
     *                 .setConfiguredResourceId(jmsProducerConfiguredResourceId)
     *                 .setDestinationJndiName("jms.topic.test")
     *                 .setConnectionFactory(connectionFactory)
     * @return the growing list of methods that are invoked from within this statement, technically we dont need the
     * return, since the passed in List is same one that is returned.
     */
    protected PIPSIMethodList extractMethodCallsFromChain(final PsiElement[] children, final PIPSIMethodList pipsiMethodList) {
        boolean isSpringBean = false;
        if (children != null && children.length > 0) {
            for (PsiElement subelement : children) {
//                if (subelement instanceof PsiNewExpression) {
//                    PsiJavaCodeReferenceElement xx = (PsiJavaCodeReferenceElement) Arrays.stream(subelement.getChildren()).filter(x -> x instanceof PsiJavaCodeReferenceElement).findFirst().orElse(null);
//                    PsiNewExpression xxx = (PsiNewExpression) subelement;
////                    pipsiMethodList.setBaseMethodinstanceVariable(((PsiJavaCodeReferenceElement) Arrays.stream(subelement.getChildren()).filter(x -> x instanceof PsiJavaCodeReferenceElement).findFirst().orElse(null)).getReference());
//// maybe use the iterator here.
//                } else

                if (subelement instanceof PsiReferenceExpression) {
                    PsiElement[] subChildren = subelement.getChildren();
                    if (subChildren.length <= 2) {
                        // Typically the second method in the chain is the one after getComponentBuilder() so will be e.g. jmsProducer()
                        pipsiMethodList.setBaseMethodinstanceVariable((PsiReferenceExpression)subelement);
                    }
                    extractMethodCallsFromChain(subChildren, pipsiMethodList);
                } else if (subelement instanceof PsiMethodCallExpression) {
                    extractMethodCallsFromChain(subelement.getChildren(), pipsiMethodList);
                    PIPSIMethod pipsiMethod = getPIPSIMethod((PsiMethodCallExpression)subelement);
                    pipsiMethodList.addPIPSIMethod(pipsiMethod);
                } else if (subelement instanceof PsiModifierList &&
                        (subelement.getText().startsWith("@Resource") || subelement.getText().startsWith("@Bean") || subelement.getText().startsWith("@Autowired"))) {
                    isSpringBean = true;
                    pipsiMethodList.setBaseType(PIPSIMethodList.SPRING_BEAN);
//                } else if (isSpringBean  && subelement instanceof PsiIdentifier) {
//                    pipsiMethodList.setBaseMethodinstanceVariable((PsiReferenceExpression)subelement);
                }
            }
        }
        return pipsiMethodList;
    }



    protected PsiIdentifier getFirstIdentifierFromExpression(final PsiReferenceExpression referenceExpression) {
        if (referenceExpression != null) {
            return (PsiIdentifier) Arrays.stream(referenceExpression.getChildren())
                    .filter(item -> item instanceof PsiIdentifier)
                    .findFirst()
                    .orElse(null);
        } else {
            return null;
        }
    }

    /**
     * Extract the PSIMethod from the PsiMethodCallExpression e.g. extract
     * PMethod{name='getModuleBuilder', parameters=PsiExpressionList}
     * from
     * PsiMethodCallExpression:builderFactory.getModuleBuilder("fms-ftp")
     * @param psiMethodCallExpression is the PsiMethodCallExpression that contains the PSIMethod
     * @return the enclosed PSIMethod NOTE - this may be null if Intellij has not build its internal model
     */
    protected PIPSIMethod getPIPSIMethod(final PsiMethodCallExpression psiMethodCallExpression) {
        PIPSIMethod pipsiMethod = null;
//        PsiReferenceExpression referenceExpression = psiMethodCallExpression.getMethodExpression();
//        PsiIdentifier methodName = (PsiIdentifier) Arrays.stream(referenceExpression.getChildren()).filter(item -> item instanceof PsiIdentifier).findFirst().orElse(null);
        PsiIdentifier methodName = getFirstIdentifierFromExpression(psiMethodCallExpression.getMethodExpression());
        if (methodName != null) {
            pipsiMethod = new PIPSIMethod(methodName.getText());

        }
        if ( methodName != null && methodName.getParent() != null && methodName.getParent().getReference() != null) {
            PsiReference methodParent = methodName.getParent().getReference();
            // argumentList is ("fms-ftp")
            PsiExpression[] argumentList = psiMethodCallExpression.getArgumentList().getExpressions();
            // in theory, at this point, getFtpConsumerFlow should have a reference which we can use to resolve, but the
            // unit test does not use compile code so it might be that this does not resolve.
            // since it is written that "it is not always successful. If the code currently open in the IDE does not compile,
            // or in other situations, it’s normal for PsiReference.resolve() to return null - all code working with references
            // must be prepared to handle that."
            pipsiMethod = new PIPSIMethod(methodName.getText(), (PsiMethod) methodParent.resolve(), argumentList);
        }
        return pipsiMethod;
    }

    //--------------------------------------



    protected PsiElement getFirstInstanceOfType(PsiStatement statements, Class searchForType) {
        return Arrays.stream(statements.getChildren())
                .filter(x -> searchForType.isInstance(x))
                .findFirst().orElse(null);
    }
    /**
     * Given the list of arguments, extract the specified argument value.
     * @param methodArguments the full list of arguments
     * @param argumentNumber the index of the desired argument (starting at 0)
     * @param stripQuotes if true, will strip any quote marks from around the text argument
     * @return the extracted argument
     */
    protected String getArgumentValueFromArgumentList(final PsiExpression[] methodArguments, final int argumentNumber, final boolean stripQuotes) {
        String argumentValue = "";
        if (methodArguments != null && methodArguments.length > argumentNumber) {
            argumentValue = methodArguments[argumentNumber].getText();
            if (stripQuotes && argumentValue!= null) {
                argumentValue = argumentValue.replace("\"", "");
            }
        }
        return argumentValue;
    }

    protected PsiIdentifier getFirstIdentifierFromElements(final PsiElement[] psiElements) {
        if (psiElements != null) {
            return (PsiIdentifier) Arrays.stream(psiElements)
                    .filter(item -> item instanceof PsiIdentifier)
                    .findFirst()
                    .orElse(null);
        } else {
            return null;
        }
    }

    protected PsiMethodCallExpression getFirstMethodCallExpressionFromElements(final PsiElement[] psiElements) {
        if (psiElements != null) {
            return (PsiMethodCallExpression) Arrays.stream(psiElements)
                    .filter(item -> item instanceof PsiMethodCallExpressionImpl)
                    .findFirst()
                    .orElse(null);
        } else {
            return null;
        }
    }


    /**
     * The module builder holds the module name and the variable which is the root of all elements of the module
     * @param methodList the list of methods invoked by the module builder e.g. getModuleBuilder("ftp-jms-im");
     */
    protected IkasanModule extractModuleName(final PIPSIMethodList methodList) {
        PIPSIMethod moduleBuilderMethod = methodList.getFirstMethodByName(MODULE_BUILDER_SET_NAME_METHOD);
//        ikasanModule.setName(moduleBuilderMethod.getLiteralParameterAsString(0, true));
        ikasanModule.setName(getReferenceOrLiteral(moduleBuilderMethod, 0));
        return  ikasanModule;
    }
//    /**
//     * The public entry point, builds the Pseudo ikasan Module used by the plugin.
//     * @param moduleConfigPsiFile the java file containing the module declaration.
//     * @return A populated ikasan Module used by the plugin.
//     */
//    // OLD way
//    public IkasanModule buildIkasanModulex(final PsiFile moduleConfigPsiFile) {
//        log.debug("Extracting ikasan model from file " + moduleConfigPsiFile.getText());
//        PsiClass moduleConfigClass = getClassFromPsiFile(moduleConfigPsiFile);
//        // Visit every method statement in the whole file ...
//        moduleConfigPsiFile.accept(new JavaRecursiveElementVisitor() {
//            @Override
//            // the visitor is called by file and will pass in PsiDeclarationStatement
//            // General form visitXX(XX variable)
//            public void visitDeclarationStatement(PsiDeclarationStatement variable) {
//                super.visitDeclarationStatement(variable);
//                for (PsiElement element : variable.getDeclaredElements()) {
//                    if (element instanceof PsiLocalVariable) {
//                        PsiLocalVariable psiLocalVariable = (PsiLocalVariable)element;
//
//                        log.debug("Element was " + element + " type was " + psiLocalVariable.getType());
//
//                        // ModuleBuilder mb = builderFactory.getModuleBuilder("fms-ftp");
//                        if (StudioUtils.getLastToken("\\.", psiLocalVariable.getType().getCanonicalText()).equals(DEFAULT_MODULE_BUILDER_TYPE)) {
//                            PIPSIMethodList moduleBuilderMethodList = extractMethodCalls(psiLocalVariable.getChildren(), new PIPSIMethodList());
//                            extractModuleName(moduleBuilderMethodList);
//                        }
//
//                        // Module module = mb.withDescription("Ftp Jms Sample Module").addFlow(ftpToJmsFlow).addFlow(jmsToFtpFlow).build();
//                        if (StudioUtils.getLastToken("\\.", psiLocalVariable.getType().getCanonicalText()).equals(DEFAULT_MODULE_TYPE)) {
//                            PIPSIMethodList moduleMethodList = extractMethodCalls(psiLocalVariable.getChildren(), new PIPSIMethodList());
//                            parseModuleStatement(moduleMethodList, moduleConfigClass);
//                        }
//
//                    }
//                }
//            }
//        });
//
//        return iIkasanModule;
//    }
}
