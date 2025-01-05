package org.ikasan.studio.ui.model;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Computable;
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
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.IkasanPomModel;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.psi.PIPSIIkasanModel;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.ikasan.studio.core.model.ikasan.instance.IkasanPomModel.MAVEN_COMPILER_SOURCE;
import static org.ikasan.studio.core.model.ikasan.instance.IkasanPomModel.MAVEN_COMPILER_TARGET;
import static org.ikasan.studio.ui.model.psi.PIPSIIkasanModel.MODULE_PROPERTIES_FILENAME_WITH_EXTENSION;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;


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
    public static final String POM_XML = "pom.xml";
    public static final String JSON_MODEL_FULL_PATH = SRC_MAIN_MODEL + "/" + MODEL_JSON;
    public static final String APPLICATION_PROPERTIES_FULL_PATH = SRC_MAIN_RESOURCES + "/" + MODULE_PROPERTIES_FILENAME_WITH_EXTENSION;

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

    /**
     * Load the content of the application properties file
     * @param project currently being worked on
     * @return a map of the Ikasan application properties, typically located in application.properties
     */
    public static Map<String, String> getApplicationPropertiesMapFromVirtualDisk(Project project) {
        Map<String, String> applicationProperties = null;
        VirtualFile selectedContentRoot = getSpecificContentRoot(project, StudioPsiUtils.GENERATED_CONTENT_ROOT);
        if (selectedContentRoot != null) {
            PsiFile applicationPropertiesVF = getFileFromPath(project, selectedContentRoot, APPLICATION_PROPERTIES_FULL_PATH);
            if (applicationPropertiesVF == null) {
                LOG.warn("STUDIO: Could not get application properties file from path " + APPLICATION_PROPERTIES_FULL_PATH);
            } else {
                applicationProperties = StudioBuildUtils.convertStringToMap(applicationPropertiesVF.getText());
            }
        }
        return applicationProperties;
    }

    /**
     * Recreate the in-memory Module from the persisted model.json.
     * WARNING: You MUST not call this from Event Dispatcher Thread, instead call from background e.g.
     *         ApplicationManager.getApplication().executeOnPooledThread(() -> {
     *             synchGenerateModelInstanceFromJSON(projectKey);
     *             ... any other actions relying on above action completion
     *         });
     * @param projectKey of project to refresh
     */
    public static void synchGenerateModelInstanceFromJSON(String projectKey) {
        PsiFile jsonModelPsiFile = StudioPsiUtils.getModelJsonPsiFile(UiContext.getProject(projectKey));
        if (jsonModelPsiFile != null) {
            String json = ApplicationManager.getApplication().runReadAction((Computable<String>) jsonModelPsiFile::getText);
            Module newModule = null;
            try {
                newModule = ComponentIO.deserializeModuleInstanceString(json, JSON_MODEL_FULL_PATH);
            } catch (StudioBuildException se) {
                LOG.warn("STUDIO: SERIOUS ERROR: during resetModelFromDisk, reported when reading " + StudioPsiUtils.JSON_MODEL_FULL_PATH + " message: " + se.getMessage() + " trace: " + Arrays.asList(se.getStackTrace()));
                StudioUIUtils.displayIdeaErrorMessage(projectKey, "Error: Please fix " + StudioPsiUtils.JSON_MODEL_FULL_PATH + " then use the Refresh Button");
                // The dumb module should contain just enough to prevent the plugin from crashing
                UiContext.setIkasanModule(projectKey, Module.getDumbModuleVersion());
            }
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
                pomAddStandardProperties(ikasanPomModel);
                if (ikasanPomModel.isDirty()) {
                    parentPomSaveToVirtualDisk(project, ikasanPomModel);
                }
            } else {
                LOG.warn("STUDIO: WARN: checkForDependencyChangesAndSaveIfChanged invoked but ikasanPomModel was null project [" + projectKey +"] newDependencies [" + newDependencies + "]");
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
        AtomicReference<PsiFile> returnPomFile = new AtomicReference<>();

        ApplicationManager.getApplication().runReadAction(() -> {
            PsiFile[] pomFiles = PsiShortNamesCache.getInstance(project).getFilesByName("pom.xml");
            for (PsiFile pomFile : pomFiles) {
                if (pomFile != null && pomFile.getContainingDirectory().toString().endsWith(containingDirectory)) {
                    returnPomFile.set(pomFile);
                    break;
                }
            }
        });
        LOG.warn("Studio: Warn: Could not find any pom.xml " + project.getName());
        return returnPomFile.get();
    }


    /**
     * Apply the Intellij formatters to validate the Java source file
     * @param project to update
     * @param psiFile to update
     */
    private static void standardJavaFormatting(final Project project, final PsiFile psiFile) {
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiFile);
        CodeStyleManager.getInstance(project).reformat(psiFile);
    }

    /**
     * Apply the Intellij formatters to validate the properties source file
     * @param project to update
     * @param psiFile to update
     */
    private static void standardPropertiesFormatting(final Project project, final PsiFile psiFile) {
        CodeStyleManager.getInstance(project).reformat(psiFile);
    }

    /**
     * Create and save a java source file
     * @param project to be updated
     * @param contentRoot for the file
     * @param packageName for the java file
     * @param clazzName for the java file
     * @param content of the file that is to be created / updated
     * @param focus if true, open this file in the IDE
     * @param replaceExisting if false, will only allow creation of a new file if it doesn't exist, it can't overwrite an existing file.
     * @return the reference to the PsiJavaFile
     */
    public static PsiJavaFile createJavaSourceFile(final Project project, final String contentRoot, final String packageName,
                                                   final  String clazzName, final String content, boolean focus,
                                                   boolean replaceExisting) {
        PsiJavaFile newPsiFile = null;
        String fileName = clazzName + ".java";
        VirtualFile sourceRoot = StudioPsiUtils.getSourceDirectoryForContentRoot(project, project.getBasePath(), contentRoot, StudioPsiUtils.SRC_MAIN_JAVA_CODE);
        if (sourceRoot == null) {
            Thread thread = Thread.currentThread();
            LOG.warn("Studio: Serious: createJavaSourceFile cant find sourceRoot for a contentRoot of " + contentRoot + " and directory " + StudioPsiUtils.SRC_MAIN_JAVA_CODE + " trace:" + Arrays.toString(thread.getStackTrace()));
        } else {
            PsiDirectory baseDir = PsiDirectoryFactory.getInstance(project).createDirectory(sourceRoot);
            PsiDirectory myPackage = StudioPsiUtils.createPackage(baseDir, packageName);

            PsiFile oldPsiFile = myPackage.findFile(fileName);
            newPsiFile = (PsiJavaFile) PsiFileFactory.getInstance(project).createFileFromText(
                    fileName,
                    FileTypeManager.getInstance().getFileTypeByExtension(UiContext.JAVA_FILE_EXTENSION),
                    content);

            if (oldPsiFile == null || ! areEqualIgnoringWhitespace(newPsiFile.getText(), oldPsiFile.getText())) {
                if (oldPsiFile != null) {
                    if (!replaceExisting) {
                        return null;
                    }
                    // Recreate
                    oldPsiFile.delete();
                }
                newPsiFile = (PsiJavaFile) myPackage.add(newPsiFile);
                standardJavaFormatting(project, newPsiFile);
            } else {
                newPsiFile = (PsiJavaFile) oldPsiFile;
                LOG.info("File " + clazzName + " unchanged, not re-saving");
            }
            if (focus) {
                newPsiFile.navigate(true); // Open the newly created file
            }
        }
        return newPsiFile;
    }

