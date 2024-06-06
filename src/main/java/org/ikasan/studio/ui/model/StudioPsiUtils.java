package org.ikasan.studio.ui.model;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.IncorrectOperationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.IkasanPomModel;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.psi.PIPSIIkasanModel;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.ikasan.studio.core.model.ikasan.instance.IkasanPomModel.MAVEN_COMPILER_SOURCE;
import static org.ikasan.studio.core.model.ikasan.instance.IkasanPomModel.MAVEN_COMPILER_TARGET;

public class StudioPsiUtils {
    private static final Logger LOG = Logger.getInstance("#StudioPsiUtils");
    public static final String TEMP_CONTENT_ROOT = "temp://";
    public static final String GENERATED_CONTENT_ROOT = "/generated";
    public static final String USER_CONTENT_ROOT = "/user";
    public static final String MAIN_JAVA = "main/java";
    public static final String SRC_MAIN_JAVA_CODE = "src/" + MAIN_JAVA;
    public static final String MAIN_RESOURCES = "main/resources";
    public static final String SRC_MAIN_RESOURCES = "src/" + MAIN_RESOURCES;

    public static final String SRC_MAIN = "src/main";
    public static final String JSON_MODEL_SUB_DIR = "model";
    public static final String SRC_MAIN_MODEL = SRC_MAIN + "/" + JSON_MODEL_SUB_DIR;
    public static final String MODEL_JSON = "model.json";
    public static final String JSON_MODEL_FULL_PATH = SRC_MAIN_MODEL + "/" + MODEL_JSON;

    // Enforce utility nature upon class
    private StudioPsiUtils() {}

    /**
     * Load the content of the major pom
     * @param project currently being worked on
     * @return the studio representation of the POM
     */
    public static IkasanPomModel pomLoadFromVirtualDisk(Project project, String containingDirectory) {
        IkasanPomModel ikasanPomModel = null;
            Model model;
            PsiFile pomPsiFile = pomGetTopLevel(project, containingDirectory);
            if (pomPsiFile != null) {
                try (Reader reader = new StringReader(pomPsiFile.getText())) {
                    MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
                    model = xpp3Reader.read(reader);
                    ikasanPomModel = new IkasanPomModel(model);
                    UiContext.setIkasanPomModel(project.getName(), ikasanPomModel);
                } catch (IOException | XmlPullParserException ex) {
                    LOG.warn("STUDIO: Unable to load project ikasanPomModel", ex);
                }
            }
        return ikasanPomModel;
    }

    //@ todo make a plugin property to switch on / off assumeModuleConfigClass
    public static void generateModelInstanceFromJSON(String projectKey, boolean assumeModuleConfigClass) throws StudioBuildException {
        PsiFile jsonModelPsiFile = StudioPsiUtils.getModelFile(UiContext.getProject(projectKey), GENERATED_CONTENT_ROOT);
        if (jsonModelPsiFile != null) {
            String json = jsonModelPsiFile.getText();
            Module newModule = ComponentIO.deserializeModuleInstanceString(json, JSON_MODEL_FULL_PATH);
            if (newModule == null) {
                Thread thread = Thread.currentThread();
                LOG.error("STUDIO: Attempt to set model resulted in a null model" + Arrays.toString(thread.getStackTrace()));
            }
            UiContext.setIkasanModule(projectKey, newModule);
        } else {
            LOG.info("STUDIO: Could not read the " + JSON_MODEL_FULL_PATH + ", this is probably a new project");
        }
    }


    /**
     * Add the new dependencies IF they are not already in the pom
     * @param projectKey for the project being worked on
     * @param newDependencies to be added, a map of Dependency.getManagementKey() -> Dependency
     */
    public static void checkForDependencyChangesAndSaveIfChanged(String projectKey, Set<Dependency> newDependencies) {
        IkasanPomModel ikasanPomModel;
        if (newDependencies != null && !newDependencies.isEmpty()) {
            Project project = UiContext.getProject(projectKey);
            ikasanPomModel = pomLoadFromVirtualDisk(project, project.getName()); // Have to load each time because might have been independently updated.

            if (ikasanPomModel != null) {
                for (Dependency newDependency : newDependencies) {
                    ikasanPomModel.checkIfDependancyAlreadyExists(newDependency);
                }
            }
            pomAddStandardProperties(ikasanPomModel);
            if (ikasanPomModel.isDirty()) {
                parentPomSaveToVirtualDisk(project, ikasanPomModel);
//            ProjectManager.createFlowElement().reloadProject(project);
            }
        }
    }


