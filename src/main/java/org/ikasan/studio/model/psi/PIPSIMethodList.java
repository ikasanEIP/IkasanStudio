package org.ikasan.studio.model.psi;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiReferenceExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper method for list of methods.
 * Remember, a list can contain multiple methods of the same name e.f. addFlow
 */
class PIPSIMethodList {
    public static final String SPRING_BEAN = "springBean";
    private static final Logger LOG = Logger.getInstance("#PIPSIMethodList");
    PsiReferenceExpression baseMethodinstanceVariable;  // something like jmsProducer()
    String baseType;                                        // e.g. Filter, Splitter etc
    List<PIPSIMethod> pipsiMethods;                         // The fluent method chain used to enrich the basic type
    List<String>interfaces;                                 // Interfaces implemented by the baseType e.g. ConfiguredResource

    public PIPSIMethodList() {
        pipsiMethods = new ArrayList<>();
    }

//    public void addPIPSIMethod(PIPSIMethod pipsiMethod) {
//        pipsiMethods.add(pipsiMethod);
//    }
//    public void addAllPIPSIMethod(List<PIPSIMethod> pipsiMethod) {
//        pipsiMethods.addAll(pipsiMethod);
//    }
//
//    /**
//     * Go through the method list looking for methods of the same name
//     * @param methodName being looked for
//     * @return a list of PIPSIMethods whose name matches the supplied methodName
//     */
//    public List<PIPSIMethod> getMethodsByName(String methodName) {
//        return pipsiMethods.stream()
//                .filter(item -> item.getName().equals(methodName))
//                .collect(Collectors
//                .toCollection(ArrayList::new));
//    }
//
//    public PIPSIMethod getMethodByIndex(int index) {
//        if (pipsiMethods != null && pipsiMethods.size() > index) {
//            return pipsiMethods.get(index);
//        } else {
//            return null;
//        }
//    }
//
//    public String getConfiguredResource() {
//        String configuredResource = null;
//        if (interfaces != null && !interfaces.isEmpty()) {
//            configuredResource = interfaces
//                    .stream()
//                    .filter(x -> x.contains("ConfiguredResource"))
//                    .findFirst()
//                    .orElse(null);
//            if (configuredResource != null) {
//                configuredResource = configuredResource
//                        .replace("org.ikasan.spec.configuration.ConfiguredResource<", "")
//                        .replace(">","");
//            }
//        }
//        return configuredResource;
//    }
//
//    public void setPipsiMethods(List<PIPSIMethod> pipsiMethods) {
//        this.pipsiMethods = pipsiMethods;
//    }
//
//    public PIPSIMethod getFirstMethodByName(String methodName) {
//        PIPSIMethod methodFound = null;
//        try {
//            methodFound = pipsiMethods.stream()
//                    .filter(item -> item.getName().equals(methodName))
//                    .findFirst()
//                    .orElse(null);
//        } catch (NoSuchElementException nse) {
//            LOG.warn("Search for method of name [" + methodName + "] failed");
//        }
//
//        return methodFound;
//    }
//
//    public PIPSIMethodList(List<PIPSIMethod> pipsiMethods) {
//        this.pipsiMethods = pipsiMethods;
//    }
//    public List<PIPSIMethod> getPipsiMethods() {
//        return pipsiMethods;
//    }
//
//    public PsiReferenceExpression getBaseMethodinstanceVariable() {
//        return baseMethodinstanceVariable;
//    }
//
//    public void setBaseMethodinstanceVariable(PsiReferenceExpression baseMethodinstanceVariable) {
//        this.baseMethodinstanceVariable = baseMethodinstanceVariable;
//        if (baseType == null && baseMethodinstanceVariable != null && baseMethodinstanceVariable.getType() != null) {
//            baseType = baseMethodinstanceVariable.getType().getCanonicalText();
//        }
//    }
//
//    public String getBaseType() {
//        return baseType;
//    }
//
//    public void setBaseType(String baseType) {
//        this.baseType = baseType;
//    }
//
//    public List<String> getInterfaces() {
//        return interfaces;
//    }
//
//    public void setInterfaces(List<String> interfaces) {
//        this.interfaces = interfaces;
//    }
}
