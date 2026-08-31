package org.ikasan.studio.ui.model.psi;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.GeneratorUtils;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.model.StudioPsiUtils;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta.REQUIRES_STUB;
import static org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta.USER_IMPLEMENTED_CLASS_NAME;

/**
 * Heavy because relocating a class needs a real (disposable) Project for PSI/refactor-processor support -
 * see {@link org.ikasan.studio.ui.model.StudioPsiStudioBuildUtilsHeavyTests} for the same pattern.
 */
public class UserImplementedClassRelocatorHeavyTest extends HeavyPlatformTestCase {
    private static final String TEST_DATA_DIR = "/ikasanStandardSampleApps/general/";
    private Module module;
    private Flow flow1;
    private Flow flow2;

    static {
        // Loading metapack component metadata transitively touches Apache MINA SSHD's IoServiceFactory, whose
        // static init spins up a background NIO2 (AsynchronousChannelGroup) thread the first time anything in
        // this JVM loads it - the thread is process-lifetime, not per-test. If this test class happens to be
        // the first thing in the whole suite to touch metapack loading, HeavyPlatformTestCase's ThreadLeakTracker
        // (which snapshots threads at the start of each individual test's setUp()) sees that thread appear
        // during this test and misreports it as leaked by this test. Forcing the load here, in a static
        // initializer that runs at class-load time - before any test's setUp() snapshot is taken - makes sure
        // the thread already exists by the time any per-test leak check runs, whichever test class ends up first.
        // See PIPSIIkasanModelPropertiesNavigationHeavyTest for the same workaround.
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
        FlowElement dummyConsumer1 = TestFixtures.getEventGeneratingConsumer(TestFixtures.BASE_META_PACK);
        flow1 = TestFixtures.getUnbuiltFlow(TestFixtures.BASE_META_PACK).name("MyFlow1").consumer(dummyConsumer1).build();
        FlowElement dummyConsumer2 = TestFixtures.getEventGeneratingConsumer(TestFixtures.BASE_META_PACK);
        flow2 = Flow.flowBuilder().metapackVersion(TestFixtures.BASE_META_PACK).description("second flow").name("MyFlow2").consumer(dummyConsumer2).build();
        module = TestFixtures.getMyFirstModuleIkasanModule(TestFixtures.BASE_META_PACK, List.of(flow1, flow2));
    }

    public void test_neverGeneratedYet_noFileOperationsAtAll() throws Exception {
        // Broker's class name defaults from its own componentName field ("__fieldName:componentName" in its
        // metadata), not from a flow-embedding template - a cross-flow move must leave the name itself alone.
        FlowElement broker = TestFixtures.getBroker(TestFixtures.BASE_META_PACK);
        broker.setContainingFlow(flow1);
        broker.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, "F1TestBroker");
        String oldPackage = GeneratorUtils.getUserImplementedClassesPackageName(module, flow1);

        UserImplementedClassRelocator.relocateIfNeeded(myProject, module, broker, flow1, flow2);

