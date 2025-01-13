package org.ikasan.studio.ui.model;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessHandlerFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.ikasan.studio.StudioRuntimeException;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.IkasanPomModel;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.psi.PIPSIIkasanModel;
import org.ikasan.studio.ui.viewmodel.AbstractViewHandlerIntellij;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import com.intellij.openapi.roots.OrderEnumerator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.ikasan.studio.core.model.ikasan.instance.IkasanPomModel.MAVEN_COMPILER_SOURCE;
import static org.ikasan.studio.core.model.ikasan.instance.IkasanPomModel.MAVEN_COMPILER_TARGET;
import static org.ikasan.studio.ui.StudioUIUtils.displayIdeaWarnMessage;
import static org.ikasan.studio.ui.model.psi.PIPSIIkasanModel.MODULE_PROPERTIES_FILENAME_WITH_EXTENSION;


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
     * @param projectKey essentially project.getName(), we NEVER pass project because the IDE can refresh at any time.
     * @return the studio representation of the POM
     */
    public static IkasanPomModel pomLoadFromVirtualDisk(String projectKey, String containingDirectory) {
        IkasanPomModel ikasanPomModel = null;
        Model model;
        PsiFile pomPsiFile = pomGetTopLevel(projectKey, containingDirectory);
        if (pomPsiFile != null) {
            try (Reader reader = new StringReader(pomPsiFile.getText())) {
                MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
                model = xpp3Reader.read(reader);
                ikasanPomModel = new IkasanPomModel(model);
                UiContext.setIkasanPomModel(UiContext.getProject(projectKey).getName(), ikasanPomModel);
            } catch (IOException | XmlPullParserException ex) {
                LOG.warn("STUDIO: Unable to load project ikasanPomModel", ex);
            }
        }
        return ikasanPomModel;
    }

    /**
     * Get the PsiFile that refers to the model.json for this project
     * @param projectKey essentially project.getName(), we NEVER pass project because the IDE can refresh at any time.
     * @return a PsiFile reference to the model.json
     */
    public static PsiFile getModelJsonPsiFile(final String projectKey) {
        AtomicReference<PsiFile> jsonModel = new AtomicReference<>();
        VirtualFile selectedContentRoot = getSpecificContentRootFromCache(projectKey, GENERATED_CONTENT_ROOT);
        if (selectedContentRoot != null) {
            VirtualFile virtualModelJson = selectedContentRoot.findFileByRelativePath(JSON_MODEL_FULL_PATH);
            if (virtualModelJson != null) {
                ApplicationManager.getApplication().runReadAction(() -> jsonModel.set(PsiManager.getInstance(UiContext.getProject(projectKey)).findFile(virtualModelJson)));
            } else {
                LOG.warn("STUDIO: Could not get virtual model.json from path " + JSON_MODEL_FULL_PATH);
            }
            if (jsonModel.get() == null) {
                LOG.warn("STUDIO: Could not get psi file from path " + JSON_MODEL_FULL_PATH);
            }
        }
        return jsonModel.get();
    }

    /**
     * Load the content of the application properties file
     * @param projectKey essentially project.getName(), we NEVER pass project because the IDE can refresh at any time.
     * @return a map of the Ikasan application properties, typically located in application.properties
     */
    public static Map<String, String> getApplicationPropertiesMapFromVirtualDisk(String projectKey) {
        Map<String, String> applicationProperties = null;
        VirtualFile selectedContentRoot = getSpecificContentRootFromCache(projectKey, StudioPsiUtils.GENERATED_CONTENT_ROOT);
        if (selectedContentRoot != null) {
            PsiFile applicationPropertiesVF = getPsiFileFromPath(projectKey, selectedContentRoot, APPLICATION_PROPERTIES_FULL_PATH);
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
     * @param projectKey essentially project.getName(), we NEVER pass project because the IDE can refresh at any time.
     */
    public static void synchGenerateModelInstanceFromJSON(String projectKey) {
        PsiFile jsonModelPsiFile = StudioPsiUtils.getModelJsonPsiFile(projectKey);
        if (jsonModelPsiFile != null) {
            String json = ApplicationManager.getApplication().runReadAction((Computable<String>) jsonModelPsiFile::getText);
            Module newModule = null;
            try {
                newModule = ComponentIO.deserializeModuleInstanceString(json, JSON_MODEL_FULL_PATH);
            } catch (StudioBuildException se) {
                LOG.warn("STUDIO: SERIOUS: during resetModelFromDisk, reported when reading " + StudioPsiUtils.JSON_MODEL_FULL_PATH + " message: " + se.getMessage() + " trace: " + Arrays.asList(se.getStackTrace()));
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
     * @param projectKey essentially project.getName(), we NEVER pass project because the IDE can refresh at any time.
     * @param newDependencies to be added, a map of Dependency.getManagementKey() -> Dependency
     */
    public static void checkForDependencyChangesAndSaveIfChanged(String projectKey, Set<Dependency> newDependencies) {
        IkasanPomModel ikasanPomModel;
        if (newDependencies != null && !newDependencies.isEmpty()) {
            ikasanPomModel = pomLoadFromVirtualDisk(projectKey, UiContext.getProject(projectKey).getName()); // Have to load each time because might have been independently updated.

            if (ikasanPomModel != null) {
                for (Dependency newDependency : newDependencies) {
                    ikasanPomModel.checkIfDependancyAlreadyExists(newDependency);
                }
                pomAddStandardProperties(ikasanPomModel);
                if (ikasanPomModel.isDirty()) {
                    createPomFile(projectKey, "", "", ikasanPomModel.getModelAsString());
                    MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(UiContext.getProject(projectKey));
                    if (mavenProjectsManager != null) {
                        mavenProjectsManager.forceUpdateAllProjectsOrFindAllAvailablePomFiles();
                    }
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
     * Attempt to get the top level pom for the project
     * @param projectKey essentially project.getName(), we NEVER pass project because the IDE can refresh at any time.
     * @return A PsiFile handle to the pom or null
     */
    public static PsiFile pomGetTopLevel(String projectKey, String containingDirectory) {
        VirtualFile virtualProjectRoot = getProjectBaseDir(projectKey);
        if (virtualProjectRoot == null) {
            Thread thread = Thread.currentThread();
            LOG.warn("STUDIO: Serious: createSourceFile cant find basePath for project " + projectKey + " trace:" + Arrays.toString(thread.getStackTrace()));
        } else {
            VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByUrl("file://" + virtualProjectRoot.getPath() + "/" + POM_XML);
            if (virtualFile != null) {
                PsiFile returnFile = null;
                try {
                    returnFile = ReadAction.compute(() -> PsiManager.getInstance(UiContext.getProject(projectKey)).findFile(virtualFile));
                } catch (Exception ee) {
                    // ReadAction.compute can swallow exceptions if not explicitly caught
                    LOG.warn("STUDIO: WARN: The read action of pomGetTopLevel for params " +
                    " projectKey [" + projectKey + "] containingDirectory [" + containingDirectory + "] " +
                    " threw an exception, message " + ee.getMessage() + " Trace [" + Arrays.asList(ee.getStackTrace()));
                }
                return returnFile;
            }
        }
        LOG.warn("STUDIO: WARNING: Could not find any " + POM_XML + " " + UiContext.getProject(projectKey).getName() + " project root " + virtualProjectRoot.getPath());
        return null;
    }

    /**
     * Create and save a java source file
     * @param projectKey essentially project.getName(), we NEVER pass project because the IDE can refresh at any time.
     * @param contentRoot for the file, typically generated or user
     * @param subDir under the sourceRoodDir, this can be dot delimited a.b.c
     * @param content of the file that is to be created / updated
     */
    // StudioPsiUtils.createPomFile(projectKey, StudioPsiUtils.GENERATED_CONTENT_ROOT, "h2", h2StartStopPomString);
    public static void createPomFile(final String projectKey, final String contentRoot, final String subDir,final String content) {
        if (projectKey == null || content == null) {
            LOG.warn("STUDIO: SERIOUS: Invalid calll to createJavaSourceFile projectKey [" + projectKey + "] contentRoot [" + contentRoot +
                    "] subDir [" + subDir + "] content [" + content + "]");
        }
        String relativeFilePath =
                (!isBlank(contentRoot) ? contentRoot : "") +
                        (!isBlank(subDir) ? "/" + subDir : "") +
                        "/" + POM_XML;

        createFileWithDirectories(projectKey,
                relativeFilePath,
                content,
                null);
    }

    /**
     * Create and save a java source file
     * @param projectKey essentially project.getName(), we NEVER pass project because the IDE can refresh at any time.
     * @param contentRoot for the file, typically generated or user
     * @param sourceRootDir for this file  e.g. src/main, src/test
     * @param subDir under the sourceRoodDir, this can be dot delimited a.b.c
     * @param clazzName for the java file
     * @param content of the file that is to be created / updated
     * @param focus if true, open this file in the IDE
     * @param replaceExisting if false, will only allow creation of a new file if it doesn't exist, it can't overwrite an existing file.
     */
    public static void createJavaSourceFile(final String projectKey, final String contentRoot, final String sourceRootDir, final String subDir,
                                            final  String clazzName, final String content, boolean focus,
                                            final boolean replaceExisting, AbstractViewHandlerIntellij componentViewHandler) {
        if (projectKey == null || content == null || sourceRootDir == null || subDir == null || clazzName == null || content == null) {
            LOG.warn("STUDIO: SERIOUS: Invalid calll to createJavaSourceFile projectKey [" + projectKey + "] contentRoot [" + contentRoot +
                    "] sourceRootDir [" + sourceRootDir + "] subDir [" + subDir + "] clazzName [" + clazzName + "] content [" + content + "]");
        }
        String relativeFilePath =
                (!isBlank(contentRoot) ? contentRoot : "") +
                (!isBlank(sourceRootDir) ? "/" + sourceRootDir : "")  +
                (!isBlank(subDir) ? "/" + subDir.replace(".", "/") : "") +
                "/" + clazzName+".java";
        createFileWithDirectories(projectKey,
                relativeFilePath,
                content,
                componentViewHandler);
    }


    /**
     * Conveniance method to create a model.json for the supplied content. The location of the
     * model.json is standard so does not need to be supplied
     * @param projectKey essentially project.getName(), we NEVER pass project because the IDE can refresh at any time.
     * @param content of the file that is to be created / updated
     */
    public static void createPropertiesFile(final String projectKey, final String content) {
        LOG.info("STUDIO: INFO: Saving properties, projectKey [" + projectKey + "] content size " + content.length() + " bytes");

        createFileWithDirectories(projectKey,
                GENERATED_CONTENT_ROOT + "/" + SRC_MAIN_RESOURCES + "/" + MODULE_PROPERTIES_FILENAME_WITH_EXTENSION,
                content, null);
    }

    /**
     * Conveniance method to create a model.json for the supplied content. The location of the
     * model.json is standard so does not need to be supplied
     * @param projectKey essentially project.getName(), we NEVER pass project because the IDE can refresh at any time.
     * @param content of the file that is to be created / updated
     */
    public static void createJsonModelFile(final String projectKey, final String content) {
        LOG.info("STUDIO: INFO: Saving Json Model, projectKey [" + projectKey + "] content size " + content.length() + " bytes");

        createFileWithDirectories(projectKey,
                GENERATED_CONTENT_ROOT + "/" + SRC_MAIN + "/" + JSON_MODEL_SUB_DIR + "/" + MODEL_JSON,
                content, null);
    }


    /**
     * Fail fast string comparison ignoring white space
     * {@code @TODO} maybe allow white space between quotes
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


    public static void refreshCodeFromModelAndCauseRedraw(String projectKey) {
        // @TODO MODEL
        refreshCodeFromModel(projectKey);
        causeRedraw(projectKey);
    }

    public static void refreshCodeFromModel(String projectKey) {
        PIPSIIkasanModel pipsiIkasanModel = UiContext.getPipsiIkasanModel(projectKey);
        pipsiIkasanModel.saveModelJsonToDisk();
        pipsiIkasanModel.asynchGenerateSourceFromModelJsonInstanceAndSaveToDisk();
    }

    public static void causeRedraw(String projectKey) {
        UiContext.getDesignerCanvas(projectKey).setInitialiseAllDimensions(true);
        UiContext.getDesignerCanvas(projectKey).repaint();
    }


    /**
     * Get the Virtual file for the project root
     * @param projectKey essentially project.getName(), we NEVER pass project because the IDE can refresh at any time.
     */
    public static VirtualFile getProjectBaseDir(String projectKey) {
        String basePath = UiContext.getProject(projectKey).getBasePath();
        return LocalFileSystem.getInstance().findFileByPath(basePath);
    }

    /**
     * Creates a file and all necessary directories in the IntelliJ VFS.
     *
     * @param projectKey essentially project.getName(), we NEVER pass project because the IDE can refresh at any time.
     * @param relativePath The relative path (including the file name) from the project's base directory.
     * @param fileContent The content to write into the file (optional).
     */
    public static void createFileWithDirectories(final String projectKey, final String relativePath,
                                                 final String fileContent, final AbstractViewHandlerIntellij componentViewHandler) {
        Project project = UiContext.getProject(projectKey);
        // Separate the path and file name
        int lastSeparatorIndex = relativePath.lastIndexOf('/');
        String directoryPath = lastSeparatorIndex == -1 ? "" : relativePath.substring(0, lastSeparatorIndex);
        String fileName = relativePath.substring(lastSeparatorIndex + 1);

        LOG.info("STUDIO: INFO: createFileWithDirectories " +
                "relativePath [" + relativePath + "]" +
                "] directoryPath [" + directoryPath + "]" +
                "] fileName [" + fileName + "]"
//                "] fileContent [" + fileContent + "]"
        );

        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                // Get the base directory of the project
                VirtualFile baseDir = project.getBaseDir();
                if (baseDir == null) {
                    throw new IllegalStateException("Project base directory is null");
                }
                // Create or locate the directories
                VirtualFile targetDir = createDirectories(baseDir, directoryPath);

                // Handle the file
                if (targetDir != null) {
                    VirtualFile file = targetDir.findChild(fileName);
                    if (file == null) {
                        // File does not exist; create it
                        file = targetDir.createChildData(StudioPsiUtils.class, fileName);
                    }
                    writeContentAndFormat(project, file, fileContent, componentViewHandler);
                }
            } catch (IOException e) {
                LOG.info("STUDIO: ERROR: createFileWithDirectories " + relativePath + " " + fileContent +
                        " message [" + e.getMessage() + "] stackTrace [" + Arrays.toString(e.getStackTrace())+ "]");
                throw new StudioRuntimeException("Failed to create file or directories", e);
            }
        });
    }

    private static void writeContentAndFormat(Project project, VirtualFile file, String fileContent, final AbstractViewHandlerIntellij componentViewHandler) {
        try {
            if (fileContent != null) {
                FileDocumentManager documentManager = FileDocumentManager.getInstance();
                Document document = documentManager.getDocument(file);

                if (document != null) {
                    // File is open in the editor; update the document
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        document.setText(fileContent);
                    });

                    // Commit the document to sync it with the PSI, otherwise format below will error
                    PsiDocumentManager.getInstance(project).commitDocument(document);

                    // Save the document to persist changes to the VirtualFile
                    documentManager.saveDocument(document);
                } else {
                    // File is not open in the editor; update the VirtualFile directly
                    WriteAction.run(() -> {
                        file.setBinaryContent(fileContent.getBytes(StandardCharsets.UTF_8));
                        file.refresh(false, false); // Refresh the file after modification
                    });
                }

                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);

                if (psiFile != null) {
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        // Format the Java file
                        if (file.getName().endsWith(".java")) {
                            JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiFile);
                        }
                        CodeStyleManager.getInstance(project).reformat(psiFile);
                    });
                    if (componentViewHandler != null) {
                        componentViewHandler.setPsiFile(psiFile);
                    }
                }
            }
        } catch (IOException e) {
            LOG.warn("STUDIO: ERROR: writeContentAndFormat " + file + " message [" + e.getMessage() + "] trace [" + Arrays.toString(e.getStackTrace())+ "]");
            throw new StudioRuntimeException("Failed to write or format the file", e);
        }
    }


    /**
     * Creates directories recursively in the IntelliJ VFS.
     *
     * @param baseDir     The starting directory (e.g., the project base directory).
     * @param relativePath The relative path to create.
     * @return The VirtualFile representing the final directory, or null if creation fails.
     * @throws IOException If an error occurs during directory creation.
     */
    private static VirtualFile createDirectories(VirtualFile baseDir, String relativePath) throws IOException {
        if (relativePath.isEmpty()) {
            return baseDir;
        }

        String[] parts = relativePath.split("/");
        VirtualFile currentDir = baseDir;

        for (String part : parts) {
            if (!part.isEmpty()) {
                VirtualFile child = currentDir.findChild(part);
                if (child == null) {
                    child = currentDir.createChildDirectory(StudioPsiUtils.class, part);
                }
                currentDir = child;
            }
        }

        // Refresh the directory after creation
        currentDir.refresh(true, true);
        return currentDir;
    }

