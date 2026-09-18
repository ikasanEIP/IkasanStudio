package org.ikasan.studio.core.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.studio.core.io.ComponentIO;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class OfflineModelProposalTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String OPS = """
            [{"type":"renameComponent","flow":"Transfer","component":"ReadFiles","name":"ReadEvents"}]
            """;

    static String proposal(byte[] saved, String operations) throws Exception {
        return JSON.writeValueAsString(Map.of("formatVersion", 1,
                "baseModelSha256", OfflineModelProposal.sha256(saved), "operations", JSON.readTree(operations)));
    }

    @Test void acceptsMatchingSavedAndLiveModelWithoutChangingEither() throws Exception {
        var live = ModelProposalTest.model();
        byte[] saved = ComponentIO.toValidatedModuleJson(live).getBytes(StandardCharsets.UTF_8);
        var before = LiveModelSnapshot.capture(live);
        var prepared = OfflineModelProposal.prepare(proposal(saved, OPS), saved, before);
        assertThat(prepared.draft().getFlows().get(0).getConsumer().getIdentity()).isEqualTo("ReadEvents");
        assertThat(LiveModelSnapshot.capture(live)).isEqualTo(before);
    }

    @Test void rejectsStaleDiskAndUnsavedLiveChanges() throws Exception {
        var live = ModelProposalTest.model();
        byte[] saved = ComponentIO.toValidatedModuleJson(live).getBytes(StandardCharsets.UTF_8);
        String proposal = proposal(saved, OPS);
        assertThatThrownBy(() -> OfflineModelProposal.prepare(proposal,
                (new String(saved, StandardCharsets.UTF_8) + " ").getBytes(StandardCharsets.UTF_8), LiveModelSnapshot.capture(live)))
                .hasMessageContaining("saved model has changed");
        live.getFlows().get(0).getConsumer().setName("ChangedInStudio");
        assertThatThrownBy(() -> OfflineModelProposal.prepare(proposal, saved, LiveModelSnapshot.capture(live)))
                .hasMessageContaining("Studio has changes");
    }

    @Test void rejectsMalformedUnsupportedAndAmbiguousFiles() throws Exception {
        var live = ModelProposalTest.model();
        byte[] saved = ComponentIO.toValidatedModuleJson(live).getBytes(StandardCharsets.UTF_8);
        String valid = proposal(saved, OPS);
        for (String invalid : new String[]{"[]", valid.replace("\"formatVersion\":1", "\"formatVersion\":2"),
                valid.replace("\"formatVersion\":1", "\"formatVersion\":1,\"formatVersion\":1"),
                valid + "{}", valid.replace("\"operations\":", "\"unknown\":0,\"operations\":"),
                proposal(saved, "[]"), "x".repeat(1_048_577)}) {
            assertThatThrownBy(() -> OfflineModelProposal.prepare(invalid, saved, LiveModelSnapshot.capture(live)))
                    .isInstanceOf(Exception.class);
        }
    }
}
