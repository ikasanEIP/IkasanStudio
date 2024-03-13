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
            String json = jsonModelPsiFile.getText();
            Module newModule;
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
    }


    /**
     * Add the new dependencies IF they are not already in the pom
     * @param projectKey for the project being worked on
     * @param newDependencies to be added, a map of Dependency.getManagementKey() -> Dependency
     */
    public static void pomAddDependencies(String projectKey, List<Dependency> newDependencies) {
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

        // Always need to re-get the PsiFile since a reload might have disposed previous handle.
        PsiFile currentPomPsiFile = pomGetTopLevel(project);

        PsiDirectory containingDirectory = currentPomPsiFile.getContainingDirectory();
        String fileName = currentPomPsiFile.getName();

        // Delete the old POM file
        if (currentPomPsiFile != null) {
            currentPomPsiFile.delete();
        }

        XmlFile newPomFile = (XmlFile)PsiFileFactory.getInstance(project).createFileFromText(fileName, FileTypeManager.getInstance().getFileTypeByExtension(Context.XML_FILE_EXTENSION), pomAsString);
        // When you add the file to the directory, you need the resulting psiFile not the one you sent in.
        newPomFile = (XmlFile)containingDirectory.add(newPomFile);
        CodeStyleManager.getInstance(project).reformat(newPomFile);
    }


    /**
     * Attempt to get the top level pom for the project
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
        long t3 = System.currentTimeMillis();

        message.append("] method 1 = ")
                .append(t2-t1)
                .append(" ms method 2 = ")
                .append(t3-t2);
        return message.toString();
    }


    public static void refreshCodeFromModelAndCauseRedraw(String projectKey) {
        // @TODO MODEL
        PIPSIIkasanModel pipsiIkasanModel = Context.getPipsiIkasanModel(projectKey);
        pipsiIkasanModel.generateJsonFromModelInstance();
        pipsiIkasanModel.generateSourceFromModelInstance();
        Context.getDesignerCanvas(projectKey).setInitialiseAllDimensions(true);
        Context.getDesignerCanvas(projectKey).repaint();
    }

    public static void getAllSourceRootsForProject(Project project) {
        String projectName = project.getName();
        VirtualFile[] vFiles = ProjectRootManager.getInstance(project).getContentSourceRoots();
        String sourceRootsList = Arrays.stream(vFiles).map(VirtualFile::getUrl).collect(Collectors.joining("\n"));
        LOG.info("Source roots for the " + projectName + " plugin:\n" + sourceRootsList +  "Project Properties");
    }

    public static final String JAVA_CODE = "main/java";
    public static final String JAVA_RESOURCES = "main/resources";

    public static final String JAVA_TESTS = "test";
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
     */
    public static PsiDirectory createOrGetDirectory(PsiDirectory parent, String newDirectoryName) {
        PsiDirectory newDirectory = null;
        if (parent != null && newDirectoryName != null) {
            boolean alreadyExists = false;

            for (PsiDirectory subdirectoryOfParent : parent.getSubdirectories()) {
                if (subdirectoryOfParent.getName().equalsIgnoreCase(newDirectoryName)) {
                    newDirectory = subdirectoryOfParent;
                    alreadyExists = true;
                    break;
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
}