private static final Map<String, VirtualFile> virtualRoots = new HashMap<>();
    /**
     * Get the source root that contains the supplied string, possible to get java source, resources or test
     * @param projectKey essentially project.getName(), we NEVER pass project because the IDE can refresh at any time.
     * @param relativeRootDir to look for e.g. main/java, main/resources
     * @return the rood of the module / source directory that contains the supplied string.
     */
    public static VirtualFile getExistingSourceDirectoryForContentRoot(String projectKey, final String basePath, final String contentRoot, String relativeRootDir) {
        String targetDirectory = basePath + contentRoot + (relativeRootDir != null ? "/" + relativeRootDir : "");
        LOG.info("STUDIO: INFO: getExistingSourceDirectoryForContentRoot basePath = " + basePath + " contentRoot" + contentRoot +
        " relativeRootDir = " + relativeRootDir + " targetDirectory = " + targetDirectory);
        String targetDirectoryKey = UiContext.getProject(projectKey).getName() + "-" + targetDirectory;

        VirtualFile sourceCodeRoot = checkVirtualRoots(targetDirectoryKey);
        if (sourceCodeRoot == null) {
            // ProjectRoots are more likely to provide an exact match
            ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(UiContext.getProject(projectKey));
            fileIndex.iterateContent(file -> {
                if (fileIndex.isInSource(file)) {
                    if (file.isValid() && file.isDirectory()) {
                        virtualRoots.put(UiContext.getProject(projectKey).getName() + "-" + file.getPath(), file);
                    }
                }
                return true; // Continue iterating
            });
        }
        sourceCodeRoot = checkVirtualRoots(targetDirectoryKey);
        if (sourceCodeRoot == null) {
            Thread thread = Thread.currentThread();
            LOG.warn("STUDIO: WARNING: Could not find any existing source roots for project [" + projectKey + "] and contentRoot [" + contentRoot + "] and relativeRoot [" + relativeRootDir + "] trace:[" + Arrays.toString(thread.getStackTrace())+"]");
        }
        return sourceCodeRoot;
    }

    /**
     * Helper method to check the virtualRoots cache to see if the targetDirectory can be found from the cache
     * @param targetDirectoryKey to search
     * @return the VirtualFileRoot corresponding to the key.
     */
    private static VirtualFile checkVirtualRoots(String targetDirectoryKey) {
        VirtualFile sourceCodeRoot = virtualRoots.get(targetDirectoryKey);
        if (sourceCodeRoot != null && !sourceCodeRoot.isValid() && !sourceCodeRoot.isDirectory()) {
            virtualRoots.remove(targetDirectoryKey);
            sourceCodeRoot = null;
        }
        return sourceCodeRoot;
    }


    public static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    /**
     * Get the virtual file representing the relative (to the project root) of the supplied file.
     * Note virtual files will miss any unsaved changes currently in the editor and may become invalid if a project is refreshed
     * @param projecKey  essentially project.getName(), we NEVER pass project because the IDE can refresh at any time.
     * @param relativeFilePath from the project root e.g. pom.xml, src/main/resources/application.properties
     * @return the virtual file reflecting the file path
     */
    public static VirtualFile getVirtualFile(String projecKey, String relativeFilePath) {
        return VfsUtil.findRelativeFile(relativeFilePath, getProjectBaseDir(projecKey));
    }

    /**
     * Read the physical file from the file system and return as a String.
     * Note that the VFS is used to obtain the file so any unsaved edits will be missed.
     * @param projecKey  essentially project.getName(), we NEVER pass project because the IDE can refresh at any time.
     * @param relativeFilePath from the project root e.g. pom.xml, src/main/resources/application.properties
     * @return the String contents of the file, or null if it does not exist
     */
    public static String readFileAsString(String projecKey, String relativeFilePath) {
        VirtualFile virtualFilePathPath = getVirtualFile(projecKey, relativeFilePath);
        if (virtualFilePathPath != null && virtualFilePathPath.isValid()) {
            return readVirtualFileAsString(virtualFilePathPath);
        } else {
            return null;
        }
    }

    /**
     * Read the physical file from the file system represented by the supplied virtual file
     * @param absoluteFilePath of the file to be read
     * @return the String contents of the file, or null if it does not exist
     */
    public static String readVirtualFileAsString(VirtualFile absoluteFilePath) {
        if (absoluteFilePath == null || !absoluteFilePath.exists()) {
            LOG.warn("STUDIO: WARN: readFileAsString invalid parameters absoluteFilePath " + absoluteFilePath +
                    " exists " + absoluteFilePath +
                    " StackTrace: " + Arrays.asList( Thread.currentThread().getStackTrace()));
        }

        try (InputStream is = absoluteFilePath.getInputStream();
             Scanner scanner = new Scanner(is, StandardCharsets.UTF_8)) {

            scanner.useDelimiter("\\A"); // Read entire file content
            return scanner.hasNext() ? scanner.next() : "";
        } catch (Exception ee) {
            LOG.warn("STUDIO: WARN: readFileAsString failed to read the file " + absoluteFilePath.getPresentableUrl() + " exception was " + ee.getMessage() +
                    " StackTrace: " + Arrays.asList( Thread.currentThread().getStackTrace()));
        }
        return null;
    }

    public static PsiFile getPsiFileFromPath(final String projectKey, VirtualFile root, String filePath) {
        PsiFile returnFile = null;

        if (root != null && filePath != null && filePath.length() > 1) {
            String fileName = FilenameUtils.getName(filePath);
            String directoryPath = FilenameUtils.getFullPathNoEndSeparator(filePath);
            PsiDirectory psiDirectory = getPsiDirectoryLeafFromPath(projectKey, root, directoryPath);
            if (psiDirectory != null) {
                returnFile = psiDirectory.findFile(fileName);
            }
        }
        return returnFile;
    }

    public static PsiDirectory getPsiDirectoryLeafFromPath(final String projectKey, VirtualFile root, String target) {
        if (root.isDirectory()) {
            return getPsiDirectoryLeafFromPath(PsiDirectoryFactory.getInstance(UiContext.getProject(projectKey)).createDirectory(root), target);
        }
        return null;
    }

    /**
     * Attempt to access the leaf of 'target' by traversing each directory downwards from root.
     * @param root One of the project roots e.f. ~/ws/proj
     * @param target A dir or subdir off that root e.g. 'src' or 'src/main/model'
     * @return The PsiDirectory that reflects the last directory in target or nulll if not found
     */
    public static PsiDirectory getPsiDirectoryLeafFromPath(PsiDirectory root, String target) {
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

    /**
     * Search the project root cache for the supplied content root.
     * Note, virtualFiles can become invalid if the IDE refreshes the project, use isValid() before use.
     * @param projectKey essentially project.getName(), we NEVER pass project because the IDE can refresh at any time.
     * @param contentRoot to ve searched for
     * @return the content root if its found, nulll otherwise.
     */
    protected static VirtualFile getSpecificContentRootFromCache(final String projectKey, final String contentRoot) {
        VirtualFile targetContentRoot = null;
        VirtualFile[] contentRootVFiles = ProjectRootManager.getInstance(UiContext.getProject(projectKey)).getContentRoots();

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
     * Ensures the directory exists at the specified level, creating it if necessary.
     *
     * @param projectKey essentially project.getName(), we NEVER pass project because the IDE can refresh at any time.
     * @param parentDir    The VirtualFile of the parent directory.
     * @param directoryName The name of the directory to ensure.
     * @return The PsiDirectory for the created or existing directory.
     */
    private static PsiDirectory ensureDirectoryExists(String projectKey, VirtualFile parentDir, String directoryName) {
        final VirtualFile[] newDir = new VirtualFile[1]; // To hold the created directory

        VirtualFile existingDir = parentDir.findChild(directoryName);
        WriteCommandAction.runWriteCommandAction(UiContext.getProject(projectKey), () -> {

            if (existingDir == null) {
                // Directory doesn't exist, create it
                try {
                    newDir[0] = parentDir.createChildDirectory(null, directoryName);
                } catch (IOException e) {
                    String messages = "An error attempting tp create directory " + directoryName + " message was " + e.getMessage();
                    displayIdeaWarnMessage(UiContext.getProject(projectKey).getName(), messages);
                    LOG.warn("STUDIO: SERIOUS: " + messages);
                }
            } else if (!existingDir.isDirectory()) {
                throw new IllegalStateException("A file with the name " + directoryName + " already exists but is not a directory.");
            } else {
                // Directory already exists
                newDir[0] = existingDir;
            }
        });

        // Wrap the VirtualFile as a PsiDirectory
        return PsiManager.getInstance(UiContext.getProject(projectKey)).findDirectory(newDir[0]);
    }


    /**
     * Find the directory representing the base package, remove any sub package (subdirectory) that is not in the subPackagesToKeep
     * @param projectKey essentially project.getName(), we NEVER pass project because the IDE can refresh at any time.
     * @param basePackage the package that contains the subpackage leaves
     * @param subPackagesToKeep a set of package names tha are valid i.e. you want to kepp
     */
    public static void deleteSubPackagesNotIn(String projectKey, final String contentRoot, final String basePackage, Set<String> subPackagesToKeep) {
        LOG.info("STUDIO: INFO: deleteSubPackagesNotIn will keep the following packages " + subPackagesToKeep);
        VirtualFile baseDir = getProjectBaseDir(projectKey);

        if (baseDir == null) {
            LOG.warn("Studio: WARN: Could not get project root for directory for project [" + projectKey + "]");
        } else {
            final VirtualFile sourceRoot = StudioPsiUtils.getExistingSourceDirectoryForContentRoot(projectKey, baseDir.getPath(), contentRoot, StudioPsiUtils.SRC_MAIN_JAVA_CODE);
            final PsiDirectory sourceRootDir = PsiDirectoryFactory.getInstance(UiContext.getProject(projectKey)).createDirectory(sourceRoot);

            CompletableFuture<PsiDirectory[]> leafPackageDirectoryFuture = CompletableFuture.supplyAsync(() ->
            {
                try {
                    ReadAction.compute(() -> {
                        PsiDirectory leafPackageDirectory = getDirectoryForPackage(sourceRootDir, basePackage);
                        return leafPackageDirectory.getSubdirectories();
                    });
                } catch (Exception ee) {
                    // ReadAction.compute can swallow exceptions if not explicitly caught
                    LOG.warn("STUDIO: SERIOUS: The read action of pomGetTopLevel for params " +
                            " projectKey [" + projectKey + "] contentRoot [" + contentRoot + "] basePackage + [" + basePackage + "] subPackagesToKeep [" + subPackagesToKeep + "] " +
                            " threw an exception, message " + ee.getMessage() + " Trace [" + Arrays.asList(ee.getStackTrace()));
                }
                return null;
            });
            leafPackageDirectoryFuture.thenAccept(resultsObject ->
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (resultsObject == null) {
                        LOG.warn("STUDIO: SERIOUS: The previous read action of pomGetTopLevel returned null for params " +
                                " projectKey [" + projectKey + "] contentRoot [" + contentRoot + "] basePackage + [" + basePackage + "] subPackagesToKeep [" + subPackagesToKeep + "] " +
                                " trace [" + Arrays.asList(Thread.currentThread().getStackTrace()));
                    } else {
                        for (PsiDirectory directory : resultsObject) {
                            if (!subPackagesToKeep.contains(directory.getName())) {
                                LOG.info("STUDIO: Deleting directory " + directory.getName() + " the basePackage " + basePackage + " should only have these directories " + subPackagesToKeep);
                                directory.delete();
                            }
                        }
                    }

                })
            );
        }
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

    /**
     * Execute a Java command line, displaying the results in the Application output Window
     * @param projectKey essentially project.getName(), we NEVER pass project because the IDE can refresh at any time.
     * @param virtualFile of the class to be executed
     * @param fullyQualifiedClassName of the class to be executed
     */
    public static void runClassFromEditor(String projectKey, VirtualFile virtualFile, String fullyQualifiedClassName) {
        Project project = UiContext.getProject(projectKey);
        // Perform slow operations on a background thread
        AppExecutorUtil.getAppExecutorService().execute(() -> {
            try {
                // Retrieve the Module of the file
                com.intellij.openapi.module.Module module = ModuleUtilCore.findModuleForFile(virtualFile, project);
                if (module == null) {
                    StudioUIUtils.displayIdeaWarnMessage(projectKey, "Problems were experienced getting the Intellij module, launch is unavailable at this time");
                    LOG.warn("STUDIO: SERIOUS: runClassFromEditor could not find module for virtual files [" + virtualFile.getPath() + "] for project [" + projectKey + "]");
                    return;
                }

                // Get the output directory for the module
                CompilerModuleExtension moduleExtension = CompilerModuleExtension.getInstance(module);
                if (moduleExtension == null || moduleExtension.getCompilerOutputPath() == null) {
                    StudioUIUtils.displayIdeaWarnMessage(projectKey, "Problems were experienced getting the moduleExtension, launch is unavailable at this time");
                    LOG.warn("STUDIO: SERIOUS: runClassFromEditor could not find moduleExtension for virtual file [" + virtualFile.getPath() +
                            "] for project [" + projectKey + "] moduleExtension [" + moduleExtension + "]");
                    return;
                }

                // Retrieve the SDK for the module
                Sdk moduleSdk = ModuleRootManager.getInstance(module).getSdk();
                if (moduleSdk == null || !(moduleSdk.getSdkType() instanceof JavaSdk)) {
                    StudioUIUtils.displayIdeaWarnMessage(projectKey, "Problems were experienced getting the Intellij sdk, launch is unavailable at this time");
                    LOG.warn("STUDIO: SERIOUS: runClassFromEditor could not find module SDK for virtual file [" + virtualFile.getPath() +
                            "] for project [" + projectKey + "] moduleSDK [" + moduleSdk + "]");
                    return;
                }

                // Locate the Java executable
                String javaExecutablePath = JavaSdk.getInstance().getVMExecutablePath(moduleSdk);
                if (javaExecutablePath == null) {
                    StudioUIUtils.displayIdeaWarnMessage(projectKey, "Problems were experienced getting the JDK, launch is unavailable at this time");
                    LOG.warn("STUDIO: SERIOUS: runClassFromEditor could not find module java executable path for virtual file [" + virtualFile.getPath() +
                            "] for project [" + projectKey + "] moduleSDK [" + moduleSdk + "]");
                    return;
                }

                String userClasses = project.getBasePath() + USER_CONTENT_ROOT + "/target/classes";
                // Resolve the classpath for the module
                List<String> classPathElements = new ArrayList<>();
                OrderEnumerator.orderEntries(module).recursively().forEachLibrary(library -> {
                    VirtualFile[] files = library.getFiles(OrderRootType.CLASSES);
                    for (VirtualFile fileEntry : files) {
                        String path = fileEntry.getPath();
                        // !/ is appended when OrderEnumerator retrieves the URLs, need to strip
                        if (path.endsWith("!/")) {
                            path = path.substring(0, path.length() - 2);
                        }
                        classPathElements.add(path);
                    }
                    classPathElements.add(userClasses);
                    return true;
                });

                String outputPath = moduleExtension.getCompilerOutputPath().getPath();
                classPathElements.add(outputPath);
                String classPath = String.join(File.pathSeparator, classPathElements);
                GeneralCommandLine commandLine = new GeneralCommandLine()
                        .withExePath(javaExecutablePath)
                        .withParameters("-cp", classPath, fullyQualifiedClassName);

                // Switch back to the EDT to execute UI-related tasks
                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        ConsoleView consoleView = new ConsoleViewImpl(project, true);
                        ProcessHandler processHandler = ProcessHandlerFactory.getInstance().createProcessHandler(commandLine);
                        consoleView.attachToProcess(processHandler);
                        RunContentDescriptor descriptor = new RunContentDescriptor(consoleView, processHandler, consoleView.getComponent(), "Console Output");
                        RunContentManager.getInstance(project)
                                .showRunContent(DefaultRunExecutor.getRunExecutorInstance(), descriptor);
                        UiContext.setAppserverProcessHandle(projectKey, processHandler);
                        processHandler.startNotify();
                    } catch (Exception e) {
                        StudioUIUtils.displayIdeaWarnMessage(projectKey, "Problems were launching the application, launch is unavailable at this time");
                        LOG.warn("STUDIO: SERIOUS: runClassFromEditor threw an exception during invokeLater for virtual file [" + virtualFile.getPath() +
                                "] for project [" + projectKey + "] message [" + e.getMessage() + "] trace [" + Arrays.asList(e.getStackTrace()) + "]");
                    }
                }, ModalityState.defaultModalityState());

            } catch (Exception e) {
                StudioUIUtils.displayIdeaWarnMessage(projectKey, "Problems were launching the application, launch is unavailable at this time");
                LOG.warn("STUDIO: SERIOUS: runClassFromEditor threw an exception during getAppExecutorService for virtual file [" + virtualFile.getPath() +
                        "] for project [" + projectKey + "] message [" + e.getMessage() + "] trace [" + Arrays.asList(e.getStackTrace()) + "]");
            }
        });
    }

    /**
     * Attempt to stop the java process currently running in the console.
     * @param projectKey essentially project.getName(), we NEVER pass project because the IDE can refresh at any time.
     */
    public static void stopRunningProcess(String projectKey) {
        ProcessHandler processHandler = UiContext.getAppserverProcessHandle(projectKey);
        if (processHandler != null && !processHandler.isProcessTerminated()) {
            processHandler.destroyProcess(); // Stops the process
        }
    }
}