package org.ikasan.studio.testing.packs;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Tag;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** Audit every bundled FTL, including support classes that have no palette component. */
@Tag("packs")
class GeneratedTemplateJavadocTest {
    @ParameterizedTest
    @MethodSource("org.ikasan.studio.testing.packs.PackExpectations#metaPacksToTest")
    void javadocTagsHaveDescriptionsAndAreRecognized(String pack) throws Exception {
        var blocks = Pattern.compile("/\\*\\*.*?\\*/", Pattern.DOTALL);
        var tags = Pattern.compile("(?m)^\\h*\\*\\h*@([A-Za-z]+)([^\\r\\n]*)");
        Set<String> supported = Set.of("author", "deprecated", "exception", "param", "return", "see", "serial", "serialData", "serialField", "since", "throws", "version");
        try (var paths = Files.walk(Path.of("src/main/resources/studio/metapack", pack, "templates"))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".ftl")).toList()) {
                var docs = blocks.matcher(Files.readString(path));
                while (docs.find()) {
                    var found = tags.matcher(docs.group());
                    while (found.find()) {
                        String tag = found.group(1);
                        String body = found.group(2).trim();
                        assertTrue(supported.contains(tag), path + ": unrecognized Javadoc tag @" + tag);
                        if (Set.of("param", "throws", "exception").contains(tag)) {
                            assertEquals(2, body.split("\\s+", 2).length, path + ": missing description for @" + tag + " " + body);
                        } else if (tag.equals("return")) {
                            assertFalse(body.isBlank(), path + ": missing return description");
                        }
                    }
                }
            }
        }
    }
}
