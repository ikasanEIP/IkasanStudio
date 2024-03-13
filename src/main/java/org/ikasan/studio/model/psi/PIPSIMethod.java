package org.ikasan.studio.model.psi;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethod;

import java.util.Arrays;

/**
 *  PlugIn PSI Method
 *  A cut down version of the PSI method containing only what we are interested in.
 */
class PIPSIMethod {
    private String name;
    private PsiMethod methodDeclaration;
    private PsiExpression[] parameters;

    public PIPSIMethod(String name) {
        this.name = name;
    }
    public PIPSIMethod(String name, PsiMethod methodDeclaration, PsiExpression[] parameters) {
        this.name = name;
        this.methodDeclaration = methodDeclaration;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public PsiExpression[] getParameters() {
        return parameters;
    }

    public void setParameters(PsiExpression[] parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "PIPSIMethod{" +
                "name='" + name + '\'' +
                ", methodDeclaration=" + methodDeclaration +
                ", parameters=" + Arrays.toString(parameters) +
                '}';
    }
}
