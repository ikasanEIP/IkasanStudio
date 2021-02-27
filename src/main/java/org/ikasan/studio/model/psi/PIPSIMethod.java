package org.ikasan.studio.model.psi;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethod;

import java.util.Arrays;

/**
 *  PlugIn PSI Method
 *  A cut down version of the PSI method containing only what we are interested in.
 */
public class PIPSIMethod {
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
    public PsiMethod getMethodDeclaration() {
        return methodDeclaration;
    }
    public void setMethodDeclaration(PsiMethod methodDeclaration) {
        this.methodDeclaration = methodDeclaration;
    }
    public PsiExpression[] getParameter() {
        return parameters;
    }

    public PsiExpression getParameter(int argumentNumber) {
        if (parameters != null && parameters.length > argumentNumber) {
            return parameters[argumentNumber];
        } else {
            return null;
        }
    }

    // Can expenad to things like get literal etc
    public String getLiteralParameterAsString(int argumentNumber, boolean stripQuotes) {
        PsiExpression param = getParameter(argumentNumber);
        if (param != null) {
            if (stripQuotes) {
                return param.getText().replace("\"", "");
            } else {
                return param.getText();
            }
        } else {
            return "";
        }
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
