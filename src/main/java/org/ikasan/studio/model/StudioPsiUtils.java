package org.ikasan.studio.model;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.IncorrectOperationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.ikasan.studio.Context;
import org.ikasan.studio.StudioException;
import org.ikasan.studio.io.ComponentIO;
import org.ikasan.studio.model.ikasan.IkasanPomModel;
import org.ikasan.studio.model.ikasan.instance.Module;
import org.ikasan.studio.model.psi.PIPSIIkasanModel;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.ikasan.studio.model.ikasan.IkasanPomModel.MAVEN_COMPILER_SOURCE;
import static org.ikasan.studio.model.ikasan.IkasanPomModel.MAVEN_COMPILER_TARGET;

public class StudioPsiUtils {
    private static final Logger LOG = Logger.getInstance("#StudioPsiUtils");

    // Enforce utility nature upon class
    private StudioPsiUtils() {}

//    public static String getSimpleFileData(PsiFile file) {
//        StringBuilder message = new StringBuilder();
//        if(file != null)
//        {
//            message.append("File name was [" + file.getName() +"]\n");
//            message.append("File was [" + file +"]\n");
//            Language lang = file.getLanguage();
//            message.append("Language was [" + lang.getDisplayName().toLowerCase() +"]");
//        }
//        return message.toString();
//    }

//    /**
//     * Remove the start and end quotes from a String to prevent double quoting.
//     * @param value to be examined
//     * @return the string with the start and end quites removed if there were any present
//     */
//    public static String stripStartAndEndQuotes(String value) {
//        if (value != null && !value.isEmpty()) {
//            if (value.startsWith("\"")) {
//                value = value.substring(1);
//            }
//            if (value.endsWith("\"")) {
//                value = value.substring(0,value.length()-1);
//            }
//        }
//        return value;
//    }
//
//    public static String findClassFile(Project project, String className) {
//        PsiFile[] files = FilenameIndex.getFilesByName(project, className, GlobalSearchScope.projectScope(project));
//        return Arrays.stream(files).map(x->"looking for file " + className + ", found [" + x.getName() +"]").collect(Collectors.joining(","));
//    }
//
//    /**
//     * Attempt to load in all files ending in properties
//     * @TODO we could scope this to the JAVA root to avoid catching test properties.
//     * @param project to load the files file
//     * @return a java Properties instance
//     */
//    public static Properties getApplicationProperties(Project project) {
//        Properties properties = new Properties();
//
//        Collection<VirtualFile> virtualFiles = FileTypeIndex.getFiles(FileTypeManager.getInstance().getFileTypeByExtension(Context.PROPERTIES_FILE_EXTENSION), GlobalSearchScope.projectScope(project));
//        for (VirtualFile virtualFile : virtualFiles) {
//            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
//            try {
//                properties.load(new StringReader(psiFile.getText()));
//            } catch (IOException e) {
//                LOG.warn("Problems loading in application properties " + e);
//            }
//        }
//        return properties;
//    }

    /**
     * Load the content of the major pom
     * @param project currently being worked on
     * @return the studio representation of the POM
     */
    public static IkasanPomModel pomLoad(Project project) {
        IkasanPomModel pom = Context.getPom(project.getName());
        if (pom == null) {
            Model model;
            PsiFile pomPsiFile = pomGetTopLevel(project);
            if (pomPsiFile != null) {
                try (Reader reader = new StringReader(pomPsiFile.getText())) {
                    MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
                    model = xpp3Reader.read(reader);
                    pom = new IkasanPomModel(model, pomPsiFile);
                    Context.setPom(project.getName(), pom);
                } catch (IOException | XmlPullParserException ex) {
                    LOG.warn("Unable to load project pom", ex);
                }
            }
        }
        return pom;
    }




