package org.ikasan.studio.ui.actions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StopTestMailServerActionTest {
    @Test
    void redactsHarnessSecretsAndPrivateKeyPaths() {
        String diagnostic = "password=ikasan privateKeyFilename=/home/user/.ssh/id_ed25519 "
                + "passphrase: secret";

        String redacted = StopTestMailServerAction.redact(diagnostic);

        assertThat(redacted)
                .doesNotContain("ikasan", "id_ed25519", "secret")
                .contains("password=<redacted>", "privateKeyFilename=<redacted>", "passphrase: <redacted>");
    }
}
