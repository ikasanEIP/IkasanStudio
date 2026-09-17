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

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
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
    private Path connectionFile;
    private String configuration;
    private volatile Proposal pending;

    record Snapshot(Module source, Map<String, Object> model) { }
    static final class Proposal {
        final String id = UUID.randomUUID().toString();
        final Snapshot snapshot;
        final ModelProposal.Prepared prepared;
        final String details;
        volatile String status = "awaiting_review";
        Proposal(Snapshot snapshot, ModelProposal.Prepared prepared, String details) { this.snapshot = snapshot; this.prepared = prepared; this.details = details; }
    }
    public StudioAiService(Project project) { this.project = project; }
    public boolean isRunning() { return server != null; }

    /** Runs off EDT, returning a ready-to-copy MCP server configuration. */
    public String start() throws Exception {
        synchronized (this) { if (server != null) return configuration; }
        onEdt(() -> { requireReady(); return null; });
        synchronized (this) {
            if (disposed || project.isDisposed()) throw new IllegalStateException("Project closed");
            if (server != null) return configuration;
            Path directory = Files.createTempDirectory(Path.of(PathManager.getSystemPath()), "ikasan-ai-");
            if (Files.getFileStore(directory).supportsFileAttributeView("posix"))
                Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"));
            Path script = directory.resolve("studio_mcp.py");
            try (var source = getClass().getResourceAsStream("/studio/ai/studio_mcp.py")) {
                if (source == null) throw new IllegalStateException("Studio MCP adapter is missing");
                Files.copy(source, script);
            }
            Path connection = directory.resolve("connection.json");
            String token = UUID.randomUUID() + "-" + UUID.randomUUID();
            HttpServer created = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            ExecutorService workers = AppExecutorUtil.createBoundedApplicationPoolExecutor("Ikasan Studio AI bridge", 2);
            var protocol = new StudioMcpProtocol(this::call);
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
            try {
                Files.writeString(connection, JSON.writeValueAsString(Map.of("url", "http://127.0.0.1:" + created.getAddress().getPort() + "/rpc", "token", token)));
                if (Files.getFileStore(connection).supportsFileAttributeView("posix"))
                    Files.setPosixFilePermissions(connection, PosixFilePermissions.fromString("rw-------"));
                configuration = JSON.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("mcpServers", Map.of("ikasan-studio",
                        Map.of("command", "python3", "args", List.of(script.toString(), "--connection", connection.toString())))));
                connectionFile = connection;
                executor = workers;
                server = created;
                created.start();
                return configuration;
            } catch (Exception failure) { created.stop(0); workers.shutdownNow(); throw failure; }
        }
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
                new StudioAiProposalDialog(project, this, proposal).show();
            }
            return null;
        });
        return Map.of("proposalId", proposal.id, "status", proposal.status, "summary", prepared.summary());
    }

    private Snapshot capture() { requireReady(); Module module = context().getIkasanModule(); return new Snapshot(module, LiveModelSnapshot.capture(module)); }
    private UiContext context() { return project.getService(UiContext.class); }
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

    void apply(Proposal proposal) {
        if (pending != proposal || !isRunning()) throw new IllegalStateException(StudioBundle.message("ai.Stale"));
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
        if (pending != null) { pending.status = "cancelled"; pending = null; }
        Path file = connectionFile;
        connectionFile = null;
        if (file != null) AppExecutorUtil.getAppExecutorService().execute(() -> {
            try { Files.deleteIfExists(file); } catch (java.io.IOException ignored) { /* Server and token already invalid. */ }
        });
    }
    @Override public void dispose() { disposed = true; stop(); }
}
