package org.ikasan.studio.core.model.analysis;

import org.ikasan.studio.core.model.ikasan.instance.BasicElement;
import org.ikasan.studio.core.model.ikasan.instance.ComponentProperty;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.Module;

import javax.lang.model.SourceVersion;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/** Previewable literal class/package replacements in configurable flow properties, never model identities. */
public final class ModelReferenceReplacement {
    private ModelReferenceReplacement() { }

    public record Change(Flow flow, BasicElement element, String key, ComponentProperty property,
                         Object before, Object after) { }

    public static List<Change> preview(Module module, String find, String replacement) {
        if (!SourceVersion.isName(find) || !SourceVersion.isName(replacement) || find.equals(replacement)) {
            throw new IllegalArgumentException("Enter different Java class or package names");
        }
        Pattern pattern = Pattern.compile("(?<![\\p{javaJavaIdentifierPart}.])" + Pattern.quote(find)
                + "(?![\\p{javaJavaIdentifierPart}])");
        List<Change> changes = new ArrayList<>();
        for (Flow flow : module.getFlows()) {
            collect(changes, flow, flow, pattern, replacement);
            for (var element : flow.getFlowElementsNoExternalEndPoints()) collect(changes, flow, element, pattern, replacement);
            if (flow.getExceptionResolver() != null) {
                for (var resolution : flow.getExceptionResolver().getExceptionResolutionList()) {
                    collect(changes, flow, resolution, pattern, replacement);
                }
            }
        }
        return List.copyOf(changes);
    }

    private static void collect(List<Change> changes, Flow flow, BasicElement element, Pattern pattern, String replacement) {
        element.getComponentProperties().forEach((key, property) -> {
            if (Set.of("name", "componentName", "exceptionsCaught", "testHarnessOwner").contains(key)) return;
            Object before = property.getValue();
            Object after = replace(before, pattern, replacement);
            if (!Objects.equals(before, after)) changes.add(new Change(flow, element, key, property,
                    before instanceof List<?> list ? List.copyOf(list) : before, after));
        });
    }

    private static Object replace(Object value, Pattern pattern, String replacement) {
        if (value instanceof String text) return pattern.matcher(text).replaceAll(Matcher.quoteReplacement(replacement));
        if (value instanceof List<?> list) return list.stream().map(item -> replace(item, pattern, replacement)).toList();
        return value;
    }

    /** Check every value first: a stale preview must never partially modify the model. */
    public static void apply(Module module, List<Change> changes, boolean forward) {
        for (Change change : changes) {
            boolean present = module.getFlows().stream().anyMatch(flow -> flow == change.flow());
            boolean elementPresent = change.element() == change.flow()
                    || change.flow().getFlowElementsNoExternalEndPoints().stream().anyMatch(element -> element == change.element())
                    || (change.flow().getExceptionResolver() != null && change.flow().getExceptionResolver()
                            .getExceptionResolutionList().stream().anyMatch(element -> element == change.element()));
            if (!present || !elementPresent || change.element().getProperty(change.key()) != change.property()
                    || !Objects.equals(change.property().getValue(), forward ? change.before() : change.after())) {
                throw new IllegalStateException("The model changed after the replacement preview");
            }
        }
        changes.forEach(change -> change.property().setValue(forward ? change.after() : change.before()));
    }
}
