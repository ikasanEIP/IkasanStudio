package org.ikasan.metapack;

import java.nio.file.*;
import java.util.*;
import org.ikasan.studio.core.metapack.ComponentLibrary;
import org.ikasan.studio.core.metapack.loading.MetaPackInstaller;
import org.ikasan.studio.core.model.ikasan.instance.*;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.ikasan.studio.testkit.MetaPackTestKit;

/** Invoked in separate JVMs with target/classes excluded: V5 must come from the installed JAR. */
public class ExternalPackProbe {
    public static void main(String[] args) throws Exception {
        String id = "V5.0.1-SNAPSHOT";
        if (args[0].equals("install")) {
            if (ComponentLibrary.getMetapackList().contains(id)) throw new AssertionError("V5 already baked into host");
            MetaPackInstaller.install(Path.of(args[1]), Path.of(System.getProperty("demo.pack.directory")), () -> {});
            if (ComponentLibrary.getMetapackList().contains(id)) throw new AssertionError("Install mutated fixed session");
            System.out.println("PASS: host has no V5; installer accepted external JAR; restart required");
        } else {
            if (!ComponentLibrary.getMetapackList().equals(List.of(id))) throw new AssertionError("External discovery failed");
            if (!ComponentLibrary.getMetapackList(false).isEmpty()) throw new AssertionError("Snapshot opt-out failed");
            if (!ComponentLibrary.getMetapackList(true).contains(id)) throw new AssertionError("Snapshot opt-in failed");
            if (!"26".equals(ComponentLibrary.getMetaPackManifest(id).javaVersion())) throw new AssertionError("Wrong JDK");
            Flow flow = Flow.flowBuilder().metapackVersion(id).name("Example Flow").build();
            flow.setConsumer(FlowElementFactory.createFlowElement(id,
                    ComponentLibrary.getIkasanComponentByKeyMandatory(id, "Event Generating Consumer"),
                    flow, flow.getFlowRoute(), "Input"));
            flow.getFlowRoute().getFlowElements().add(FlowElementFactory.createFlowElement(id,
                    ComponentLibrary.getIkasanComponentByKeyMandatory(id, "Dev Null Producer"),
                    flow, flow.getFlowRoute(), "Output"));
            Module module = Module.moduleBuilder().version(id).name("ExternalIkasan5Demo")
                    .applicationPackageName("org.example.demo").port("8080").h2PortNumber("8092")
                    .h2WebPortNumber("8093").flows(List.of(flow)).build();
            Path output = Path.of(args[1]);
            for (var artifact : MetaPackTestKit.render(module).entrySet()) {
                Path target = output.resolve(artifact.getKey());
                Files.createDirectories(target.getParent()); Files.writeString(target, artifact.getValue());
            }
            System.out.println("PASS: restarted host discovers installed V5, applies snapshot policy and renders a flow");
        }
    }
}
