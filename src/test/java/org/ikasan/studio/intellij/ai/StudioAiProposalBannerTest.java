package org.ikasan.studio.intellij.ai;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.openapi.Disposable;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class StudioAiProposalBannerTest extends HeavyPlatformTestCase {
    public void testSuccessfulApplicationClearsEarlierBannerButFailureAndNewerProposalDoNot() {
        PropertiesComponent properties = PropertiesComponent.getInstance(getProject());
        StudioAiProposalInboxService inbox = getProject().getService(StudioAiProposalInboxService.class);
        Disposable owner = Disposer.newDisposable();
        try {
            properties.setValue(StudioAiProposalInboxService.PENDING, "old-draft.studio-proposal.json");
            properties.setValue(StudioAiProposalInboxService.REVISION, "old");
            StudioAiProposalBanner banner = new StudioAiProposalBanner(getProject(), owner);
            CompletableFuture<Void> failed = new java.util.concurrent.CompletableFuture<>();
            inbox.clearPreviousAfterSuccess(failed);
            failed.completeExceptionally(new IllegalStateException("Generation failed"));
            com.intellij.util.ui.UIUtil.dispatchAllInvocationEvents();
            assertTrue(banner.isVisible());
            CompletableFuture<Void> generation = new java.util.concurrent.CompletableFuture<>();
            inbox.clearPreviousAfterSuccess(generation);
            assertTrue(banner.isVisible());
            generation.complete(null);
            com.intellij.util.ui.UIUtil.dispatchAllInvocationEvents();
            assertFalse(banner.isVisible());
            assertNull(inbox.getPendingProposal());

            properties.setValue(StudioAiProposalInboxService.PENDING, "old-draft.studio-proposal.json");
            properties.setValue(StudioAiProposalInboxService.REVISION, "old");
            CompletableFuture<Void> delayed = new java.util.concurrent.CompletableFuture<>();
            inbox.clearPreviousAfterSuccess(delayed);
            properties.setValue(StudioAiProposalInboxService.PENDING, "new-request.studio-proposal.json");
            properties.setValue(StudioAiProposalInboxService.REVISION, "new");
            getProject().getMessageBus().syncPublisher(StudioAiProposalInboxService.CHANGED).run();
            delayed.complete(null);
            com.intellij.util.ui.UIUtil.dispatchAllInvocationEvents();
            assertTrue(banner.isVisible());
            assertEquals("new-request.studio-proposal.json", inbox.getPendingProposal().getFileName().toString());
        } finally {
            Disposer.dispose(owner);
            properties.unsetValue(StudioAiProposalInboxService.PENDING);
            properties.unsetValue(StudioAiProposalInboxService.REVISION);
        }
    }

    public void testPendingProposalRestoresAcrossEditorRecreationAndDismissPersists() {
        PropertiesComponent properties = PropertiesComponent.getInstance(getProject());
        properties.setValue(StudioAiProposalInboxService.PENDING, "rename.studio-proposal.json");
        properties.setValue(StudioAiProposalInboxService.REVISION, "revision-1");
        Disposable owner = Disposer.newDisposable();
        try {
            StudioAiProposalBanner banner = new StudioAiProposalBanner(getProject(), owner);
            assertTrue(banner.isVisible());
            StudioAiProposalInboxService inbox = getProject().getService(StudioAiProposalInboxService.class);
            assertEquals("rename.studio-proposal.json", inbox.getPendingProposal().getFileName().toString());
            Disposer.dispose(owner);
            owner = Disposer.newDisposable();
            StudioAiProposalBanner reopened = new StudioAiProposalBanner(getProject(), owner);
            assertTrue(reopened.isVisible());
            // A newly constructed service restores from the same persisted workspace properties.
            StudioAiProposalInboxService restored = new StudioAiProposalInboxService(getProject());
            assertEquals(inbox.getPendingProposal(), restored.getPendingProposal());
            inbox.dismissPending();
            assertFalse(reopened.isVisible());
            assertNull(restored.getPendingProposal());
            assertNull(properties.getValue(StudioAiProposalInboxService.REVISION));
        } finally {
            Disposer.dispose(owner);
            properties.unsetValue(StudioAiProposalInboxService.PENDING);
            properties.unsetValue(StudioAiProposalInboxService.REVISION);
        }
    }

    public void testOlderReviewCannotClearNewerProposalAndUpdatesRefreshVisibleBanner() {
        PropertiesComponent properties = PropertiesComponent.getInstance(getProject());
        Disposable owner = Disposer.newDisposable();
        try {
            StudioAiProposalBanner banner = new StudioAiProposalBanner(getProject(), owner);
            assertFalse(banner.isVisible());
            properties.setValue(StudioAiProposalInboxService.PENDING, "rename.studio-proposal.json");
            properties.setValue(StudioAiProposalInboxService.REVISION, "revision-2");
            getProject().getMessageBus().syncPublisher(StudioAiProposalInboxService.CHANGED).run();
            assertTrue(banner.isVisible());
            StudioAiProposalInboxService inbox = getProject().getService(StudioAiProposalInboxService.class);
            Path path = inbox.getPendingProposal();
            inbox.dismiss(path, "revision-1");
            assertTrue(banner.isVisible());
            inbox.dismiss(path, "revision-2");
            assertFalse(banner.isVisible());
        } finally {
            Disposer.dispose(owner);
            properties.unsetValue(StudioAiProposalInboxService.PENDING);
            properties.unsetValue(StudioAiProposalInboxService.REVISION);
        }
    }
}
