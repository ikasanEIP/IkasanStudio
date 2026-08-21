package org.ikasan.studio.ui.model.psi;

import com.intellij.testFramework.HeavyPlatformTestCase;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.PropertiesTemplate;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.ui.model.StudioPsiUtils;
import org.ikasan.studio.ui.viewmodel.AbstractViewHandlerIntellij;
import org.ikasan.studio.ui.viewmodel.IkasanFlowViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandlerCache;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * "Jump to Properties": verifies {@link PIPSIIkasanModel#initialisePsiFileHandles()} - the same public entry
 * point production uses to resolve navigation targets on project reload - correctly locates, in a genuinely
 * generated application.properties, both a component's first externalized property and a flow's bespoke
 * "ikasan.flow.configuration[...]" block, purely by deterministic text search (no markers/comments involved) -
 * and correctly finds no target at all for a component/flow with nothing externalized.
 * Heavy because it needs a real (disposable) Project for PSI/VirtualFile support - see
 * {@link org.ikasan.studio.ui.model.StudioPsiStudioBuildUtilsHeavyTests} for the same pattern. Deliberately a
 * single test method (one Flow with the target, another without) rather than two, to keep the Heavy platform
 * project setup/teardown - the expensive part - to one cycle.
 */
public class PIPSIIkasanModelPropertiesNavigationHeavyTest extends HeavyPlatformTestCase {
    // Same test project root StudioPsiStudioBuildUtilsHeavyTests uses - required for
    // StudioPsiUtils.getProjectBaseDir(project) (and so createPropertiesFile) to resolve a real base directory.
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
        StudioPsiUtils.createPropertiesFile(myProject, propertiesContent);

        UiContext uiContext = myProject.getService(UiContext.class);
        uiContext.setIkasanModule(module);
        uiContext.setViewHandlerFactory(new ViewHandlerCache(myProject));

        AbstractViewHandlerIntellij consumerViewHandlerBefore = ViewHandlerCache.getFlowComponentViewHandler(myProject, ftpConsumer);
        assertThat("no target should exist before initialisePsiFileHandles has run",
                consumerViewHandlerBefore.hasPropertiesNavigationTarget(), is(false));

        new PIPSIIkasanModel(myProject).initialisePsiFileHandles();

        // Component target: FtpConsumer's alphabetically-first externalized property is clientID.
        AbstractViewHandlerIntellij consumerViewHandler = ViewHandlerCache.getFlowComponentViewHandler(myProject, ftpConsumer);
        assertThat(consumerViewHandler.hasPropertiesNavigationTarget(), is(true));
        // Read back from the resolved PsiFile itself, not the pre-write local string - IntelliJ may reformat
        // the written file (e.g. line-ending/whitespace normalisation), which would shift offsets.
        String writtenText = consumerViewHandler.getPropertiesPsiFile().getText();
        int consumerOffset = consumerViewHandler.getOffsetInPropertiesFileToNavigateTo();
        String expectedConsumerKey = "myflow1.ftp.consumer.clientID=";
        assertThat(writtenText.substring(consumerOffset, consumerOffset + expectedConsumerKey.length()), is(expectedConsumerKey));

        // Flow target: the bespoke ikasan.flow.configuration[...] block, keyed by flow identity.
        IkasanFlowViewHandler flowViewHandler = ViewHandlerCache.getFlowViewHandler(myProject, flowWithTargets);
        assertThat(flowViewHandler.hasPropertiesNavigationTarget(), is(true));
        int flowOffset = flowViewHandler.getOffsetInPropertiesFileToNavigateTo();
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
