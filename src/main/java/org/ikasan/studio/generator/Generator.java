package org.ikasan.studio.generator;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import org.apache.commons.io.FilenameUtils;
import org.ikasan.studio.Context;
import org.ikasan.studio.model.StudioPsiUtils;

import java.util.HashMap;
import java.util.Map;

public abstract class Generator {
    private static final Logger LOG = Logger.getInstance("#Generator");
    public static final String STUDIO_PACKAGE_TAG = "studioPackageTag";
    public static final String CLASS_NAME_TAG = "className";
    public static final String COMPONENT_TAG = "component";
    public static final String FLOW_ELEMENT_TAG = "flowElement";
    public static final String INTERFACE_NAME_TAG = "interfaceName";
    public static final String PREFIX_TAG = "prefix";
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

    public static PsiJavaFile createJavaSourceFile(final Project project, final String packageName, final  String clazzName, final String content, boolean focus, boolean replaceExisting) {
        String fileName = clazzName + ".java";
        VirtualFile sourceRoot = StudioPsiUtils.getSourceRootEndingWith(project, StudioPsiUtils.JAVA_CODE);
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

        PsiJavaFile newPsiFile = (PsiJavaFile)PsiFileFactory.getInstance(project).createFileFromText(
                fileName,
                FileTypeManager.getInstance().getFileTypeByExtension(Context.JAVA_FILE_EXTENSION),
                content);
        // When you add the file to the directory, you need the resulting psiFile not the one you sent in.
        standardJavaFormatting(project, newPsiFile);
        newPsiFile = (PsiJavaFile)myPackage.add(newPsiFile);

        if (focus) {
            newPsiFile.navigate(true); // Open the newly created file
        }
        return newPsiFile;
    }

    public static PsiFile createJsonModelFile(final Project project, final String content) {
        return createFile(project, Context.JSON_MODEL_PARENT_DIR, Context.JSON_MODEL_SUB_DIR,
                Context.JSON_MODEL_FILE_WITH_EXTENSION, content, false);
    }

    public static PsiFile createResourceFile(final Project project, final String subDir, final  String fileNameWithExtension, final String content, boolean focus) {
        return createFile(project, StudioPsiUtils.JAVA_RESOURCES, subDir,
                fileNameWithExtension, content, focus);
    }

    public static PsiFile createFile(final Project project, final String rootDirectory, final String subDir,
                                     final String fileNameWithExtension, final String content, boolean focus) {
        PsiFile psiFile = null;
        PsiDirectory srcDir = StudioPsiUtils.getOrCreateSourceRootEndingWith(project, rootDirectory);
        if (srcDir != null) {
            PsiDirectory directory = srcDir;
            if (subDir != null) {
                directory = StudioPsiUtils.createOrGetDirectory(srcDir, subDir);
            }

            psiFile = directory.findFile(fileNameWithExtension) ;
            if (psiFile != null) {
                psiFile.delete();
            }
            String fileType = FilenameUtils.getExtension(fileNameWithExtension);
            psiFile = PsiFileFactory.getInstance(project).createFileFromText(fileNameWithExtension, FileTypeManager.getInstance().getFileTypeByExtension(fileType), content);
            // When you add the file to the directory, you need the resulting psiFilem not the one you sent in.
            psiFile = (PsiFile)directory.add(psiFile);
            standardPropertiesFormatting(project, psiFile);
//            // Required post edit steps
//            PsiDocumentManager documentManager = PsiDocumentManager.createFlowElement(project);
//            documentManager.doPostponedOperationsAndUnblockDocument(documentManager.getDocument(psiFile));
            if (focus) {
                psiFile.navigate(true); // Open the newly created file
            }
        } else {
            //@todo add this to system alerts in Intellij
            LOG.warn("The resources directory was missing, please add it and restart the project, could not save file " + fileNameWithExtension);
        }

        return psiFile;
    }

    private static void standardJavaFormatting(final Project project, final PsiFile psiFile) {
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiFile);
        CodeStyleManager.getInstance(project).reformat(psiFile);
    }

    private static void standardPropertiesFormatting(final Project project, final PsiFile psiFile) {
        CodeStyleManager.getInstance(project).reformat(psiFile);
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
