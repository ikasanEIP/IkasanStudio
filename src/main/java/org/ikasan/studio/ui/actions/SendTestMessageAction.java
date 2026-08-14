package org.ikasan.studio.ui.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.ikasan.studio.core.StudioBuildUtils;
import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.StudioUIUtils;
import org.ikasan.studio.ui.UiContext;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * Prompts for a payload and POSTs it to the running module's /studio/inject/{flowName} endpoint (generated
 * by StudioInjectControllerTemplate), which is only reachable when the module was launched via "Debug module"
 * (the studio-debug Spring profile). Bypasses the real broker entirely.
 */
public class SendTestMessageAction implements ActionListener {
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);
    // Ikasan's default seeded admin user (see overwriteDBCreation.sql in ikasan-h2-standalone-persistence),
    // granted the ALL authority so it can reach any endpoint. If a module's admin password has been changed
    // from the default, this will fail with a 401 - see the dedicated message for that case below.
    private static final String DEFAULT_CREDENTIALS = "admin:admin";

    private final Project project;
    private final BasicElement ikasanBasicElement;

    public SendTestMessageAction(Project project, BasicElement ikasanBasicElement) {
        this.project = project;
        this.ikasanBasicElement = ikasanBasicElement;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (!(ikasanBasicElement instanceof FlowElement flowElement) || !flowElement.getComponentMeta().isConsumer()) {
            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.SendTestMessageCanOnlyBeUsedOnConsumers"));
            return;
        }

        SendTestMessagePayloadDialog payloadDialog = new SendTestMessagePayloadDialog(project);
        if (!payloadDialog.showAndGet()) {
            return;
        }
        String payload = payloadDialog.getPayload();

        Module module = project.getService(UiContext.class).getIkasanModule();
        String flowName = flowElement.getContainingFlow().getIdentity();
        String port = module.getPort() != null ? module.getPort() : "8080";

        ProgressManager.getInstance().run(new Task.Backgroundable(project, StudioBundle.message("message.SendingTestMessage")) {
            // Deliberately not @NotNull-annotated: this project avoids @NotNull (see CLAUDE.md) because
            // the IntelliJ Gradle plugin instruments it with a runtime assertion that would surface as an
            // uncaught plugin exception rather than failing gracefully.
            @SuppressWarnings("NullableProblems")
            @Override
            public void run(ProgressIndicator indicator) {
                try {
                    // The generated app sets server.servlet.context-path to the module's name (see
                    // propertiesTemplate_en.ftl), so every endpoint lives under /<module-name>/... - the same
                    // transform (StudioBuildUtils.toUrlString) must be used here to match it exactly.
                    // Flow names can contain spaces/other characters that aren't valid raw path characters
                    // (e.g. "flow 1") - the multi-arg URI constructor percent-encodes the path for us,
                    // unlike URI.create(String), which throws on illegal characters.
                    String contextPath = "/" + StudioBuildUtils.toUrlString(module.getIdentity());
                    URI uri = new URI("http", null, "localhost", Integer.parseInt(port), contextPath + "/studio/inject/" + flowName, null, null);

                    ObjectMapper objectMapper = new ObjectMapper();
                    String requestBody = objectMapper.writeValueAsString(Map.of("payload", payload));

                    HttpClient client = HttpClient.newBuilder()
                            .connectTimeout(HTTP_TIMEOUT)
                            .build();
                    String credentials = Base64.getEncoder().encodeToString(DEFAULT_CREDENTIALS.getBytes(StandardCharsets.UTF_8));
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(uri)
                            .timeout(HTTP_TIMEOUT)
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Basic " + credentials)
                            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                            .build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        JsonNode responseBody = objectMapper.readTree(response.body());
                        String identifier = responseBody.path("identifier").asText("");
                        ApplicationManager.getApplication().invokeLater(() ->
                                StudioUIUtils.displayIdeaInfoMessage(project, StudioBundle.message("message.TestMessageSent", identifier)));
                    } else if (response.statusCode() == 401) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.TestMessageAuthenticationFailed")));
                    } else {
                        String errorDetail = response.statusCode() + ": " + response.body();
                        ApplicationManager.getApplication().invokeLater(() ->
                                StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.CouldNotSendTestMessage", errorDetail)));
                    }
                } catch (Exception e) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            StudioUIUtils.displayIdeaWarnMessage(project, StudioBundle.message("message.CouldNotSendTestMessage", e.getMessage())));
                }
            }
        });
    }
}
