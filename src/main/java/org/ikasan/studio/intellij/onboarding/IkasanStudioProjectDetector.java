package org.ikasan.studio.intellij.onboarding;

import com.intellij.openapi.diagnostic.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Recognises projects created by current and earlier Ikasan Studio archetypes.
 */
public final class IkasanStudioProjectDetector {
    public static final String PROJECT_MARKER = ".ikasan-studio-project";
    private static final Logger LOG = Logger.getInstance(IkasanStudioProjectDetector.class);

    private IkasanStudioProjectDetector() {
    }

    public static boolean isIkasanStudioProject(Path projectRoot) {
        if (projectRoot == null || !Files.isDirectory(projectRoot)) {
            return false;
        }
        if (Files.isRegularFile(projectRoot.resolve(PROJECT_MARKER))) {
            return true;
        }

        Path rootPom = projectRoot.resolve("pom.xml");
        Path generatedPom = projectRoot.resolve("generated/pom.xml");
        Path userPom = projectRoot.resolve("user/pom.xml");
        Path model = projectRoot.resolve("generated/src/main/model/model.json");
        if (!Files.isRegularFile(rootPom)
                || !Files.isRegularFile(generatedPom)
                || !Files.isRegularFile(userPom)
                || !Files.isRegularFile(model)) {
            return false;
        }

        try {
            Set<String> modules = readMavenModules(rootPom);
            return modules.contains("generated") && modules.contains("user");
        } catch (Exception e) {
            LOG.warn("STUDIO: Could not inspect project POM while detecting an Ikasan Studio project", e);
            return false;
        }
    }

    private static Set<String> readMavenModules(Path pom) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        Document document = factory.newDocumentBuilder().parse(pom.toFile());
        NodeList moduleNodes = document.getElementsByTagNameNS("*", "module");
        Set<String> modules = new HashSet<>();
        for (int index = 0; index < moduleNodes.getLength(); index++) {
            Node module = moduleNodes.item(index);
            String value = module.getTextContent();
            if (value != null) {
                modules.add(value.trim().replace('\\', '/'));
            }
        }
        return modules;
    }
}
