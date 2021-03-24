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
import org.apache.log4j.Logger;
import org.ikasan.studio.model.StudioPsiUtils;

import java.util.HashMap;
import java.util.Map;

public class Generator {
    private static final Logger log = Logger.getLogger(Generator.class);
    public static final String STUDIO_PACKAGE_TAG = "studioPackageTag";
    public static final String CLASS_NAME_TAG = "className";
    public static final String COMPONENT_TAG = "component";
    public static final String INTERFACE_NAME_TAG = "interfaceName";
    public static final String PROPERTIES_TAG = "properties";
    public static final String FLOWS_TAG = "flows";
    public static final String FLOW_TAG = "flow";
    public static final String MODULE_TAG = "module";
    public static final String FLOW_NAME_TAG = "flowName";
    public static final String STUDIO_BASE_PACKAGE_TAG = "studioBasePackage";
    public static final String STUDIO_BOOT_PACKAGE = "org.ikasan.studio.boot";
    public static final String STUDIO_BESPOKE_PACKAGE = "org.user";
    public static final String STUDIO_COMPONENT_PACKAGE = "org.ikasan.studio.component";

    // Enforce Utility class.
    protected Generator() {}

    public static PsiJavaFile createTemplateFile(final Project project, final String packageName, final  String clazzName, final String content, boolean focus, boolean replaceExisting) {
        String fileName = clazzName + ".java";
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
        // When you add the file to the directory, you need the resulting psiFile not the one you sent in.
        standardJavaFormatting(project, newPsiFile);
        newPsiFile = (PsiJavaFile)myPackage.add(newPsiFile);

        if (focus) {
            newPsiFile.navigate(true); // Open the newly created file
        }
        return newPsiFile;
    }

    public static PsiFile createResourceFile(final Project project, final String subDir,final  String propertiesFileNme, final String content, boolean focus) {
        PsiFile psiFile = null;
        String fileName = propertiesFileNme + ".properties";
        VirtualFile sourceRoot = StudioPsiUtils.getSourceRootContaining(project, StudioPsiUtils.JAVA_RESOURCES);
        if (sourceRoot != null) {
            PsiDirectory baseDir = PsiDirectoryFactory.getInstance(project).createDirectory(sourceRoot);
            PsiDirectory directory = baseDir;
            if (subDir != null) {
                directory = StudioPsiUtils.createDirectory(baseDir, subDir);
            }

            psiFile = directory.findFile(fileName) ;
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
        } else {
            //@todo add this to system alerts in Intellij
            log.error("The resources directory was missing, please add it and restart the project");
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

    /**
     * Create the configs map used by the template language, pre-populate with configs used by all templates
     * @return The String to Object map used to populate templates.
     */
    protected static Map<String, Object> getBasicTemplateConfigs() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(STUDIO_BASE_PACKAGE_TAG, STUDIO_BOOT_PACKAGE);
        return configs;
    }
}
