package org.ikasan.studio.model.psi;

import com.intellij.psi.PsiReferenceExpression;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Helper method for list of methods.
 *
 * Remember, a list can contain multiple methods of the same name e.f. addFlow
 */
public class PIPSIMethodList {
    public static final String SPRING_BEAN = "springBean";
    private static final Logger log = Logger.getLogger(PIPSIMethodList.class);
    PsiReferenceExpression baseMethodinstanceVariable;  // something like jmsProducer()
    String baseType;
    List<PIPSIMethod> pipsiMethods;

    public PIPSIMethodList() {
        pipsiMethods = new ArrayList<>();
    }

    public void addPIPSIMethod(PIPSIMethod pipsiMethod) {
        pipsiMethods.add(pipsiMethod);
    }
    public void addAllPIPSIMethod(List<PIPSIMethod> pipsiMethod) {
        pipsiMethods.addAll(pipsiMethod);
    }

    /**
     * Go through the method list looking for methods of the same name
     * @param methodName being looked for
     * @return a list of PIPSIMethods whose name matches the supplied methodName
     */
    public List<PIPSIMethod> getMethodsByName(String methodName) {
        return pipsiMethods.stream()
                .filter(item -> item.getName().equals(methodName))
                .collect(Collectors
                .toCollection(ArrayList::new));
    }

    public PIPSIMethod getMethodByIndex(int index) {
        if (pipsiMethods != null && pipsiMethods.size() > index) {
            return pipsiMethods.get(index);
        } else {
            return null;
        }
    }

    public void setPipsiMethods(List<PIPSIMethod> pipsiMethods) {
        this.pipsiMethods = pipsiMethods;
    }

    public PIPSIMethod getFirstMethodByName(String methodName) {
        PIPSIMethod methodFound = null;
        try {
            methodFound = pipsiMethods.stream()
                    .filter(item -> item.getName().equals(methodName))
                    .findFirst()
                    .orElse(null);
        } catch (NoSuchElementException nse) {
            log.warn("Search for method of name [" + methodName + "] failed");
        }

        return methodFound;
    }

    public PIPSIMethodList(List<PIPSIMethod> pipsiMethods) {
        this.pipsiMethods = pipsiMethods;
    }
    public List<PIPSIMethod> getPipsiMethods() {
        return pipsiMethods;
    }

    public PsiReferenceExpression getBaseMethodinstanceVariable() {
        return baseMethodinstanceVariable;
    }

    public void setBaseMethodinstanceVariable(PsiReferenceExpression baseMethodinstanceVariable) {
        this.baseMethodinstanceVariable = baseMethodinstanceVariable;
        if (baseType == null && baseMethodinstanceVariable != null && baseMethodinstanceVariable.getType() != null) {
            baseType = baseMethodinstanceVariable.getType().getCanonicalText();
        }
    }

    public String getBaseType() {
        return baseType;
    }

    public void setBaseType(String baseType) {
        this.baseType = baseType;
    }
}
