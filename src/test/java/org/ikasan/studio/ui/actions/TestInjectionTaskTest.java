package org.ikasan.studio.ui.actions;

import com.intellij.openapi.progress.ProcessCanceledException;
import org.ikasan.studio.ui.StudioBundle;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TestInjectionTaskTest {
    @Test
    void successfulResponsesRetainActionSpecificFeedback() {
        var notice = TestInjectionTask.perform("flow", () -> response(200, "{\"identifier\":\"message-1\"}"),
                json -> TestInjectionTask.Notice.info(json.path("identifier").asText()));
        assertThat(notice).isEqualTo(TestInjectionTask.Notice.info("message-1"));
        assertThat(TestInjectionTask.perform("flow", () -> response(200, "{}"),
                json -> TestInjectionTask.Notice.warning("regenerate")))
                .isEqualTo(TestInjectionTask.Notice.warning("regenerate"));
    }

    @Test
    void authenticationAndHttpFailuresDoNotInvokeSuccessHandler() {
        var authentication = TestInjectionTask.perform("flow", () -> response(401, "not JSON"), json -> {
            throw new AssertionError("Must not invoke success handler");
        });
        assertThat(authentication).isEqualTo(TestInjectionTask.Notice.warning(
                StudioBundle.message("message.TestMessageAuthenticationFailed")));
        var failure = TestInjectionTask.perform("flow", () -> response(503, "unavailable"), json -> {
            throw new AssertionError("Must not invoke success handler");
        });
        assertThat(failure).isNotNull();
        assertThat(failure.warning()).isTrue();
        assertThat(failure.message()).contains("503: unavailable");
    }

    @Test
    void startupAndMalformedResponseFailuresProvideFeedback() {
        assertThat(TestInjectionTask.perform("flow", () -> { throw new ConnectException(); }, json -> null))
                .isEqualTo(TestInjectionTask.Notice.warning(StudioBundle.message("message.ModuleNotYetAcceptingConnections")));
        var malformed = TestInjectionTask.perform("flow", () -> response(200, "not JSON"), json -> {
            throw new AssertionError("Must not invoke success handler");
        });
        assertThat(malformed).isNotNull();
        assertThat(malformed.warning()).isTrue();
    }

    @Test
    void cancellationAndInterruptionAreNotReportedAsModuleFailures() {
        assertThatThrownBy(() -> TestInjectionTask.perform("flow", () -> { throw new ProcessCanceledException(); }, json -> null))
                .isInstanceOf(ProcessCanceledException.class);
        try {
            assertThat(TestInjectionTask.perform("flow", () -> { throw new InterruptedException(); }, json -> null)).isNull();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            // Clear the interrupt flag even if an assertion fails, so later tests are unaffected.
            //noinspection ResultOfMethodCallIgnored
            Thread.interrupted();
        }
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> response(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }
}