    //@ todo make a plugin property to switch on / off assumeModuleConfigClass
    public static void generateModelInstanceFromJSON(String projectKey, boolean assumeModuleConfigClass) {
        PsiFile jsonModelPsiFile = StudioPsiUtils.getModelFile(Context.getProject(projectKey));
        if (jsonModelPsiFile != null) {
//        Module ikasanModule = Context.getIkasanModule(projectKey);
//
//        PsiDirectory modelDirectory = getModelDirectory(Context.getProject(projectKey));
//
//
//        PsiFile psiFile = modelDirectory.findFile(Context.JSON_MODEL_FILE) ;
//        if (psiFile != null ) {
            String json = jsonModelPsiFile.getText();
            Module newModule = null;
            try {
                newModule = ComponentIO.deserializeModuleInstanceString(json, Context.JSON_MODEL_FULL_PATH);
            } catch (StudioException e) {
                LOG.warn("Could not read module json");
                throw new RuntimeException(e);
            }

            Context.setIkasanModule(projectKey, newModule);
        } else {
            LOG.warn("Could not read the model.json");
        }
//        ikasanModule.reset();
//        PIPSIIkasanModel pipsiIkasanModel = Context.getPipsiIkasanModel(projectKey);
//        if (pipsiIkasanModel.getModuleConfigClazz() == null || !pipsiIkasanModel.getModuleConfigClazz().isValid() ) {
//            updatePIPSIIkasanModelWithModuleConfigClazz(projectKey, assumeModuleConfigClass);
//        }
//        if (pipsiIkasanModel.getModuleConfigClazz() != null && pipsiIkasanModel.getModuleConfigClazz().isValid()) {
//            pipsiIkasanModel.updateIkasanModule();
//        }
//        ikasanModule.resetRegenratePermissions();
    }


    /**
     * Add the new dependencies IF they are not already in the pom
     * @param projectKey for the project being worked on
     * @param newDependencies to be added, a map of Dependency.getManagementKey() -> Dependency
     */
    public static void pomAddDependancies(String projectKey, List<Dependency> newDependencies) {
        IkasanPomModel pom;
        if (newDependencies != null && !newDependencies.isEmpty()) {
            Project project = Context.getProject(projectKey);
            pom = pomLoad(project); // Have to load each time because might have been independently updated.

            if (pom != null) {
                for (Dependency newDependency : newDependencies) {
                    pom.addDependency(newDependency);
                }
            }
            pomAddStandardProperties(pom);
            if (pom.isDirty()) {
                pomSave(project, pom);
//            ProjectManager.createFlowElement().reloadProject(project);
            }
        }
    }

    /**
     * Add in the standard properties for the pom, based on project level config e.g. JDK
     * @param pom is the root level pom to be updated
     */
    private static void pomAddStandardProperties(IkasanPomModel pom) {
        pom.addProperty(MAVEN_COMPILER_TARGET, "1.8");
        pom.addProperty(MAVEN_COMPILER_SOURCE, "1.8");
    }

    public static void pomSave(final Project project, final IkasanPomModel pom) {

        String pomAsString = pom.getModelAsString();
//        MavenXpp3Writer writer = new MavenXpp3Writer();
//        StringWriter pomStringWriter = new StringWriter();
//        try {
//            writer.write(pomStringWriter, pom.getModel());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        // Always need to re-get the PsiFile since a reload might have disposed previous handle.
        PsiFile currentPomPsiFile = pomGetTopLevel(project);

        PsiDirectory containtingDirectory = currentPomPsiFile.getContainingDirectory();
        String fileName = currentPomPsiFile.getName();

        // Delete the old POM file
        if (currentPomPsiFile != null) {
            currentPomPsiFile.delete();
        }

//        pom.setPomPsiFile(pomGetTopLevel(project));
//        PsiDirectory containtingDirectory = pom.getPomPsiFile().getContainingDirectory();
//        String fileName = pom.getPomPsiFile().getComponentName();
//
//        // Delete the old POM file
//        if (pom.getPomPsiFile() != null) {
//            pom.getPomPsiFile().delete();
//            pom.setPomPsiFile(null);
//        }

        XmlFile newPomFile = (XmlFile)PsiFileFactory.getInstance(project).createFileFromText(fileName, FileTypeManager.getInstance().getFileTypeByExtension(Context.XML_FILE_EXTENSION), pomAsString);
        // When you add the file to the directory, you need the resulting psiFile not the one you sent in.
        newPomFile = (XmlFile)containtingDirectory.add(newPomFile);
        CodeStyleManager.getInstance(project).reformat(newPomFile);
//        pom.setPomPsiFile(newPomFile);
    }


    /**
     * Attempt to get the top level pom for the prject
     * @param project to be searched
     * @return A PsiFile handle to the pom or null
     */
    public static PsiFile pomGetTopLevel(Project project) {
        PsiFile[] pomFiles = PsiShortNamesCache.getInstance(project).getFilesByName("pom.xml");
        if (pomFiles.length > 0) {
            return pomFiles[0];
        } else {
            return null;
        }
    }

