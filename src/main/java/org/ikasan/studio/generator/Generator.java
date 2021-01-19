package org.ikasan.studio.generator;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.testFramework.PsiTestUtil;
import org.ikasan.studio.model.StudioPsiUtils;

public class Generator {
    public static final String DEFAULT_STUDIO_PACKAGE = "org.ikasan.studio";

    public static void createTemplateFile(final AnActionEvent ae, final Project project, final String packageName, final  String clazzName, final String pakeagelessContent, boolean focus) {
        String fileName = clazzName + ".java";
        String content = "package " + packageName + ";" + pakeagelessContent;
        VirtualFile sourceRoot = StudioPsiUtils.getSourceRootContaining(ae.getProject(),StudioPsiUtils.JAVA_CODE);
        PsiDirectory baseDir = PsiDirectoryFactory.getInstance(ae.getProject()).createDirectory(sourceRoot);
        PsiDirectory myPackage = StudioPsiUtils.createPackage(baseDir, packageName);
        PsiFile psiFile = myPackage.findFile(fileName) ;
        if (psiFile == null) {
            psiFile = PsiFileFactory.getInstance(project).createFileFromText(fileName, StdFileTypes.JAVA, content);
            // When you add the file to the directory, you need the resulting psiFilem not the one you sent in.
            psiFile = (PsiFile)myPackage.add(psiFile);
            standardJavaFormatting(project, psiFile);
        } else {
            standardJavaFormatting(project, psiFile);
            // Required post edit steps
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(ae.getProject());
            documentManager.doPostponedOperationsAndUnblockDocument(documentManager.getDocument(psiFile));
        }
        if (focus) {
            psiFile.navigate(true); // Open the newly created file
        }
    }

    public static void createResourceFile(final AnActionEvent ae, final Project project, final String subDir,final  String propertiesFileNme, final String content, boolean focus) {
        String fileName = propertiesFileNme + ".properties";
        VirtualFile sourceRoot = StudioPsiUtils.getSourceRootContaining(ae.getProject(),StudioPsiUtils.JAVA_RESOURCES);
        PsiDirectory baseDir = PsiDirectoryFactory.getInstance(ae.getProject()).createDirectory(sourceRoot);
        PsiDirectory directory = baseDir;
        if (subDir != null) {
            directory = StudioPsiUtils.createDirectory(baseDir, subDir);
        }

        PsiFile psiFile = directory.findFile(fileName) ;
        if (psiFile == null) {
            psiFile = PsiFileFactory.getInstance(project).createFileFromText(fileName, StdFileTypes.PROPERTIES, content);
            // When you add the file to the directory, you need the resulting psiFilem not the one you sent in.
            psiFile = (PsiFile)directory.add(psiFile);
            standardPropertiesFormatting(project, psiFile);
        } else {
            standardPropertiesFormatting(project, psiFile);
            // Required post edit steps
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(ae.getProject());
            documentManager.doPostponedOperationsAndUnblockDocument(documentManager.getDocument(psiFile));
        }
        if (focus) {
            psiFile.navigate(true); // Open the newly created file
        }
    }

    private static void standardJavaFormatting(final Project project, final PsiFile psiFile) {
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiFile);
        CodeStyleManager.getInstance(project).reformat(psiFile);
        // Technically this is a testing Util
        PsiTestUtil.checkFileStructure(psiFile);
    }

    private static void standardPropertiesFormatting(final Project project, final PsiFile psiFile) {
        CodeStyleManager.getInstance(project).reformat(psiFile);
        // Technically this is a testing Util
        PsiTestUtil.checkFileStructure(psiFile);
    }
}
