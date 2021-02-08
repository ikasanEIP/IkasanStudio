package org.ikasan.studio.generator;

import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.testFramework.PsiTestUtil;
import org.ikasan.studio.model.StudioPsiUtils;

public class Generator {
    public static final String CLASS_NAME_TAG = "className";
    public static final String COMPONENT_TAG = "component";
    public static final String FLOWS_TAG = "flows";
    public static final String MODULE_TAG = "module";
    public static final String FLOW_NAME_TAG = "flowName";


    public static final String DEFAULT_STUDIO_PACKAGE = "org.ikasan.studio";
    public static final GeneratorStrategy GENERATOR_STRATEGY = GeneratorStrategy.VELOCITY;

    public static PsiJavaFile createTemplateFile(final Project project, final String packageName, final  String clazzName, final String pakeagelessContent, boolean focus, boolean replaceExisting) {
        String fileName = clazzName + ".java";
        String content = "package " + packageName + ";" + pakeagelessContent;
        VirtualFile sourceRoot = StudioPsiUtils.getSourceRootContaining(project, StudioPsiUtils.JAVA_CODE);
        PsiDirectory baseDir = PsiDirectoryFactory.getInstance(project).createDirectory(sourceRoot);
        PsiDirectory myPackage = StudioPsiUtils.createPackage(baseDir, packageName);
        PsiFile psiFile = myPackage.findFile(fileName) ;
        if (psiFile != null) {
            if (!replaceExisting) {
                return null;
            }
            // Recreate
            psiFile.delete();
        }

        PsiJavaFile newPsiFile = (PsiJavaFile)PsiFileFactory.getInstance(project).createFileFromText(fileName, StdFileTypes.JAVA, content);
        // When you add the file to the directory, you need the resulting psiFilem not the one you sent in.
        newPsiFile = (PsiJavaFile)myPackage.add(newPsiFile);
        standardJavaFormatting(project, newPsiFile);

        if (focus) {
            newPsiFile.navigate(true); // Open the newly created file
        }
        return newPsiFile;
    }

    public static PsiFile createResourceFile(final Project project, final String subDir,final  String propertiesFileNme, final String content, boolean focus) {
        String fileName = propertiesFileNme + ".properties";
        VirtualFile sourceRoot = StudioPsiUtils.getSourceRootContaining(project, StudioPsiUtils.JAVA_RESOURCES);
        PsiDirectory baseDir = PsiDirectoryFactory.getInstance(project).createDirectory(sourceRoot);
        PsiDirectory directory = baseDir;
        if (subDir != null) {
            directory = StudioPsiUtils.createDirectory(baseDir, subDir);
        }

        PsiFile psiFile = directory.findFile(fileName) ;
        if (psiFile != null) {
            psiFile.delete();
        }

        psiFile = PsiFileFactory.getInstance(project).createFileFromText(fileName, StdFileTypes.PROPERTIES, content);
        // When you add the file to the directory, you need the resulting psiFilem not the one you sent in.
        psiFile = (PsiFile)directory.add(psiFile);
        standardPropertiesFormatting(project, psiFile);
//            // Required post edit steps
//            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
//            documentManager.doPostponedOperationsAndUnblockDocument(documentManager.getDocument(psiFile));
        if (focus) {
            psiFile.navigate(true); // Open the newly created file
        }
        return psiFile;
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
