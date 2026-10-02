package org.ikasan.studio.testing.packs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.studio.core.model.StringCollectionValues;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.net.*;
import java.lang.reflect.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class StringCollectionRuntimeTest {
    @TempDir Path root;
    @ParameterizedTest @ValueSource(strings={"V3.3.9", "V4.1.6"})
    void compiledHelperReadsExactStringsAndLegacyOverrides(String pack) throws Exception {
        String helper = Files.readString(Path.of("src/main/resources/studio/metapack", pack,
                "templates/org/ikasan/studio/generator/stringCollectionProperties_en.ftl"));
        helper = helper.substring(helper.indexOf('\n') + 1);
        List<String> list = List.of("a,b", "quote\"slash\\newline\n", "a,b", "");
        Map<String, String> map = Map.of("k", "v,\"\\\n");
        Path source = root.resolve("CollectionsProbe.java");
        Files.writeString(source, "public class CollectionsProbe {\n" + helper
                + "\npublic Object literalList() { return " + StringCollectionValues.javaLiteral(List.class, list) + "; }"
                + "\npublic Object literalMap() { return " + StringCollectionValues.javaLiteral(Map.class, map) + "; }\n}");
        Path environment = root.resolve("Environment.java");
        Files.writeString(environment, "package org.springframework.core.env; public interface Environment { String getRequiredProperty(String key); }");
        Path autowired = root.resolve("Autowired.java");
        Files.writeString(autowired, "package org.springframework.beans.factory.annotation; public @interface Autowired {}");
        String classpath = String.join(java.io.File.pathSeparator, List.of(location(ObjectMapper.class),
                location(com.fasterxml.jackson.core.JsonParser.class), location(com.fasterxml.jackson.annotation.JsonProperty.class)));
        var compiler = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "javac").toString(),
                "--release", pack.equals("V3.3.9") ? "11" : "17", "-encoding", "UTF-8", "-cp", classpath,
                "-d", root.toString(), source.toString(), environment.toString(), autowired.toString())
                .redirectErrorStream(true).redirectOutput(root.resolve("compiler.log").toFile()).start();
        try {
            assertThat(compiler.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(compiler.exitValue()).withFailMessage(Files.readString(root.resolve("compiler.log"))).isZero();
        } finally { if (compiler.isAlive()) compiler.destroyForcibly(); }
        try (var loader = new URLClassLoader(new URL[]{root.toUri().toURL()}, getClass().getClassLoader())) {
            Class<?> type = loader.loadClass("CollectionsProbe");
            Object probe = type.getConstructor().newInstance();
            var field = type.getDeclaredField("studioEnvironment"); field.setAccessible(true);
            Map<String, String> values = new HashMap<>();
            values.put("list", StringCollectionValues.json(list)); values.put("map", StringCollectionValues.json(map));
            field.set(probe, java.lang.reflect.Proxy.newProxyInstance(loader, new Class<?>[]{field.getType()}, (p, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "equals" -> p == args[0];
                        case "hashCode" -> System.identityHashCode(p);
                        case "toString" -> "String collection test environment";
                        default -> throw new UnsupportedOperationException(method.toString());
                    };
                }
                if (method.getName().equals("getRequiredProperty") && args != null
                        && args.length == 1 && args[0] instanceof String key) {
                    return Objects.requireNonNull(values.get(key), "Missing test property: " + key);
                }
                throw new UnsupportedOperationException(method.toString());
            }));
            Method readList = type.getDeclaredMethod("studioStringList", String.class); readList.setAccessible(true);
            Method readMap = type.getDeclaredMethod("studioStringMap", String.class); readMap.setAccessible(true);
            assertThat(readList.invoke(probe, "list")).isEqualTo(list);
            assertThat(readMap.invoke(probe, "map")).isEqualTo(map);
            assertThat(type.getMethod("literalList").invoke(probe)).isEqualTo(list);
            assertThat(type.getMethod("literalMap").invoke(probe)).isEqualTo(map);
            values.put("list", "one,two,one");
            assertThat(readList.invoke(probe, "list")).isEqualTo(List.of("one", "two", "one"));
            values.put("list", "[]"); assertThat(readList.invoke(probe, "list")).isEqualTo(List.of());
            values.put("list", "[1]"); assertThatThrownBy(() -> readList.invoke(probe, "list")).hasCauseInstanceOf(IllegalArgumentException.class);
            values.put("map", "{\"k\":\"a\",\"k\":\"b\"}");
            assertThatThrownBy(() -> readMap.invoke(probe, "map")).hasCauseInstanceOf(IllegalArgumentException.class);
        }
    }
    private String location(Class<?> type) throws Exception {
        var resource = Objects.requireNonNull(type.getResource(type.getSimpleName() + ".class"),
                "Missing class resource for " + type.getName());
        String url = resource.toExternalForm();
        if (url.contains("!/")) return Path.of(java.net.URI.create(url.substring(url.indexOf("file:"), url.indexOf("!/")))).toString();
        Path location = Path.of(resource.toURI());
        for (String ignored : type.getName().split("\\.")) location = location.getParent();
        return location.toString();
    }
}
