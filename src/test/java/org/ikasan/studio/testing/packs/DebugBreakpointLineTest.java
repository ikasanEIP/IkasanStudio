package org.ikasan.studio.testing.packs;

import org.ikasan.studio.core.TestFixtures;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class DebugBreakpointLineTest extends AbstractGeneratorTestFixtures {
    @ParameterizedTest
    @ValueSource(strings = {"V3.3.9", "V4.1.6"})
    void breakpointStatementHasOneBytecodeLocation(String pack, @TempDir Path directory) throws Exception {
        var module = TestFixtures.getMyFirstModuleIkasanModule(pack, new ArrayList<>());
        String source = generateUserImplementedComponentTemplate(pack, module, TestFixtures.getDebugTransition(pack));
        var packageMatcher = Pattern.compile("package ([\\w.]+);").matcher(source);
        var classMatcher = Pattern.compile("public class (\\w+)").matcher(source);
        assertTrue(packageMatcher.find());
        assertTrue(classMatcher.find());
        String className = classMatcher.group(1);
        String qualifiedName = packageMatcher.group(1) + "." + className;
        List<String> lines = source.lines().toList();
        int breakpointLine = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("logger.debug(")) breakpointLine = i + 1;
        }
        assertTrue(breakpointLine > 0);
        // Minimal dependencies keep the bytecode check independent of the test runner's class loader.
        List<Path> sources = List.of(
                write(directory, className + ".java", source),
                write(directory, "Component.java", "package org.springframework.stereotype; public @interface Component { String value(); }"),
                write(directory, "Logger.java", "package org.slf4j; public interface Logger { void debug(String text, Object value); }"),
                write(directory, "LoggerFactory.java", "package org.slf4j; public class LoggerFactory { public static Logger getLogger(Class<?> type) { return null; } }"),
                write(directory, "DebugTransitionComponent.java", "package org.ikasan.studio.component; public abstract class DebugTransitionComponent { public abstract void debug(Object payload); }"));
        List<String> arguments = new ArrayList<>(List.of("-g", "--release", pack.equals("V3.3.9") ? "11" : "17", "-d", directory.toString()));
        sources.forEach(file -> arguments.add(file.toString()));
        arguments.add(0, "javac");
        Path compilerOutput = directory.resolve("javac.txt");
        Process compiler = new ProcessBuilder(arguments).redirectErrorStream(true)
                .redirectOutput(compilerOutput.toFile()).start();
        if (!compiler.waitFor(20, TimeUnit.SECONDS)) { compiler.destroyForcibly(); fail("javac timed out"); }
        assertEquals(0, compiler.exitValue(), Files.readString(compilerOutput));
        Path output = directory.resolve("javap.txt");
        Process process = new ProcessBuilder("javap",
                "-l", "-classpath", directory.toString(), qualifiedName)
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        if (!process.waitFor(20, TimeUnit.SECONDS)) { process.destroyForcibly(); fail("javap timed out"); }
        String table = Files.readString(output);
        assertEquals(0, process.exitValue(), table);
        String entry = "line " + breakpointLine + ":";
        assertEquals(1, table.lines().filter(line -> line.strip().startsWith(entry)).count(), table);
    }

    private static Path write(Path directory, String name, String source) throws Exception {
        return Files.writeString(directory.resolve(name), source);
    }
}
