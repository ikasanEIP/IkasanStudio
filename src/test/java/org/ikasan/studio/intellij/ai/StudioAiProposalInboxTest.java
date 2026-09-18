package org.ikasan.studio.intellij.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class StudioAiProposalInboxTest {
    @TempDir Path root;

    @Test void ignoresOldFilesAndAnnouncesNewFilesOnlyAfterTheyStopChanging() throws Exception {
        Path folder = Files.createDirectory(root.resolve("ai-proposals"));
        Path old = Files.writeString(folder.resolve("old.studio-proposal.json"), "old");
        var inbox = new StudioAiProposalInbox();
        assertThat(inbox.observe(StudioAiProposalInbox.scan(folder))).isEmpty();
        assertThat(inbox.observe(StudioAiProposalInbox.scan(folder))).isEmpty();
        Path fresh = Files.writeString(folder.resolve("new.studio-proposal.json"), "partial");
        assertThat(inbox.observe(StudioAiProposalInbox.scan(folder))).isEmpty();
        Files.writeString(fresh, "complete proposal");
        assertThat(inbox.observe(StudioAiProposalInbox.scan(folder))).isEmpty();
        assertThat(inbox.observe(StudioAiProposalInbox.scan(folder))).contains(fresh);
        assertThat(inbox.observe(StudioAiProposalInbox.scan(folder))).isEmpty();
        Files.writeString(old, "updated proposal");
        assertThat(inbox.observe(StudioAiProposalInbox.scan(folder))).isEmpty();
        assertThat(inbox.observe(StudioAiProposalInbox.scan(folder))).contains(old);
    }

    @Test void supportsMissingFolderAndAtomicRenameAndIgnoresOtherFilesAndDirectories() throws Exception {
        Path folder = root.resolve("ai-proposals");
        var inbox = new StudioAiProposalInbox();
        assertThat(inbox.observe(StudioAiProposalInbox.scan(folder))).isEmpty();
        Files.createDirectory(folder);
        Path temporary = Files.writeString(folder.resolve("proposal.tmp"), "ready");
        Files.createDirectory(folder.resolve("directory.studio-proposal.json"));
        Files.writeString(folder.resolve("unrelated.json"), "{}");
        assertThat(inbox.observe(StudioAiProposalInbox.scan(folder))).isEmpty();
        Path target = Files.move(temporary, folder.resolve("ready.studio-proposal.json"));
        assertThat(inbox.observe(StudioAiProposalInbox.scan(folder))).isEmpty();
        assertThat(inbox.observe(StudioAiProposalInbox.scan(folder))).contains(target);
        Files.delete(target);
        assertThat(inbox.observe(StudioAiProposalInbox.scan(folder))).isEmpty();
        assertThat(StudioAiProposalInbox.latest(StudioAiProposalInbox.scan(folder))).isEmpty();
    }

    @Test void latestUsesModificationTimeWithDeterministicTiesAndKeepsProjectsSeparate() {
        Path a = root.resolve("a.studio-proposal.json"), b = root.resolve("b.studio-proposal.json");
        var older = new StudioAiProposalInbox.Stamp(1, FileTime.fromMillis(1));
        var newer = new StudioAiProposalInbox.Stamp(1, FileTime.fromMillis(2));
        assertThat(StudioAiProposalInbox.latest(Map.of(a, newer, b, older))).contains(a);
        assertThat(StudioAiProposalInbox.latest(Map.of(a, newer, b, newer))).contains(b);
        var first = new StudioAiProposalInbox();
        var second = new StudioAiProposalInbox();
        first.observe(Map.of()); second.observe(Map.of());
        first.observe(Map.of(a, newer));
        assertThat(second.observe(Map.of())).isEmpty();
        assertThat(first.observe(Map.of(a, newer))).contains(a);
    }
}
