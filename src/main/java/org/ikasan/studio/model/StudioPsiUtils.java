package org.ikasan.studio.model;

import com.intellij.lang.Language;
import com.intellij.lang.jvm.JvmMethod;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.PsiShortNamesCache;
import org.apache.log4j.Logger;
import org.ikasan.studio.Context;
import org.ikasan.studio.Utils;
import org.ikasan.studio.model.Ikasan.IkasanModule;
import org.ikasan.studio.model.psi.PIPSIIkasanModel;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static com.intellij.ide.lightEdit.LightEditUtil.getProject;

public class StudioPsiUtils {
    private static final Logger log = Logger.getLogger(StudioPsiUtils.class);

    public static String getSimpleFileData(PsiFile file) {
        StringBuilder message = new StringBuilder();
        if(file != null)
        {
            message.append("File name was [" + file.getName() +"]\n");
            message.append("File was [" + file.toString() +"]\n");
            Language lang = file.getLanguage();
            message.append("Language was [" + lang.getDisplayName().toLowerCase() +"]");
//         message.append("Content was [" + file.getText() + "-");
        }
        return message.toString();
    }

    public static String findClassFile(Project project, String className) {
        StringBuffer message = new StringBuffer();
        PsiFile[] files = FilenameIndex.getFilesByName(project, className, GlobalSearchScope.projectScope(project));
        for (PsiFile myFile : files) {
            message.append("looking for file " + className + ", found [" + myFile.getName() +"]");
        }
        return message.toString();
    }

    public static String dumpProject(Project project) {
        StringBuilder dump = new StringBuilder();
        dump.append("** All class names **");
        String[] classNames = PsiShortNamesCache.getInstance(project).getAllClassNames();
        dump.append(classNames);
        dump.append("** All field names **");
        String[] fieldNames = PsiShortNamesCache.getInstance(project).getAllFieldNames();
        dump.append(fieldNames);
        dump.append("** All file names **");
        String[] fileNames = PsiShortNamesCache.getInstance(project).getAllFileNames();
        dump.append(fileNames);
        dump.append("** All method names **");
        String[] methodNames = PsiShortNamesCache.getInstance(project).getAllMethodNames();
        dump.append(methodNames);
        return dump.toString();
    }

    /**
     * Primarily, this class is looking for the method that returns 'Module' but could be used to get anything
     *
     * I suspect this is NOT very efficient.
     *
     * @param project
     * @param methodReturnType
     * @return the method whose return type matches methodReturnType
     */
    public  static PsiMethod findFirstMethodByReturnType(Project project, String methodReturnType) {
        PsiMethod returnPsiMethod = null;
        PsiShortNamesCache cache = PsiShortNamesCache.getInstance(project);
        // In this release,  processAllClassNames does not scope all the classnames, in future release it might so keep code.
        
//        cache.processAllClassNames(new Processor<String>() {
//            @Override
//            public boolean process(String className) {
//                System.out.println("try className " + className);
//                if (!returnPsiMethodFound) {
//                    System.out.println("try " + className);
//                    PsiClass[] psiClasses = cache.getClassesByName(className, ProjectScope.getProjectScope(project));
//                    if (psiClasses != null) {
//                        for (PsiClass psiClass : psiClasses) {
//                            System.out.println(" L1 " + psiClass.getName());
//                            PsiMethod[] psiMethods = psiClass.getAllMethods();
//                            if (psiMethods != null) {
//                                for (PsiMethod psiMethod : psiMethods) {
//                                    PsiType returnType = psiMethod.getReturnType();
//
//                                    if (returnType == null) {
//                                        System.out.println(" L2 " + psiMethod.getName() + " ret is null");
//                                    } else {
//                                        System.out.println(" L2 " + psiMethod.getName() + " ret is " + returnType.getCanonicalText(true) + " eq " + returnType.equalsToText(methodReturnType));
//                                    }
//                                    if (returnType != null && returnType.equalsToText(methodReturnType)) {
//                                        returnPsiMethod = psiMethod;
//                                        break;
//                                    }
//                                }
//                            }
//                            if (returnPsiMethod != null) {
//                                break;
//                            }
//                        }
//                    }
//                }
//                return returnPsiMethodFound;
//            }
//        }, ProjectScope.getProjectScope(project), IdFilter.getProjectIdFilter(project, false));
//        return returnPsiMethod;

// or this should work but does not

//        cache.processAllMethodNames(new Processor<String>() {
//            @Override
//            public boolean process(String methodNames) {
//    System.out.println("try methodNames " + methodNames);
//                    if (!returnPsiMethodFound) {
//                        PsiMethod[] psiMethods = cache.getMethodsByName( methodNames, ProjectScope.getProjectScope(project));
//                        if (psiMethods != null) {
//                            for (PsiMethod psiMethod : psiMethods) {
//                                PsiType returnType = psiMethod.getReturnType();
//    System.out.println(" L2 " + psiMethod.getName() + " ret is " + returnType.getCanonicalText(true) + " eq " + returnType.equalsToText(methodReturnType));
//                                if (returnType != null && returnType.equalsToText(methodReturnType)) {
//                                    returnPsiMethod = psiMethod;
//                                    returnPsiMethodFound = true;
//                                    break;
//                                }
//                            }
//                        }
//                    }
//                return returnPsiMethodFound;
//            }
//        }, ProjectScope.getProjectScope(project), IdFilter.getProjectIdFilter(project, false));
//        return returnPsiMethod;
//            });


        // Very brute force, go through 52K classes, if its not in project scope ignore, otherwise inspect
        for (String className : cache.getAllClassNames()) {
            PsiClass[] psiClasses = cache.getClassesByName(className, ProjectScope.getProjectScope(project));
            if (psiClasses != null) {
                for (PsiClass psiClass : psiClasses) {
                    returnPsiMethod = findMethodFromClassByReturnType(psiClass, methodReturnType);
                    if (returnPsiMethod != null) {
                        break;
                    }
                }
            }
            if (returnPsiMethod != null) {
                break;
            }
        }
        return returnPsiMethod;
    }