//    public static void replaceExistingFile(final Project project, final String contentRoot, final String packageName, final  String clazzName, final String content, boolean focus, boolean replaceExisting) {
//        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
//        WriteCommandAction.runWriteCommandAction(project, () -> {
//            PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
//            Document document = psiDocumentManager.getDocument(psiFile);
//
//            if (document != null) {
//                // Modify the document content
//                String newContent = "Your new content here";
//                document.setText(newContent);
//
//                // Ensure changes are committed to PSI
//                psiDocumentManager.commitDocument(document);
//            }
//        });
//    }





    public static PsiFile createJsonModelFile(final Project project, final String contentRoot, final String content) {
        return createFile(project, contentRoot, SRC_MAIN, JSON_MODEL_SUB_DIR, MODEL_JSON, content, false);
    }

    /**
     * Generic method to validate and save a file to disk
     * @param project to be updated
     * @param contentRoot for this project
     * @param sourceRootDir for this file
     * @param subDir under sourceRoot
     * @param fileNameWithExtension to be saved
     * @param content of the new file
     * @param focus if true, set the IDE focus on this file
     * @return the PsiFile reference to the created file.
     */
    public static PsiFile createFile(final Project project, final String contentRoot, final String sourceRootDir,
                                     final String subDir, final String fileNameWithExtension, final String content, boolean focus) {
        PsiFile newPsiFile = null;
        PsiDirectory sourceRoot = StudioPsiUtils.getOrCreateDirectoryInContentRoot(project, contentRoot, sourceRootDir);
        if (sourceRoot == null) {
            //@todo add this to system alerts in Intellij
            LOG.warn("STUDIO: SERIOUS: Could no create file [" + fileNameWithExtension + "] in contentRoot [" + contentRoot + "] sourceRootDir [" + sourceRootDir + "] subDir [" + subDir + "]");
        } else {
            PsiDirectory directory = sourceRoot;
            if (subDir != null) {
                directory = StudioPsiUtils.createOrGetDirectory(sourceRoot, subDir);
            }

            PsiFile oldPsiFile = directory.findFile(fileNameWithExtension) ;
            String fileType = FilenameUtils.getExtension(fileNameWithExtension);
            newPsiFile = PsiFileFactory.getInstance(project).createFileFromText(
                    fileNameWithExtension,
                    FileTypeManager.getInstance().getFileTypeByExtension(fileType),
                    content);
            // When you add the file to the directory, you need the resulting psiFile not the one you sent in.
            if (oldPsiFile == null || ! areEqualIgnoringWhitespace(newPsiFile.getText(), oldPsiFile.getText())) {
                if (oldPsiFile != null) {
                    oldPsiFile.delete();
                }
                newPsiFile = (PsiFile)directory.add(newPsiFile);
                standardPropertiesFormatting(project, newPsiFile);
            } else {
                newPsiFile = oldPsiFile;
                LOG.info("File " + fileNameWithExtension + " unchanged, not re-saving");
            }

            if (focus) {
                newPsiFile.navigate(true); // Open the newly created file
            }
        }
        return newPsiFile;
    }

    /**
     * Fail fast string comparison ignoring white space
     * @TODO maybe allow white space between quotes
     * @param oldString to be compared
     * @param newString to be compared
     * @return true if both string are equal, ignoring any whitespace
     */
    public static boolean areEqualIgnoringWhitespace(String oldString, String newString) {
        if (oldString == null || newString == null) {
            return false; // Handle null values if needed
        }

        int ii = 0, jj = 0;
        int len1 = oldString.length(), len2 = newString.length();

        while (ii < len1 && jj < len2) {
            // Skip whitespace in str1
            while (ii < len1 && Character.isWhitespace(oldString.charAt(ii))) {
                ii++;
            }
            // Skip whitespace in str2
            while (jj < len2 && Character.isWhitespace(newString.charAt(jj))) {
                jj++;
            }

            // If one string has reached its end but the other has not
            if (ii >= len1 || jj >= len2) {
                break;
            }

            // Compare characters
            if (oldString.charAt(ii) != newString.charAt(jj)) {
                return false; // Found a mismatch
            }

            ii++;
            jj++;
        }

        // Skip remaining whitespace in both strings
        while (ii < len1 && Character.isWhitespace(oldString.charAt(ii))) {
            ii++;
        }
        while (jj < len2 && Character.isWhitespace(newString.charAt(jj))) {
            jj++;
        }

        // If both indices have reached the end, strings are equal
        return ii == len1 && jj == len2;
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
        pipsiIkasanModel.saveModelJsonToDisk();
        pipsiIkasanModel.asynchGenerateSourceFromModelJsonInstanceAndSaveToDisk();
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
    public static VirtualFile getSourceDirectoryForContentRoot(Project project, final String basePath, final String contentRoot, String relativeRootDir) {
//        String targetDirectory = project.getBasePath() + contentRoot + "/" + relativeRootDir;
        String targetDirectory = basePath + contentRoot + (relativeRootDir != null ? "/" + relativeRootDir : "");
        VirtualFile sourceCodeRoot = null;
        VirtualFile[] srcRootVFiles = ProjectRootManager.getInstance(project).getContentSourceRoots();
        for (VirtualFile vFile : srcRootVFiles) {
            if (vFile.toString().endsWith(targetDirectory)) {
                sourceCodeRoot = vFile;
                break;
            }
        }

        // Try any root that maybe contain this directory
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
            LOG.warn("STUDIO: WARN: Could not find any source roots for project [" + project + "] and contentRoot [" + contentRoot + "] and relativeRoot [" + relativeRootDir + "] trace:[" + Arrays.toString(thread.getStackTrace())+"]");
        }
        return sourceCodeRoot;
    }

    /**
     * Get the PsiFile that refers to the model.json for this project
     * @param project to search
     * @return a PsiFile reference to the model.json
     */
    public static PsiFile getModelJsonPsiFile(final Project project) {
        AtomicReference<PsiFile> jsonModel = new AtomicReference<>();
        VirtualFile selectedContentRoot = getSpecificContentRoot(project, GENERATED_CONTENT_ROOT);
        if (selectedContentRoot != null) {
            VirtualFile virtualModelJson = selectedContentRoot.findFileByRelativePath(JSON_MODEL_FULL_PATH);
            if (virtualModelJson != null) {
                ApplicationManager.getApplication().runReadAction(() -> jsonModel.set(PsiManager.getInstance(project).findFile(virtualModelJson)));
            } else {
                LOG.warn("STUDIO: Could not get virtual model.json from path " + JSON_MODEL_FULL_PATH);
            }
            if (jsonModel.get() == null) {
                LOG.warn("STUDIO: Could not get psi file from path " + JSON_MODEL_FULL_PATH);
            }
        }
        return jsonModel.get();
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

    /**
     * Attempt to access the leaf of 'target' by traversing each directory downwards from root.
     * @param root One of the project roots e.f. ~/ws/proj
     * @param target A dir or subdir off that root e.g. 'src' or 'src/main/model'
     * @return The PsiDirectory that reflects the last directory in target or nulll if not found
     */
    public static PsiDirectory getDirectory(PsiDirectory root, String target) {
        PsiDirectory returnDirectory = root;
        if (root != null && target != null && !target.isEmpty()) {
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

    public static PsiDirectory getOrCreateDirectoryInContentRoot(final Project project, final String contentRoot, final String relativeRootDir) {
        PsiDirectory targetDir = null;

        // First, does it exist
        VirtualFile sourceCodeRoot = StudioPsiUtils.getSourceDirectoryForContentRoot(project, project.getBasePath(), contentRoot, relativeRootDir);

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
        if (contentRootVFiles.length != 1) {
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
            newDirectory = parent.findSubdirectory(newDirectoryName);
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
        VirtualFile sourceRoot = StudioPsiUtils.getSourceDirectoryForContentRoot(project, project.getBasePath(), contentRoot, StudioPsiUtils.SRC_MAIN_JAVA_CODE);
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
