package org.ikasan.studio.intellij.testing;

import com.intellij.execution.RunManager;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.PsiTestUtil;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class RunFlowTestActionHeavyTest extends HeavyPlatformTestCase {
    private TestProducer producer;
    private VirtualFile testFile;

    @Override protected void setUp() throws Exception {
        super.setUp();
        createTestProjectStructure("src/test/testData/ikasanStandardSampleApps/general/");
        StudioProjectFiles.createJavaSourceFile(myProject, "/user-flow-tests", "src/test/java", "org.example",
                "GreetingFlowTest", "package org.example; public class GreetingFlowTest {}", null);
        VirtualFile projectBaseDir = StudioProjectFiles.getProjectBaseDir(myProject);
        assertNotNull(projectBaseDir);
        testFile = projectBaseDir.findFileByRelativePath("user-flow-tests/src/test/java/org/example/GreetingFlowTest.java");
        assertNotNull(testFile);
        VirtualFile root = testFile.getParent().getParent().getParent();
        PsiTestUtil.addContentRoot(myModule, root);
        PsiTestUtil.addSourceRoot(myModule, root, true);
        producer = new TestProducer();
        RunConfigurationProducer.EP_NAME.getPoint().registerExtension(producer, getTestRootDisposable());
    }

    public void testResolvesOwningModuleAndHonoursProducerFirstRun() {
        var target = ApplicationManager.getApplication().executeOnPooledThread(
                () -> ReadAction.compute(() -> RunFlowTestAction.resolve(myProject, testFile)));
        // Pump the event queue rather than blocking the EDT while platform producer code resolves.
        com.intellij.testFramework.PlatformTestUtil.waitForFuture(target);
        try {
            var resolved = target.get();
            assertNotNull(resolved);
            var configuration = (ApplicationConfiguration) resolved.settings().getConfiguration();
            assertEquals("org.example.GreetingFlowTest", configuration.getMainClassName());
            assertSame(myModule, configuration.getConfigurationModule().getModule());
            var launched = new AtomicBoolean();
            resolved.start(() -> launched.set(true));
            assertTrue(producer.firstRun);
            assertTrue(launched.get());
        } catch (Exception failure) { throw new AssertionError(failure); }
    }

    public void testReusesExistingConfigurationAndPreservesCustomSettings() {
        var original = ReadAction.compute(() -> RunFlowTestAction.resolve(myProject, testFile));
        assertNotNull(original);
        ((ApplicationConfiguration) original.settings().getConfiguration()).setVMParameters("-Dflow.custom=true");
        RunManager.getInstance(myProject).addConfiguration(original.settings());
        var resolved = ReadAction.compute(() -> RunFlowTestAction.resolve(myProject, testFile));
        assertNotNull(resolved);
        assertSame(original.settings(), resolved.settings());
        assertEquals("-Dflow.custom=true", ((ApplicationConfiguration) resolved.settings().getConfiguration()).getVMParameters());
    }

    public void testUnimportedFileDoesNotCreateConfiguration() {
        var file = new LightVirtualFile("GreetingFlowTest.java", com.intellij.ide.highlighter.JavaFileType.INSTANCE,
                "package org.example; public class GreetingFlowTest {}");
        assertNull(ReadAction.compute(() -> RunFlowTestAction.resolve(myProject, file)));
    }

    /** Exercises platform producer selection without depending on a particular test-framework plugin. */
    public static final class TestProducer extends LazyRunConfigurationProducer<ApplicationConfiguration> {
        boolean firstRun;
        @Override public @NotNull ConfigurationFactory getConfigurationFactory() {
            return ApplicationConfigurationType.getInstance().getConfigurationFactories()[0];
        }
        @Override protected boolean setupConfigurationFromContext(@NotNull ApplicationConfiguration configuration,
                ConfigurationContext context, @NotNull Ref<PsiElement> sourceElement) {
            if (!(context.getPsiLocation() instanceof PsiClass type) || !"GreetingFlowTest".equals(type.getName())) return false;
            configuration.setMainClassName(type.getQualifiedName());
            configuration.setModule(context.getModule());
            configuration.setName("GreetingFlowTest");
            return true;
        }
        @Override public boolean isConfigurationFromContext(@NotNull ApplicationConfiguration configuration, ConfigurationContext context) {
            return context.getPsiLocation() instanceof PsiClass type
                    && java.util.Objects.equals(type.getQualifiedName(), configuration.getMainClassName())
                    && configuration.getConfigurationModule().getModule() == context.getModule();
        }
        @Override public void onFirstRun(@NotNull ConfigurationFromContext configuration, @NotNull ConfigurationContext context, Runnable launch) {
            firstRun = true;
            launch.run();
        }
    }
}
