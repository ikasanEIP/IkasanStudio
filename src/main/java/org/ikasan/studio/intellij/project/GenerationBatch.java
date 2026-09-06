package org.ikasan.studio.intellij.project;

import org.ikasan.studio.ui.viewmodel.AbstractViewHandlerIntellij;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Invocation-scoped staging area for one coherent generated-project update. */
final class GenerationBatch {
    record Artifact(String relativePath, String content, AbstractViewHandlerIntellij viewHandler) { }

    private final Map<String, Artifact> artifacts = new LinkedHashMap<>();
    private final Set<String> authorisedUserReplacements = new HashSet<>();
    private final List<Runnable> afterCommit = new ArrayList<>();

    void stage(String relativePath, String content, AbstractViewHandlerIntellij viewHandler) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Generated artifact path must not be blank");
        }
        if (content == null) {
            throw new IllegalArgumentException("Generated artifact content must not be null: " + relativePath);
        }
        Artifact previous = artifacts.putIfAbsent(relativePath, new Artifact(relativePath, content, viewHandler));
        if (previous != null && !previous.content().equals(content)) {
            throw new IllegalStateException("Generation produced conflicting content for " + relativePath);
        }
    }

    List<Artifact> artifacts() {
        return List.copyOf(artifacts.values());
    }

    void authoriseUserReplacement(String relativePath) {
        authorisedUserReplacements.add(relativePath);
    }

    boolean isUserReplacementAuthorised(String relativePath) {
        return authorisedUserReplacements.contains(relativePath);
    }

    void afterCommit(Runnable action) {
        afterCommit.add(action);
    }

    void runAfterCommit() {
        afterCommit.forEach(Runnable::run);
    }
}
