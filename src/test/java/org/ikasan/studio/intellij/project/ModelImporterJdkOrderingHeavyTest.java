package org.ikasan.studio.intellij.project;

import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generation.GenerationRequest;
import org.ikasan.studio.core.generator.ModelTemplate;
import org.ikasan.studio.intellij.migration.MigrationJdk;
import org.ikasan.studio.ui.StudioUIUtils;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.mockito.MockedStatic;

public class ModelImporterJdkOrderingHeavyTest extends HeavyPlatformTestCase {
    public void testMavenRefreshFollowsCommittedGenerationAndJdkAlignment() throws Exception {
        String source = ModelTemplate.create(TestFixtures.getMyFirstModuleIkasanModule("V3.3.9", List.of()));
        CompletableFuture<Void> completion = new CompletableFuture<>();
        ArrayList<String> order = new ArrayList<>();
        Sdk sdk = mock(Sdk.class);
        when(sdk.getName()).thenReturn("test-java-11");
        ProjectJdkTable table = mock(ProjectJdkTable.class);
        when(table.getAllJdks()).thenReturn(new Sdk[]{sdk});
        MavenProjectsManager manager = mock(MavenProjectsManager.class);
        doAnswer(invocation -> { order.add("maven"); return null; }).when(manager).forceUpdateAllProjectsOrFindAllAvailablePomFiles();
        try (MockedStatic<StudioProjectFiles> files = mockStatic(StudioProjectFiles.class);
             MockedStatic<ProjectJdkTable> jdks = mockStatic(ProjectJdkTable.class);
             MockedStatic<MigrationJdk> migration = mockStatic(MigrationJdk.class);
             MockedStatic<MavenProjectsManager> maven = mockStatic(MavenProjectsManager.class);
             MockedStatic<StudioUIUtils> messages = mockStatic(StudioUIUtils.class)) {
            files.when(() -> StudioProjectFiles.refreshCodeFromModel(myProject, GenerationRequest.full(), false)).thenReturn(completion);
            jdks.when(ProjectJdkTable::getInstance).thenReturn(table);
            migration.when(() -> MigrationJdk.matches(sdk, 11)).thenReturn(true);
            migration.when(() -> MigrationJdk.apply(myProject, sdk, 11)).thenAnswer(invocation -> { order.add("jdk"); return null; });
            maven.when(() -> MavenProjectsManager.getInstance(myProject)).thenReturn(manager);
            ModelImporter.importFromText(myProject, source, "import ordering test");
            assertTrue("Do not configure or refresh before generation commits", order.isEmpty());
            files.verify(() -> StudioProjectFiles.refreshCodeFromModel(myProject, GenerationRequest.full(), false));
            completion.complete(null);
            PlatformTestUtil.waitWithEventsDispatching("Import completion never refreshed Maven", () -> order.size() == 2, 10);
            assertEquals(List.of("jdk", "maven"), order);
            messages.verify(() -> StudioUIUtils.displayIdeaInfoMessage(eq(myProject), anyString()));
        }
    }

    public void testFailedGenerationDoesNotReconfigureJdkOrRefreshMaven() throws Exception {
        String source = ModelTemplate.create(TestFixtures.getMyFirstModuleIkasanModule("V3.3.9", List.of()));
        CompletableFuture<Void> completion = new CompletableFuture<>();
        MavenProjectsManager manager = mock(MavenProjectsManager.class);
        AtomicBoolean warned = new java.util.concurrent.atomic.AtomicBoolean();
        try (MockedStatic<StudioProjectFiles> files = mockStatic(StudioProjectFiles.class);
             MockedStatic<MigrationJdk> migration = mockStatic(MigrationJdk.class);
             MockedStatic<MavenProjectsManager> maven = mockStatic(MavenProjectsManager.class);
             MockedStatic<StudioUIUtils> messages = mockStatic(StudioUIUtils.class)) {
            files.when(() -> StudioProjectFiles.refreshCodeFromModel(myProject, GenerationRequest.full(), false)).thenReturn(completion);
            maven.when(() -> MavenProjectsManager.getInstance(myProject)).thenReturn(manager);
            messages.when(() -> StudioUIUtils.displayIdeaWarnMessage(eq(myProject), anyString()))
                    .thenAnswer(invocation -> { warned.set(true); return null; });
            ModelImporter.importFromText(myProject, source, "failed import ordering test");
            completion.completeExceptionally(new IllegalStateException("injected generation failure"));
            PlatformTestUtil.waitWithEventsDispatching("Generation failure was not reported", warned::get, 10);
            migration.verifyNoInteractions();
            verifyNoInteractions(manager);
        }
    }
}
