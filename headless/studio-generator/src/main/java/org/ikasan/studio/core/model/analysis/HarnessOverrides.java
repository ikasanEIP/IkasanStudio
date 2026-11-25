package org.ikasan.studio.core.model.analysis;

import java.nio.charset.StandardCharsets;
import java.util.Base64.Encoder;
import java.util.Base64;
import java.util.List;
import org.ikasan.studio.core.StudioBuildException;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.model.ComponentMeta;
import org.ikasan.studio.core.metapack.model.MetaPackManifest;
import org.ikasan.studio.core.model.analysis.TestJmsHarnessLinks.Link;
import org.ikasan.studio.core.model.ikasan.instance.Flow;
import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;

/** Stable launch identities, independent of configured endpoints and credentials. */
public final class HarnessOverrides {
    public static final String SELECTION = "studio.harness.components";
    private HarnessOverrides() { }
    public record Target(String id, String kind, FlowElement element) {
        public String label() {
            return new String(Base64.getUrlDecoder().decode(id.substring(0, id.indexOf('.'))), StandardCharsets.UTF_8)
                    + " / " + element.getIdentity() + " (" + kind + ")";
        }
    }
    public static String id(FlowElement element) {
        return id(element.getContainingFlow().getIdentity(), element.getIdentity());
    }
    public static String id(String flow, String component) {
        Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(flow.getBytes(StandardCharsets.UTF_8))
                + "." + encoder.encodeToString(component.getBytes(StandardCharsets.UTF_8));
    }
    public static String kind(Module module, FlowElement element) {
        if (module == null || element == null || element.getComponentMeta() == null) return "";
        try {
            MetaPackManifest manifest = ComponentLibrary.getMetaPackManifest(module.getMetaVersion());
            if (manifest.generatorApiVersion() == null || manifest.generatorApiVersion() < 4) return "";
        } catch (StudioBuildException unavailable) {
            return ""; // Pack loading reports its own diagnostics; the selector offers no unsupported overrides.
        }
        ComponentMeta meta = element.getComponentMeta();
        if (!meta.isUsesBuilderInFactory() || meta.isGeneratesUserImplementedClass() || meta.isUseImplementingClassInFactory()) return "";
        if ("SFTP Endpoint".equals(meta.getEndpointKey())) return "SFTP";
        if (meta.supportsTestFtpServer()) return "FTP";
        if (meta.supportsTestMailServer()) return "Email";
        if (TestJmsHarnessLinks.destinationOverride(module, element) != null) return "JMS";
        return "";
    }
    public static List<Target> targets(Module module) {
        if (module == null || module.getFlows() == null) return List.of();
        return module.getFlows().stream().flatMap(f -> f.getFlowElementsNoExternalEndPoints().stream()
                .map(e -> new Target(id(f.getIdentity(), e.getIdentity()), kind(module, e), e)))
                .filter(t -> !t.kind().isEmpty())
                // A JMS reader follows its owner's selection; it is not a second selectable connection.
                .filter(t -> !"JMS".equals(t.kind()) || TestJmsHarnessLinks.findLinks(module).stream()
                        .anyMatch(link -> link.ownerProducer() == t.element())).toList();
    }
    // Called reflectively by the component factory FreeMarker templates.
    @SuppressWarnings("unused")
    public static String selectionId(Module module, Flow flow, FlowElement element) {
        for (Link link : TestJmsHarnessLinks.findLinks(module)) {
            if (link.harnessFlow().getConsumer() == element) return id(link.ownerProducer());
        }
        return id(flow.getIdentity(), element.getIdentity());
    }
}