    /**
     * For the given class, look for the first method with the given return type.
     * @param psiClass to look for
     * @param methodReturnType (canonical and fully qualified) string of the return type searched for.
     * @return
     */
    public static PsiMethod findMethodFromClassByReturnType(PsiClass psiClass, String methodReturnType) {
        PsiMethod methodFound = null ;
        PsiMethod[] psiMethods = psiClass.getAllMethods();
        if (psiMethods != null) {
            for (PsiMethod psiMethod : psiMethods) {
                PsiType returnType = psiMethod.getReturnType();
                //@todo determine if "<?>" needs to be here or elsewhere, maybe pass in
                if (returnType != null && (returnType.equalsToText(methodReturnType) || returnType.equalsToText(methodReturnType+"<?>"))) {
                    methodFound = psiMethod;
                    break;
                }
            }
        }
        return methodFound;
    }

    /***
     * Find the class in the project
     * @param project the class
     * @param className the non-qualified or fully qualified classname
     * @return
     */
    public static PsiClass[] findClass(Project project, String className) {
        // Note, getClassesByName will only work with non-qualified classname
        @NotNull PsiClass[] files = null;
        if (className.contains(".")) {
            String baseClassName = Utils.getLastToken( "\\.", className);
            files = PsiShortNamesCache.getInstance(project).getClassesByName(baseClassName, ProjectScope.getProjectScope(project));
            if (null != files) {
                files = Arrays.stream(files).filter(x -> className.equals(x.getQualifiedName())).toArray(PsiClass[]::new);
            }
        } else {
            files = PsiShortNamesCache.getInstance(project).getClassesByName(className, ProjectScope.getProjectScope(project));
        }

        if (files != null) {
            for (PsiClass myClass : files) {
                log.debug("looking for class " + className + ", found [" + myClass.getName() +"]");
            }
        }
        return files;
    }

     /***
     * Find the first class of the given classname in the project
     * @param project the class
     * @param className the non-qualified or fully qualified classname
     * @return
     */
    public static PsiClass findFirstClass(Project project, String className) {
        PsiClass[] classes = findClass(project, className);
        if (classes.length > 0) {
            if (classes.length > 1) {
                log.warn("Found more than one class of name " + className+ " but only expected 1");
            }
            return classes[0];
        } else {
            return null;
        }
    }

    public static JvmMethod findFirstMethod (PsiClass clazz, String methodName) {
        JvmMethod[] methods = clazz.findMethodsByName(methodName);
        if (methods.length > 0) {
            if (methods.length > 1) {
                log.warn("Found more than one class of name " + methodName + " but only expected 1");
            }
            return methods[0];
        } else {
            return null;
        }
    }

    //@ todo make a plugin property to switch on / off assumeModuleConfigClass
    public static void resetIkasanModuleFromSourceCode(String projectKey, boolean assumeModuleConfigClass) {
        IkasanModule ikasanModule = Context.getIkasanModule(projectKey);
        ikasanModule.reset();
        PsiClass moduleConfigClazz = null ;
        Project project = Context.getProject(projectKey);
        if (!assumeModuleConfigClass) {
            //@todo this seems quite expensive, see if better way
            PsiMethod getModuleMethod = findFirstMethodByReturnType(project, PIPSIIkasanModel.MODULE_BEAN_CLASS);
            if (getModuleMethod != null) {
                moduleConfigClazz = getModuleMethod.getContainingClass();
            }
        } else {
            moduleConfigClazz = StudioPsiUtils.findFirstClass(Context.getProject(projectKey), "ModuleConfig");
        }
        if (moduleConfigClazz != null && moduleConfigClazz.getContainingFile() != null) {
            PIPSIIkasanModel pipsiIkasanModel = Context.getPipsiIkasanModel(projectKey);
            pipsiIkasanModel.setModuleConfigClazz(moduleConfigClazz);
            pipsiIkasanModel.updateIkasanModule();
        }
    }

//    private OgnlFile createFile(final String text) throws {
//        return (OgnlFile) PsiFileFactory.getInstance(getProject())
//                .createFileFromText("test.ognl", OgnlLanguage.INSTANCE, text);
//    }
}
