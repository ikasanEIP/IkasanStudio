package org.ikasan.studio.intellij.project;

import org.ikasan.studio.core.importer.IkasanRuntimeImport;
import org.ikasan.studio.ui.StudioBundle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Compact presentation of structured findings, without parsing messages or discarding other warnings. */
final class RuntimeImportReviewText {
    private RuntimeImportReviewText() { }

    static String format(IkasanRuntimeImport.Result result) {
        var structuredMessages = result.reviewItems().stream().map(IkasanRuntimeImport.ReviewItem::message)
                .collect(Collectors.toSet());
        var sections = new ArrayList<String>();
        var other = result.review().stream().filter(message -> !structuredMessages.contains(message)).toList();
        if (!other.isEmpty()) sections.add(StudioBundle.message("import.runtime.review.other") + "\n" + String.join("\n", other));
        append(sections, result, IkasanRuntimeImport.ReviewKind.CONFIGURATION, "configuration");
        append(sections, result, IkasanRuntimeImport.ReviewKind.IMPLEMENTATION, "implementations");
        return String.join("\n\n", sections);
    }

    private record Group(String subject, String evidence) { }

    private static void append(List<String> sections, IkasanRuntimeImport.Result result,
                               IkasanRuntimeImport.ReviewKind kind, String key) {
        var items = result.reviewItems().stream().filter(item -> item.kind() == kind).toList();
        if (items.isEmpty()) return;
        Map<Group, List<String>> groups = new LinkedHashMap<>();
        for (var item : items) groups.computeIfAbsent(new Group(item.subject(), item.evidence()), ignored -> new ArrayList<>())
                .add(item.component().flow() + " → " + item.component().component());
        var text = new StringBuilder(StudioBundle.message("import.runtime.review." + key, groups.size()))
                .append('\n').append(StudioBundle.message("import.runtime.review." + key + "Help"));
        groups.forEach((group, locations) -> {
            text.append("\n• ").append(group.subject());
            if (!group.evidence().isBlank()) text.append(" (").append(group.evidence()).append(')');
            locations.forEach(location -> text.append("\n  ").append(location));
        });
        sections.add(text.toString());
    }
}