    /**
     * Add in the standard properties for the ikasanPomModel, based on project level config e.g. JDK
     * @param ikasanPomModel is the root level ikasanPomModel to be updated
     */
    private static void pomAddStandardProperties(IkasanPomModel ikasanPomModel) {
        ikasanPomModel.addProperty(MAVEN_COMPILER_TARGET, "1.8");
        ikasanPomModel.addProperty(MAVEN_COMPILER_SOURCE, "1.8");
    }

    /**
     * Persist IkasanPomModel back to virtual disk, typically done if something was added to IkasanPomModel
     * @param project that the pom model belongs to
     * @param ikasanPomModel to save
     */
    public static void parentPomSaveToVirtualDisk(final Project project, final IkasanPomModel ikasanPomModel) {
        String pomAsString = ikasanPomModel.getModelAsString();

        // Always need to re-get the PsiFile since a reload might have disposed previous handle.
        PsiFile currentPomPsiFile = pomGetTopLevel(project, project.getName());

        PsiDirectory containingDirectory = currentPomPsiFile.getContainingDirectory();
        String fileName = currentPomPsiFile.getName();

        // Delete the old POM file
        if (currentPomPsiFile != null) {
            currentPomPsiFile.delete();
        }

        XmlFile newPomFile = (XmlFile)PsiFileFactory.getInstance(project).createFileFromText(fileName, FileTypeManager.getInstance().getFileTypeByExtension(UiContext.XML_FILE_EXTENSION), pomAsString);
        // When you add the file to the directory, you need the resulting psiFile not the one you sent in.
        newPomFile = (XmlFile)containingDirectory.add(newPomFile);
        CodeStyleManager.getInstance(project).reformat(newPomFile);
    }


    /**
     * Attempt to get the top level pom for the project
     * @param project to be searched
     * @return A PsiFile handle to the pom or null
     */
    public static PsiFile pomGetTopLevel(Project project, String containingDirectory) {
        PsiFile[] pomFiles = PsiShortNamesCache.getInstance(project).getFilesByName("pom.xml");
        for (PsiFile pomFile : pomFiles) {
            if (pomFile != null && pomFile.getContainingDirectory().toString().endsWith(containingDirectory)) {
                return pomFile;
            }
        }
        LOG.warn("Studio: Warn: Could not find any pom.xml " + project.getName());
        return null;
    }

    public static PsiFile createFile1(final String filename, final String text, Project project) {
        //  ************************* PsiJavaParserFacadeImpl ********************************
//        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
//        PsiMethodCallExpression equalsCall = (PsiMethodCallExpression) factory.createExpressionFromText("a.equals(b)", null);
//        @NotNull com.intellij.openapi.module.Module[] module = ModuleManager.getInstance(project).getModules();
//        PsiDirectory baseDir = PsiDirectoryFactory.getInstance(project).createOrGetDirectory(project.getBaseDir());
        return PsiFileFactory.getInstance(project).createFileFromText(filename, FileTypeManager.getInstance().getFileTypeByExtension(UiContext.JAVA_FILE_EXTENSION), text);
    }

