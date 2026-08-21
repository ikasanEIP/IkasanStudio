package org.ikasan.studio.ui.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
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
import org.ikasan.studio.ui.model.psi.GenerationRequest;
import org.ikasan.studio.ui.viewmodel.AbstractViewHandlerIntellij;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.ikasan.studio.core.model.ikasan.instance.IkasanPomModel.MAVEN_COMPILER_SOURCE;
import static org.ikasan.studio.core.model.ikasan.instance.IkasanPomModel.MAVEN_COMPILER_TARGET;
import static org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta.APPLICATION_PACKAGE_NAME;
import static org.ikasan.studio.ui.model.psi.PIPSIIkasanModel.MODULE_PROPERTIES_FILENAME_WITH_EXTENSION;


public class StudioPsiUtils {
    private static final Logger LOG = Logger.getInstance("#StudioPsiUtils");
//    public static final String TEMP_CONTENT_ROOT = "temp://";
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
     * @param project the Intellij project instance project.getIName(), we NEVER pass project because the IDE can refresh at any time.
     * @return the studio representation of the POM
     */
    public static IkasanPomModel pomLoadFromVirtualDisk(Project project) {
        IkasanPomModel ikasanPomModel = null;
        Model model;
        UiContext uiContext = project.getService(UiContext.class);
        PsiFile pomPsiFile = pomGetTopLevel(project, project.getName());
        if (pomPsiFile != null) {
            String pomText = ReadAction.compute(pomPsiFile::getText);
            try (Reader reader = new StringReader(pomText)) {
                MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
                model = xpp3Reader.read(reader);
                ikasanPomModel = new IkasanPomModel(model);
                uiContext.setIkasanPomModel(ikasanPomModel);
            } catch (IOException | XmlPullParserException ex) {
                LOG.warn("STUDIO: Unable to load project ikasanPomModel", ex);
            }
        }
        return ikasanPomModel;
    }

