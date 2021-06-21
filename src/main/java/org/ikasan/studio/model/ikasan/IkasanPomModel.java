package org.ikasan.studio.model.ikasan;

import com.intellij.psi.PsiFile;
import org.apache.maven.model.Model;

public class IkasanPomModel {
    Model model;
    PsiFile pomPsiFile;

    public IkasanPomModel() {
    }

    public IkasanPomModel(Model model, PsiFile pomPsiFile) {
        this.model = model;
        this.pomPsiFile = pomPsiFile;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public PsiFile getPomPsiFile() {
        return pomPsiFile;
    }

    public void setPomPsiFile(PsiFile pomPsiFile) {
        this.pomPsiFile = pomPsiFile;
    }
}