    private static void standardJavaFormatting(final Project project, final PsiFile psiFile) {
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiFile);
        CodeStyleManager.getInstance(project).reformat(psiFile);
    }

    public static PsiJavaFile createJavaSourceFile(final Project project, final String contentRoot, final String packageName, final  String clazzName, final String content, boolean focus, boolean replaceExisting) {
        PsiJavaFile newPsiFile = null;
        String fileName = clazzName + ".java";
        VirtualFile sourceRoot = StudioPsiUtils.getSourceDirectoryForContentRoot(project, contentRoot, StudioPsiUtils.SRC_MAIN_JAVA_CODE);
        if (sourceRoot == null) {
            Thread thread = Thread.currentThread();
            LOG.warn("Studio: Serious: createJavaSourceFile cant find sourceRoot for a contentRoot of " + contentRoot + " and directory " + StudioPsiUtils.SRC_MAIN_JAVA_CODE + " trace:" + Arrays.toString(thread.getStackTrace()));
        } else {
            PsiDirectory baseDir = PsiDirectoryFactory.getInstance(project).createDirectory(sourceRoot);
            PsiDirectory myPackage = StudioPsiUtils.createPackage(baseDir, packageName);
            PsiFile psiFile = myPackage.findFile(fileName);
            if (psiFile != null) {
                if (!replaceExisting) {
                    return null;
                }
                // Recreate
                psiFile.delete();
            }

            newPsiFile = (PsiJavaFile) PsiFileFactory.getInstance(project).createFileFromText(
                    fileName,
                    FileTypeManager.getInstance().getFileTypeByExtension(UiContext.JAVA_FILE_EXTENSION),
                    content);
            // When you add the file to the directory, you need the resulting psiFile not the one you sent in.
            standardJavaFormatting(project, newPsiFile);
            newPsiFile = (PsiJavaFile) myPackage.add(newPsiFile);

            if (focus) {
                newPsiFile.navigate(true); // Open the newly created file
            }
        }
        return newPsiFile;
    }


    private static void standardPropertiesFormatting(final Project project, final PsiFile psiFile) {
        CodeStyleManager.getInstance(project).reformat(psiFile);
    }

    public static PsiFile createJsonModelFile(final Project project, final String contentRoot, final String content) {
        return createFile(project, contentRoot, SRC_MAIN, JSON_MODEL_SUB_DIR,
                MODEL_JSON, content, false);
    }

    public static PsiFile createResourceFile(final Project project, final String contentRoot, final String subDir, final  String fileNameWithExtension, final String content, boolean focus) {
        return createFile(project, contentRoot, StudioPsiUtils.SRC_MAIN_RESOURCES, subDir, fileNameWithExtension, content, focus);
    }

    public static PsiFile createFile(final Project project, final String contentRoot, final String sourceRootDir, final String subDir,
                                     final String fileNameWithExtension, final String content, boolean focus) {
        PsiFile psiFile = null;
        PsiDirectory srcDir = StudioPsiUtils.getOrCreateDirectoryInContentRoot(project, contentRoot, sourceRootDir);
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
            // When you add the file to the directory, you need the resulting psiFile not the one you sent in.
            psiFile = (PsiFile)directory.add(psiFile);
            standardPropertiesFormatting(project, psiFile);
            if (focus) {
                psiFile.navigate(true); // Open the newly created file
            }
        } else {
            //@todo add this to system alerts in Intellij
            LOG.warn("STUDIO: SERIOUS: Could no create file [" + fileNameWithExtension + "] in contentRoot [" + contentRoot + "] sourceRootDir [" + sourceRootDir + "] subDir [" + subDir + "]");
        }

        return psiFile;
    }


    /**
     * This class attempts to find a file, it does not attempt to retrieve it
     * @param project to that contains the file
     * @param filename to search for
     * @return A message string to indicate progress.
     */
    public static String findFile(Project project, String filename) {
        StringBuilder message = new StringBuilder();

        PsiFile[] files2 = PsiShortNamesCache.getInstance(project).getFilesByName(filename);
        long t1 = System.currentTimeMillis();
        message
                .append("looking for file ")
                .append(filename)
                .append(" method 1 found [");
        for (PsiFile myFile : files2) {
            message.append(myFile.getName());
        }
        long t2 = System.currentTimeMillis();
        long t3 = System.currentTimeMillis();

        message.append("] method 1 = ")
                .append(t2-t1)
                .append(" ms method 2 = ")
                .append(t3-t2);
        return message.toString();
    }


    public static void refreshCodeFromModelAndCauseRedraw(String projectKey) {
        // @TODO MODEL
        PIPSIIkasanModel pipsiIkasanModel = UiContext.getPipsiIkasanModel(projectKey);
        pipsiIkasanModel.generateJsonFromModelInstance();
        pipsiIkasanModel.generateSourceFromModelInstance3();
        causeRedraw(projectKey);
    }

    public static void causeRedraw(String projectKey) {
        UiContext.getDesignerCanvas(projectKey).setInitialiseAllDimensions(true);
        UiContext.getDesignerCanvas(projectKey).repaint();
    }



    public static void getAllSourceRootsForProject(Project project) {
        String projectName = project.getName();
        VirtualFile[] vFiles = ProjectRootManager.getInstance(project).getContentSourceRoots();
        String sourceRootsList = Arrays.stream(vFiles).map(VirtualFile::getUrl).collect(Collectors.joining("\n"));
        LOG.info("STUDIO: Source roots for the " + projectName + " plugin:\n" + sourceRootsList +  "Project Properties");
    }


    /**
     * Get the source root that contains the supplied string, possible to get java source, resources or test
     * @param project to work on
     * @param relativeRootDir to look for e.g. main/java, main/resources
     * @return the rood of the module / source directory that contains the supplied string.
     */
    public static VirtualFile getSourceDirectoryForContentRoot1(Project project, final String contentRoot, String relativeRootDir) {
        String targetDirectory = contentRoot + "/" +relativeRootDir;
        VirtualFile sourceCodeRoot = null;
        VirtualFile[] srcRootVFiles = ProjectRootManager.getInstance(project).getContentSourceRoots();
        for (VirtualFile vFile : srcRootVFiles) {
            if (vFile.toString().endsWith(targetDirectory)) {
                sourceCodeRoot = vFile;
                break;
            }
        }
        if (sourceCodeRoot == null) {
            Thread thread = Thread.currentThread();
            LOG.warn("STUDIO: WARN: Could not find any source roots for project [" + project + "] and contentRoot [" + contentRoot + "] and relatveRoot [" + relativeRootDir + "] trace:[" + Arrays.toString(thread.getStackTrace())+"]");
        }
        return sourceCodeRoot;
    }


    /**
     * Get the source root that contains the supplied string, possible to get java source, resources or test
     * @param project to work on
     * @param relativeRootDir to look for e.g. main/java, main/resources
     * @return the rood of the module / source directory that contains the supplied string.
     */
    public static VirtualFile getSourceDirectoryForContentRoot(Project project, final String contentRoot, String relativeRootDir) {

        String targetDirectory = project.getBasePath() + contentRoot + "/" + relativeRootDir;
        VirtualFile sourceCodeRoot = null;
        VirtualFile[] srcRootVFiles = ProjectRootManager.getInstance(project).getContentSourceRoots();
        for (VirtualFile vFile : srcRootVFiles) {
            if (vFile.toString().endsWith(targetDirectory)) {
                sourceCodeRoot = vFile;
                break;
            }
        }

        // Try any root that mayb contain this directory
        if (sourceCodeRoot == null) {
            for (VirtualFile vFile : srcRootVFiles) {
                if (vFile.toString().contains(targetDirectory)) {
                    sourceCodeRoot = vFile;
                    break;
                }
            }
            // Now creep up parent till we get exact match
            while (sourceCodeRoot != null && sourceCodeRoot.getParent() != null) {
                sourceCodeRoot = sourceCodeRoot.getParent();
                if (sourceCodeRoot.toString().endsWith(targetDirectory)) {
                    break;
                }
            }
        }
        if (sourceCodeRoot == null) {
            Thread thread = Thread.currentThread();
            LOG.warn("STUDIO: WARN: Could not find any source roots for project [" + project + "] and contentRoot [" + contentRoot + "] and relatveRoot [" + relativeRootDir + "] trace:[" + Arrays.toString(thread.getStackTrace())+"]");
        }
        return sourceCodeRoot;
    }





    public static PsiFile getModelFile(final Project project, final String contentRoot) {
        PsiFile jsonModel = null;
        VirtualFile selectedContentRoot = getSpecificContentRoot(project, contentRoot);
        if (selectedContentRoot != null) {
//            VirtualFile[] contentRootVFiles = ProjectRootManager.getInstance(project).getContentRoots();
//        VirtualFile[] contentRootVFiles = ProjectRootManager.getInstance(project).getContentRoots();
//        if (contentRootVFiles.length != 1) {
//            LOG.warn("STUDIO: Could not find content root directory [" + Arrays.toString(contentRootVFiles) + "]");
//        } else {
//            VirtualFile contentRoot = contentRootVFiles[0];
            jsonModel = getFileFromPath(project, selectedContentRoot, JSON_MODEL_FULL_PATH);
            if (jsonModel == null) {
                LOG.warn("STUDIO: Could not get file from path " + JSON_MODEL_FULL_PATH);
            }
        }
        return jsonModel;
    }



    public static PsiFile getFileFromPath(final Project project, VirtualFile root, String filePath) {
        PsiFile returnFile = null;

        if (root != null && filePath != null && filePath.length() > 1) {
            String fileName = FilenameUtils.getName(filePath);
            String directoryPath = FilenameUtils.getFullPathNoEndSeparator(filePath);
            PsiDirectory psiDirectory = getDirectory(project, root, directoryPath);
            if (psiDirectory != null) {
                returnFile = psiDirectory.findFile(fileName);
            }
        }
        return returnFile;
    }

    public static PsiDirectory getDirectory(final Project project, VirtualFile root, String target) {
        if (root.isDirectory()) {
            return getDirectory(PsiDirectoryFactory.getInstance(project).createDirectory(root), target);
        }
        return null;
    }
    public static PsiDirectory getDirectory(PsiDirectory root, String target) {
        PsiDirectory returnDirectory = root;
        if (root != null && target != null && !target.isEmpty()) {
//            String[] subDirs = target.split(Pattern.quote(File.separator));
            String[] subDirs = target.split(Pattern.quote("/"));
            for(String dir : subDirs) {
                returnDirectory = returnDirectory.findSubdirectory(dir);
                if (returnDirectory == null) {
                    break;
                }
            }
        }
        return returnDirectory;
    }

    public static PsiDirectory getOrCreateDirectory(final Project project, final String contentRoot, final String relativeRootDir) {
        PsiDirectory targetDir = null;

        // First, does it exist
        VirtualFile sourceCodeRoot = StudioPsiUtils.getSourceDirectoryForContentRoot(project, contentRoot, relativeRootDir);

        // if not, create the relevant directories
        if (sourceCodeRoot == null) {

            VirtualFile selectedContentRoot = getSpecificContentRoot(project, contentRoot);
            if (selectedContentRoot != null) {
//            VirtualFile[] contentRootVFiles = ProjectRootManager.getInstance(project).getContentRoots();
//
//
//            if (contentRootVFiles == null || contentRootVFiles.length != 1) {
//                LOG.warn("STUDIO: Could not find the source root for directory [" + relativeRootDir + "] or the content root [" + Arrays.toString(contentRootVFiles) + "]");
//            } else {
//                // get the content root that end with passed in content rood
//                VirtualFile selectedContentRoot = contentRootVFiles[0];
                PsiDirectory contentDir = PsiDirectoryFactory.getInstance(project).createDirectory(selectedContentRoot);
                PsiDirectory srcDir = StudioPsiUtils.createOrGetDirectory(contentDir, "src");
                targetDir = StudioPsiUtils.createOrGetDirectory(srcDir, relativeRootDir);
            }
        } else {
            targetDir = PsiDirectoryFactory.getInstance(project).createDirectory(sourceCodeRoot);
        }
        return targetDir;
    }

    public static PsiDirectory getOrCreateDirectoryInContentRoot(final Project project, final String contentRoot, final String relativeRootDir) {
        PsiDirectory targetDir = null;

        // First, does it exist
        VirtualFile sourceCodeRoot = StudioPsiUtils.getSourceDirectoryForContentRoot(project, contentRoot, relativeRootDir);

        // if not, create the relevant directories
        if (sourceCodeRoot == null) {

            VirtualFile selectedContentRoot = getSpecificContentRoot(project, contentRoot);
            if (selectedContentRoot != null) {
//            VirtualFile[] contentRootVFiles = ProjectRootManager.getInstance(project).getContentRoots();
//
//
//            if (contentRootVFiles == null || contentRootVFiles.length != 1) {
//                LOG.warn("STUDIO: Could not find the source root for directory [" + relativeRootDir + "] or the content root [" + Arrays.toString(contentRootVFiles) + "]");
//            } else {
//                // get the content root that end with passed in content rood
//                VirtualFile selectedContentRoot = contentRootVFiles[0];
                PsiDirectory contentDir = PsiDirectoryFactory.getInstance(project).createDirectory(selectedContentRoot);
                PsiDirectory srcDir = StudioPsiUtils.createOrGetDirectory(contentDir, "src");
                targetDir = StudioPsiUtils.createOrGetDirectory(srcDir, relativeRootDir);
            }
        } else {
            targetDir = PsiDirectoryFactory.getInstance(project).createDirectory(sourceCodeRoot);
        }
        return targetDir;
    }

    protected static VirtualFile getSpecificContentRoot(final Project project, final String contentRoot) {
        VirtualFile targetContentRoot = null;
        VirtualFile[] contentRootVFiles = ProjectRootManager.getInstance(project).getContentRoots();
        if (contentRootVFiles == null || contentRootVFiles.length != 1) {
            for (VirtualFile vFile : contentRootVFiles) {
                if (vFile.toString().endsWith(contentRoot)) {
                    targetContentRoot = vFile;
                }
            }
        } else {
            LOG.warn("STUDIO: Could not find the content root for directory [" + contentRoot + "] from content roots [" + Arrays.toString(contentRootVFiles) + "]");
        }
        return targetContentRoot;
    }

    /**
     * Create the supplied directory in the PSI file system if it does not already exist
     * @param parent directory to contain the new directory
     * @param newDirectoryName to be created
     * @return the directory created or a handle to the existing directory if it exists
     */
    public static PsiDirectory createOrGetDirectory(PsiDirectory parent, String newDirectoryName) {
        PsiDirectory newDirectory = null;
        if (parent != null && newDirectoryName != null) {
//            boolean alreadyExists = false;

            newDirectory = parent.findSubdirectory(newDirectoryName);
//            for (PsiDirectory subdirectoryOfParent : parent.getSubdirectories()) {
//                if (subdirectoryOfParent.getName().equalsIgnoreCase(newDirectoryName)) {
//                    newDirectory = subdirectoryOfParent;
//                    alreadyExists = true;
//                    break;
//                }
//            }
            // doesn't exist, create it.
//            if (!alreadyExists) {
            if (newDirectory == null) {
                newDirectory = parent.createSubdirectory(newDirectoryName);
            }
        }
        return newDirectory;
    }


    /**
     * Find the directory representing the base package, remove any sub package (subdirectory) that is not in the subPackagesToKeep
     * @param project the intellij project to being targetted
     * @param basePackage the package that contains the subpackage leaves
     * @param subPackagesToKeep a set of package names tha are valid i.e. you want to kepp
     */
    public static void deleteSubPackagesNotIn(Project project, final String contentRoot, String basePackage, Set<String> subPackagesToKeep) {
        VirtualFile sourceRoot = StudioPsiUtils.getSourceDirectoryForContentRoot(project, contentRoot, StudioPsiUtils.SRC_MAIN_JAVA_CODE);
        PsiDirectory sourceRootDir = PsiDirectoryFactory.getInstance(project).createDirectory(sourceRoot);
        PsiDirectory leafPackageDirectory = getDirectoryForPackage(sourceRootDir, basePackage);
        if (leafPackageDirectory != null) {
            PsiDirectory[] subDirectories = leafPackageDirectory.getSubdirectories();
            for (PsiDirectory directory : subDirectories) {
                if (!subPackagesToKeep.contains(directory.getName())) {
                    LOG.info("STUDIO: Deleting directory " + directory.getName() + " the basePackage " + basePackage + " should only have these directories " + subPackagesToKeep);
                    directory.delete();
                }
            }
        }
//        PsiDirectory myPackage = StudioPsiUtils.createPackage(baseDir, packageName);
        // @todo
    }

    /**
     * Create the directories for the supplied package name, returning the handle to the leaf directory
     * @param sourceRootDir that contains the package
     * @param qualifiedPackage i.e. dotted notation
     * @return parent directory of package
     */
    public static PsiDirectory createPackage(PsiDirectory sourceRootDir, String qualifiedPackage)
            throws IncorrectOperationException {
        PsiDirectory parentDir = sourceRootDir;
        if (sourceRootDir != null) {
            StringTokenizer token = new StringTokenizer(qualifiedPackage, ".");
            while (token.hasMoreTokens()) {
                String dirName = token.nextToken();
                parentDir = createOrGetDirectory(parentDir, dirName);
            }
        }
        return parentDir;
    }

    /**
     * Traverse from the provided root directory to the leaf directory representing the leaf package
     * @param sourceRootDir to start from
     * @param qualifiedPackage to search
     * @return either a PsiDirectory representing the leaf package or null if not found
     * @throws IncorrectOperationException if there issues with the virtual file system
     */
    public static PsiDirectory getDirectoryForPackage(PsiDirectory sourceRootDir, String qualifiedPackage)
            throws IncorrectOperationException {
        PsiDirectory parentDir = sourceRootDir;
        if (sourceRootDir != null) {
            StringTokenizer token = new StringTokenizer(qualifiedPackage, ".");
            while (token.hasMoreTokens()) {
                String dirName = token.nextToken();
                parentDir = parentDir.findSubdirectory(dirName);
                if (parentDir==null) {
                    break;
                }
            }
        }
        return parentDir;
    }
}
