package org.ikasan.studio.ui.model;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
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
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.ikasan.studio.core.model.ikasan.instance.IkasanPomModel.MAVEN_COMPILER_SOURCE;
import static org.ikasan.studio.core.model.ikasan.instance.IkasanPomModel.MAVEN_COMPILER_TARGET;
import static org.ikasan.studio.ui.StudioUIUtils.displayIdeaWarnMessage;
import static org.ikasan.studio.ui.model.psi.PIPSIIkasanModel.MODULE_PROPERTIES_FILENAME_WITH_EXTENSION;


public class LargeJava {
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
     * @param projectKey essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
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
     * @param projectKey essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
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
     * @param projectKey essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
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
     * @param projectKey essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
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
                StudioUIUtils.displayIdeaErrorMessage(projectKey, "Error: Please fix " + StudioPsiUtils.JSON_MODEL_FULL_PATH + " then use the Load Button");
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
     * @param projectKey essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
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

                    createPomFile(projectKey, "", "", "", POM_XML, ikasanPomModel.getModelAsString(), false, true);
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
     * @param projectKey essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
     * @return A PsiFile handle to the pom or null
     */
    public static PsiFile pomGetTopLevel(String projectKey, String containingDirectory) {
        LOG.info("STUDIO: 1111 pomGetTopLevel " + containingDirectory);

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
     * @param projectKey essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
     * @param contentRoot for the file, typically generated or user
     * @param sourceRootDir for this file  e.g. src/main, src/test
     * @param subDir under the sourceRoodDir, this can be dot delimited a.b.c
     * @param fileNameWithExtension for the java file
     * @param content of the file that is to be created / updated
     * @param focus if true, open this file in the IDE
     * @param replaceExisting if false, will only allow creation of a new file if it doesn't exist, it can't overwrite an existing file.
     * @return the reference to the PsiJavaFile
     */
    public static void createPomFile(final String projectKey, final String contentRoot, final String sourceRootDir, final String subDir,
                                     final  String fileNameWithExtension, final String content, boolean focus,
                                     final boolean replaceExisting) {
        if (projectKey == null || fileNameWithExtension == null || content == null) {
            LOG.warn("STUDIO: SERIOUS: Invalid calll to createJavaSourceFile projectKey [" + projectKey + "] contentRoot [" + contentRoot +
                    "] sourceRootDir [" + sourceRootDir + "] subDir [" + subDir + "] clazzName [" + fileNameWithExtension + "] content [" + content + "]");
        }
        createSourceFile(projectKey, contentRoot, sourceRootDir, subDir.replace(".", "/"), fileNameWithExtension+".java", content, focus, replaceExisting);
    }

    /**
     * Create and save a java source file
     * @param projectKey essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
     * @param contentRoot for the file, typically generated or user
     * @param sourceRootDir for this file  e.g. src/main, src/test
     * @param subDir under the sourceRoodDir, this can be dot delimited a.b.c
     * @param clazzName for the java file
     * @param content of the file that is to be created / updated
     * @param focus if true, open this file in the IDE
     * @param replaceExisting if false, will only allow creation of a new file if it doesn't exist, it can't overwrite an existing file.
     * @return the reference to the PsiJavaFile
     */
    public static void createJavaSourceFile(final String projectKey, final String contentRoot, final String sourceRootDir, final String subDir,
                                            final  String clazzName, final String content, boolean focus,
                                            final boolean replaceExisting) {
        if (projectKey == null || content == null || sourceRootDir == null || subDir == null || clazzName == null || content == null) {
            LOG.warn("STUDIO: SERIOUS: Invalid calll to createJavaSourceFile projectKey [" + projectKey + "] contentRoot [" + contentRoot +
                    "] sourceRootDir [" + sourceRootDir + "] subDir [" + subDir + "] clazzName [" + clazzName + "] content [" + content + "]");
        }
        createSourceFile(projectKey, contentRoot, sourceRootDir, subDir.replace(".", "/"), clazzName+".java", content, focus, replaceExisting);
    }

    private static com.intellij.openapi.module.Module getModule(String projectKey, String moduleName) {
        moduleName = moduleName.replace("/", "");
        ModuleManager moduleManager = ModuleManager.getInstance(UiContext.getProject(projectKey));
        com.intellij.openapi.module.Module[] modules = moduleManager.getModules();
        for (com.intellij.openapi.module.Module module : modules) {
            if (module.getName().equals(moduleName)) {
                return module;
            };
        }
        return null;
    }


    /**
     * Conveniance method to create a model.json for the supplied content. The location of the
     * model.json is standard so does not need to be supplied
     * @param projectKey essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
     * @param content of the file that is to be created / updated
     */
    public static CompletableFuture createJsonModelFile(final String projectKey, final String content) {
        LOG.warn("STUDIO: Savaing Json Model, content [" + content + "]");
        return createSourceFile(projectKey, GENERATED_CONTENT_ROOT, SRC_MAIN, JSON_MODEL_SUB_DIR, MODEL_JSON, content, false, true);
    }

    /**
     * Create and save a java source file
     * @param projectKey essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
     * @param contentRoot for the file, typically generated or user
     * @param sourceRootDir for this file  e.g. src/main, src/test
     * @param subDir under the sourceRoodDir, this can be dot delimited or slash delimited x/y
     * @param fileName for the java file
     * @param content of the file that is to be created / updated
     * @param focus if true, open this file in the IDE
     * @param replaceExisting if false, will only allow creation of a new file if it doesn't exist, it can't overwrite an existing file.
     * @return the reference to the PsiJavaFile
     */
//    public static PsiJavaFile createJavaSourceFile(final Project project, final String contentRoot, final String packageName,
    public static CompletableFuture createSourceFile(final String projectKey, final String contentRoot, final String sourceRootDir, final String subDir,
                                                     final  String fileName, final String content, boolean focus,
                                                     final boolean replaceExisting) {

        LOG.warn("STUDIO ZZZZZ0 createSourceFile contentRoot " + contentRoot + " sourceRootDir " + sourceRootDir + " subDir " + subDir + " fileName " +
                fileName);
//        AtomicReference<PsiFile> returnPsiFile = new AtomicReference<>();

        CompletableFuture newPsiFileReadFuture = null;
        VirtualFile virtualProjectRoot = getProjectBaseDir(projectKey);
        if (virtualProjectRoot == null) {
            Thread thread = Thread.currentThread();
            LOG.warn("STUDIO: SERIOUS: createSourceFile cant find basePath for project " + projectKey + " trace:" + Arrays.toString(thread.getStackTrace()));
        } else {

            doReadAction(
                    projectKey, contentRoot, sourceRootDir, subDir,
                    fileName, content,
                    virtualProjectRoot);

//            final FileType fileType = FileType.fromFileName(fileName);
//            String basePathTrailingSlash = virtualProjectRoot.getPath();
//            String relativeDir =
//                    (!isBlank(contentRoot) ? contentRoot : "") +
//                    (!isBlank(sourceRootDir) ? "/" + sourceRootDir : "")  +
//                    (!isBlank(subDir) ? "/" + subDir : "");
//            String relativeFilePath = relativeDir + fileName;
//            String targetDir = basePathTrailingSlash + relativeDir;
//            AtomicReference<PsiDirectory> psiDirectoryPath = new AtomicReference<>();
//
//            // By placing the declaration here, hoping that will prevent them from going out of scope
//            AtomicReference<PsiFile> oldPsiFile = new AtomicReference<>();
//            AtomicReference<PsiFile> newPsiFile = new AtomicReference<>();
//            AtomicReference<PsiFile>[] readResults = new AtomicReference[2];
//            newPsiFileReadFuture = CompletableFuture.supplyAsync(() -> {
//                try {
//                    ReadAction.compute(() -> {
//                        LOG.info("STUDIO: createSourceFile ZZZZZ0.2 " + fileName + " asynch Start " + targetDir);
//                        String oldFileContent = readFileAsString(projectKey, relativeFilePath);
//                        newPsiFile.set(PsiFileFactory
//                                .getInstance(UiContext.getProject(projectKey))
//                                .createFileFromText(
//                                    fileName,
//                                    FileTypeManager.getInstance().getFileTypeByExtension(fileType.getExtension()),
//                                    content));
//                        // Removed the fully qualified packages and adds relevant imports
//                        if (fileType == FileType.JAVA) {
//                            JavaCodeStyleManager.getInstance(UiContext.getProject(projectKey)).shortenClassReferences(newPsiFile.get()); // Not on EDT
//                        }
//                        CodeStyleManager.getInstance(UiContext.getProject(projectKey)).reformat(newPsiFile.get());
//
//
//                        // If the files have not changed, no point continuing
//                        if (!areEqualIgnoringWhitespace(oldFileContent, newPsiFile.get().getText())) {
//                            readResults[1] = newPsiFile;
//                            // if old file was null, it didn't exist so create new
//                            // This is a replacement
//                            if (oldFileContent != null) {
//                                VirtualFile oldFile = getVirtualFile(projectKey, relativeFilePath);
//                                if (oldFile != null && oldFile.isValid()) {
//                                    oldPsiFile.set(PsiManager.getInstance(UiContext.getProject(projectKey)).findFile(oldFile));
//                                    readResults[0] = oldPsiFile;
//                                }
//
//                            }
//                        }
//System.out.println("Read thread return result 1");
//                        return readResults;
//                    });
//                } catch (Exception ee) {
//                    LOG.warn("STUDIO: SERIOUS: An exception occurred during the read thread of createSourceFile for params " +
//                            "projectKey [" + projectKey + "] contentRoot [" + contentRoot + "] sourceRootDir [" + sourceRootDir +
//                            "] subDir [" + subDir + "] fileName [" + fileName + "] content [" + content + "] focus [" + focus + "] replaceExisting [" + replaceExisting +
//                            "] messages was [" + ee.getMessage() + "] Stack trace [" + Arrays.asList(ee.getStackTrace()) + "]");
//System.out.println("Read thread return result 2");
//                    return null;
//                }
//System.out.println("Read thread return result 3");
//                return readResults;
//            });

            newPsiFileReadFuture.thenAccept(resultsObject ->
                    ApplicationManager.getApplication().invokeLater(() -> {
                        // Many things can prevent the first phase from working, including a project refresh
                        // Intellij best practice is abort gracefully and rely on caller to reissue if needed.


                    }));
        }
        return newPsiFileReadFuture;
    }

//
//    /**
//     * Retrieves the Document corresponding to the given VirtualFile.
//     *
//     * @param file the VirtualFile to retrieve the Document for.
//     * @return the corresponding Document, or null if the file is not a text file or doesn't support documents.
//     */
//    public static Document getDocumentForVirtualFile(VirtualFile file) {
//        if (file == null || file.isDirectory()) {
//            throw new IllegalArgumentException("Invalid file: " + file);
//        }
//
//        FileDocumentManager documentManager = FileDocumentManager.getInstance();
//        return documentManager.getDocument(file);
//    }
//
//    public static String getTextFromVirtualFile(VirtualFile file) {
//        if (file == null || file.isDirectory()) {
//            throw new IllegalArgumentException("Invalid file: " + file);
//        }
//
//        FileDocumentManager documentManager = FileDocumentManager.getInstance();
//        Document document = documentManager.getDocument(file);
//
//        if (document != null) {
//            return document.getText(); // Returns the text content of the file
//        } else {
//            throw new IllegalStateException("Could not retrieve a document for the file: " + file.getPath());
//        }
//    }


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

//    /**
//     * This class attempts to find a file, it does not attempt to retrieve it
//     * @param projectKey essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
//     * @param filename to search for
//     * @return A message string to indicate progress.
//     */
//    public static String findFile(String projectKey, String filename) {
//        StringBuilder message = new StringBuilder();
//
//        PsiFile[] files2 = PsiShortNamesCache.getInstance(UiContext.getProject(projectKey)).getFilesByName(filename);
//        long t1 = System.currentTimeMillis();
//        message
//                .append("looking for file ")
//                .append(filename)
//                .append(" method 1 found [");
//        for (PsiFile myFile : files2) {
//            message.append(myFile.getIdentity());
//        }
//        long t2 = System.currentTimeMillis();
//        long t3 = System.currentTimeMillis();
//
//        message.append("] method 1 = ")
//                .append(t2-t1)
//                .append(" ms method 2 = ")
//                .append(t3-t2);
//        return message.toString();
//    }


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

//    public static void getAllSourceRootsForProject(String projectKey) {
//        String projectName = UiContext.getProject(projectKey).getIdentity();
//        VirtualFile[] vFiles = ProjectRootManager.getInstance(UiContext.getProject(projectKey)).getContentSourceRoots();
//        String sourceRootsList = Arrays.stream(vFiles).map(VirtualFile::getUrl).collect(Collectors.joining("\n"));
//        LOG.info("STUDIO: Source roots for the " + projectName + " plugin:\n" + sourceRootsList +  "Project Properties");
//    }


//    public static void getProjectSourceRoots(Project project) {
//        System.out.println("STUDIO: getProjectSourceRoots started " + project);
//        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
//        fileIndex.iterateContent(file -> {
//            if (fileIndex.isInSource(file)) {
//                System.out.println("STUDIO: getProjectSourceRoots Source Root: " + file.getPath());
//            }
//            return true; // Continue iterating
//        });
//    }

//    public static VirtualFile[] getModuleSourceRoots(com.intellij.openapi.module.Module module) {
//System.out.println("STUDIO: getModuleSourceRoots started " + module);
//        VirtualFile[] sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots();
//        for (VirtualFile sourceRoot : sourceRoots) {
//            System.out.println("STUDIO: Source Root: " + sourceRoot.getPath());
//        }
//
//
//        return ModuleRootManager.getInstance(module).getSourceRoots();
//    }
//
//    public static VirtualFile[] getContentRoots(com.intellij.openapi.module.Module module) {
//System.out.println("STUDIO: getContentRoots started " + module);
//        VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
//        for (VirtualFile contentRoot : contentRoots) {
//            System.out.println("STUDIO: Content Root: " + contentRoot.getPath());
//        }
//        return ModuleRootManager.getInstance(module).getContentRoots();
//    }



    /**
     * Get the Virtual file for the project root
     * @param projectKey essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
     */
    public static VirtualFile getProjectBaseDir(String projectKey) {
        String basePath = UiContext.getProject(projectKey).getBasePath();
        return LocalFileSystem.getInstance().findFileByPath(basePath);
//        com.intellij.openapi.module.Module projectModule = getModule(projectKey, UiContext.getProject(projectKey).getIdentity());
//        VirtualFile[] contentRoots = getContentRoots(projectModule);
//        if (contentRoots.length == 0) {
//            throw new StudioException("No content roots found for " + UiContext.getProject(projectKey).getIdentity());
//        } else {
//            return contentRoots[0];
//        }
    }

    public static final AtomicReference<PsiFile>[] doReadAction(
            final String projectKey, final String contentRoot, final String sourceRootDir, final String subDir,
            final  String fileName, final String content,

            final VirtualFile virtualProjectRoot) {

        final FileType fileType = FileType.fromFileName(fileName);
        String basePathTrailingSlash = virtualProjectRoot.getPath();
        String relativeDir =
                (!isBlank(contentRoot) ? contentRoot : "") +
                        (!isBlank(sourceRootDir) ? "/" + sourceRootDir : "")  +
                        (!isBlank(subDir) ? "/" + subDir : "");
        String relativeFilePath = relativeDir + fileName;
        String targetDir = basePathTrailingSlash + relativeDir;

        AtomicReference<PsiDirectory> psiDirectoryPath = new AtomicReference<>();

        // By placing the declaration here, hoping that will prevent them from going out of scope
        AtomicReference<PsiFile> oldPsiFile = new AtomicReference<>();
        AtomicReference<PsiFile> newPsiFile = new AtomicReference<>();
        AtomicReference<PsiFile>[] readResults = new AtomicReference[2];

        LOG.info("STUDIO: createSourceFile ZZZZZ0.2 " + fileName + " asynch Start " + targetDir);
        String oldFileContent = readFileAsString(projectKey, relativeFilePath);
        newPsiFile.set(PsiFileFactory
                .getInstance(UiContext.getProject(projectKey))
                .createFileFromText(
                        fileName,
                        FileTypeManager.getInstance().getFileTypeByExtension(fileType.getExtension()),
                        content));
        // Removed the fully qualified packages and adds relevant imports
        if (fileType == FileType.JAVA) {
            JavaCodeStyleManager.getInstance(UiContext.getProject(projectKey)).shortenClassReferences(newPsiFile.get()); // Not on EDT
        }
        CodeStyleManager.getInstance(UiContext.getProject(projectKey)).reformat(newPsiFile.get());


        // If the files have not changed, no point continuing
        if (!areEqualIgnoringWhitespace(oldFileContent, newPsiFile.get().getText())) {
            readResults[1] = newPsiFile;
            // if old file was null, it didn't exist so create new
            // This is a replacement
            if (oldFileContent != null) {
                VirtualFile oldFile = getVirtualFile(projectKey, relativeFilePath);
                if (oldFile != null && oldFile.isValid()) {
                    oldPsiFile.set(PsiManager.getInstance(UiContext.getProject(projectKey)).findFile(oldFile));
                    readResults[0] = oldPsiFile;
                }

            }
        }
        System.out.println("Read thread return result 1");
        return readResults;
    }

    private static void doWriteAction(
            final String projectKey, final String contentRoot, final String sourceRootDir, final String subDir,
            final  String fileName, final String content, boolean focus,
            final boolean replaceExisting,
            final AtomicReference<PsiFile>[] results) {

        if (UiContext.getProject(projectKey).isDisposed()) {
            System.out.println("pSTUDIO: SERIOUS: Stage 1 project shutdown "+ System.currentTimeMillis());
            LOG.warn("STUDIO: SERIOUS: Stage 1 project shutdown "+ System.currentTimeMillis());
        }

//    if (resultsObject == null) {
//        LOG.warn("STUDIO: SERIOUS: An exception occurred during the read thread of previous createSourceFile for params " +
//                "projectKey [" + projectKey + "] contentRoot [" + contentRoot + "] sourceRootDir [" + sourceRootDir +
//                "] subDir [" + subDir + "] fileName [" + fileName + "] content [" + content + "] focus [" + focus + "] replaceExisting [" + replaceExisting +
//                "] Stack trace " + Arrays.asList(Thread.currentThread().getStackTrace()));
//    } else {
//        AtomicReference<PsiFile>[] results = (AtomicReference<PsiFile>[]) resultsObject;

        if (results[0] == null && results[1] == null) {
            // There were no changes, no further action required
            LOG.warn("STUDIO: Attempt made to update " + fileName + " was ignored because relaceExisting is false");
        } else if (results[0] == null) {
            // This is a new file, there was no old fie
            PsiFile futureNewPsiFile = ((AtomicReference<PsiFile>) results[1]).get();
            if (UiContext.getProject(projectKey).isDisposed()) {
                LOG.warn("STUDIO: SERIOUS: Stage 2 project shutdown"+ System.currentTimeMillis());
            }

            if (!futureNewPsiFile.isValid()) {
                LOG.warn("STUDIO: SERIOUS: During the write phase of createSourceFile, the new file has become invalid, params " +
                        "projectKey [" + projectKey + "] contentRoot [" + contentRoot + "] sourceRootDir [" + sourceRootDir +
                        "] subDir [" + subDir + "] fileName [" + fileName + "] content [" + content + "] focus [" + focus + "] replaceExisting [" + replaceExisting +
                        " Stack trace " + Arrays.asList(Thread.currentThread().getStackTrace()));
                return;
            }
//            WriteCommandAction.runWriteCommandAction(UiContext.getProject(projectKey), () -> {
//                psiDirectoryPath.get().add(futureNewPsiFile.copy());
////                                returnPsiFile.set(futureNewPsiFile);
//            });
        } else {

            if (UiContext.getProject(projectKey).isDisposed()) {
                LOG.warn("STUDIO: SERIOUS: Stage 3 project shutdown"+ System.currentTimeMillis());
            }

            // This is a replacement of the olf dile with the new file.
            PsiFile futureOldPsiFile = ((AtomicReference<PsiFile>) results[0]).get();
            PsiFile futureNewPsiFile = ((AtomicReference<PsiFile>) results[1]).get();
            if (!futureOldPsiFile.isValid() || !futureNewPsiFile.isValid()) {
                LOG.warn("STUDIO: SERIOUS: During the write phase of createSourceFile, one of the files have become invalid, params " +
                        "projectKey [" + projectKey + "] contentRoot [" + contentRoot + "] sourceRootDir [" + sourceRootDir +
                        "] subDir [" + subDir + "] fileName [" + fileName + "] content [" + content + "] focus [" + focus + "] replaceExisting [" + replaceExisting +
                        " Stack trace " + Arrays.asList(Thread.currentThread().getStackTrace()));
                return;
            }
            Document document = PsiDocumentManager.getInstance(UiContext.getProject(projectKey)).getDocument(futureOldPsiFile);
//                        Document document = getDocumentForVirtualFile(oldVirtualFile);
            // Replace content inside a write action
            WriteCommandAction.runWriteCommandAction(UiContext.getProject(projectKey), () -> {
                document.setText(futureNewPsiFile.getText());
                PsiDocumentManager.getInstance(UiContext.getProject(projectKey)).commitDocument(document);
            });
//                            returnPsiFile.set(futureNewPsiFile);
//                            changed.set(true);
//        }
        }
    }

    /**
     * Creates a file and all necessary directories in the IntelliJ VFS.
     *
     * @param project     The current IntelliJ project.
     * @param relativePath The relative path (including the file name) from the project's base directory.
     * @param fileContent The content to write into the file (optional).
     */
    public static void createFileWithDirectories(Project project, String relativePath, String fileContent) {
        // Separate the path and file name
        int lastSeparatorIndex = relativePath.lastIndexOf('/');
        String directoryPath = lastSeparatorIndex == -1 ? "" : relativePath.substring(0, lastSeparatorIndex);
        String fileName = relativePath.substring(lastSeparatorIndex + 1);

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
                    long now = System.currentTimeMillis();
                    if (file == null) {
                        // File does not exist; create it

                        System.out.println("XX Writing new file '" + fileName + "' to '" + relativePath + "'" + (System.currentTimeMillis() - now) + "ms");
                        file = targetDir.createChildData(StudioPsiUtils.class, fileName);
                        writeContentAndFormat(project, file, fileContent);
                    } else {
                        System.out.println("XX Writing existing file '" + fileName + "' to '" + relativePath + "'" + (System.currentTimeMillis() - now) + "ms");
                        // File exists; decide what to do (e.g., overwrite)
                        writeContentAndFormat(project, file, fileContent);
                    }
                    System.out.println("XX Writing finished '" + file.getPath());
                    System.out.println("XX Writing finished '" + fileName + "' to '" + relativePath + "'" + (System.currentTimeMillis() - now) + "ms");
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to create file or directories", e);
            }
        });
    }

    private static void writeContentAndFormat(Project project, VirtualFile file, String fileContent) throws IOException {
        if (fileContent != null) {
            // Write the content to the file
            file.setBinaryContent(fileContent.getBytes(StandardCharsets.UTF_8));

            if (file.getName().endsWith(".java")) {
                // Format the Java file
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                if (psiFile != null) {
                    CodeStyleManager.getInstance(project).reformat(psiFile);
                }
            }
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
    public static VirtualFile createDirectories(VirtualFile baseDir, String relativePath) throws IOException {
        if (relativePath.isEmpty()) {
            return baseDir;
        }

        String[] parts = relativePath.split("/");
        VirtualFile currentDir = baseDir;

        for (String part : parts) {
            VirtualFile child = currentDir.findChild(part);
            if (child == null) {
                child = currentDir.createChildDirectory(StudioPsiUtils.class, part);
            }
            currentDir = child;
        }

        return currentDir;
    }

    private static final Map<String, VirtualFile> virtualRoots = new HashMap<String, VirtualFile>();
    /**
     * Get the source root that contains the supplied string, possible to get java source, resources or test
     * @param projectKey essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
     * @param relativeRootDir to look for e.g. main/java, main/resources
     * @return the rood of the module / source directory that contains the supplied string.
     */
    public static VirtualFile getExistingSourceDirectoryForContentRoot(String projectKey, final String basePath, final String contentRoot, String relativeRootDir) {
//        String targetDirectory = project.getBasePath() + contentRoot + "/" + relativeRootDir;

        String targetDirectory = basePath + contentRoot + (relativeRootDir != null ? "/" + relativeRootDir : "");
        LOG.warn("STUDIO XXXXXXXXXXXXXXXXXXXX5.1 getExistingSourceDirectoryForContentRoot basePath = " + basePath + " contentRoot" + contentRoot +
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
        LOG.warn("STUDIO XXXXXXXXXXXXXXXXXXXX5.2 getExistingSourceDirectoryForContentRoot sourceCodeRoot = " + sourceCodeRoot);
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

    /**
     * Get the source root that contains the supplied string, possible to get java source, resources or test
     * @param projectKey essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
     * @param targetDir to look for e.g. main/java, main/resources
     * @return the rood of the module / source directory that contains the supplied string.
     */
    public static VirtualFile getExistingVirtualDirectoryFromCache(String projectKey, String targetDir) {
//        String targetDirectory = project.getBasePath() + contentRoot + "/" + relativeRootDir;
        LOG.warn("STUDIO XXXXXXXXXXXXXXXXXXXX5.1a getExistingSourceVirtualDirectory targetDir = " + targetDir);
        String targetDirectoryKey = UiContext.getProject(projectKey).getName() + "-" + targetDir;

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
        return sourceCodeRoot;
    }




    public static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

//    public static String findAndReadFile(String filePath) {
//        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(filePath);
//        if (file == null) {
//            throw new IllegalArgumentException("File not found: " + filePath);
//        }
//        return readFileContents(file);
//    }
//    public static String readFileContents(VirtualFile file) {
//        if (file == null || !file.exists()) {
//            throw new IllegalArgumentException("File does not exist");
//        }
//
//        try (BufferedReader reader = new BufferedReader(
//                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
//
//            StringBuilder content = new StringBuilder();
//            String line;
//            while ((line = reader.readLine()) != null) {
//                content.append(line).append("\n");
//            }
//            return content.toString();
//        } catch (Exception e) {
//            throw new RuntimeException("Error reading file: " + file.getPath(), e);
//        }
//    }
//
//    public static String readPsiFile(Project project, VirtualFile file) {
//        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
//        if (psiFile == null) {
//            throw new IllegalArgumentException("PSI File not found: " + file.getPath());
//        }
//        return psiFile.getText(); // Returns the file's text content
//    }

    /**
     * Get the virtual file representing the relative (to the project root) of the supplied file.
     * Note virtual files will miss any unsaved changes currently in the editor and may become invalid if a project is refreshed
     * @param projecKey  essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
     * @param relativeFilePath from the project root e.g. pom.xml, src/main/resources/application.properties
     * @return the virtual file reflecting the file path
     */
    public static VirtualFile getVirtualFile(String projecKey, String relativeFilePath) {
        return VfsUtil.findRelativeFile(relativeFilePath, getProjectBaseDir(projecKey));
    }

    /**
     * Read the physical file from the file system and return as a String.
     * Note that the VFS is used to obtain the file so any unsaved edits will be missed.
     * @param projecKey  essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
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
             Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {

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
     * @param projectKey essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
     * @param contentRoot to ve searched for
     * @return the content root if its found, nulll otherwise.
     */
    protected static VirtualFile getSpecificContentRootFromCache(final String projectKey, final String contentRoot) {
        LOG.warn("STUDIO: INFO: getSpecificContentRootFromCache contentRoot = " + contentRoot);
        VirtualFile targetContentRoot = null;
        VirtualFile[] contentRootVFiles = ProjectRootManager.getInstance(UiContext.getProject(projectKey)).getContentRoots();
        LOG.warn("STUDIO: INFO: getSpecificContentRootFromCache contentRootVFiles = " + Arrays.asList(contentRootVFiles));

        if (contentRootVFiles.length != 1) {
            for (VirtualFile vFile : contentRootVFiles) {
                if (vFile.toString().endsWith(contentRoot)) {
                    targetContentRoot = vFile;
                }
            }
        } else {
            LOG.warn("STUDIO: Could not find the content root for directory [" + contentRoot + "] from content roots [" + Arrays.toString(contentRootVFiles) + "]");
        }
        LOG.warn("STUDIO: INFO: getSpecificContentRootFromCache targetContentRoot = " + targetContentRoot);
        return targetContentRoot;
    }

//    /**
//     * Create the supplied directory in the PSI file system if it does not already exist
//     * @param parent directory to contain the new directory
//     * @param newDirectoryName to be created
//     * @return the directory created or a handle to the existing directory if it exists
//     */
//    public static PsiDirectory createOrGetDirectory(PsiDirectory parent, String newDirectoryName) {
//        PsiDirectory newDirectory = null;
//        if (parent != null && newDirectoryName != null) {
//            newDirectory = parent.findSubdirectory(newDirectoryName);
//            if (newDirectory == null) {
//                newDirectory = parent.createSubdirectory(newDirectoryName);
//            }
//        }
//        return newDirectory;
//    }

    /**
     * Ensures all directories in the specified path exist, creating them if necessary.
     *
     * @param projectKey essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
     * @param parentDir The VirtualFile of the parent directory, this is typically the content root of the project.
     * @param relativePath      The directory path relative to parentDir
     * @param token     Used to split the relative path into individual directories
     * @return The PsiDirectory for the final directory in the path.
     * @throws IllegalArgumentException if the parent directory is null or invalid.
     */
    public static PsiDirectory createOrGetDirectory2(String projectKey, VirtualFile parentDir, String relativePath, String token) {
        LOG.info("STUDIO: started createOrGetDirectory2 : parentDir was " + parentDir + " is directory " + parentDir.isDirectory() + " projectKey " + projectKey + " relativePath " + relativePath + " token " + token) ;
        if (parentDir == null || !parentDir.isDirectory()) {
            LOG.info("STUDIO: SERIOUS: parentDir was " + parentDir + " is directory " + parentDir.isDirectory() + " projectKey " + projectKey + " relativePath " + relativePath + " token " + token) ;
            throw new IllegalArgumentException("Invalid parent directory: " + (parentDir == null ? "null" : parentDir.getPath()));
        }
        String[] pathComponents = relativePath.split(token); // Split the path into components
        VirtualFile currentDir = parentDir;
        LOG.info("STUDIO: INFO: pathComponents = " + Arrays.asList(pathComponents));
        for (String component : pathComponents) {
            LOG.info("STUDIO: INFO: component = " + component);
            // skip leading slash or accidental //
            if (!component.isBlank()) {
                currentDir = ensureDirectoryExists(projectKey, currentDir, component).getVirtualFile();
            }
        }
        // Return the PsiDirectory for the final directory in the path
        return PsiManager.getInstance(UiContext.getProject(projectKey)).findDirectory(currentDir);
    }


    /**
     * Ensures the directory exists at the specified level, creating it if necessary.
     *
     * @param projectKey essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
     * @param parentDir    The VirtualFile of the parent directory.
     * @param directoryName The name of the directory to ensure.
     * @return The PsiDirectory for the created or existing directory.
     */
    private static PsiDirectory ensureDirectoryExists(String projectKey, VirtualFile parentDir, String directoryName) {
        final VirtualFile[] newDir = new VirtualFile[1]; // To hold the created directory
        ;
        VirtualFile existingDir = parentDir.findChild(directoryName);
        WriteCommandAction.runWriteCommandAction(UiContext.getProject(projectKey), () -> {

            if (existingDir == null) {
                // Directory doesn't exist, create it
                try {
                    newDir[0] = parentDir.createChildDirectory(null, directoryName);
                } catch (IOException e) {
                    String messages = "An error attempting tp create directory " + directoryName + " message was " + e.getMessage();
                    displayIdeaWarnMessage(UiContext.getProject(projectKey).getName(), messages);
                    LOG.warn("Studio: Serious: " + messages);
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
     * @param projectKey essentially project.getIName(), we NEVER pass project because the IDE can refresh at any time.
     * @param basePackage the package that contains the subpackage leaves
     * @param subPackagesToKeep a set of package names tha are valid i.e. you want to kepp
     */
    public static void deleteSubPackagesNotIn(String projectKey, final String contentRoot, final String basePackage, Set<String> subPackagesToKeep) {
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
//    /**
//     * Create the directories for the supplied package name, returning the handle to the leaf directory
//     * @param sourceRootDir that contains the package
//     * @param qualifiedPackage i.e. dotted notation
//     * @return parent directory of package
//     */
//    public static PsiDirectory createPackage(PsiDirectory sourceRootDir, String qualifiedPackage)
//            throws IncorrectOperationException {
//        PsiDirectory parentDir = sourceRootDir;
//        if (sourceRootDir != null) {
//            StringTokenizer token = new StringTokenizer(qualifiedPackage, ".");
//            while (token.hasMoreTokens()) {
//                String dirName = token.nextToken();
//                parentDir = createOrGetDirectory(parentDir, dirName);
//            }
//        }
//        return parentDir;
//    }
//
//    /**
//     * Wrapper for creating directories within a WriteCommandAction.
//     *
//     * @param parentDirectory The parent directory VirtualFile.
//     * @param relativePath    The relative path to create.
//     * @param token           Used to split the relative path into individual directories
//     * @return The VirtualFile for the deepest subdirectory created or found.
//     */
//    public static VirtualFile createDirectoriesInWriteAction(VirtualFile parentDirectory, String relativePath, String token) {
//        return WriteCommandAction.writeCommandAction(null).compute(() -> {
//            try {
//                return createVirtualDirectories(parentDirectory, relativePath, token);
//            } catch (Exception e) {
//                throw new RuntimeException("Failed to create directories: " + relativePath, e);
//            }
//        });
//    }
//
//    /**
//     * Ensures the directory structure exists, creating any missing directories along the path.
//     * This must be executed in a writeCommandAction
//     * @param parentDirectory The parent directory VirtualFile.
//     * @param relativePath    The relative path to create (e.g., "dir1/dir2" or "dir1.dir2").
//     * @param token           Used to split the relative path into individual directories
//     * @return The VirtualFile for the deepest subdirectory created or found.
//     * @throws Exception if directory creation fails.
//     */
//    public static VirtualFile createVirtualDirectories(VirtualFile parentDirectory, String relativePath, String token) throws Exception {
//        String[] parts = relativePath.split(token);
//        VirtualFile current = parentDirectory;
//        for (String part : parts) {
//            VirtualFile next = current.findChild(part);
//            if (next == null) {
//                // Create the missing directory
//                current = current.createChildDirectory(null, part);
//            } else {
//                current = next;
//            }
//        }
//        LOG.warn("MMM returning created directory " + current);
//        return current;
//    }
}