        assertThat("nothing was ever generated, so no file should have been created",
                StudioPsiUtils.getUserImplementedClassFile(myProject, oldPackage, "F1TestBroker"), nullValue());
        assertThat("the class name is componentName-derived, not flow-derived - it must not change",
                broker.getPropertyValue(USER_IMPLEMENTED_CLASS_NAME), is("F1TestBroker"));
        String newPackage = GeneratorUtils.getUserImplementedClassesPackageName(module, flow2);
        assertThat("still nothing generated at the new location either - that's the normal generation path's job",
                StudioPsiUtils.getUserImplementedClassFile(myProject, newPackage, "F1TestBroker"), nullValue());
    }

    public void test_debugMove_deletesOrphanedStub_andRetargetsIdentity() throws Exception {
        FlowElement debug = TestFixtures.getDebugTransition(TestFixtures.BASE_META_PACK);
        debug.setContainingFlow(flow1);
        String oldClassName = "F1AnchorDebug";
        debug.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, oldClassName);
        String oldPackage = GeneratorUtils.getUserImplementedClassesPackageName(module, flow1);
        StudioPsiUtils.createJavaSourceFile(myProject, StudioPsiUtils.USER_CONTENT_ROOT, StudioPsiUtils.SRC_MAIN_JAVA_CODE,
                oldPackage, oldClassName, "package " + oldPackage + ";\n\npublic class " + oldClassName + " {\n}\n", null);
        assertThat("sanity: the old stub should exist before the move",
                StudioPsiUtils.getUserImplementedClassFile(myProject, oldPackage, oldClassName), notNullValue());

        UserImplementedClassRelocator.relocateIfNeeded(myProject, module, debug, flow1, flow2);

        assertThat("Debug content is always regenerated unconditionally, so the orphaned old stub should be deleted rather than relocated",
                StudioPsiUtils.getUserImplementedClassFile(myProject, oldPackage, oldClassName), nullValue());
        assertThat(debug.getPropertyValue(USER_IMPLEMENTED_CLASS_NAME), is(not(oldClassName)));
    }

    public void test_nonDebugMove_relocatesHandWrittenClassToNewPackage_preservingContentAndName() throws Exception {
        FlowElement broker = TestFixtures.getBroker(TestFixtures.BASE_META_PACK);
        broker.setContainingFlow(flow1);
        String className = "F1TestBroker";
        broker.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, className);
        String oldPackage = GeneratorUtils.getUserImplementedClassesPackageName(module, flow1);
        String distinctiveMarker = "// DISTINCTIVE_HAND_WRITTEN_MARKER_" + System.nanoTime();
        String originalContent = "package " + oldPackage + ";\n\npublic class " + className + " {\n    " + distinctiveMarker + "\n}\n";
        StudioPsiUtils.createJavaSourceFile(myProject, StudioPsiUtils.USER_CONTENT_ROOT, StudioPsiUtils.SRC_MAIN_JAVA_CODE,
                oldPackage, className, originalContent, null);
        assertThat("sanity: the old, hand-written stub should exist before the move",
                StudioPsiUtils.getUserImplementedClassFile(myProject, oldPackage, className), notNullValue());

        UserImplementedClassRelocator.relocateIfNeeded(myProject, module, broker, flow1, flow2);

        assertThat("the old file must no longer sit at the old flow's package once relocated",
                StudioPsiUtils.getUserImplementedClassFile(myProject, oldPackage, className), nullValue());
        assertThat("the class name itself (componentName-derived) must not have changed, only its package",
                broker.getPropertyValue(USER_IMPLEMENTED_CLASS_NAME), is(className));
        String newPackage = GeneratorUtils.getUserImplementedClassesPackageName(module, flow2);
        VirtualFile newFile = StudioPsiUtils.getUserImplementedClassFile(myProject, newPackage, className);
        assertThat("the relocated file should now exist at the new flow's package under the same name", newFile, notNullValue());
        String relocatedContent = StudioPsiUtils.readVirtualFileAsString(newFile);
        assertThat("the relocated file's content should be readable", relocatedContent, notNullValue());
        assertThat("the hand-written body must survive the relocation intact",
                relocatedContent.contains(distinctiveMarker), is(true));
    }

    public void test_requiresStubFalse_neverTouchesAnExistingUserSuppliedClass() throws Exception {
        FlowElement broker = TestFixtures.getBroker(TestFixtures.BASE_META_PACK);
        broker.setContainingFlow(flow1);
        broker.setPropertyValue(REQUIRES_STUB, false);
        broker.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, "org.example.preexisting.MyOwnBroker");

        UserImplementedClassRelocator.relocateIfNeeded(myProject, module, broker, flow1, flow2);

        assertThat("a user-supplied class not owned/generated by Studio must never be touched",
                broker.getPropertyValue(USER_IMPLEMENTED_CLASS_NAME), is("org.example.preexisting.MyOwnBroker"));
    }

    public void test_classNameCollisionInDestinationFlow_getsSuffixed() throws Exception {
        String collidingName = "SharedBrokerName";

        // A sibling already sitting in flow2 under the name the mover is about to arrive with.
        FlowElement existingSiblingInFlow2 = TestFixtures.getBroker(TestFixtures.BASE_META_PACK);
        existingSiblingInFlow2.setContainingFlow(flow2);
        existingSiblingInFlow2.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, collidingName);
        flow2.getFlowRoute().getFlowElements().add(existingSiblingInFlow2);

        FlowElement movingBroker = TestFixtures.getBroker(TestFixtures.BASE_META_PACK);
        movingBroker.setContainingFlow(flow1);
        movingBroker.setPropertyValue(USER_IMPLEMENTED_CLASS_NAME, collidingName);

        UserImplementedClassRelocator.relocateIfNeeded(myProject, module, movingBroker, flow1, flow2);

        Object resolvedName = movingBroker.getPropertyValue(USER_IMPLEMENTED_CLASS_NAME);
        assertThat("the mover must not collide with the sibling already using that name in the destination flow",
                resolvedName, is(not(collidingName)));
        assertThat("collision avoidance should suffix the base candidate rather than produce something unrelated",
                ((String) resolvedName).startsWith(collidingName), is(true));
    }
}