    public static PsiFile createFile1(final String filename, final String text, Project project) {
        //  ************************* PsiJavaParserFacadeImpl ********************************
        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
        PsiMethodCallExpression equalsCall = (PsiMethodCallExpression) factory.createExpressionFromText("a.equals(b)", null);
//        @NotNull com.intellij.openapi.module.Module[] module = ModuleManager.getInstance(project).getModules();
//        PsiDirectory baseDir = PsiDirectoryFactory.getInstance(project).createOrGetDirectory(project.getBaseDir());
        return PsiFileFactory.getInstance(project).createFileFromText(filename, FileTypeManager.getInstance().getFileTypeByExtension(Context.JAVA_FILE_EXTENSION), text);
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

        // Mehtod 2 has now been deprecated.
//        message.append("] method 2 found [");
//        PsiFile[] files = FilenameIndex.getFilesByName(project, filename, GlobalSearchScope.projectScope(project));
//        for (PsiFile myFile : files) {
//            message.append("" + myFile.getName() +"] ");
//        }
        long t3 = System.currentTimeMillis();

        message.append("] method 1 = " + (t2-t1) + " ms method 2 = " + (t3-t2));
        return message.toString();
    }


    public static void refreshCodeFromModelAndCauseRedraw(String projectKey) {
        // @TODO MODEL
        PIPSIIkasanModel pipsiIkasanModel = Context.getPipsiIkasanModel(projectKey);
        pipsiIkasanModel.generateJsonFromModelInstance();
        pipsiIkasanModel.generateSourceFromModelInstance();
//        StudioPsiUtils.generateModelInstanceFromJSON(projectKey, false);
        Context.getDesignerCanvas(projectKey).setInitialiseAllDimensions(true);
        Context.getDesignerCanvas(projectKey).repaint();
    }

//    public static String dumpProject(Project project) {
//        StringBuilder dump = new StringBuilder();
//        dump.append("** All class names **");
//        String[] classNames = PsiShortNamesCache.getInstance(project).getAllClassNames();
//        dump.append(classNames);
//        dump.append("** All field names **");
//        String[] fieldNames = PsiShortNamesCache.getInstance(project).getAllFieldNames();
//        dump.append(fieldNames);
//        dump.append("** All file names **");
//        String[] fileNames = PsiShortNamesCache.getInstance(project).getAllFileNames();
//        dump.append(fileNames);
//        dump.append("** All method names **");
//        String[] methodNames = PsiShortNamesCache.getInstance(project).getAllMethodNames();
//        dump.append(methodNames);
//        return dump.toString();
//    }

    public static void getAllSourceRootsForProject(Project project) {
        String projectName = project.getName();
        VirtualFile[] vFiles = ProjectRootManager.getInstance(project).getContentSourceRoots();
        String sourceRootsList = Arrays.stream(vFiles).map(VirtualFile::getUrl).collect(Collectors.joining("\n"));
        LOG.info("Source roots for the " + projectName + " plugin:\n" + sourceRootsList +  "Project Properties");
    }

    public static String JAVA_CODE = "main/java";
    public static String JAVA_RESOURCES = "main/resources";

    public static String JAVA_TESTS = "test";
    /**
     * Get the source root that contains the supplied string, possible to get java source, resources or test
     * @param project to work on
     * @param relativeRootDir to look for e.g. main/java, main/resources
     * @return the rood of the module / source directory that contains the supplied string.
     */
    public static VirtualFile getSourceRootEndingWith(Project project, String relativeRootDir) {
        VirtualFile sourceCodeRoot = null;
        VirtualFile[] srcRootVFiles = ProjectRootManager.getInstance(project).getContentSourceRoots();
        for (VirtualFile vFile : srcRootVFiles) {
            if (vFile.toString().endsWith(relativeRootDir)) {
                sourceCodeRoot = vFile;
                break;
            }
        }
        return sourceCodeRoot;
    }