    /**
     * Get the PsiFile that refers to the model.json for this project
     * @param project is the Intellij project instance
     * @return a PsiFile reference to the model.json
     */
    public static PsiFile getModelJsonPsiFile(final Project project) {
        AtomicReference<PsiFile> jsonModel = new AtomicReference<>();
        VirtualFile selectedContentRoot = getSpecificContentRootFromCache(project);
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

    /**
     * Load the content of the application properties file
     * @param project is the Intellij project instance
     * @return a map of the Ikasan application properties, typically located in application.properties
     */
    public static Map<String, String> getApplicationPropertiesMapFromVirtualDisk(Project project) {
        Map<String, String> applicationProperties = null;
        VirtualFile selectedContentRoot = getSpecificContentRootFromCache(project);
        if (selectedContentRoot != null) {
            PsiFile applicationPropertiesVF = getPsiFileFromPath(project, selectedContentRoot, APPLICATION_PROPERTIES_FULL_PATH);
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
     *             synchGenerateModelInstanceFromJSON(project);
     *             ... any other actions relying on above action completion
     *         });
     * @param project is the Intellij project instance
     */
    public static void synchGenerateModelInstanceFromJSON(Project project) {
        PsiFile jsonModelPsiFile = StudioPsiUtils.getModelJsonPsiFile(project);
        if (jsonModelPsiFile != null) {
            UiContext uiContext = project.getService(UiContext.class);
            String json = ApplicationManager.getApplication().runReadAction((Computable<String>) jsonModelPsiFile::getText);
            Module newModule = null;
            try {
                newModule = ComponentIO.deserializeModuleInstanceString(json, JSON_MODEL_FULL_PATH);
            } catch (StudioBuildException se) {
                LOG.warn("STUDIO: SERIOUS: during resetModelFromDisk, reported when reading " + StudioPsiUtils.JSON_MODEL_FULL_PATH + " message: " + se.getMessage() + " trace: " + Arrays.asList(se.getStackTrace()));
                StudioUIUtils.displayIdeaErrorMessage(project, "Error: Please fix model.json then click 'Load', error was [" + se.getMessage() + "]");
                // The dumb module should contain just enough to prevent the plugin from crashing
                uiContext.setIkasanModule(Module.getDumbModuleVersion());
            }
            if (newModule == null) {
                Thread thread = Thread.currentThread();
                LOG.warn("STUDIO: SERIOUS: Attempt to set model resulted in a null model" + Arrays.toString(thread.getStackTrace()));
            }

            // When the project is initialised, capture the package option and set in cache for later use in module initilisation.
            String applicationPackageName = StudioPsiUtils.getJsonAttribute(json, APPLICATION_PACKAGE_NAME);
            if (applicationPackageName != null) {
                uiContext.getOptions().setPackageName(applicationPackageName);
            }
            uiContext.setIkasanModule(newModule);
        } else {
            LOG.info("STUDIO: Could not read the " + JSON_MODEL_FULL_PATH + ", this is probably a new project");
        }
    }

    /**
     * Add the new dependencies IF they are not already in the pom
     * @param project is the Intellij project instance
     * @param newDependencies to be added, a map of Dependency.getManagementKey() -> Dependency
     * @param metaVersion of the module (e.g. "V3.3.8", "V4.0.x"), used to pick the matching JDK level
     */
    public static void checkForDependencyChangesAndSaveIfChanged(Project project, Set<Dependency> newDependencies, String metaVersion) {
        IkasanPomModel ikasanPomModel;
        if (newDependencies != null && !newDependencies.isEmpty()) {
            ikasanPomModel = pomLoadFromVirtualDisk(project); // Have to load each time because might have been independently updated.

            if (ikasanPomModel != null) {
                for (Dependency newDependency : newDependencies) {
                    ikasanPomModel.checkIfDependancyAlreadyExists(newDependency);
                }
                pomAddStandardProperties(ikasanPomModel, metaVersion);
                if (ikasanPomModel.isDirty()) {
                    createPomFile(project, "", "", ikasanPomModel.getModelAsString());
                    MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(project);
                    if (mavenProjectsManager != null) {
                        mavenProjectsManager.forceUpdateAllProjectsOrFindAllAvailablePomFiles();
                    }
                }
            } else {
                LOG.warn("STUDIO: WARN: checkForDependencyChangesAndSaveIfChanged invoked but ikasanPomModel was null project [" + project +"] newDependencies [" + newDependencies + "]");
            }
        }
    }


    /**
     * Add in the standard properties for the ikasanPomModel, based on project level config e.g. JDK
     * @param ikasanPomModel is the root level ikasanPomModel to be updated
     * @param metaVersion of the module, used to pick the matching JDK level
     */
    private static void pomAddStandardProperties(IkasanPomModel ikasanPomModel, String metaVersion) {
        String javaCompilerLevel = resolveJavaCompilerLevel(metaVersion);
        ikasanPomModel.addProperty(MAVEN_COMPILER_TARGET, javaCompilerLevel);
        ikasanPomModel.addProperty(MAVEN_COMPILER_SOURCE, javaCompilerLevel);
    }

    /**
     * Each Ikasan core major version has its own minimum/target JDK level (confirmed against the actual
     * ikasaneip/pom.xml of each): V3.x core targets JDK 11, V4.x core targets JDK 17 (and already uses
     * JDK 15+ text blocks internally). Default to 11 for anything else (e.g. VHS3.3.x, which is V3-based).
     * @param metaVersion of the module (e.g. "V3.3.8", "V4.0.x")
     * @return the maven.compiler.source/target value to use
     */
    private static String resolveJavaCompilerLevel(String metaVersion) {
        if (metaVersion != null && metaVersion.startsWith("V4")) {
            return "17";
        }
        return "11";
    }

    /**
     * Attempt to get the top level pom for the project
     * @param project is the Intellij project instance
     * @return A PsiFile handle to the pom or null
     */
    public static PsiFile pomGetTopLevel(Project project, String containingDirectory) {
        VirtualFile virtualProjectRoot = getProjectBaseDir(project);
        if (virtualProjectRoot == null) {
            Thread thread = Thread.currentThread();
            LOG.warn("STUDIO: Serious: createSourceFile cant find basePath for project " + project + " trace:" + Arrays.toString(thread.getStackTrace()));
        } else {
            VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByUrl("file://" + virtualProjectRoot.getPath() + "/" + POM_XML);
            if (virtualFile != null) {
                PsiFile returnFile = null;
                try {
                    returnFile = ReadAction.compute(() -> PsiManager.getInstance(project).findFile(virtualFile));
                } catch (Exception ee) {
                    // ReadAction.compute can swallow exceptions if not explicitly caught
                    LOG.warn("STUDIO: WARN: The read action of pomGetTopLevel for params " +
                    " project [" + project + "] containingDirectory [" + containingDirectory + "] " +
                    " threw an exception, message " + ee.getMessage() + " Trace [" + Arrays.asList(ee.getStackTrace()));
                }
                return returnFile;
            }
        }
        LOG.warn("STUDIO: WARNING: Could not find any " + POM_XML + " " + project.getName() + " project root " + (virtualProjectRoot == null ? "null" : virtualProjectRoot.getPath()));
        return null;
    }

    /**
     * Create and save a java source file
     * @param project is the Intellij project instance
     * @param contentRoot for the file, typically generated or user
     * @param subDir under the sourceRoodDir, this can be dot delimited a.b.c
     * @param content of the file that is to be created / updated
     */
    // StudioPsiUtils.createPomFile(project, StudioPsiUtils.GENERATED_CONTENT_ROOT, "h2", h2StartStopPomString);
    public static void createPomFile(final Project project, final String contentRoot, final String subDir, final String content) {
        if (project == null || content == null) {
            LOG.warn("STUDIO: SERIOUS: Invalid calll to createJavaSourceFile project [" + project + "] contentRoot [" + contentRoot +
                    "] subDir [" + subDir + "] content [" + content + "]");
        }
        String relativeFilePath =
                (!isBlank(contentRoot) ? contentRoot : "") +
                        (!isBlank(subDir) ? "/" + subDir : "") +
                        "/" + POM_XML;

        createFileWithDirectories(project,
                relativeFilePath,
                content,
                null);
    }

    /**
     * Create and save a java source file
     * @param project is the Intellij project instance
     * @param contentRoot for the file, typically generated or user
     * @param sourceRootDir for this file  e.g. src/main, src/test
     * @param subDir under the sourceRoodDir, this can be dot delimited a.b.c
     * @param clazzName for the java file
     * @param content of the file that is to be created / updated
     * @param componentViewHandler to sue for this component
     */
    public static void createJavaSourceFile(final Project project, final String contentRoot, final String sourceRootDir, final String subDir,
                                            final  String clazzName, final String content, AbstractViewHandlerIntellij componentViewHandler) {
        if (project == null || content == null || sourceRootDir == null || subDir == null || clazzName == null) {
            LOG.warn("STUDIO: SERIOUS: Invalid calll to createJavaSourceFile project [" + project + "] contentRoot [" + contentRoot +
                    "] sourceRootDir [" + sourceRootDir + "] subDir [" + subDir + "] clazzName [" + clazzName + "] content [" + content + "]");
        }
        String relativeFilePath =
                (!isBlank(contentRoot) ? contentRoot : "") +
                (!isBlank(sourceRootDir) ? "/" + sourceRootDir : "")  +
                (!isBlank(subDir) ? "/" + subDir.replace(".", "/") : "") +
                "/" + clazzName+".java";
        createFileWithDirectories(project,
                relativeFilePath,
                content,
                componentViewHandler);
    }


    /**
     * Conveniance method to create a model.json for the supplied content. The location of the
     * model.json is standard so does not need to be supplied
     * @param project is the Intellij project instance
     * @param content of the file that is to be created / updated
     */
    public static void createPropertiesFile(final Project project, final String content) {
        LOG.info("STUDIO: INFO: Saving properties, project [" + project + "] content size " + content.length() + " bytes");

        createFileWithDirectories(project,
                GENERATED_CONTENT_ROOT + "/" + SRC_MAIN_RESOURCES + "/" + MODULE_PROPERTIES_FILENAME_WITH_EXTENSION,
                content, null);
    }

    /**
     * Conveniance method to create a model.json for the supplied content. The location of the
     * model.json is standard so does not need to be supplied
     * @param project is the Intellij project instance
     * @param content of the file that is to be created / updated
     */
    public static void createJsonModelFile(final Project project, final String content) {
        LOG.info("STUDIO: INFO: Saving Json Model, project [" + project + "] content size " + content.length() + " bytes");

        createFileWithDirectories(project,
                GENERATED_CONTENT_ROOT + "/" + SRC_MAIN + "/" + JSON_MODEL_SUB_DIR + "/" + MODEL_JSON,
                content, null);
    }

    public static void refreshCodeFromModelAndCauseRedraw(Project project) {
        // @TODO MODEL
        refreshCodeFromModel(project);
        causeRedraw(project);
    }

    public static void refreshCodeFromModel(Project project) {
        refreshCodeFromModel(project, GenerationRequest.full());
    }

    public static void refreshCodeFromModel(Project project, GenerationRequest generationRequest) {
        PIPSIIkasanModel pipsiIkasanModel = project.getService(UiContext.class).getPipsiIkasanModel();
        pipsiIkasanModel.saveModelJsonToDisk();
        pipsiIkasanModel.asynchGenerateSourceFromModelJsonInstanceAndSaveToDisk(generationRequest);
    }

    public static void refreshCodeFromModelAndCauseRedraw(Project project, GenerationRequest generationRequest) {
        refreshCodeFromModel(project, generationRequest);
        causeRedraw(project);
    }

    public static void causeRedraw(Project project) {
        UiContext uiContext = project.getService(UiContext.class);
        uiContext.getDesignerCanvas().setInitialiseAllDimensions(true);
        uiContext.getDesignerCanvas().repaint();
    }


    /**
     * Get the Virtual file for the project root
     * @param project is the Intellij project instance
     */
    public static VirtualFile getProjectBaseDir(Project project) {
        String basePath = project.getBasePath();
        if (basePath == null) {
            return null;
        } else {
            return LocalFileSystem.getInstance().findFileByPath(basePath);
        }
    }

    /**
     * Creates a file and all necessary directories in the IntelliJ VFS.
     *
     * @param project is the Intellij project instance
     * @param relativePath The relative path (including the file name) from the project's base directory.
     * @param fileContent The content to write into the file (optional).
     */
    public static void createFileWithDirectories(final Project project, final String relativePath,
                                                 final String fileContent, final AbstractViewHandlerIntellij componentViewHandler) {
        // Separate the path and file name
        int lastSeparatorIndex = relativePath.lastIndexOf('/');
        String directoryPath = lastSeparatorIndex == -1 ? "" : relativePath.substring(0, lastSeparatorIndex);
        String fileName = relativePath.substring(lastSeparatorIndex + 1);

        LOG.info("STUDIO: INFO: createFileWithDirectories " +
                "relativePath [" + relativePath + "]" +
                "] directoryPath [" + directoryPath + "]" +
                "] fileName [" + fileName + "]"
        );

        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                // Get the base directory of the project
                VirtualFile baseDir = getProjectBaseDir(project);
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

    private static void writeContentAndFormat(Project project, VirtualFile file, String fileContent,
                                              final AbstractViewHandlerIntellij componentViewHandler) {
        try {
            if (fileContent != null) {
                FileDocumentManager documentManager = FileDocumentManager.getInstance();
                Document document = documentManager.getDocument(file);
                String existingContent = document != null
                        ? document.getText()
                        : new String(file.contentsToByteArray(), StandardCharsets.UTF_8);

                if (fileContent.equals(existingContent)) {
                    // Avoid manufacturing PSI/document changes for artifacts whose rendered output did
                    // not change. Besides saving indexing and formatting work, this keeps generated-file
                    // noise out of IntelliJ's undo infrastructure.
                    if (componentViewHandler != null) {
                        PsiFile existingPsiFile = PsiManager.getInstance(project).findFile(file);
                        if (existingPsiFile != null) {
                            componentViewHandler.setPsiFile(existingPsiFile);
                        }
                    }
                    return;
                }

                if (document != null) {
                    // File is open in the editor; update the document
                    runGeneratedDocumentWriteCommand(project, document, () -> document.setText(fileContent));

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

                DumbService.getInstance(project).runWhenSmart(() -> {
                    if (psiFile != null && psiFile.isWritable()) {
                        Document psiDocument = PsiDocumentManager.getInstance(project).getDocument(psiFile);
                        runGeneratedDocumentWriteCommand(project, psiDocument, () -> {
                            if (file.getName().endsWith(".java")) {
                                JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiFile);
                            }
                            CodeStyleManager.getInstance(project).reformat(psiFile);
                            if (componentViewHandler != null) {
                                componentViewHandler.setPsiFile(psiFile);
                            }
                        });
                    }
                });
            }
        } catch (IOException e) {
            LOG.warn("STUDIO: ERROR: writeContentAndFormat " + file + " message [" + e.getMessage() + "] trace [" + Arrays.toString(e.getStackTrace())+ "]");
            throw new StudioRuntimeException("Failed to write or format the file", e);
        }
    }

    /**
     * Runs a document/PSI mutation as a normal IntelliJ write command, while keeping generator-initiated
     * persistence out of the user's undo history. This is a transient scope, so subsequent manual edits to
     * generated files or protected user stubs retain normal editor undo. The suppression must surround the
     * mutation itself because IntelliJ observes document events regardless of the command's active-editor
     * association.
     */
    private static void runGeneratedDocumentWriteCommand(Project project, Document document, Runnable mutation) {
        Runnable command = () -> WriteCommandAction.writeCommandAction(project)
                .shouldRecordActionForActiveDocument(false)
                .run(mutation::run);
        if (document != null) {
            UndoUtil.disableUndoIn(document, command);
        } else {
            command.run();
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
     * @param project the intellij project
     * @param relativeRootDir to look for e.g. main/java, main/resources
     * @return the rood of the module / source directory that contains the supplied string.
     */
    public static VirtualFile getExistingSourceDirectoryForContentRoot(Project project, final String basePath, final String contentRoot, String relativeRootDir) {
        String targetDirectory = basePath + contentRoot + (relativeRootDir != null ? "/" + relativeRootDir : "");
        LOG.info("STUDIO: INFO: getExistingSourceDirectoryForContentRoot basePath = " + basePath + " contentRoot" + contentRoot +
        " relativeRootDir = " + relativeRootDir + " targetDirectory = " + targetDirectory);
        String targetDirectoryKey = project.getName() + "-" + targetDirectory;

        VirtualFile sourceCodeRoot = checkVirtualRoots(targetDirectoryKey);
        if (sourceCodeRoot == null) {
            // ProjectRoots are more likely to provide an exact match
            ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
            fileIndex.iterateContent(file -> {
                if (fileIndex.isInSource(file)) {
                    if (file.isValid() && file.isDirectory()) {
                        virtualRoots.put(project.getName() + "-" + file.getPath(), file);
                    }
                }
                return true; // Continue iterating
            });
        }
        sourceCodeRoot = checkVirtualRoots(targetDirectoryKey);
        if (sourceCodeRoot == null) {
            Thread thread = Thread.currentThread();
            LOG.warn("STUDIO: WARNING: Could not find any existing source roots for project [" + project + "] and contentRoot [" + contentRoot + "] and relativeRoot [" + relativeRootDir + "] trace:[" + Arrays.toString(thread.getStackTrace())+"]");
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
     * @param projecKey  essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
     * @param relativeFilePath from the project root e.g. pom.xml, src/main/resources/application.properties
     * @return the virtual file reflecting the file path
     */
    public static VirtualFile getVirtualFile(Project projecKey, String relativeFilePath) {
        return VfsUtil.findRelativeFile(relativeFilePath, getProjectBaseDir(projecKey));
    }

    /**
     * Read the physical file from the file system and return as a String.
     * Note that the VFS is used to obtain the file so any unsaved edits will be missed.
     * @param projecKey  essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
     * @param relativeFilePath from the project root e.g. pom.xml, src/main/resources/application.properties
     * @return the String contents of the file, or null if it does not exist
     */
    public static String readFileAsString(Project projecKey, String relativeFilePath) {
        VirtualFile virtualFilePathPath = getVirtualFile(projecKey, relativeFilePath);
        if (virtualFilePathPath != null && virtualFilePathPath.isValid()) {
            return readVirtualFileAsString(virtualFilePathPath);
        } else {
            return null;
        }
    }

    /**
     * Locate the on-disk java file for a user implemented class (e.g. a Debug or CustomConverter stub), if it
     * has been generated. Mirrors the relative path construction used to write the file in
     * {@code PIPSIIkasanModel.generateAndSaveUserImplementClassStubsForFlow}.
     * @param project is the Intellij project instance
     * @param packageName of the user implemented class, see {@code GeneratorUtils.getUserImplementedClassesPackageName}
     * @param className of the user implemented class
     * @return the VirtualFile if it exists on disk, otherwise null
     */
    public static VirtualFile getUserImplementedClassFile(Project project, String packageName, String className) {
        if (project == null || isBlank(packageName) || isBlank(className)) {
            return null;
        }
        String relativeFilePath = USER_CONTENT_ROOT.substring(1) + "/" + SRC_MAIN_JAVA_CODE + "/" +
                packageName.replace(".", "/") + "/" + className + ".java";
        VirtualFile virtualFile = getVirtualFile(project, relativeFilePath);
        return (virtualFile != null && virtualFile.isValid()) ? virtualFile : null;
    }

    private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Copy an existing file to a timestamped backup alongside it (e.g. Foo.java -&gt; Foo.java.backup20260821_153012)
     * before it is about to be regenerated/overwritten, so the user's prior hand-written content isn't lost.
     * @param project is the Intellij project instance
     * @param fileToBackup the file to copy, ignored if null or already invalid - nothing to back up is not an error
     */
    public static void backupFile(Project project, VirtualFile fileToBackup) {
        if (fileToBackup == null || !fileToBackup.isValid()) {
            return;
        }
        String backupName = fileToBackup.getName() + ".backup" + LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMAT);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                fileToBackup.copy(StudioPsiUtils.class, fileToBackup.getParent(), backupName);
            } catch (IOException ee) {
                LOG.warn("STUDIO: WARN: Unable to backup file " + fileToBackup.getPath() + " exception was " + ee.getMessage());
            }
        });
    }

    /**
     * Delete a single file, e.g. a user implemented class that is no longer referenced by any component.
     * @param project is the Intellij project instance
     * @param fileToDelete the file to remove, ignored if null or already invalid
     */
    public static void deleteFile(Project project, VirtualFile fileToDelete) {
        if (fileToDelete == null || !fileToDelete.isValid()) {
            return;
        }
        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                fileToDelete.delete(StudioPsiUtils.class);
            } catch (IOException ee) {
                LOG.warn("STUDIO: WARN: Unable to delete file " + fileToDelete.getPath() + " exception was " + ee.getMessage());
            }
        });
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
        } else {

            try (InputStream is = absoluteFilePath.getInputStream();
                 Scanner scanner = new Scanner(is, StandardCharsets.UTF_8)) {

                scanner.useDelimiter("\\A"); // Read entire file content
                return scanner.hasNext() ? scanner.next() : "";
            } catch (Exception ee) {
                LOG.warn("STUDIO: WARN: readFileAsString failed to read the file " + absoluteFilePath.getPresentableUrl() + " exception was " + ee.getMessage() +
                        " StackTrace: " + Arrays.asList(Thread.currentThread().getStackTrace()));
            }
        }
        return null;
    }

    public static PsiFile getPsiFileFromPath(final Project project, VirtualFile root, String filePath) {
        PsiFile returnFile = null;

        if (root != null && filePath != null && filePath.length() > 1) {
            String fileName = FilenameUtils.getName(filePath);
            String directoryPath = FilenameUtils.getFullPathNoEndSeparator(filePath);
            PsiDirectory psiDirectory = getPsiDirectoryLeafFromPath(project, root, directoryPath);
            if (psiDirectory != null) {
                returnFile = psiDirectory.findFile(fileName);
            }
        }
        return returnFile;
    }

    public static PsiDirectory getPsiDirectoryLeafFromPath(final Project project, VirtualFile root, String target) {
        if (root.isDirectory()) {
            return getPsiDirectoryLeafFromPath(PsiDirectoryFactory.getInstance(project).createDirectory(root), target);
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
     *
     * @param project is the Intellij project instance
     * @return the content root if its found, nulll otherwise.
     */
    protected static VirtualFile getSpecificContentRootFromCache(final Project project) {
        VirtualFile targetContentRoot = null;
        VirtualFile[] contentRootVFiles = ProjectRootManager.getInstance(project).getContentRoots();

        for (VirtualFile vFile : contentRootVFiles) {
            if (vFile.toString().endsWith(StudioPsiUtils.GENERATED_CONTENT_ROOT)) {
                targetContentRoot = vFile;
                break;
            }
        }
        return targetContentRoot;
    }

    /**
     * Indicates that IntelliJ has imported the generated Maven module into the project model.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted") // kept positive/clear at its 2 call sites' "not ready yet" guard clauses
    public static boolean hasGeneratedContentRoot(Project project) {
        return getSpecificContentRootFromCache(project) != null;
    }



    /**
     * Find the directory representing the base package, remove any sub package (subdirectory) that is not in the subPackagesToKeep
     * @param project is the Intellij project instance
     * @param basePackage the package that contains the subpackage leaves
     * @param subPackagesToKeep a set of package names tha are valid i.e. you want to kepp
     */
    public static void deleteSubPackagesNotIn(Project project, final String contentRoot, final String basePackage, Set<String> subPackagesToKeep) {
        LOG.info("STUDIO: INFO: deleteSubPackagesNotIn will keep the following packages " + subPackagesToKeep);
        VirtualFile baseDir = getProjectBaseDir(project);

        if (baseDir == null) {
            LOG.warn("Studio: WARN: Could not get project root for directory for project [" + project + "]");
        } else {
//            final VirtualFile sourceRoot = StudioPsiUtils.getExistingSourceDirectoryForContentRoot(project, baseDir.getPath(), contentRoot, StudioPsiUtils.SRC_MAIN_JAVA_CODE);
//            final PsiDirectory sourceRootDir = PsiDirectoryFactory.getInstance(project).createDirectory(sourceRoot);

            CompletableFuture<PsiDirectory[]> leafPackageDirectoryFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return ReadAction.compute(() -> {
                        final VirtualFile sourceRoot = StudioPsiUtils.getExistingSourceDirectoryForContentRoot(project, baseDir.getPath(), contentRoot, StudioPsiUtils.SRC_MAIN_JAVA_CODE);
                        final PsiDirectory sourceRootDir = PsiDirectoryFactory.getInstance(project).createDirectory(sourceRoot);
                        PsiDirectory leafPackageDirectory = getDirectoryForPackage(sourceRootDir, basePackage);
                        return leafPackageDirectory.getSubdirectories(); // ✅ this is now returned
                    });
                } catch (Exception ee) {
                    LOG.warn("STUDIO: SERIOUS: The read action of deleteSubPackagesNotIn for params " +
                            " project [" + project + "] contentRoot [" + contentRoot + "] basePackage + [" + basePackage + "] subPackagesToKeep [" + subPackagesToKeep + "] " +
                            " threw an exception, message " + ee.getMessage() + " Trace [" + Arrays.asList(ee.getStackTrace()));
                }
                return null;
            });
            leafPackageDirectoryFuture.thenAccept(resultsObject ->
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (resultsObject == null) {
                        LOG.warn("STUDIO: SERIOUS: The previous read action of deleteSubPackagesNotIn returned null for params " +
                                " project [" + project + "] contentRoot [" + contentRoot + "] basePackage + [" + basePackage + "] subPackagesToKeep [" + subPackagesToKeep + "] " +
                                " trace [" + Arrays.asList(Thread.currentThread().getStackTrace()));
                    } else {
                        WriteCommandAction.runWriteCommandAction(project, () -> {
                            for (PsiDirectory directory : resultsObject) {
                                if (!subPackagesToKeep.contains(directory.getName())) {
                                    LOG.info("STUDIO: Deleting directory " + directory.getName() + " the basePackage " + basePackage + " should only have these directories " + subPackagesToKeep);
                                    try {
                                        directory.delete();
                                    } catch (Exception e) {
                                        LOG.warn("STUDIO: Unable to delete directory " + directory.getName(), e);
                                    }
                                }
                            }
                        });
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
     * Extract an attribute value from a JSON string.
     * This utility method safely extracts a value from a JSON document by attribute name.
     * It handles null and empty strings gracefully, returning null if the JSON is invalid
     * or the attribute is not found.
     *
     * @param jsonString the JSON string to parse (e.g., "{\"name\":\"test\",\"applicationPackageName\":\"org.example\"}")
     * @param attributeName the name of the attribute to extract (e.g., "applicationPackageName")
     * @return the string value of the attribute, or null if jsonString is null/empty, JSON is invalid, or attribute not found
     * Example usage:
     *   String json = "{\"name\":\"untitled54\",\"applicationPackageName\":\"org.example\"}";
     *   String appPackageName = getJsonAttribute(json, "applicationPackageName");
     *   // Result: "org.example"
     */
    public static String getJsonAttribute(String jsonString, String attributeName) {
        // Handle null or empty input
        if (jsonString == null || jsonString.trim().isEmpty() || attributeName == null || attributeName.trim().isEmpty()) {
            return null;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(jsonString);

            // Check if node exists and get the attribute
            if (node != null && node.has(attributeName)) {
                JsonNode attributeNode = node.get(attributeName);

                // Handle null or missing node
                if (attributeNode != null && !attributeNode.isNull()) {
                    return attributeNode.asText();
                }
            }
        } catch (Exception e) {
            // Log the exception for debugging but return null gracefully
            LOG.warn("STUDIO: WARN: Failed to parse JSON or extract attribute [" + attributeName + "]. " +
                    "JSON: [" + jsonString + "]. Error: " + e.getMessage());
        }

        return null;
    }
}
