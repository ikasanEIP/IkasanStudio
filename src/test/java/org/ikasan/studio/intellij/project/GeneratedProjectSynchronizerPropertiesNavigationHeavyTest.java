package org.ikasan.studio.intellij.project;

import com.intellij.testFramework.HeavyPlatformTestCase;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.PropertiesTemplate;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.viewmodel.AbstractViewHandlerIntellij;
import org.ikasan.studio.ui.viewmodel.IkasanFlowViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * "Jump to Properties": verifies {@link GeneratedProjectSynchronizer#initialisePsiFileHandles()} - the same public entry
 * point production uses to resolve navigation targets on project reload - correctly locates, in a genuinely
 * generated application.properties, both a component's first externalized property and a flow's bespoke
 * "ikasan.flow.configuration[...]" block, purely by deterministic text search (no markers/comments involved) -
 * and correctly finds no target at all for a component/flow with nothing externalized.
 * Heavy because it needs a real (disposable) Project for PSI/VirtualFile support - see
 * {@link StudioPsiStudioBuildUtilsHeavyTests} for the same pattern. Deliberately a
 * single test method (one Flow with the target, another without) rather than two, to keep the Heavy platform
 * project setup/teardown - the expensive part - to one cycle.
 */
public class GeneratedProjectSynchronizerPropertiesNavigationHeavyTest extends HeavyPlatformTestCase {
    // Same test project root StudioPsiStudioBuildUtilsHeavyTests uses - required for
    // StudioProjectFiles.getProjectBaseDir(project) (and so createPropertiesFile) to resolve a real base directory.
    private static final String TEST_DATA_DIR = "/ikasanStandardSampleApps/general/";

    static {
        // Loading metapack component metadata transitively touches Apache MINA SSHD's IoServiceFactory, whose
        // static init spins up a background NIO2 (AsynchronousChannelGroup) thread the first time anything in
        // this JVM loads it - the thread is process-lifetime, not per-test. If this test class happens to be
        // the first thing in the whole suite to touch metapack loading, HeavyPlatformTestCase's ThreadLeakTracker
        // (which snapshots threads at the start of each individual test's setUp()) sees that thread appear
        // during this test and misreports it as leaked by this test. Forcing the load here, in a static
        // initializer that runs at class-load time - before any test's setUp() snapshot is taken - makes sure
        // the thread already exists by the time any per-test leak check runs, whichever test class ends up first.
        try {
            TestFixtures.getEventGeneratingConsumer(TestFixtures.BASE_META_PACK);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createTestProjectStructure("src/test/testData" + TEST_DATA_DIR);
    }

    public void test_routerCodeNavigationIncludesNestedBranchesAndKeepsMissingClassFallback() throws Exception {
        String pack = TestFixtures.BASE_META_PACK;
        Flow flow = TestFixtures.getUnbuiltFlow(pack)
                .consumer(TestFixtures.getEventGeneratingConsumer(pack)).build();
        var root = flow.getFlowRoute();
        var branch = org.ikasan.studio.core.model.ikasan.instance.FlowRoute.flowRouteBuilder()
                .flow(flow).routeName("route1").build();
        root.getChildRoutes().add(branch);
        var rootRouter = router(pack, flow, root, "Multi Recipient Router", "RootRouter");
        var single = router(pack, flow, branch, "Single Recipient Router", "EitherOrRouter");
        var multi = router(pack, flow, branch, "Multi Recipient Router", "NestedMultiRouter");
        var missing = router(pack, flow, branch, "Single Recipient Router", "MissingRouter");
        Module module = TestFixtures.getMyFirstModuleIkasanModule(pack, List.of(flow));
        UiContext context = myProject.getService(UiContext.class);
        context.setIkasanModule(module);
        context.setViewHandlerFactory(new ViewHandlerCache(myProject));
        String flowPackage = org.ikasan.studio.core.generator.Generator.STUDIO_FLOW_PACKAGE + "." + flow.getJavaPackageName();
        String userPackage = org.ikasan.studio.core.generator.GeneratorUtils.getUserImplementedClassesPackageName(module, flow);
        String flowPath = "generated/src/main/java/" + flowPackage.replace('.', '/') + "/" + flow.getJavaClassName() + ".java";
        var base = StudioProjectFiles.getProjectBaseDir(myProject);
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(myProject, () -> {
            try {
                com.intellij.openapi.vfs.VfsUtil.saveText(
                        com.intellij.openapi.vfs.VfsUtil.createDirectoryIfMissing(base, flowPath.substring(0, flowPath.lastIndexOf('/')))
                                .findOrCreateChildData(this, flow.getJavaClassName() + ".java"),
                        "package " + flowPackage + "; public class " + flow.getJavaClassName() + " {}");
                var userDir = com.intellij.openapi.vfs.VfsUtil.createDirectoryIfMissing(base,
                        "user/src/main/java/" + userPackage.replace('.', '/'));
                for (String name : List.of("RootRouter", "EitherOrRouter", "NestedMultiRouter")) {
                    com.intellij.openapi.vfs.VfsUtil.saveText(userDir.findOrCreateChildData(this, name + ".java"),
                            "package " + userPackage + "; public class " + name + " {}");
                }
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
        for (int refresh = 0; refresh < 2; refresh++) {
            new GeneratedProjectSynchronizer(myProject).initialisePsiFileHandles();
            com.intellij.openapi.application.impl.NonBlockingReadActionImpl.waitForAsyncTaskCompletion();
            for (FlowElement router : List.of(rootRouter, single, multi)) {
                var target = ViewHandlerCache.getFlowComponentViewHandler(myProject, router).getCodeNavigationTarget();
                assertTrue(target.isPresent());
                assertEquals(router.getComponentName() + ".java", target.psiFile().getName());
            }
            assertEquals(flow.getJavaClassName() + ".java",
                    ViewHandlerCache.getFlowComponentViewHandler(myProject, missing).getCodeNavigationTarget().psiFile().getName());
        }
    }

    private FlowElement router(String pack, Flow flow,
                               org.ikasan.studio.core.model.ikasan.instance.FlowRoute route,
                               String type, String name) throws Exception {
        var meta = org.ikasan.studio.core.metapack.ComponentLibrary.getIkasanComponentByKeyMandatory(pack, type);
        var router = org.ikasan.studio.core.model.ikasan.instance.FlowElementFactory
                .createFlowElement(pack, meta, flow, route, name);
        router.setPropertyValue("userImplementedClassName", name);
        route.getFlowElements().add(router);
        return router;
    }

    public void test_jumpToPropertiesTargets() throws Exception {
        FlowElement ftpConsumer = TestFixtures.getFtpConsumer(TestFixtures.BASE_META_PACK);
        Flow flowWithTargets = TestFixtures.getUnbuiltFlow(TestFixtures.BASE_META_PACK)
                .consumer(ftpConsumer)
                .build();
        flowWithTargets.setPropertyValue("isRecording", true);

        // A consumer with none of its properties set, in a flow with no recording/etc config either - nothing
        // for either navigation mechanism to find.
        FlowElement eventGeneratingConsumer = TestFixtures.getEventGeneratingConsumer(TestFixtures.BASE_META_PACK);
        Flow flowWithoutTargets = Flow.flowBuilder()
                .metapackVersion(TestFixtures.BASE_META_PACK)
                .description("Flow with nothing externalized")
                .name("MyFlow2")
                .consumer(eventGeneratingConsumer)
                .build();

        Module module = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, List.of(flowWithTargets, flowWithoutTargets));

        String propertiesContent = PropertiesTemplate.create(module);
        StudioProjectFiles.createPropertiesFile(myProject, propertiesContent);

        UiContext uiContext = myProject.getService(UiContext.class);
        uiContext.setIkasanModule(module);
        uiContext.setViewHandlerFactory(new ViewHandlerCache(myProject));

        AbstractViewHandlerIntellij consumerViewHandlerBefore = ViewHandlerCache.getFlowComponentViewHandler(myProject, ftpConsumer);
        assertThat("no target should exist before initialisePsiFileHandles has run",
                consumerViewHandlerBefore.hasPropertiesNavigationTarget(), is(false));

        new GeneratedProjectSynchronizer(myProject).initialisePsiFileHandles();
        com.intellij.openapi.application.impl.NonBlockingReadActionImpl.waitForAsyncTaskCompletion();

        // Component target: FtpConsumer's alphabetically-first externalized property is clientID.
        AbstractViewHandlerIntellij consumerViewHandler = ViewHandlerCache.getFlowComponentViewHandler(myProject, ftpConsumer);
        assertThat(consumerViewHandler.hasPropertiesNavigationTarget(), is(true));
        // Read back from the resolved PsiFile itself, not the pre-write local string - IntelliJ may reformat
        // the written file (e.g. line-ending/whitespace normalisation), which would shift offsets.
        String writtenText = consumerViewHandler.getPropertiesNavigationTarget().psiFile().getText();
        int consumerOffset = consumerViewHandler.getPropertiesNavigationTarget().offset();
        String expectedConsumerKey = "myflow1.ftp.consumer.clientID=";
        assertThat(writtenText.substring(consumerOffset, consumerOffset + expectedConsumerKey.length()), is(expectedConsumerKey));

        // Flow target: the bespoke ikasan.flow.configuration[...] block, keyed by flow identity.
        IkasanFlowViewHandler flowViewHandler = ViewHandlerCache.getFlowViewHandler(myProject, flowWithTargets);
        assertThat(flowViewHandler.hasPropertiesNavigationTarget(), is(true));
        int flowOffset = flowViewHandler.getPropertiesNavigationTarget().offset();
        String expectedFlowPrefix = "ikasan.flow.configuration[MyFlow1].";
        assertThat(writtenText.substring(flowOffset, flowOffset + expectedFlowPrefix.length()), is(expectedFlowPrefix));

        // Negative case: nothing externalized for this component or flow.
        AbstractViewHandlerIntellij noTargetComponentViewHandler = ViewHandlerCache.getFlowComponentViewHandler(myProject, eventGeneratingConsumer);
        assertThat("a component with nothing externalized must not get a target",
                noTargetComponentViewHandler.hasPropertiesNavigationTarget(), is(false));
        IkasanFlowViewHandler noTargetFlowViewHandler = ViewHandlerCache.getFlowViewHandler(myProject, flowWithoutTargets);
        assertThat("a flow with no isRecording/recordedEventTimeToLive/invokeContextListeners must not get a target",
                noTargetFlowViewHandler.hasPropertiesNavigationTarget(), is(false));
    }
}
