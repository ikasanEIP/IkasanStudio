package org.ikasan.studio.intellij.ai;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.HeavyPlatformTestCase;

public class StudioAiProposalBannerTest extends HeavyPlatformTestCase {
    public void testSuccessfulApplicationClearsEarlierBannerButFailureAndNewerProposalDoNot() {
        var properties = PropertiesComponent.getInstance(getProject());
        var inbox = getProject().getService(StudioAiProposalInboxService.class);
        var owner = Disposer.newDisposable();
        try {
            properties.setValue(StudioAiProposalInboxService.PENDING, "old-draft.studio-proposal.json");
            properties.setValue(StudioAiProposalInboxService.REVISION, "old");
            var banner = new StudioAiProposalBanner(getProject(), owner);
            var failed = new java.util.concurrent.CompletableFuture<Void>();
            inbox.clearPreviousAfterSuccess(failed);
            failed.completeExceptionally(new IllegalStateException("Generation failed"));
            com.intellij.util.ui.UIUtil.dispatchAllInvocationEvents();
            assertTrue(banner.isVisible());
            var generation = new java.util.concurrent.CompletableFuture<Void>();
            inbox.clearPreviousAfterSuccess(generation);
            assertTrue(banner.isVisible());
            generation.complete(null);
            com.intellij.util.ui.UIUtil.dispatchAllInvocationEvents();
            assertFalse(banner.isVisible());
            assertNull(inbox.getPendingProposal());

            properties.setValue(StudioAiProposalInboxService.PENDING, "old-draft.studio-proposal.json");
            properties.setValue(StudioAiProposalInboxService.REVISION, "old");
            var delayed = new java.util.concurrent.CompletableFuture<Void>();
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
        var properties = PropertiesComponent.getInstance(getProject());
        properties.setValue(StudioAiProposalInboxService.PENDING, "rename.studio-proposal.json");
        properties.setValue(StudioAiProposalInboxService.REVISION, "revision-1");
        var owner = Disposer.newDisposable();
        try {
            var banner = new StudioAiProposalBanner(getProject(), owner);
            assertTrue(banner.isVisible());
            var inbox = getProject().getService(StudioAiProposalInboxService.class);
            assertEquals("rename.studio-proposal.json", inbox.getPendingProposal().getFileName().toString());
            Disposer.dispose(owner);
            owner = Disposer.newDisposable();
            var reopened = new StudioAiProposalBanner(getProject(), owner);
            assertTrue(reopened.isVisible());
            // A newly constructed service restores from the same persisted workspace properties.
            var restored = new StudioAiProposalInboxService(getProject());
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
        var properties = PropertiesComponent.getInstance(getProject());
        var owner = Disposer.newDisposable();
        try {
            var banner = new StudioAiProposalBanner(getProject(), owner);
            assertFalse(banner.isVisible());
            properties.setValue(StudioAiProposalInboxService.PENDING, "rename.studio-proposal.json");
            properties.setValue(StudioAiProposalInboxService.REVISION, "revision-2");
            getProject().getMessageBus().syncPublisher(StudioAiProposalInboxService.CHANGED).run();
            assertTrue(banner.isVisible());
            var inbox = getProject().getService(StudioAiProposalInboxService.class);
            var path = inbox.getPendingProposal();
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
