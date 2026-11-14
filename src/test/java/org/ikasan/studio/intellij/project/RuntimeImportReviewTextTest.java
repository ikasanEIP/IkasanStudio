package org.ikasan.studio.intellij.project;

import org.ikasan.studio.core.importer.IkasanRuntimeImport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ikasan.studio.core.importer.IkasanRuntimeImport.ReviewKind.*;

class RuntimeImportReviewTextTest {
    @Test void groupsRepeatedClassesAndFieldsWithoutLosingLocationsOrOtherWarnings() {
        var items = List.of(
                item(IMPLEMENTATION, "First: flow → nested", "Consumer", "example.Shared", "", "implementation one"),
                item(INFERRED_VARIANT, "First", "Consumer", "SFTP Consumer", "example.SftpConfiguration", "variant"),
                item(CONFIGURATION, "First", "Consumer", "privateKeyPassphrase", "", "configuration one"),
                item(IMPLEMENTATION, "Second", "Producer", "example.Shared", "", "implementation two"),
                item(CONFIGURATION, "Second", "Producer", "privateKeyPassphrase", "", "configuration two"));
        var review = new java.util.ArrayList<>(items.stream().map(IkasanRuntimeImport.ReviewItem::message).toList());
        review.add("Patch upgrade: compile and test before running.");
        var result = new IkasanRuntimeImport.Result("{}", review, items);
        String text = RuntimeImportReviewText.format(result);
        assertThat(text).contains("Implementation classes to supply (1)", "Configuration warnings (1)",
                "First: flow → nested → Consumer", "Second → Producer", "First → Consumer", review.get(5));
        assertThat(text).doesNotContain("Matched component variants", "example.SftpConfiguration", "\nvariant");
        assertThat(text.split("example.Shared", -1)).hasSize(2);
        assertThat(text.split("privateKeyPassphrase", -1)).hasSize(2);
        assertThat(text.indexOf("Patch upgrade")).isLessThan(text.indexOf("Configuration warnings"));
        assertThat(text.indexOf("Configuration warnings")).isLessThan(text.indexOf("Implementation classes"));
        assertThat(result.review()).containsExactlyElementsOf(review);
    }

    @Test void unstructuredMessagesRemainVisibleWithoutGuessingTheirCategory() {
        String message = "Odd: component → supply existing implementation example.Name: retain this whole warning.";
        String text = RuntimeImportReviewText.format(new IkasanRuntimeImport.Result("{}", List.of(message, "Another check")));
        assertThat(text).contains(message, "Another check").doesNotContain("Implementation classes");
        assertThat(RuntimeImportReviewText.format(new IkasanRuntimeImport.Result("{}", List.of()))).isEmpty();
    }

    private static IkasanRuntimeImport.ReviewItem item(IkasanRuntimeImport.ReviewKind kind, String flow,
                                                      String component, String subject, String evidence, String message) {
        return new IkasanRuntimeImport.ReviewItem(kind, new IkasanRuntimeImport.ComponentRef(flow, component),
                subject, evidence, message);
    }
}