    public static PsiFile getModelFile(final Project project) {
        PsiFile jsonModel = null;
        VirtualFile[] contentRootVFiles = ProjectRootManager.getInstance(project).getContentRoots();
        if (contentRootVFiles == null || contentRootVFiles.length != 1) {
            LOG.warn("Could not find content root directory [" + Arrays.toString(contentRootVFiles) + "]");
        } else {
            VirtualFile contentRoot = contentRootVFiles[0];
            jsonModel = getFileFromPath(project, contentRootVFiles[0], "src/" + Context.JSON_MODEL_FULL_PATH);
//            targetDir = getDirectory(project, contentRoot, "src/" + Context.JSON_MODEL_DIR);

//            PsiDirectoryFactory.getInstance(project).createDirectory(contentRoot);
////            PsiDirectory contentDir = PsiDirectoryFactory.getInstance(project).createDirectory(contentRoot);
//            targetDir = PsiDirectoryFactory.getInstance(project).createDirectory(contentRoot);
// VirtualFileManager.getInstance().findFileByUrl("file://" + localPath);
//            PsiDirectory srcDir = StudioPsiUtils.createOrGetDirectory(contentDir, "src", createIfNotExists);
//            PsiDirectory mainDir = StudioPsiUtils.createOrGetDirectory(srcDir, Context.JSON_MODEL_PARENT_DIR, createIfNotExists);
//            targetDir = StudioPsiUtils.createOrGetDirectory(mainDir, Context.JSON_MODEL_SUB_DIR, createIfNotExists);
        }
        return jsonModel;
    }

    public static PsiFile getFileFromPath(final Project project, VirtualFile root, String filePath) {
        PsiFile retunrFile = null;

        if (root != null && filePath != null && filePath.length() > 1) {
            String fileName = FilenameUtils.getName(filePath);
            String directoryPath = FilenameUtils.getFullPathNoEndSeparator(filePath);
            PsiDirectory psiDirectory = getDirectory(project, root, directoryPath);
            if (psiDirectory != null) {
                retunrFile = psiDirectory.findFile(fileName);
            }
        }
        return retunrFile;
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
            String[] subDirs = target.split(Pattern.quote(File.separator));
            for(String dir : subDirs) {
                returnDirectory = returnDirectory.findSubdirectory(dir);
                if (returnDirectory == null) {
                    break;
                }
            }
        }
        return returnDirectory;
    }


    public static PsiDirectory getOrCreateSourceRootEndingWith(final Project project, final String relativeRootDir) {
        PsiDirectory targetDir = null;
        VirtualFile sourceCodeRoot = StudioPsiUtils.getSourceRootEndingWith(project, relativeRootDir);
        if (sourceCodeRoot == null) {
            VirtualFile[] contentRootVFiles = ProjectRootManager.getInstance(project).getContentRoots();
            if (contentRootVFiles == null || contentRootVFiles.length != 1) {
                LOG.warn("Could not find the source root for directory [" + relativeRootDir + "] or the content root [" + Arrays.toString(contentRootVFiles) + "]");
            } else {
                VirtualFile contentRoot = contentRootVFiles[0];
                PsiDirectory contentDir = PsiDirectoryFactory.getInstance(project).createDirectory(contentRoot);
                PsiDirectory srcDir = StudioPsiUtils.createOrGetDirectory(contentDir, "src");
                targetDir = StudioPsiUtils.createOrGetDirectory(srcDir, relativeRootDir);
            }
        } else {
            targetDir = PsiDirectoryFactory.getInstance(project).createDirectory(sourceCodeRoot);
        }
        return targetDir;
    }


    /**
     * Create the supplied directory in the PSI file system if it does not already exist
     * @param parent directory to contain the new directory
     * @param newDirectoryName to be created
     * @return the directory created or a handle to the existing directory if it exists
     * @throws IncorrectOperationException
     */
    public static PsiDirectory createOrGetDirectory(PsiDirectory parent, String newDirectoryName)
            throws IncorrectOperationException {
        PsiDirectory newDirectory = null;
        if (parent != null || newDirectoryName != null) {
            Boolean alreadyExists = false;

            if (newDirectoryName != null) {
                for (PsiDirectory subdirectoryOfParent : parent.getSubdirectories()) {
                    if (subdirectoryOfParent.getName().equalsIgnoreCase(newDirectoryName)) {
                        newDirectory = subdirectoryOfParent;
                        alreadyExists = true;
                        break;
                    }
                }
            }
            // doesn't exist, create it.
            if (!alreadyExists) {
                newDirectory = parent.createSubdirectory(newDirectoryName);
            }
        }

        return newDirectory;
    }





    /**
     * Create the directories for the supplied package name, returning the handle to the leaf directory
     * @param sourceRootDir that contains the package
     * @param qualifiedPackage i.e. dotted notation
     * @return parent directory of pacjage
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
}
