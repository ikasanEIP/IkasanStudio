package org.ikasan.studio.intellij.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.undo.*;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.sun.net.httpserver.HttpServer;
import org.ikasan.studio.core.ai.LiveModelSnapshot;
import org.ikasan.studio.core.ai.ModelProposal;
import org.ikasan.studio.core.generation.GenerationRequest;
import org.ikasan.studio.core.generator.AiProjectContractGenerator;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.intellij.project.StudioProjectFiles;
import org.ikasan.studio.ui.StudioBundle;
import org.ikasan.studio.ui.UiContext;
import org.ikasan.studio.intellij.settings.IkasanStudioSettings;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/** Opt-in project-local bridge. HTTP is private IPC for the bundled MCP stdio adapter. */
@Service(Service.Level.PROJECT)
public final class StudioAiService implements Disposable {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final Project project;
    private final Map<String, Snapshot> snapshots = new LinkedHashMap<>();
    private final Map<String, Proposal> proposals = new LinkedHashMap<>();
    private volatile HttpServer server;
    private volatile boolean disposed;
    private ExecutorService executor;
    private StudioAiConnectionFiles connectionFiles;
    private CompletableFuture<Void> cleanup = CompletableFuture.completedFuture(null);
    private volatile String lastAccessTransport;

    public String getLastAccessTransport() { return lastAccessTransport; }

    /** The native host supplies the target project; never infer it from focus or open-project order. */
    public static String nativeCall(Project project, String name, String arguments) throws Exception {
        StudioAiService service = project.getService(StudioAiService.class);
        if (!service.isRunning()) throw new IllegalStateException(StudioBundle.message("ai.EnableFirst"));
        Object result = service.callFrom(name, JSON.readTree(arguments), "native");
        return JSON.writeValueAsString(result);
    }
    private String configuration;
    private volatile Proposal pending;

    record Snapshot(Module source, Map<String, Object> model) { }
    static final class Proposal {
        boolean userCodeReviewRequired;
        final String id = UUID.randomUUID().toString();
        final Snapshot snapshot;
        final ModelProposal.Prepared prepared;
        final String details;
        volatile String status = "awaiting_review";
        boolean fileBased;
        Proposal(Snapshot snapshot, ModelProposal.Prepared prepared, String details) { this.snapshot = snapshot; this.prepared = prepared; this.details = details; }
    }
    public StudioAiService(Project project) { this.project = project; }
    public boolean isRunning() { return server != null; }

