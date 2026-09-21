package org.ikasan.studio.intellij.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import org.ikasan.studio.core.persistence.json.StudioJson;
import static org.assertj.core.api.Assertions.*;

class StudioAiProposalFeedbackTest {
    @TempDir Path directory;
    @Test void writesCorrelatedReceiptWithoutChangingProposalOrEnteringInbox() throws Exception {
        Path proposal = directory.resolve("example.studio-proposal.json");
        String json = "{\"operations\":[]}";
        Files.writeString(proposal, json);
        var feedback = new StudioAiProposalFeedback(proposal, json);
        feedback.write("rejected", "Operation 2: unknown property");
        var result = StudioJson.newObjectMapper().readTree(Files.readString(StudioAiProposalFeedback.resultPath(proposal)));
        assertThat(result.path("status").asText()).isEqualTo("rejected");
        assertThat(result.path("proposalSha256").asText()).isEqualTo(StudioAiProposalFeedback.sha256(Files.readAllBytes(proposal)));
        assertThat(result.path("message").asText()).contains("Operation 2");
        assertThat(result.path("nextStep").asText()).contains("Retry at most twice");
        assertThat(Files.readString(proposal)).isEqualTo(json);
        assertThat(StudioAiProposalInbox.scan(directory)).containsOnlyKeys(proposal);
        feedback.write("generating", "Generating");
        feedback.write("applied", "Done");
        result = StudioJson.newObjectMapper().readTree(Files.readString(StudioAiProposalFeedback.resultPath(proposal)));
        assertThat(result.path("status").asText()).isEqualTo("applied");
        assertThat(result.path("nextStep").asText()).contains("compile and test");
    }
    @Test void oldCompletionDoesNotOverwriteFeedbackForChangedProposal() throws Exception {
        Path proposal = directory.resolve("example.studio-proposal.json");
        Files.writeString(proposal, "old");
        var old = new StudioAiProposalFeedback(proposal, "old");
        Files.writeString(proposal, "new");
        new StudioAiProposalFeedback(proposal, "new").write("awaiting_review", "Review");
        String expected = Files.readString(StudioAiProposalFeedback.resultPath(proposal));
        old.write("applied", "Old completion");
        assertThat(Files.readString(StudioAiProposalFeedback.resultPath(proposal))).isEqualTo(expected);
    }
}
