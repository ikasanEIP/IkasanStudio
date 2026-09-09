package org.ikasan.studio.testing.engine;

import org.ikasan.studio.core.generator.FreemarkerUtils;
import org.ikasan.studio.core.generator.StudioGeneratorException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Small authored templates exercise engine behaviour without an official Ikasan release. */
@org.junit.jupiter.api.Tag("engine")
class TemplateEngineTest {
    private static String render(String pack, String name, String value) throws StudioGeneratorException {
        return FreemarkerUtils.generateFromTemplate(pack, name, new HashMap<>(Map.of("value", value)));
    }

    @Test
    void resolvesTemplateAndIncludesWithinTheRequestedPack() throws Exception {
        assertEquals("alpha:first", render("EngineAlpha", "main.ftl", "first"));
        assertEquals("beta:second", render("EngineBeta", "main.ftl", "second"));
        assertEquals("alpha:third", render("EngineAlpha", "main.ftl", "third"));
    }

    @Test
    void decodesUtf8AndNormalizesLineEndings() throws Exception {
        assertEquals("日本語 café\nvalue\n", render("EngineAlpha", "text.ftl", "value"));
    }

    @Test
    void exposesJavaUtilitiesToTemplates() throws Exception {
        assertEquals("123", render("EngineAlpha", "utility.ftl", "123"));
    }

    @Test
    void missingTemplateDoesNotFallBackToAnotherPack() throws Exception {
        assertEquals("only-alpha", render("EngineAlpha", "exclusive.ftl", "unused"));
        assertThrows(StudioGeneratorException.class, () -> render("EngineBeta", "exclusive.ftl", "unused"));
    }

    @Test
    void simultaneousRendersKeepPackAndModelInputsIsolated() throws Exception {
        CompletableFuture<?>[] renders = new CompletableFuture<?>[20];
        for (int i = 0; i < renders.length; i++) {
            final int index = i;
            renders[i] = CompletableFuture.runAsync(() -> {
                try {
                    String pack = index % 2 == 0 ? "EngineAlpha" : "EngineBeta";
                    String prefix = index % 2 == 0 ? "alpha:" : "beta:";
                    assertEquals(prefix + index, render(pack, "main.ftl", String.valueOf(index)));
                } catch (StudioGeneratorException failure) {
                    throw new AssertionError(failure);
                }
            });
        }
        CompletableFuture.allOf(renders).get(10, TimeUnit.SECONDS);
    }
}