    /** Runs off EDT, returning a ready-to-copy MCP server configuration. */
    public String start() throws Exception {
        synchronized (this) { if (server != null) return configuration; }
        synchronized (this) {
            if (disposed || project.isDisposed()) throw new IllegalStateException("Project closed");
            if (server != null) return configuration;
            cleanup.join();
            String projectPath = project.getBasePath();
            if (projectPath == null) throw new IllegalStateException("Open a project before connecting AI.");
            StudioAiConnectionFiles files;
            try (var source = getClass().getResourceAsStream("/studio/ai/studio-mcp-adapter.jar")) {
                if (source == null) throw new IllegalStateException("Studio MCP adapter is missing");
                files = StudioAiConnectionFiles.open(Path.of(PathManager.getConfigPath()), projectPath, source.readAllBytes());
            }
            Path adapter = files.adapter();
            Path connection = files.connection();
            String token = UUID.randomUUID() + "-" + UUID.randomUUID();
            HttpServer created = null;
            ExecutorService workers = null;
            try {
                created = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                workers = AppExecutorUtil.createBoundedApplicationPoolExecutor("Ikasan Studio AI bridge", 2);
                var protocol = new StudioMcpProtocol((name, arguments) -> callFrom(name, arguments, "adapter"));
                created.createContext("/rpc", exchange -> {
                    try (exchange) {
                        String auth = exchange.getRequestHeaders().getFirst("Authorization");
                        if (exchange.getRequestHeaders().containsKey("Origin") || auth == null
                                || !MessageDigest.isEqual(auth.getBytes(StandardCharsets.UTF_8), ("Bearer " + token).getBytes(StandardCharsets.UTF_8))) {
                            exchange.sendResponseHeaders(403, -1); return;
                        }
                        if (!"POST".equals(exchange.getRequestMethod()) || !"/rpc".equals(exchange.getRequestURI().getPath())) {
                            exchange.sendResponseHeaders(405, -1); return;
                        }
                        byte[] input = exchange.getRequestBody().readNBytes(1_048_577);
                        if (input.length > 1_048_576) { exchange.sendResponseHeaders(413, -1); return; }
                        Object response;
                        try { response = protocol.handle(JSON.readTree(input)); }
                        catch (Exception invalid) { exchange.sendResponseHeaders(400, -1); return; }
                        if (response == null) { exchange.sendResponseHeaders(202, -1); return; }
                        byte[] output = JSON.writeValueAsBytes(response);
                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                        exchange.sendResponseHeaders(200, output.length);
                        exchange.getResponseBody().write(output);
                    }
                });
                created.setExecutor(workers);
                files.publish(JSON.writeValueAsString(Map.of("url", "http://127.0.0.1:" + created.getAddress().getPort() + "/rpc", "token", token)));
                configuration = JSON.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("mcpServers", Map.of("ikasan-studio",
                        Map.of("command", javaExecutable().toString(), "args", List.of("-jar", adapter.toString(), "--connection", connection.toString())))));
                connectionFiles = files;
                lastAccessTransport = null;
                executor = workers;
                server = created;
                created.start();
                return configuration;
            } catch (Exception failure) {
                server = null;
                executor = null;
                connectionFiles = null;
                configuration = null;
                if (created != null) created.stop(0);
                if (workers != null) workers.shutdownNow();
                try { files.close(); } catch (Exception cleanupFailure) { failure.addSuppressed(cleanupFailure); }
                throw failure;
            }
        }
    }

    private static Path javaExecutable() {
        Path bin = Path.of(System.getProperty("java.home"), "bin").toAbsolutePath();
        Path executable = bin.resolve(com.intellij.openapi.util.SystemInfo.isWindows ? "java.exe" : "java");
        if (!Files.isRegularFile(executable)) throw new IllegalStateException("IDE Java executable is missing");
        return executable;
    }

    private Object callFrom(String name, JsonNode arguments, String transport) throws Exception {
        HttpServer session = server;
        Object result = call(name, arguments);
        if (("studio_snapshot".equals(name) || "studio_catalogue".equals(name)) && server == session && session != null)
            lastAccessTransport = transport;
        return result;
    }

    Object call(String name, JsonNode arguments) throws Exception {
        HttpServer session = server;
        if (disposed || session == null) throw new IllegalStateException("Studio AI bridge is stopped");
        if (!arguments.isObject()) throw new IllegalArgumentException("Tool arguments must be an object");
        return switch (name) {
            case "studio_snapshot" -> {
                Snapshot snapshot = onEdt(this::capture);
                String revision = UUID.randomUUID().toString();
                synchronized (this) {
                    requireSession(session);
                    synchronized (snapshots) { snapshots.put(revision, snapshot); trim(snapshots, 8); }
                }
                JsonNode model = JSON.valueToTree(snapshot.model());
                redact(model);
                yield Map.of("revision", revision, "model", model, "project", project.getName(),
                        "note", "Live model snapshot. Known credential fields are redacted; other model content is shared with your connected AI. Supported edits: linear flows only.");
            }
            case "studio_catalogue" -> {
                String version = onEdt(() -> { requireReady(); return context().getIkasanModule().getVersion(); });
                yield JSON.readTree(AiProjectContractGenerator.componentCatalogue(version));
            }
            case "studio_propose" -> propose(arguments, session);
            case "studio_proposal_status" -> {
                Proposal proposal;
                synchronized (proposals) { proposal = proposals.get(arguments.path("proposalId").asText()); }
                if (proposal == null) throw new IllegalArgumentException("Unknown or expired proposal");
                yield Map.of("proposalId", proposal.id, "status", proposal.status);
            }
            default -> throw new IllegalArgumentException("Unknown Studio tool");
        };
    }

    private void requireSession(HttpServer session) {
        if (disposed || server != session) throw new IllegalStateException("Studio AI bridge is stopped or restarted");
    }

    private Object propose(JsonNode arguments, HttpServer session) throws Exception {
        Snapshot snapshot;
        synchronized (snapshots) { snapshot = snapshots.get(arguments.path("revision").asText()); }
        if (snapshot == null) throw new IllegalArgumentException("Unknown or expired revision. Read studio_snapshot again.");
        onEdt(() -> { requireCurrent(snapshot); return null; });
        var prepared = ModelProposal.prepare(snapshot.model(), arguments.path("operations"));
        Proposal proposal = new Proposal(snapshot, prepared, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(arguments.path("operations")));
        // Record and open on EDT; no server lock is held while waiting for it.
        onEdt(() -> {
            synchronized (this) {
                requireSession(session);
                requireCurrent(snapshot);
                if (pending != null) throw new IllegalStateException("Review or cancel the pending proposal in Studio first.");
                pending = proposal;
                synchronized (proposals) { proposals.put(proposal.id, proposal); trim(proposals, 16); }
                reviewOrApply(proposal);
            }
            return null;
        });
        return Map.of("proposalId", proposal.id, "status", proposal.status, "summary", prepared.summary());
    }

    /** Runs off EDT. Import needs no MCP server and never writes the incoming model file. */
    void importProposal(String json) throws Exception { importProposal(json, false); }

    /** Returns false without opening a dialog when a watched file requires manual review. */
    boolean tryAutoImport(String json) throws Exception { return importProposal(json, true); }

    static boolean requiresUserCodeReview(Module... modules) {
        // Full generation can revisit unchanged flows. Transient overwrite flags in the live
        // model are not preserved in the JSON snapshot, so inspect both live and proposed models.
        for (Module module : modules) {
            for (var flow : module.getFlows()) {
                for (var element : flow.getFlowElementsNoExternalEndPoints()) {
                    if (element instanceof org.ikasan.studio.core.model.ikasan.instance.FlowUserImplementedElement implementation
                            && implementation.isOverwriteEnabled()) return true;
                    for (var property : element.getUserSuppliedClassProperties()) {
                        if (property.getMeta().isProtectFromOverwrite() && !property.getMeta().isNoStubRequired()
                                && property.isOverwriteEnabled()) return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean canAutoApply(Proposal proposal) {
        proposal.userCodeReviewRequired = requiresUserCodeReview(proposal.snapshot.source(), proposal.prepared.draft());
        return !IkasanStudioSettings.isAlwaysAskAiApproval() && !proposal.userCodeReviewRequired;
    }

    private void reviewOrApply(Proposal proposal) {
        if (!canAutoApply(proposal)) {
            new StudioAiProposalDialog(project, this, proposal).show();
            return;
        }
        try {
            apply(proposal).whenComplete((ignored, failure) -> ApplicationManager.getApplication().invokeLater(() -> {
                if (project.isDisposed()) return;
                String message = StudioBundle.message(failure == null ? "ai.AutoApplied" : "ai.ApplyGenerationFailed")
                        + "\n" + String.join("\n", proposal.prepared.summary());
                if (failure == null) org.ikasan.studio.ui.StudioUIUtils.displayIdeaInfoMessage(project, message);
                else org.ikasan.studio.ui.StudioUIUtils.displayIdeaErrorMessage(project, message);
            }));
        } catch (RuntimeException failure) {
            cancel(proposal);
            throw failure;
        }
    }

    private boolean importProposal(String json, boolean autoOnly) throws Exception {
        JsonNode operations = JSON.readTree(json).path("operations");
        if (autoOnly && IkasanStudioSettings.isAlwaysAskAiApproval()) return false;
        Snapshot snapshot = onEdt(this::capture);
        Path modelFile = Path.of(project.getBasePath(), "generated", "src", "main", "model", "model.json");
        byte[] saved;
        try (var input = Files.newInputStream(modelFile)) { saved = input.readNBytes(8_388_609); }
        if (saved.length > 8_388_608) throw new IllegalArgumentException("Saved Studio model exceeds 8 MiB.");
        var prepared = org.ikasan.studio.core.ai.OfflineModelProposal.prepare(json, saved, snapshot.model());
        Proposal proposal = new Proposal(snapshot, prepared,
                JSON.writerWithDefaultPrettyPrinter().writeValueAsString(JSON.readTree(json).path("operations")));
        proposal.fileBased = true;
        return onEdt(() -> {
            // Settings may have changed while validation ran in the background.
            if (autoOnly && !canAutoApply(proposal)) return false;
            requireCurrent(snapshot);
            if (pending != null) throw new IllegalStateException("Review or cancel the pending proposal in Studio first.");
            pending = proposal;
            reviewOrApply(proposal);
            return true;
        });
    }

    private Snapshot capture() { requireReady(); Module module = context().getIkasanModule(); return new Snapshot(module, LiveModelSnapshot.capture(module)); }
    private UiContext context() { return project.getService(UiContext.class); }
    /** Cheap EDT-only readiness check for connection guidance; does not read or publish a snapshot. */
    String connectionReadinessIssue() {
        try { requireReady(); return null; }
        catch (IllegalStateException notReady) { return notReady.getMessage(); }
    }

    private void requireReady() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        var context = context();
        if (project.isDisposed() || context.getIkasanModule() == null || !context.getIkasanModule().isInitialised()
                || context.isModelPersistenceBlocked() || context.isMigrationActive() || !context.getLatestGeneration().isDone()
                || context.getPipsiIkasanModel() == null)
            throw new IllegalStateException(StudioBundle.message("ai.NotReady"));
        if (context.getPropertiesPanel() != null && context.getPropertiesPanel().dataHasChangedAndOKToProcess())
            throw new IllegalStateException(StudioBundle.message("ai.PendingEdits"));
    }
    private void requireCurrent(Snapshot snapshot) {
        requireReady();
        if (context().getIkasanModule() != snapshot.source() || !snapshot.model().equals(LiveModelSnapshot.capture(snapshot.source())))
            throw new IllegalStateException(StudioBundle.message("ai.Stale"));
    }

    CompletableFuture<Void> apply(Proposal proposal) {
        if (pending != proposal || (!proposal.fileBased && !isRunning())) throw new IllegalStateException(StudioBundle.message("ai.Stale"));
        requireCurrent(proposal.snapshot);
        ModelProposal.ChangeSet changes = ModelProposal.changes(proposal.snapshot.source(), proposal.prepared);
        UndoManager undo = UndoManager.getInstance(project);
        changes.apply();
        CompletableFuture<Void> generation;
        try { generation = StudioProjectFiles.refreshCodeFromModel(project, GenerationRequest.full()); }
        catch (RuntimeException failure) { changes.undo(); throw failure; }
        Snapshot after = new Snapshot(proposal.snapshot.source(), LiveModelSnapshot.capture(proposal.snapshot.source()));
        CommandProcessor.getInstance().executeCommand(project, () -> {
            undo.undoableActionPerformed(new UndoableAction() {
                private Snapshot expected = after;
                @Override public void undo() throws UnexpectedUndoException { change(false); }
                @Override public void redo() throws UnexpectedUndoException { change(true); }
                @Override public DocumentReference[] getAffectedDocuments() { return null; }
                @Override public boolean isGlobal() { return true; }
                private void change(boolean forward) throws UnexpectedUndoException {
                    try {
                        requireCurrent(expected);
                        if (forward) changes.apply(); else changes.undo();
                        expected = new Snapshot(expected.source(), LiveModelSnapshot.capture(expected.source()));
                        refreshUi();
                        proposal.status = forward ? "applied" : "undone";
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (project.isDisposed() || context().getIkasanModule() != proposal.snapshot.source()) return;
                            try {
                                StudioProjectFiles.refreshCodeFromModel(project, GenerationRequest.full()).whenComplete((ignored, failure) -> {
                                    if (failure != null) proposal.status = "generation_failed";
                                });
                            } catch (RuntimeException failure) { proposal.status = "generation_failed"; }
                        });
                    } catch (RuntimeException stale) { throw new UnexpectedUndoException(stale.getMessage()); }
                }
            });
            refreshUi();
        }, StudioBundle.message("ai.ApplyCommand"), null);
        pending = null;
        proposal.status = "generating";
        generation.whenComplete((ignored, failure) -> proposal.status = failure == null ? "applied" : "generation_failed");
        return generation;
    }

    void cancel(Proposal proposal) {
        if (pending == proposal) { pending = null; proposal.status = "cancelled"; }
    }
    private void refreshUi() {
        context().resetSelectionAfterDeletion();
        var runtime = project.getService(org.ikasan.studio.intellij.execution.IkasanDebugSessionService.class);
        if (runtime != null) runtime.markRestartRequired(context().getIkasanModule());
        StudioProjectFiles.causeRedraw(project);
    }
    private <T> T onEdt(Supplier<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action::get);
        ApplicationManager.getApplication().invokeAndWait(task, ModalityState.any());
        try { return task.get(); }
        catch (ExecutionException failure) {
            if (failure.getCause() instanceof Exception exception) throw exception;
            throw failure;
        }
    }
    private static void trim(Map<?, ?> map, int max) { while (map.size() > max) map.remove(map.keySet().iterator().next()); }
    static void redact(JsonNode node) {
        if (node.isObject()) {
            List<String> names = new ArrayList<>(); node.fieldNames().forEachRemaining(names::add);
            for (String name : names) {
                if (name.toLowerCase(Locale.ROOT).matches(".*(password|secret|token|credential).*"))
                    ((com.fasterxml.jackson.databind.node.ObjectNode) node).put(name, "<redacted>");
                else redact(node.get(name));
            }
        } else if (node.isArray()) node.forEach(StudioAiService::redact);
    }
    public synchronized void stop() {
        if (server != null) { server.stop(0); server = null; }
        if (executor != null) { executor.shutdownNow(); executor = null; }
        synchronized (snapshots) { snapshots.clear(); }
        synchronized (proposals) { proposals.clear(); }
        if (pending != null && (!pending.fileBased || disposed)) { pending.status = "cancelled"; pending = null; }
        lastAccessTransport = null;
        StudioAiConnectionFiles files = connectionFiles;
        connectionFiles = null;
        if (files != null) cleanup = CompletableFuture.runAsync(() -> {
            try { files.close(); }
            catch (java.io.IOException failure) {
                com.intellij.openapi.diagnostic.Logger.getInstance(StudioAiService.class).warn("Could not remove stopped AI connection", failure);
            }
        }, AppExecutorUtil.getAppExecutorService());
    }
    @Override public void dispose() { disposed = true; stop(); }
}
