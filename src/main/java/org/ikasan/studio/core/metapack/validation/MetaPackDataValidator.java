package org.ikasan.studio.core.metapack.validation;

import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.model.ComponentPropertyMeta;
import org.ikasan.studio.core.metapack.model.ComponentTypeMeta;
import org.ikasan.studio.core.metapack.model.ConversionRecipeMeta;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Structural and referential validation for the contents of a loaded meta-pack. */
final class MetaPackDataValidator {
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "Broker", "Consumer", "Converter", "Debug", "End Point", "Exception Resolver",
            "Filter", "Flow", "Module", "Producer", "Router", "Splitter", "Translator");
    private static final Pattern JAVA_IDENTIFIER = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
    private static final Pattern JAVA_TYPE = Pattern.compile(
            "(?:[A-Za-z_$][A-Za-z0-9_$]*\\.)*[A-Za-z_$][A-Za-z0-9_$]*(?:\\$[A-Za-z_$][A-Za-z0-9_$]*)*"
                    + "(?:<.*>)?(?:\\[])?(?: \\(auto-converted\\))?");

    private MetaPackDataValidator() { }

    static void validate(String pack, Map<String, ComponentMeta> components) throws StudioBuildException {
        List<String> problems = new ArrayList<>();
        if (components == null || components.isEmpty()) {
            problems.add("library: contains no valid components");
        } else {
            validateContents(pack, components, problems);
        }
        if (!problems.isEmpty()) {
            throw new StudioBuildException("Meta-pack " + pack + " contains invalid metadata:\n - "
                    + String.join("\n - ", problems));
        }
    }

    private static void validateContents(String pack, Map<String, ComponentMeta> components, List<String> problems) {
        Set<ComponentTypeMeta> checkedTypes = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        Map<Integer, String> paletteOrders = new HashMap<>();
        Map<String, String> identities = new HashMap<>();
        for (Map.Entry<String, ComponentMeta> entry : components.entrySet()) {
            ComponentMeta component = entry.getValue();
            String path = "component '" + entry.getKey() + "'";
            required(problems, path + ".name", component.getName());
            if (!Objects.equals(entry.getKey(), component.getName())) add(problems, path, "library key must equal component name");
            required(problems, path + ".componentType", component.getComponentType());
            if (component.getImplementingClass() == null) add(problems, path + ".implementingClass", "is required; use an empty string when intentionally absent");
            javaName(problems, path + ".implementingClass", component.getImplementingClass());
            method(problems, path + ".flowBuilderMethod", component.getFlowBuilderMethod());
            method(problems, path + ".ikasanComponentFactoryMethod", component.getIkasanComponentFactoryMethod());
            help(problems, path + ".helpText", component.getHelpText());
            url(problems, pack, path + ".webHelpURL", component.getWebHelpURL());
            types(problems, path + ".expectedInputTypes", component.getExpectedInputTypes());
            type(problems, path + ".producedOutputType", component.getProducedOutputType());
            namespace(problems, pack, path + ".implementingClass", component.getImplementingClass());
            namespace(problems, pack, path + ".expectedInputTypes", component.getExpectedInputTypes());
            namespace(problems, pack, path + ".producedOutputType", component.getProducedOutputType());
            if (component.getEndpointKey() != null && !component.getEndpointKey().isBlank() && !components.containsKey(component.getEndpointKey())) {
                add(problems, path + ".endpointKey", "references unknown component " + component.getEndpointKey());
            }
            if (component.getEndpointTextKey() != null && !component.getEndpointTextKey().isBlank() && !component.getAllowableProperties().containsKey(component.getEndpointTextKey())) {
                add(problems, path + ".endpointTextKey", "references unknown property " + component.getEndpointTextKey());
            }
            if (component.getImportConfigurationClasses() != null) {
                for (String imported : component.getImportConfigurationClasses()) { javaName(problems, path + ".importConfigurationClasses", imported); namespace(problems, pack, path + ".importConfigurationClasses", imported); }
            }
            if (component.getImportResources() != null) {
                for (String imported : component.getImportResources()) if (imported == null || !imported.startsWith("classpath:")) add(problems, path + ".importResources", "must use a classpath: resource: " + imported);
            }

            ComponentTypeMeta category = component.getComponentTypeMeta();
            if (category == null) add(problems, path, "has no component category");
            else if (checkedTypes.add(category)) category(category, path, paletteOrders, problems);

            String identity = ComponentLibrary.getDeserialisationKey(component);
            String previous = identities.putIfAbsent(identity, component.getName());
            if (previous != null) add(problems, path, "duplicates deserialisation identity of '" + previous + "': " + identity);

            properties(pack, component, path, problems);
            recipes(pack, component, path, problems);
            if (category != null && !component.isEndpoint()) {
                resource(problems, path + ".smallIcon", component.getIconResourceDirectory() + "/small.png");
                resource(problems, path + ".normalIcon", component.getIconResourceDirectory() + "/normal.png");
            }
        }
    }

    private static void category(ComponentTypeMeta category, String owner, Map<Integer, String> orders,
                                 List<String> problems) {
        String path = owner + " category";
        required(problems, path + ".componentShortType", category.getComponentShortType());

        if (category.getComponentShortType() != null && !SUPPORTED_TYPES.contains(category.getComponentShortType())) {
            add(problems, path + ".componentShortType", "unsupported value '" + category.getComponentShortType() + "'");
        }
        if (category.getPaletteDisplayOrder() != null) {
            String previous = orders.putIfAbsent(category.getPaletteDisplayOrder(), category.getComponentShortType());
            if (previous != null && !previous.equals(category.getComponentShortType())) {
                add(problems, path + ".paletteDisplayOrder", "duplicates " + category.getPaletteDisplayOrder() + " used by " + previous);
            }
        }
        help(problems, path + ".helpText", category.getHelpText());
    }

    private static void properties(String pack, ComponentMeta component, String path, List<String> problems) {
        Map<String, ComponentPropertyMeta> properties = component.getAllowableProperties();
        if (properties == null) { add(problems, path + ".allowableProperties", "is required"); return; }
        Map<String, String> displayOrders = new HashMap<>();
        for (Map.Entry<String, ComponentPropertyMeta> entry : properties.entrySet()) {
            ComponentPropertyMeta property = entry.getValue();
            String propertyPath = path + ".allowableProperties." + entry.getKey();
            if (property == null) { add(problems, propertyPath, "must be an object"); continue; }
            required(problems, propertyPath + ".propertyName", property.getPropertyName());
            if (!entry.getKey().equals(property.getPropertyName())) add(problems, propertyPath + ".propertyName", "must equal JSON key '" + entry.getKey() + "'");
            help(problems, propertyPath + ".helpText", property.getHelpText());
            method(problems, propertyPath + ".setterMethod", property.getSetterMethod());
            type(problems, propertyPath + ".usageDataType", property.getUsageDataType());
            namespace(problems, pack, propertyPath + ".usageDataType", property.getUsageDataType());
            if (property.getValidation() != null && !property.getValidation().isBlank()) {
                try { Pattern.compile(property.getValidation()); }
                catch (PatternSyntaxException e) { add(problems, propertyPath + ".validation", "invalid regular expression: " + e.getMessage()); }
            }
            references(properties, propertyPath + ".mandatoryUnlessAnyOf", property.getMandatoryUnlessAnyOf(), problems);
            references(properties, propertyPath + ".mandatoryIfTrue",
                    property.getMandatoryIfTrue() == null ? List.of() : List.of(property.getMandatoryIfTrue()), problems);
            if (property.getPropertyDisplayOrder() != 0) {
                String section = property.isMandatory() ? "mandatory:" + Objects.toString(property.getMandatorySectionHeading(), "")
                        : "optional:" + Objects.toString(property.getPropertyGroup(), "");
                String order = section + ":" + property.getPropertyDisplayOrder();
                String previous = displayOrders.putIfAbsent(order, entry.getKey());
                if (previous != null) add(problems, propertyPath + ".propertyDisplayOrder",
                        "duplicates " + property.getPropertyDisplayOrder() + " in the same section as '" + previous + "'");
            }
            template(problems, pack, propertyPath, property.getUserImplementClassFtlTemplate());
        }
        if (component.getExpectedInputTypeProperty() != null && !component.getExpectedInputTypeProperty().isBlank()
                && !properties.containsKey(component.getExpectedInputTypeProperty())) {
            add(problems, path + ".expectedInputTypeProperty", "references unknown property '" + component.getExpectedInputTypeProperty() + "'");
        }
        references(properties, path + ".outputTypeInvalidatedByProperties",
                component.getOutputTypeInvalidatedByProperties(), problems);
    }

    private static void recipes(String pack, ComponentMeta component, String path, List<String> problems) {
        List<ConversionRecipeMeta> conversionRecipes = component.getConversionRecipes();
        if (conversionRecipes == null) return;
        Set<String> ids = new HashSet<>();
        Set<String> pairs = new HashSet<>();
        for (int i = 0; i < conversionRecipes.size(); i++) {
            ConversionRecipeMeta recipe = conversionRecipes.get(i);
            String recipePath = path + ".conversionRecipes[" + i + "]";
            required(problems, recipePath + ".id", recipe.getId());
            required(problems, recipePath + ".displayName", recipe.getDisplayName());
            required(problems, recipePath + ".sourceType", recipe.getSourceType());
            required(problems, recipePath + ".targetType", recipe.getTargetType());
            required(problems, recipePath + ".template", recipe.getTemplate());
            required(problems, recipePath + ".helpText", recipe.getHelpText());
            type(problems, recipePath + ".sourceType", recipe.getSourceType());
            type(problems, recipePath + ".targetType", recipe.getTargetType());
            help(problems, recipePath + ".helpText", recipe.getHelpText());
            if (recipe.getId() != null && !ids.add(recipe.getId())) add(problems, recipePath + ".id", "is duplicated");
            String pair = recipe.getSourceType() + "->" + recipe.getTargetType();
            if (!pairs.add(pair)) add(problems, recipePath, "duplicates source/target pair " + pair);
            template(problems, pack, recipePath, recipe.getTemplate());
        }
    }

    private static void references(Map<String, ComponentPropertyMeta> properties, String path,
                                   List<String> references, List<String> problems) {
        if (references == null) return;
        for (String reference : references) {
            if (reference == null || reference.isBlank() || !properties.containsKey(reference)) {
                add(problems, path, "references unknown property '" + reference + "'");
            }
        }
    }

    private static void template(List<String> problems, String pack, String path, String name) {
        if (name == null || name.isBlank() || !pack.matches("V\\d.*")) return;
        String base = "studio/metapack/" + pack + "/templates/org/ikasan/studio/generator/";
        String localised = name.endsWith(".ftl") ? name.substring(0, name.length() - 4) + "_en.ftl" : name;
        if (missing(base + name) && missing(base + localised)) add(problems, path + ".template", "missing '" + name + "'");
    }

    private static void types(List<String> problems, String path, String values) {
        if (values == null || values.isBlank()) return;
        for (String value : values.split(",")) type(problems, path, value.trim());
    }

    private static void type(List<String> problems, String path, String value) {
        if (value == null || value.isBlank() || Set.of("classLiteral", "configurationDefined").contains(value)) return;
        if (!JAVA_TYPE.matcher(value).matches()) add(problems, path, "invalid Java type declaration '" + value + "'");
    }

    private static void javaName(List<String> problems, String path, String value) {
        if (value != null && !value.isBlank() && !JAVA_TYPE.matcher(value).matches()) add(problems, path, "invalid qualified Java name '" + value + "'");
    }

    private static void method(List<String> problems, String path, String value) {
        if (value != null && !value.isBlank() && !JAVA_IDENTIFIER.matcher(value).matches()) add(problems, path, "invalid Java method name '" + value + "'");
    }

    private static void help(List<String> problems, String path, String value) {
        if (value == null || value.isBlank()) return;
        long starts = value.chars().filter(character -> character == '<').count();
        long ends = value.chars().filter(character -> character == '>').count();
        if (starts != ends) add(problems, path, "contains an unclosed HTML tag");
    }

    private static void url(List<String> problems, String pack, String path, String value) {
        if (value == null || value.isBlank() || "Readme.md".equals(value)) return;
        try {
            URI uri = new URI(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) add(problems, path, "must be an absolute HTTPS URL");
            String release = pack.startsWith("V") ? pack.substring(1) : pack;
            if (pack.matches("V\\d.*") && "github.com".equalsIgnoreCase(uri.getHost()) && value.contains("/ikasanEIP/ikasan/blob/")
                    && !value.contains("/blob/ikasaneip-" + release + "/")) {
                add(problems, path, "must reference the exact Ikasan tag ikasaneip-" + release);
            }
        } catch (URISyntaxException e) { add(problems, path, "invalid URL: " + e.getMessage()); }
    }

    private static void namespace(List<String> problems, String pack, String path, String value) {
        if (value == null) return;
        if (pack.startsWith("V4") && (value.contains("javax.jms") || value.contains("javax.resource") || value.contains("javax.xml.bind")))
            add(problems, path, "uses a pre-Jakarta class in an Ikasan 4 meta-pack: " + value);
        if (pack.startsWith("V3") && (value.contains("jakarta.jms") || value.contains("jakarta.resource") || value.contains("jakarta.xml.bind")))
            add(problems, path, "uses a Jakarta class in an Ikasan 3 meta-pack: " + value);
    }

    private static void resource(List<String> problems, String path, String resource) {
        if (missing(resource)) add(problems, path, "missing resource '" + resource + "'");
    }

    private static boolean missing(String resource) {
        return resource == null || resource.startsWith("null/")
                || MetaPackDataValidator.class.getClassLoader().getResource(resource) == null;
    }

    private static void required(List<String> problems, String path, String value) {
        if (value == null || value.isBlank()) add(problems, path, "is required");
    }

    private static void add(List<String> problems, String path, String message) {
        problems.add(path + ": " + message);
    }
}
