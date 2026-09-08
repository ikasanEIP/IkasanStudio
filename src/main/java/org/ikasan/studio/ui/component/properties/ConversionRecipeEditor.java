package org.ikasan.studio.ui.component.properties;

import org.ikasan.studio.core.metapack.model.ConversionRecipeMeta;
import org.ikasan.studio.core.conversion.ConversionRecipeMatcher;
import org.ikasan.studio.ui.StudioBundle;
import com.intellij.openapi.ui.ValidationInfo;
import javax.swing.*;
import java.util.*;
import java.util.List;

/** Edits widget values only: the properties panel owns commit, cancellation and overwrite approval. */
final class ConversionRecipeEditor {
    private ConversionRecipeEditor() {}
    static void bind(List<ConversionRecipeMeta> recipes, Map<String, ComponentPropertyEditRow> rows,
                     JTextArea details, String downstreamTypes, SimpleChangeListener changed) {
        bind(recipes, rows, details, downstreamTypes, changed, false);
    }

    static void bind(List<ConversionRecipeMeta> recipes, Map<String, ComponentPropertyEditRow> rows,
                     JTextArea details, String downstreamTypes, SimpleChangeListener changed,
                     boolean componentInitialisation) {
        var row = rows.get("conversionRecipeId");
        if (row == null || recipes == null) return;
        var combo = row.getInputField().getPropertyChoiceValueField();
        if (combo == null) return;
        combo.setMinimumSize(new java.awt.Dimension(0, combo.getPreferredSize().height));
        Object initial = row.getComponentProperty().getValue();
        combo.removeAllItems();
        combo.addItem("");
        for (var recipe : ConversionRecipeMatcher.ranked(recipes, value(rows, "fromType"), downstreamTypes == null ? value(rows, "toType") : downstreamTypes)) combo.addItem(recipe.getId());
        // Retain unavailable ids so opening the panel cannot silently discard a saved selection.
        if (initial != null && !initial.toString().isBlank() && recipes.stream().noneMatch(r -> r.getId().equals(initial))) combo.addItem(initial);
        combo.setSelectedItem(initial);
        var recommended = ConversionRecipeMatcher.matching(recipes, value(rows, "fromType"),
                downstreamTypes == null ? value(rows, "toType") : downstreamTypes);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setText(recipes.stream().filter(r -> r.getId().equals(value)).map(ConversionRecipeMeta::getDisplayName)
                        .findFirst().orElse(value == null || value.toString().isBlank() ? StudioBundle.message("conversion.BlankCustomConverter") : value.toString()));
                if (recommended.stream().anyMatch(r -> r.getId().equals(value)))
                    setText(getText() + " " + StudioBundle.message("conversion.Suggested"));
                return this;
            }
        });
        Runnable explain = () -> {
            var recipe = recipes.stream().filter(r -> r.getId().equals(combo.getSelectedItem())).findFirst();
            details.setText(recipe.map(r -> r.getSourceType() + " → " + r.getTargetType() + "\n" + r.getHelpText())
                    .orElse(StudioBundle.message("conversion.CustomHelp")));
            details.setCaretPosition(0);
            Set<String> configurationKeys = new LinkedHashSet<>();
            recipes.stream().map(ConversionRecipeMeta::getConfigurationProperties).filter(Objects::nonNull)
                    .forEach(configurationKeys::addAll);
            for (String key : configurationKeys) {
                var configRow = rows.get(key);
                if (configRow != null) {
                    boolean applicable = recipe.isPresent()
                            && recipe.get().getConfigurationProperties() != null
                            && recipe.get().getConfigurationProperties().contains(key);
                    configRow.getInputField().setEnabled(applicable);
                    configRow.setApplicable(applicable);
                }
            }
        };
        combo.addActionListener(event -> {
            recipes.stream().filter(r -> r.getId().equals(combo.getSelectedItem())).findFirst().ifPresent(recipe -> {
                rows.get("fromType").getInputField().getPropertyValueField().setValue(recipe.getSourceType());
                rows.get("toType").getInputField().getPropertyValueField().setValue(recipe.getTargetType());
            });
            explain.run();
            if (changed != null) changed.actionEvent();
        });
        // Default only a new component's draft - null on an existing component means Custom, and this must never
        // override an already-chosen recipe. With exactly one suggestion it's the obvious choice; with several
        // (e.g. email body vs attachment) leaving the dropdown on blank Custom was worse than picking one, since
        // a freshly dropped Converter would otherwise generate no conversion at all until the developer noticed
        // and opened its properties - preselect the top-ranked suggestion instead, still one click away from
        // changing to whichever the developer actually wants.
        if (componentInitialisation && initial == null && !recommended.isEmpty()) {
            combo.setSelectedItem(recommended.get(0).getId());
        }
        explain.run();
    }

    static List<ValidationInfo> validate(List<ConversionRecipeMeta> recipes, Map<String, ComponentPropertyEditRow> rows) {
        String id = value(rows, "conversionRecipeId");
        if (id.isBlank()) return List.of();
        if (recipes == null || recipes.stream().noneMatch(r -> r.getId().equals(id) && r.matches(value(rows, "fromType"), value(rows, "toType"))))
            return List.of(new ValidationInfo(StudioBundle.message("conversion.Incompatible"), rows.get("conversionRecipeId").getInputField().getFirstFocusComponent()));
        String charset = value(rows, "recipeCharset");
        boolean usesCharset = recipes.stream().filter(r -> r.getId().equals(id))
                .anyMatch(r -> r.getConfigurationProperties() != null && r.getConfigurationProperties().contains("recipeCharset"));
        if (usesCharset && !charset.isBlank()) {
            try { java.nio.charset.Charset.forName(charset); }
            catch (IllegalArgumentException e) { return List.of(new ValidationInfo(StudioBundle.message("conversion.InvalidCharset"), rows.get("recipeCharset").getInputField().getFirstFocusComponent())); }
        }
        return List.of();
    }

    private static String value(Map<String, ComponentPropertyEditRow> rows, String key) {
        var row = rows.get(key);
        return row == null || row.getValue() == null ? "" : row.getValue().toString();
    }
}
