package org.ikasan.studio.model.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.ikasan.instance.Module;

/**
 * Encapsulates the Intellij representation of the ikasan Module
 * The idea is to keep the ikasan Module clean of any Initellij specific details, this module will inpect the
 * code to generate the ikasan Module and update the code to reflect changes to the ikasan Module.
 */
public class PIPSIIkasanModelx {
    public static final String OLD_MODULE_BEAN_CLASS = "org.ikasan.spec.module.Module";
    private static final String WITH_DESCRIPTION_METHOD_NAME = "withDescription";
    private static final String ADD_FLOW_METHOD_NAME = "addFlow";
    private static final String MODULE_BUILDER_SET_NAME_METHOD = "getModuleBuilder";
    private static final String FLOW_BUILDER_NAME_METHOD = "getFlowBuilder";
    private static final String COMPONENT_BUILDER_NAME_METHOD = "getComponentBuilder";
    private static final String EXCEPTION_RESOLVER_NAME_METHOD = "getExceptionResolverBuilder";
    private static final String ADD_EXCEPTION_TO_ACTION = "addExceptionToAction";

    private final PsiElementFactory javaPsiFactory;
    private final String projectKey ;
    private final Module ikasanModule;
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
    public PIPSIIkasanModelx(final String projectKey) {
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
     * An update has been made to the diagram, so we need to reverse this into the code.
     */
//    public void generateSourceFromModel() {
//        generateSourceFromModel(null);
//    }
//
//    public void generateSourceFromModel(Map<String, Dependency> newDependencies) {
//        Project project = Context.getProject(projectKey);
//            CommandProcessor.getInstance().executeCommand(
//                project,
//                () -> ApplicationManager.getApplication().runWriteAction(
//                    () -> {
//                        StudioPsiUtils.pomAddDependancies(projectKey, newDependencies);
//                        //@todo start making below conditional on state changed.
//                        ModelTemplate.create(project);
//                        ApplicationTemplate.create(project);
//                        FlowTemplate.create(project);
//                        ModuleConfigTemplate.create(project);
//                        PropertiesTemplate.create(project);
//                    }),
//                    "Generate Source from Flow Diagram",
//                "Undo group ID");
//
//        ApplicationManager.getApplication().runReadAction(
//                () -> {
//                    Module ikasanModule = Context.getIkasanModule(project.getComponentName());
//                    moduleConfigClazz = ikasanModule.getViewHandler().getClassToNavigateTo();
//                    // reloadProject needed to re-read POM, must not be done till addDependancies
//                    // fully complete, hence in next executeCommand block
//                    if (newDependencies != null && !newDependencies.isEmpty() && Context.getOptions(projectKey).isAutoReloadMavenEnabled()) {
//                        ProjectManager.getInstance().reloadProject(project);
//                    }
//                });
//    }
//
//
//
//    protected PsiLocalVariable getModuleBuilderLocalVariable(PIPSIMethodList moduleMethodList) {
//        PsiLocalVariable moduleBuilderLocalVariable = null;
//        PsiReferenceExpression moduleBuiderReference = moduleMethodList.getBaseMethodinstanceVariable();
//        IkasanClazz ikasanClazz = getTypeOfReferenceExpression(moduleBuiderReference);
//
//        if (IkasanClazz.MODULE_BUILDER.equals(ikasanClazz)) {
//            // ModuleBuilder mb = builderFactory.getModuleBuilder("fms-ftp");
//            PsiElement potentialModuleBuilderLocalVariable = moduleBuiderReference.resolve();
//            if (potentialModuleBuilderLocalVariable instanceof PsiLocalVariable) {
//                moduleBuilderLocalVariable = (PsiLocalVariable)potentialModuleBuilderLocalVariable;
//            }
//
//        }
//        return moduleBuilderLocalVariable;
//    }
//
//    /**
//     * Process the ModuleBuilder expression
//     *
//     * @param moduleBuilderLocalVariable points to the module name and the variable which is the root of all elements of the module
//     * e.g. ModuleBuilder mb = builderFactory.getModuleBuilder("fms-ftp");
//     */
//    protected void updateIkasanModuleWithModuleBuilder(PsiLocalVariable moduleBuilderLocalVariable) {
//        PIPSIMethodList moduleBuilderMethodList = extractMethodCallsFromChain(moduleBuilderLocalVariable.getChildren(), new PIPSIMethodList());
//        PIPSIMethod moduleBuilderMethod = moduleBuilderMethodList.getFirstMethodByName(MODULE_BUILDER_SET_NAME_METHOD);
//        ikasanModule.setComponentName(getReferenceOrLiteralFromParameter(moduleBuilderMethod,0));
//        updateModuleWithProperties();
//    }
//
//    private void updateModuleWithProperties() {
//        Properties properties = Context.getApplicationProperties(projectKey);
//        ikasanModule.setApplicationPackageName((String)properties.get(IkasanComponentPropertyMeta.APPLICATION_PACKAGE_KEY));
//        ikasanModule.setPort((String)properties.get(IkasanComponentPropertyMeta.APPLICATION_PORT_NUMBER_KEY));
//        ikasanModule.setH2PortNumber((String)properties.get(IkasanComponentPropertyMeta.H2_PORT_NUMBER_KEY));
//    }
//
//
//    /**
//     * From the given pipsiMethod and attribute, determine if the attribute is a property, if so return the value
//     * otherwise assume the attribute was a literal and return that.
//     * @param pipsiMethod to interrogate
//     * @param parameterNumber to interrogate
//     * @return a String containing the Spring injected value or literal value
//     */
//    private String getReferenceOrLiteralFromParameter(PIPSIMethod pipsiMethod, int parameterNumber) {
//        //                //@todo we should be able to get the type as well if we need to
//        String parameter ;
//        String springValueKey = getSpringValueKey(pipsiMethod, parameterNumber);
//        if (springValueKey != null) {
//            Properties properties = Context.getApplicationProperties(projectKey);
//            parameter = properties.getProperty(springValueKey);
//        } else {
//            // A standard literal have been provided.
//            parameter = pipsiMethod.getLiteralParameterAsString(parameterNumber, true);
//        }
//        return parameter;
//    }
//
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
//    /**
//     * Given a Intellij virtual file, extract the virtual PSI class.
//     * @param psiFile to examine
//     * @return an Intellij 'Program Structure Interface' (PSI) virtual Class
//     */
//    protected PsiClass getClassFromPsiFile(final PsiFile psiFile) {
//        PsiClass returnClass = null;
//        if (psiFile instanceof  PsiJavaFile) {
//            PsiJavaFile jfile = (PsiJavaFile)psiFile;
//            PsiClass[] psiClasses = jfile.getClasses();
//            if (psiClasses.length > 0) {
//                returnClass = psiClasses[0];
//            }
//        }
//        return returnClass;
//    }
//
//    /**
//     * Parse the declaration of the module e.g.
//     * ... mb.withDescription("Ftp Jms Sample Module")
//     *                 .addFlow(ftpToJmsFlow).addFlow(jmsToFtpFlow).build();
//     * extract description then process each flow.
//     *
//     * @param methodList of the methods called by the builder e.g. withDescription, addFlow
//     */
//    protected void parseModuleStatementRHS(final PIPSIMethodList methodList, final PsiClass moduleConfigClass) {
//        // Set the module description
//        PIPSIMethod withDescriptionMethod = methodList.getFirstMethodByName(WITH_DESCRIPTION_METHOD_NAME);
//        ikasanModule.setDescription(withDescriptionMethod == null ? null :
//                getReferenceOrLiteralFromParameter(withDescriptionMethod, 0));
//
//        // Just in case we have it here also
//        //@todo what it parameter was a reference or spring reference- chase !
//        PIPSIMethod moduleBuilderMethod = methodList.getFirstMethodByName(MODULE_BUILDER_SET_NAME_METHOD);
//        if (moduleBuilderMethod != null) {
//            ikasanModule.setComponentName(getReferenceOrLiteralFromParameter(moduleBuilderMethod, 0));
//        }
//
//        // Expose the flows
//        List<PIPSIMethod> pipsiFlowMethods = methodList.getMethodsByName(ADD_FLOW_METHOD_NAME);
//        for (PIPSIMethod pipsiMethod :  pipsiFlowMethods) {
//            PsiLocalVariable flowLocalVariable = null;
//            PsiLocalVariable flowBuilderLocalVariable = null;
//            // addFlow(param)
//            PsiExpression addFlowParameter = pipsiMethod.getParameter(0);
//            PsiElement addFlowParameterResolved = null ;
//            PsiElement[] methodChainToCreateItem = null;
//            if (addFlowParameter instanceof  PsiReferenceExpression) {
//                addFlowParameterResolved = ((PsiReferenceExpression) addFlowParameter).resolve();  // e.g. local var ftpToJmsFlow   (pointing to Flow ftpToJmsFlow = getFtpConsumerFlow(mb,builderFactory.getComponentBuilder());
//                methodChainToCreateItem = addFlowParameterResolved.getChildren();
//            } else if (addFlowParameter instanceof  PsiMethodCallExpression) {
//                addFlowParameterResolved = addFlowParameter;
//                methodChainToCreateItem = new PsiElement[] { addFlowParameter } ;
//            } else {
//                ; //@todo
//            }
//            // Trace flow param back to definition,
//            //          .addFlow(sourceFlow) -> Flow sourceFlow = moduleBuilder.getFlowBuilder("dbToJMSFlow").with ...
//            //          .addFlow(jmsToFtpFlow) -> Flow ftpToJmsFlow = getFtpConsumerFlow(mb,builderFactory.getComponentBuilder())
//            //          .addFlow(get_NewFlow1(moduleBuilder, componentBuilder)) -> Flow get_NewFlow1(ModuleBuilder moduleBuilder, ComponentBuilder componentBuilder) {
//            //          .addFlow(newflow1.getNewflow1()) -> @Resource  Newflow1 newflow1;  .. newFlow1.getFlow
//
//            if (addFlowParameterResolved != null) {
//                PIPSIMethodList expressionRHS = extractMethodCallsFromChain(methodChainToCreateItem, new PIPSIMethodList());
//                // this could be another local method call, or it could be the flow definition.
//                PsiReferenceExpression expressionStart = expressionRHS.baseMethodinstanceVariable;
//                IkasanClazz expressionStartType = getTypeOfReferenceExpression(expressionStart);
//
//                if (expressionStartType.equals(IkasanClazz.MODULE_BUILDER)) {
//                    // Flow xx = moduleBuilder.getFlowBuilder()...
//                    flowLocalVariable = (PsiLocalVariable) addFlowParameterResolved;
//                    flowBuilderLocalVariable = (PsiLocalVariable) addFlowParameterResolved;
//                } else if (expressionStartType.equals(IkasanClazz.FLOW_BUILDER)) {
//                    // Flow flow = fb.withDescription("Flow demonstrates usage of JMS Concumer and JMS Producer")...
//                    flowLocalVariable = (PsiLocalVariable) addFlowParameterResolved;
//                    flowBuilderLocalVariable = (PsiLocalVariable) expressionStart.resolve();
//                } else if (expressionStartType.equals(IkasanClazz.LOCAL_METHOD)){
//                    flowLocalVariable = getFlowLocalVariableFromBespokeGetterMethod(expressionRHS.getMethodByIndex(0).getMethodDeclaration());
//                    flowBuilderLocalVariable = getFlowBuilderLocalVariableFromFlowExpression(flowLocalVariable);
//                } else if (expressionStartType.equals(IkasanClazz.BESKPOKE_CLASS)) {
//                    // Assume its a bespoke class and try to look it up, assume there is a class get'ClassName'
//                    PsiElement bespokeClassVariable = expressionStart.resolve();
//                    if (bespokeClassVariable instanceof PsiVariable) {
//                        String beskpokeClazzName = StudioPsiUtils.getTypeOfVariable((PsiVariable) bespokeClassVariable);
//                        PsiClass bespokeClazz = StudioPsiUtils.findFirstClass(getProject(), beskpokeClazzName);
//                        PsiMethod getFlowMethod = StudioPsiUtils.findMethodFromClassByReturnType(bespokeClazz, "org.ikasan.spec.flow.Flow");
//                        flowLocalVariable = getFlowLocalVariableFromBespokeGetterMethod(getFlowMethod);
//                        flowBuilderLocalVariable = getFlowBuilderLocalVariableFromFlowExpression(flowLocalVariable);
//                    }
//
//                } else {
//
//                    LOG.warn("Unable to parse methodList [" + methodList + "]");
//                }
//
//                if (flowBuilderLocalVariable != null || flowLocalVariable != null) {
//                    Flow newFlow = updateIkasanFlowWithFlowBuilder(flowBuilderLocalVariable);
//                    parseFlowMethod(flowLocalVariable, moduleConfigClass, newFlow);
//                }
//            }
//        }
//    }
//
//    protected String getParameterType(PsiReferenceExpression parameter) {
//        PsiElement parameterResolved = parameter.resolve();
//
//        if (parameterResolved instanceof PsiField && parameterResolved.getText().startsWith("@Resouce")) {
//            return "SpringResource";
//        } else {
//            return "Unknown";
//        }
//    }
//
//    protected Flow updateIkasanFlowWithFlowBuilder(final PsiLocalVariable flowBuilderLocalVar) {
//        Flow newFlow = new Flow();
//        if (flowBuilderLocalVar != null) {
//            PIPSIMethodList flowBuilderMethodCalls = extractMethodCallsFromChain(flowBuilderLocalVar.getChildren(), new PIPSIMethodList());
//            PIPSIMethod getFlowBuilderCall = flowBuilderMethodCalls.getFirstMethodByName(FLOW_BUILDER_NAME_METHOD);
////        String flowName = getFlowBuilderCall.getLiteralParameterAsString(0, true);
//            String flowName = getReferenceOrLiteralFromParameter(getFlowBuilderCall, 0);
//            newFlow.setComponentName(flowName);
//        }
//        return newFlow;
//    }
//
//
//    /**
//     * Get the flowBuilder method from the flow variable
//     * Flow ftpToJmsFlow = ftpToLogFlowBuilder.withDescription("Ftp to Jms")
//     * @param flowLocalVariable to be identified
//     * @return the flowBuilder method from the flow variable
//     */
//    protected PsiLocalVariable getFlowBuilderLocalVariableFromFlowExpression(final PsiElement flowLocalVariable) {
//        PsiLocalVariable flowBuilderLocalVariable = null;
//        if (flowLocalVariable != null) {
//            PIPSIMethodList expressionRHS = extractMethodCallsFromChain(flowLocalVariable.getChildren(), new PIPSIMethodList());
//            PsiReferenceExpression expressionStart = expressionRHS.baseMethodinstanceVariable;
//            IkasanClazz expressionStartType = getTypeOfReferenceExpression(expressionStart);
//            if (IkasanClazz.FLOW_BUILDER.equals(expressionStartType)) {
//                flowBuilderLocalVariable = (PsiLocalVariable)expressionStart.resolve();
//            }
//        }
//        return flowBuilderLocalVariable;
//    }
//
//    /**
//     * Parse the flow method e.g. getFtpConsumerFlow(..) { .. }. This works backwards from the return statement
//     * @param flowLocalVar pointing to Flow definition
//     * @param moduleConfigClass to be searched
//     */
//    protected void parseFlowMethod(final PsiLocalVariable flowLocalVar, final PsiClass moduleConfigClass, Flow newFlow) {
//
//        PIPSIMethodList flowMethodCalls = extractMethodCallsFromChain(flowLocalVar.getChildren(), new PIPSIMethodList());
//        for(PIPSIMethod pipsiMethod : flowMethodCalls.getPipsiMethods()) {
//
//            if (IkasanComponentCategory.DESCRIPTION.associatedMethodName.equals(pipsiMethod.getComponentName())) {
//                newFlow.setDescription(getReferenceOrLiteralFromParameter(pipsiMethod, 0));
//            }
//
//            IkasanComponentCategory componentCategory = IkasanComponentCategory.parseMethodName(pipsiMethod.getComponentName());
//            if (componentCategory == EXCEPTION_RESOLVER ||
//                componentCategory == BROKER ||
//                componentCategory == CONSUMER ||
//                componentCategory == CONVERTER ||
//                componentCategory == FILTER ||
//                componentCategory == PRODUCER ||
//                componentCategory == ROUTER ||
//                componentCategory == SPLITTER ||
//                componentCategory == TRANSLATER) {
//
//                String flowElementName = pipsiMethod.getLiteralParameterAsString(0, true);
//                String flowElementDescription = null;
//                FlowElement ikasanFlowComponent = null;
//
//                // componentCreator : usually the component, or componentFactory.getXX(), or new PayloadToMapConverter()
//                PsiExpression componentCreator = null;
//                if (componentCategory == EXCEPTION_RESOLVER) {
//                    componentCreator = pipsiMethod.getParameter(0);
//                } else {
//                    componentCreator = pipsiMethod.getParameter(1);
//                }
//
//                if (componentCreator != null) {
//                    // Simplest scenario e.g. new MapMessageToPayloadConverter()
//                    if (componentCreator instanceof PsiNewExpression) {
//                        PsiJavaCodeReferenceElement flowComponentConstructor = (PsiJavaCodeReferenceElement) Arrays.stream(componentCreator.getChildren()).filter(x -> x instanceof PsiJavaCodeReferenceElement).findFirst().orElse(null);
//                        if (flowComponentConstructor != null) {
//                            IkasanComponentType ikasanComponentType = IkasanComponentType.parseMethodName(flowComponentConstructor.getText());
//                            if (IkasanComponentType.UNKNOWN == ikasanComponentType) {
//                                // This is probably a bespoke component, try to deduce the component from the type
//                                String classInterface = null;
//                                try {
//                                    // @todo maybe cycle through and try all interfaces for an ikasan match in case clients play with multiple interfaces.
//                                    classInterface = ((PsiClass)((PsiJavaCodeReferenceElementImpl) flowComponentConstructor).resolve()).getImplementsList().getReferencedTypes()[0].getClassName();
//                                    ikasanComponentType = IkasanComponentType.parseMethodName(classInterface);
//                                } catch (NullPointerException npe) {
//                                    // Hate to catch NPE but until I can turn this into an element safe recursion, this will have to do.
//                                    LOG.warn("Attempt to get interface type for " + flowComponentConstructor.getText()+ " failed. Component type will be set as unknown");
//                                }
//                            }
//                            ikasanFlowComponent = FlowElement.createFlowElement(ikasanComponentType, newFlow, flowElementName, flowElementDescription);
//                        }
//                    } else {
//                    // More complex scenario, we need to traverse down the call stack until we find a component.
//                        PIPSIMethodList pipsiMethodList = getComponentBuilderMethods(componentCreator) ;
//                        ikasanFlowComponent = createFlowElementWithProperties(newFlow, flowElementName, flowElementDescription, pipsiMethodList);
//                    }
//                }
//                if (ikasanFlowComponent == null) {
//                    ikasanFlowComponent = FlowElement.createFlowElement(IkasanComponentType.UNKNOWN, newFlow, flowElementName, flowElementDescription);
//                }
//
//                // This is not the right long term class but will do for now.
//                ikasanFlowComponent.getViewHandler().setClassToNavigateTo(moduleConfigClass);
//                ikasanFlowComponent.getViewHandler().setOffsetInclassToNavigateTo(
//                        pipsiMethod.getMethodDeclaration() != null ? pipsiMethod.getMethodDeclaration().getStartOffsetInParent() : 0);
//                if (componentCategory == EXCEPTION_RESOLVER && ikasanFlowComponent instanceof IkasanExceptionResolver) {
//                    newFlow.setIkasanExceptionResolver((IkasanExceptionResolver)ikasanFlowComponent);
//                } else {
//                    newFlow.addFlowComponent(ikasanFlowComponent);
//                }
//            }
//        }
//        newFlow = addInputOutputForFlow(newFlow);
//        ikasanModule.addFlow(newFlow);
//    }
//
//    /**
//     * Get all the methods (settings) applied to a specific ikasan component
//     * @param expressionToBeSearched, this is usually the second parameter of the flows Builder's consumer or converter methods
//     *  e.g.
//     *      .consumer("JMS Consumer", jmsConsumer)
//     *      .converter("XML to Person", componentFactory.getXmlToObjectConverter())
//     *
//     * @return all the methods (settings) applied to a specific ikasan component
//     */
//    protected PIPSIMethodList getComponentBuilderMethods(PsiExpression expressionToBeSearched) {
//        PIPSIMethodList returnPIPSIMethodList = null;
//        if (expressionToBeSearched == null) {
//            return returnPIPSIMethodList;
//        } else {
//            if (expressionToBeSearched instanceof PsiReferenceExpression) {
//                // a reference AKA a local variable
//                // e.g. jmsConsumer resolves to ... Consumer jmsConsumer = builderFactory.getComponentBuilder().jmsConsumer().setConnectionFactory(consumerConnectionFactory)
//                PsiElement resolvedReference = ((PsiReferenceExpression) expressionToBeSearched).resolve();
//                if (resolvedReference != null) {
//                    PIPSIMethodList expressionRHS = extractMethodCallsFromChain(resolvedReference.getChildren(), new PIPSIMethodList());
//                    if (containsComponentBuilder(expressionRHS)) {
//                        returnPIPSIMethodList = expressionRHS;
//                        // TODO, maybe need to continue, case style
//                    } else {
//                        LOG.warn("getComponentBuilderMethods(1) failed for " + expressionToBeSearched.getText());
//                    }
//                } else {
//                    LOG.warn("getComponentBuilderMethods(2) failed to resolve local varaible " + expressionToBeSearched.getText());
//                }
//            } else if (expressionToBeSearched instanceof PsiMethodCallExpression) {
//                // A call to a method that may eventually contain a reference to a component builder e.g.
//                // componentFactory.getXmlToObjectConverter()
//                PIPSIMethodList expressionRHS =  extractMethodCallsFromChain(expressionToBeSearched, new PIPSIMethodList());
//                // The method chain directly calls componentBuilder
//                if (containsComponentBuilder(expressionRHS)) {
//                    returnPIPSIMethodList = expressionRHS;
//                } else {
//                    // The method chain calls a factory method e.g. componentFactory.getDBConsumer()
//                    // We get the class of the factory e.g. ComponentFactory, then look up the getter on that factory
//                    String factoryType = expressionRHS.baseMethodinstanceVariable.getType().getCanonicalText(true);
//                    PsiClass factoryClass = StudioPsiUtils.findFirstClass(getProject(), factoryType);
//                    String factoryClassGetterMethodName = expressionRHS.getMethodByIndex(0).getComponentName();
//                    if (factoryClass != null) {
//                        PsiMethod[] factoryClassGetterMethods = factoryClass.findMethodsByName(factoryClassGetterMethodName, false);
//                        // Assume its the first, a bit knackered if override, if ever get, need to use signature match.
//                        JvmMethod factoryClassGetterMethod = factoryClassGetterMethods[0];
//                        // Hopefully the method that creates the component
//                        PsiStatement getterReturnStatement = getReturnStatementFromMethod((PsiMethod)factoryClassGetterMethod);
//                        //// return statement
//                        // The returnReference will contain our type of component .e.g Filter, Producer etc - BUT this might be the interface type, so will it be specific enough ??
//                        PsiReferenceExpressionImpl returnReference = getLocalVariableFromReturnStatement(getterReturnStatement);
//
//                        String getterReturnType = ((PsiType)factoryClassGetterMethod.getReturnType()).getCanonicalText();
//
//                        if (returnReference != null) {
//                            // the method returns a variable, that local variable is the component
//                            PsiElement componentVariable = returnReference.resolve();
//                            if (componentVariable != null) {
//
//                                String ikasanComponentType = null;
//                                if (componentVariable instanceof PsiField && ((PsiField) componentVariable).getType() != null) {
//                                    ikasanComponentType = ((PsiField) componentVariable).getType().getCanonicalText();
//                                } else if (IkasanComponentCategory.isIkasanComponent(getterReturnType)) {
//                                    ikasanComponentType = getterReturnType;
//                                }
//                                PIPSIMethodList  pipsiMethodList = new PIPSIMethodList();
//                                List<String> implementedInterfaces = safeGetInterfacesFromReturnVariable(componentVariable);
//                                pipsiMethodList.setInterfaces(implementedInterfaces);
//                                pipsiMethodList.setBaseType(ikasanComponentType);
//
//                                // The getter method might use an @Resource or a local variable (in which case may have created a new instance)
//                                if (componentVariable instanceof PsiLocalVariable ||
//                                        (componentVariable instanceof PsiField &&
//                                        ((PsiField) componentVariable).getSourceElement().getText().contains("@Resource"))) {
//                                    String beskpokeClassName ;
//                                    if (componentVariable instanceof PsiLocalVariable) {
//                                        // this is an injected bespoke class
//                                        beskpokeClassName = ((PsiLocalVariable) componentVariable).getType().getCanonicalText();
//                                    } else {
//                                        // this is an injected bespoke class
//                                        beskpokeClassName = ((PsiField) componentVariable).getType().getCanonicalText();
//                                    }
//                                    List<PIPSIMethod> additionalParameters = extractParametersFromBespokeIkasanComponent(beskpokeClassName);
//                                    pipsiMethodList.addAllPIPSIMethod(additionalParameters);
//
//                                } else {
//                                    PsiCodeBlock containingBlock = getContainingCodeBlock(componentVariable);
//                                    if (containingBlock != null && componentVariable instanceof PsiLocalVariable) {
//                                        PIPSIMethodList additionalMethodList = getAllMethodCallsForLocalVariableInCodeBlock(containingBlock, (PsiLocalVariable)componentVariable);
//                                        pipsiMethodList.addAllPIPSIMethod(additionalMethodList.getPipsiMethods());
//                                    }
//                                }
//
//                                returnPIPSIMethodList =  pipsiMethodList;
//                            } else {
//                                // TODO ... otherwise ?
//                                LOG.warn("getComponentBuilderMethods(3) failed for " + expressionToBeSearched.getText());
//                            }
//                        } else {
//                            // the return statement itself created the component e.g.
//                            // return builderFactory.getComponentBuilder().scheduledConsumer().build()
//                            PIPSIMethodList moduleMethodList = extractMethodCallsFromChain(getterReturnStatement.getChildren(), new PIPSIMethodList());
//                            returnPIPSIMethodList =  moduleMethodList;
//                        }
//                    }
//                }
//            }
//        }
//        return returnPIPSIMethodList;
//    }
//
//
//    /**
//     * traversing the pointers to get to the Interfaces would result in a lot of null checks, or just a try catch
//     */
//    private List<String> safeGetInterfacesFromReturnVariable(PsiElement componentVariable) {
//        List<String> implementedInterfaces = null;
//        if (componentVariable instanceof PsiLocalVariable) {
//            try {
//                PsiClassType[] interfaces = ((PsiReferenceList)
//                        ((PsiClass)
//                                ((PsiJavaCodeReferenceElement)
//                                        ((PsiClassReferenceType)
//                                                ((PsiTypeElement)
//                                                        ((PsiLocalVariable) componentVariable)
//                                                                .getTypeElement())
//                                                        .getType())
//                                                .getReference())
//                                        .resolve())
//                                .getImplementsList())
//                        .getReferencedTypes();
//                if (interfaces.length > 0) {
//                    implementedInterfaces = new ArrayList<>();
//                    for (PsiClassType psiClassType : interfaces) {
//                        implementedInterfaces.add(psiClassType.getCanonicalText());
//                    }
//                }
//            } catch (NullPointerException npe) {
//                LOG.info("Attempt to get resolved interfaces for " + componentVariable + " failed");
//            }
//        }
//        return implementedInterfaces;
//    }
//
//    /**
//     * We have resolved to a bespoke class that is implementing an Ikasan Component interface
//     * This method will extract the appropriate custom properties for this special class type.
//     * @param beskpokeClassName to be explored
//     * @return A list of fake IkasanMethods that emulate the fluid interface set methods.
//     */
//    private List<PIPSIMethod> extractParametersFromBespokeIkasanComponent(String beskpokeClassName) {
//        List<PIPSIMethod> additionalParameters = new ArrayList<>();
//        // The Bespoke Ikasan Class
//        PsiClass psiClass = StudioPsiUtils.findFirstClass(getProject(), beskpokeClassName);
//        if (psiClass == null || psiClass.getMethods().length == 0) {
//            LOG.warn("Expected to find class " + beskpokeClassName + " but could not be found, skipping");
//        } else {
//            PIPSIMethod bespokeClassParam = createFakePIPSIMethod("set" + IkasanComponentPropertyMeta.BESPOKE_CLASS_NAME, psiClass.getMethods()[0], StudioUtils.getLastToken("\\.", beskpokeClassName));
//            additionalParameters.add(bespokeClassParam);
//            PsiClassType[] psiClassTypes = psiClass.getImplementsList().getReferencedTypes();
//
//            for (PsiClassType type : psiClassTypes) {
//                PsiClass resolvedType = type.resolve();
//                IkasanComponentCategory ikasanComponentCategory = null;
//                String componentType;
//                if (resolvedType != null) {
//                    componentType = IkasanComponentCategory.parseBaseClass(resolvedType.getQualifiedName()).toString();
//                } else {
//                    // These tend to be parameterised types
//                    String qualifiedName = ((PsiClassReferenceType) type).getReference().getQualifiedName();
//                    if (IkasanComponentPropertyMeta.CONFIGURED_RESOURCE_INTERFACE.equals(qualifiedName) || IkasanComponentPropertyMeta.CONFIGURATION.equals(qualifiedName)) {
//                        componentType = qualifiedName;
//                    } else {
//                        componentType = IkasanComponentCategory.parseBaseClass(qualifiedName).toString();
//                    }
//                }
//                if (componentType != null) {
//                    if (IkasanComponentCategory.CONVERTER.toString().equals(componentType)) {
//                        PsiType[] templateTypes = type.getParameters();
//                        if (templateTypes.length > 0) {
//                            PIPSIMethod fromType = createFakePIPSIMethod("set" + IkasanComponentPropertyMeta.FROM_TYPE, psiClass.getMethods()[0], templateTypes[0].getCanonicalText());
//                            additionalParameters.add(fromType);
//                        }
//                        if (templateTypes.length > 1) {
//                            PIPSIMethod toType = createFakePIPSIMethod("set" + IkasanComponentPropertyMeta.TO_TYPE, psiClass.getMethods()[0], templateTypes[1].getCanonicalText());
//                            additionalParameters.add(toType);
//                        }
//                    } else if (IkasanComponentCategory.FILTER.toString().equals(componentType)) {
//                        PsiType[] templateTypes = type.getParameters();
//                        if (templateTypes.length > 0) {
//                            PIPSIMethod fromType = createFakePIPSIMethod("set" + IkasanComponentPropertyMeta.FROM_TYPE, psiClass.getMethods()[0], templateTypes[0].getCanonicalText());
//                            additionalParameters.add(fromType);
//                        }
//                    } else if (IkasanComponentCategory.CONFIGURED_RESOURCE.toString().equals(componentType) || IkasanComponentPropertyMeta.CONFIGURED_RESOURCE_INTERFACE.equals(componentType)) {
//                        String resourceType = ((PsiClassReferenceType) type).getReference().getTypeParameters()[0].getCanonicalText();
//                        PIPSIMethod configuration = createFakePIPSIMethod("set" + IkasanComponentPropertyMeta.CONFIGURATION, psiClass.getMethods()[0], resourceType);
//                        additionalParameters.add(configuration);
//                        PIPSIMethod isConfiguredResource = createFakePIPSIMethod("set" + IkasanComponentPropertyMeta.IS_CONFIGURED_RESOURCE, psiClass.getMethods()[0], Boolean.TRUE.toString());
//                        additionalParameters.add(isConfiguredResource);
//                    } else if (IkasanComponentPropertyMeta.CONFIGURATION.equals(componentType)) {
//                        String resourceType = ((PsiClassReferenceType) type).getReference().getTypeParameters()[0].getCanonicalText();
//                        PIPSIMethod configuration = createFakePIPSIMethod("set" + IkasanComponentPropertyMeta.CONFIGURATION, psiClass.getMethods()[0], resourceType);
//                        additionalParameters.add(configuration);
//                    }
//                }
//            }
//        }
//        return additionalParameters;
//    }
//
//    private PIPSIMethod createFakePIPSIMethod(String methodName, PsiMethod jumpToMethod, String literalValue) {
//        PsiLiteralExpression stringLiteral = (PsiLiteralExpression) javaPsiFactory.createExpressionFromText("\""+literalValue+"\"", null);
//        return new PIPSIMethod(methodName, jumpToMethod, new PsiExpression[] {stringLiteral});
//    }
//
//    /**
//     * Given a local variable and the code block that contains it, go from the variable declaration and all statements after it,
//     * gathering any method call either from the first declaration or subsequent method calls against the variable.
//     * @param container code block that contains the local variable
//     * @param localVaraible that we need to gather information about
//     * @return the methods executed against the local variable.
//     */
//    protected PIPSIMethodList getAllMethodCallsForLocalVariableInCodeBlock(PsiCodeBlock container, PsiLocalVariable localVaraible) {
//        PIPSIMethodList  pipsiMethodList = new PIPSIMethodList();
//        boolean skip = true;
//        String variableName = localVaraible.getComponentName();
//        String setterSignature = variableName+".set";
//        for (PsiElement psiElement : container.getStatements()) {
//            // keep skipping until the declaration of the local variable
//            if (skip && psiElement instanceof PsiDeclarationStatement && psiElement.getFirstChild() == localVaraible) {
//                skip = false;
//                // e.g. MyFilter - not sure if this is needed yet, keep for now.
//                PsiClassReferenceType type = (PsiClassReferenceType) ((PsiLocalVariable) psiElement.getFirstChild()).getType();
//                PsiType[] superClasses = ((PsiLocalVariable) psiElement.getFirstChild()).getType().getSuperTypes();
//                continue; // mybe we still need to check if this is A NEW statements or a chained method !
//            }
//            if (skip) {
//                continue;
//            } else if (psiElement.getText().contains(setterSignature)) {
//                PIPSIMethodList  partialMethodList = extractMethodCallsFromChain(psiElement.getChildren(), new PIPSIMethodList());
//                pipsiMethodList.addAllPIPSIMethod(partialMethodList.getPipsiMethods());
//            }
//        }
//        return pipsiMethodList;
//    }
//
//    /**
//     * Given a pisElement e.g. a local variable, return the code block that contains it.
//     * @param psiElement of interest
//     * @return the PSiCodeBlock that contains the psiElement
//     */
//    protected PsiCodeBlock getContainingCodeBlock(PsiElement psiElement) {
//        PsiElement currentElement = psiElement;
//        while (currentElement != null && !(currentElement instanceof PsiCodeBlock)) {
//            currentElement = currentElement.getParent();
//        }
//        return (PsiCodeBlock)currentElement;
//    }
//
//
//    /**
//     * Look through all the chained methods for 'getComponentBuilder', return true if it is found
//     * @param expressionRHS to be searched
//     * @return true if 'getComponentBuilder' is anywhere in the chained method calls.
//     */
//    protected boolean containsComponentBuilder(PIPSIMethodList expressionRHS) {
//        boolean contains = false;
//
//        PsiReferenceExpression expressionStart = expressionRHS.baseMethodinstanceVariable;
//        IkasanClazz expressionStartType = getTypeOfReferenceExpression(expressionStart);
//        if (IkasanClazz.COMPONENT_BUILDER.equals(expressionStartType) || IkasanClazz.EXCEPTION_RESOLVER_BUILDER.equals(expressionStartType)) {
//            contains = true;
//        }
//        for (PIPSIMethod componentBuilderMethod: expressionRHS.getPipsiMethods()) {
//            String methodName = componentBuilderMethod.getComponentName();
//            if (methodName.equals(COMPONENT_BUILDER_NAME_METHOD) || methodName.equals(EXCEPTION_RESOLVER_NAME_METHOD)) {
//                contains = true ;
//                break;
//            }
//        }
//        return contains;
//    }
//
//    IkasanClazz getTypeOfReferenceExpression(PsiReferenceExpression firstRHSToken) {
//        IkasanClazz ikasanClazz = IkasanClazz.UNKNOWN;
//        if (firstRHSToken == null) {
//            LOG.warn("getTypeOfReferenceExpression called with null firstRHSToken");
//        } else {
//            PsiElement methodOrVariable = firstRHSToken.resolve();
//            if (methodOrVariable instanceof PsiVariable) {
//                ikasanClazz = IkasanClazz.parseClassType(StudioPsiUtils.getTypeOfVariable((PsiVariable) methodOrVariable));  // @todo mabe also inspec annotation for @Resource
//            } else if (methodOrVariable instanceof PsiMethod) {
//                ikasanClazz = IkasanClazz.LOCAL_METHOD;
//            }
//        }
//        return ikasanClazz;
//    }
//
//
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
//    /**
//     * Examine the provided pipsiMethod parameter to determine if it is a Spring @Value, if so, return the
//     * ${key}, otherwise return null
//     * @param pipsiMethod to examine
//     * @return either a string representing the Spring @Value key, or null if not applicable
//     */
//    private String getSpringValueKey(PIPSIMethod pipsiMethod, int parameter) {
//        String springValueKey = null;
//        PsiExpression psiExpression = pipsiMethod.getParameter(0);
//        if (psiExpression instanceof PsiReferenceExpression) {
//            PsiElement springVariable = ((PsiReferenceExpression)psiExpression).resolve();
//            if (springVariable instanceof  PsiField) {
//                PsiAnnotation[] annotations = ((PsiField)springVariable).getAnnotations();
//                if (annotations.length > 0) {
//                    List<JvmAnnotationAttribute> annotationAttributes = annotations[0].getAttributes();
//                    if (! annotationAttributes.isEmpty()) {
//                        springValueKey = ((PsiNameValuePair)annotationAttributes.get(0)).getLiteralValue();
//                        if (springValueKey!=null && springValueKey.contains("${")) {
//                            springValueKey = springValueKey.replace("$", "")
//                                    .replace("{","")
//                                    .replace("}","");
//                        }
//                    }
//
//                }
//            }
//        }
//        return springValueKey;
//    }
//
//    /**
//     * create a new IkasanFlowComponent including any properties that may have been set.
//     * @param parent flow for this element
//     * @param name of the element
//     * @param description for the element
//     * @param componentBuilderMethodList e.g. .consumer("Ftp Consumer", componentBuilder.ftpConsumer()
//     *      *                     .setCronExpression(ftpConsumerCronExpression)
//     *      *                     .setClientID(ftpConsumerClientID)
//     *      *                     ...
//     *      *                         .build()
//     * @return new new IkasanFlowComponent
//     */
//    protected FlowElement createFlowElementWithProperties(final Flow parent, final String name, final String description, final PIPSIMethodList componentBuilderMethodList) {
//        if (componentBuilderMethodList == null) {
//            return null;
//        }
//        FlowElement ikasanFlowComponent = null;
//        Map<IkasanComponentPropertyMetaKey, Object> flowElementProperties = new TreeMap<>();
//        for (PIPSIMethod pipsiMethod: componentBuilderMethodList.getPipsiMethods()) {
//            int addExceptionToActionLineNumber = 1;
//            String methodName = pipsiMethod.getComponentName();
//            if  ("build".equals(methodName) || COMPONENT_BUILDER_NAME_METHOD.equals(methodName)) {
//                // Ignore for now
//                ;
//            } else if (EXCEPTION_RESOLVER_NAME_METHOD.equals(methodName)) {
//                ikasanFlowComponent = FlowElement.createFlowElement(IkasanComponentType.parseMethodName(methodName), parent, name, description);
//            } else if (ADD_EXCEPTION_TO_ACTION.equals(methodName)) {
//                // .addExceptionToAction(RouterException.class, OnException.retry(200, 10))
//                // Exception resolver is a special case, key name will be ExceptionResolver, group will increment from 1 for each addExceptionToAction method call.
//                //  parameter 1 = RouterException.class
//                //  parameter 2 = retry
//                //  parameter 3 = 200
//                //  parameter 4 = 10
//
//                String exceptionClassRaw =  getFullQualifiedClassName(getReferenceOrLiteralFromParameter(pipsiMethod, 0));
//                // here we should get method name
//                if (exceptionClassRaw != null && !exceptionClassRaw.endsWith(".class")) {
//                    exceptionClassRaw += ".class";
//                }
//                String exceptionClass = IkasanLookup.EXCEPTION_RESOLVER_STD_EXCEPTIONS.parseClass(exceptionClassRaw);
//
//                String actionType = IkasanExceptionResolutionMeta.parseAction(
//                        getReferenceOrLiteralFromParameter(pipsiMethod, 1));
//                List<IkasanComponentPropertyMeta> actionParams = IkasanExceptionResolutionMeta.getPropertyMetaListForAction(actionType);
//                List<IkasanComponentProperty> propertyList = IkasanComponentProperty.generateIkasanComponentPropertyList(actionParams);
//                PsiExpression onExceptionMethodCall = pipsiMethod.getParameter(1);
//                if (onExceptionMethodCall instanceof PsiMethodCallExpression) {
//                    int paramCount = ((PsiMethodCallExpression)onExceptionMethodCall).getArgumentList().getExpressionCount();
//
//                    for (int index = 0; index < paramCount; index++) {
//                        if (actionParams.size() > index) {
//                            propertyList.get(index).setValueFromString(((PsiMethodCallExpression)onExceptionMethodCall).getArgumentList().getExpressions()[index].getText());
//                        }
//                    }
//                }
//                IkasanExceptionResolution ikasanExceptionResolution = new IkasanExceptionResolution(exceptionClass, actionType, propertyList);
//                if (ikasanFlowComponent instanceof IkasanExceptionResolver) {
//                    ((IkasanExceptionResolver)ikasanFlowComponent).addExceptionResolution(ikasanExceptionResolution);
//                } else {
//                    LOG.warn("Expecting an IkasanExceptionResolver but got a " + ikasanFlowComponent);
//                }
//
//
//            } else if (methodName.startsWith("set")) {
//                String parameter =  getReferenceOrLiteralFromParameter(pipsiMethod, 0);
//                // Only expect 1 param for the setter
//                flowElementProperties.put(new IkasanComponentPropertyMetaKey(methodName.replaceFirst("set", "")), parameter);
//            } else {
//                // Must be the component type e.g. jmsConsumer()
//                ikasanFlowComponent = FlowElement.createFlowElement(IkasanComponentType.parseMethodName(methodName), parent, name, description);
//
//            }
//        }
//
//        // In this case, the object was created before the setter chaining
//        if (ikasanFlowComponent == null && componentBuilderMethodList.getBaseType() != null) {
//            IkasanComponentType componentType = IkasanComponentType.parseCategoryType(componentBuilderMethodList.getBaseType());
//            ikasanFlowComponent = FlowElement.createFlowElement(componentType, parent, name, description);
//        }
//
//        if (ikasanFlowComponent != null && ! flowElementProperties.isEmpty()) {
//            // Now we know the component, we can match the properties to the known properties for that component
//            IkasanComponentType componentType = ikasanFlowComponent.getType();
//            for (Map.Entry<IkasanComponentPropertyMetaKey, Object> entry : flowElementProperties.entrySet()) {
//                IkasanComponentPropertyMeta metaData = componentType.getMetaDataForPropertyName(entry.getKey());
//                if (metaData != null) {
//
//                    ikasanFlowComponent.setPropertyValue(entry.getKey(), entry.getValue());
//                } else {
//
//                    ikasanFlowComponent.setPropertyValue(entry.getKey(),
//                        new IkasanComponentProperty(IkasanComponentPropertyMeta.getUnknownComponentMeta(entry.getKey().getPropertyName()), entry.getValue()));
//                }
//            }
//        }
//        return ikasanFlowComponent;
//    }
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
//    protected PsiStatement getReturnStatementFromMethod(final PsiMethod flowMethod) {
//        PsiStatement psiReturnStatement = null;
//        if (flowMethod != null && flowMethod.getBody() != null) {
//            PsiCodeBlock flowMethodCodeBlock = flowMethod.getBody();
//            PsiStatement[] psiStatement = flowMethodCodeBlock.getStatements();
//            if (psiStatement.length > 0) {
//                psiReturnStatement = psiStatement[psiStatement.length - 1];
//            }
//        }
//        return psiReturnStatement;
//    }
//
//
//    protected PIPSIMethodList extractMethodCallsFromChain(final PsiElement psiElement, final PIPSIMethodList pipsiMethodList) {
//        PsiElement[] psiElements = { psiElement} ;
//        return extractMethodCallsFromChain(psiElements, pipsiMethodList);
//    }
//    /**
//     * Recursive - Given a statement consisting of chained method calls, extract all the method calls, remember this works BACKWARD from the last
//     * element to the first
//     * @param children the children at the current level
//     * @param pipsiMethodList This list of methods being built up. e.g.
//     *   builderFactory.getComponentBuilder().jmsProducer()
//     *                 .setConfiguredResourceId(jmsProducerConfiguredResourceId)
//     *                 .setDestinationJndiName("jms.topic.test")
//     *                 .setConnectionFactory(connectionFactory)
//     * @return the growing list of methods that are invoked from within this statement, technically we dont need the
//     * return, since the passed in List is same one that is returned.
//     */
//    protected PIPSIMethodList extractMethodCallsFromChain(final PsiElement[] children, final PIPSIMethodList pipsiMethodList) {
//        boolean isSpringBean = false;
//        if (children != null) {
//            for (PsiElement subelement : children) {
//
//
//                if (subelement instanceof PsiReferenceExpression) {
//                    PsiElement[] subChildren = subelement.getChildren();
//                    if (subChildren.length <= 2) {
//                        // Typically the second method in the chain is the one after getComponentBuilder() so will be e.g. jmsProducer()
//                        pipsiMethodList.setBaseMethodinstanceVariable((PsiReferenceExpression)subelement);
//                    }
//                    extractMethodCallsFromChain(subChildren, pipsiMethodList);
//                } else if (subelement instanceof PsiMethodCallExpression) {
//                    extractMethodCallsFromChain(subelement.getChildren(), pipsiMethodList);
//                    PIPSIMethod pipsiMethod = getPIPSIMethod((PsiMethodCallExpression)subelement);
//                    pipsiMethodList.addPIPSIMethod(pipsiMethod);
//                } else if (subelement instanceof PsiModifierList &&
//                        (subelement.getText().startsWith("@Resource") || subelement.getText().startsWith("@Bean") || subelement.getText().startsWith("@Autowired"))) {
//                    isSpringBean = true;
//                    pipsiMethodList.setBaseType(PIPSIMethodList.SPRING_BEAN);
//
//                }
//            }
//        }
//        return pipsiMethodList;
//    }
//
//
//
//    protected PsiIdentifier getFirstIdentifierFromExpression(final PsiReferenceExpression referenceExpression) {
//        if (referenceExpression != null) {
//            return (PsiIdentifier) Arrays.stream(referenceExpression.getChildren())
//                    .filter(item -> item instanceof PsiIdentifier)
//                    .findFirst()
//                    .orElse(null);
//        } else {
//            return null;
//        }
//    }
//
//    /**
//     * Extract the PSIMethod from the PsiMethodCallExpression e.g. extract
//     * PMethod{name='getModuleBuilder', parameters=PsiExpressionList}
//     * from
//     * PsiMethodCallExpression:builderFactory.getModuleBuilder("fms-ftp")
//     * @param psiMethodCallExpression is the PsiMethodCallExpression that contains the PSIMethod
//     * @return the enclosed PSIMethod NOTE - this may be null if Intellij has not build its internal model
//     */
//    protected PIPSIMethod getPIPSIMethod(final PsiMethodCallExpression psiMethodCallExpression) {
//        PIPSIMethod pipsiMethod = null;
//
//        PsiIdentifier methodName = getFirstIdentifierFromExpression(psiMethodCallExpression.getMethodExpression());
//        if (methodName != null) {
//            pipsiMethod = new PIPSIMethod(methodName.getText());
//
//        }
//        if ( methodName != null && methodName.getParent() != null && methodName.getParent().getReference() != null) {
//            PsiReference methodParent = methodName.getParent().getReference();
//            // argumentList is ("fms-ftp")
//            PsiExpression[] argumentList = psiMethodCallExpression.getArgumentList().getExpressions();
//            // in theory, at this point, getFtpConsumerFlow should have a reference which we can use to resolve, but the
//            // unit test does not use compile code so it might be that this does not resolve.
//            // since it is written that "it is not always successful. If the code currently open in the IDE does not compile,
//            // or in other situations, it’s normal for PsiReference.resolve() to return null - all code working with references
//            // must be prepared to handle that."
//            pipsiMethod = new PIPSIMethod(methodName.getText(), (PsiMethod) methodParent.resolve(), argumentList);
//        }
//        return pipsiMethod;
//    }
//
//    //--------------------------------------
//
//
//
//    protected PsiElement getFirstInstanceOfType(PsiStatement statements, Class searchForType) {
//        return Arrays.stream(statements.getChildren())
//                .filter(searchForType::isInstance)
//                .findFirst().orElse(null);
//    }
//    /**
//     * Given the list of arguments, extract the specified argument value.
//     * @param methodArguments the full list of arguments
//     * @param argumentNumber the index of the desired argument (starting at 0)
//     * @param stripQuotes if true, will strip any quote marks from around the text argument
//     * @return the extracted argument
//     */
//    protected String getArgumentValueFromArgumentList(final PsiExpression[] methodArguments, final int argumentNumber, final boolean stripQuotes) {
//        String argumentValue = "";
//        if (methodArguments != null && methodArguments.length > argumentNumber) {
//            argumentValue = methodArguments[argumentNumber].getText();
//            if (stripQuotes && argumentValue!= null) {
//                argumentValue = argumentValue.replace("\"", "");
//            }
//        }
//        return argumentValue;
//    }
//
//    protected PsiIdentifier getFirstIdentifierFromElements(final PsiElement[] psiElements) {
//        if (psiElements != null) {
//            return (PsiIdentifier) Arrays.stream(psiElements)
//                    .filter(item -> item instanceof PsiIdentifier)
//                    .findFirst()
//                    .orElse(null);
//        } else {
//            return null;
//        }
//    }
//
//    protected PsiMethodCallExpression getFirstMethodCallExpressionFromElements(final PsiElement[] psiElements) {
//        if (psiElements != null) {
//            return (PsiMethodCallExpression) Arrays.stream(psiElements)
//                    .filter(item -> item instanceof PsiMethodCallExpressionImpl)
//                    .findFirst()
//                    .orElse(null);
//        } else {
//            return null;
//        }
//    }
//
//
//    /**
//     * The module builder holds the module name and the variable which is the root of all elements of the module
//     * @param methodList the list of methods invoked by the module builder e.g. getModuleBuilder("ftp-jms-im");
//     */
//    protected Module extractModuleName(final PIPSIMethodList methodList) {
//        PIPSIMethod moduleBuilderMethod = methodList.getFirstMethodByName(MODULE_BUILDER_SET_NAME_METHOD);
//        ikasanModule.setComponentName(getReferenceOrLiteralFromParameter(moduleBuilderMethod, 0));
//        return  ikasanModule;
//    }

}
