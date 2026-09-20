package scratch;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.generator.*;
import org.ikasan.studio.core.io.ComponentIO;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;

class ScratchFixtureTest {
    @Test void render() throws Exception {
        List<String> notes = new ArrayList<>();
        for (String name : List.of("populated_full_module_with_exception_resolver", "populated_module_with_exception_resolver", "populated_module_with_router")) {
            try {
                String json = Files.readString(Path.of("src/test/resources/org/ikasan/studio/" + name + ".json"));
                Module m = ComponentIO.validatePersistedModuleJson(json, name, false);
                Path d = Path.of("/tmp/claude-1001/-home-hidavi-dev-ws-IkasanStudio-dev2/155bf7ab-6d73-4824-a378-95e413a06f03/scratchpad/gen5");
                Files.writeString(d.resolve(name + "__App.java"), ApplicationTemplate.create(m));
                Files.writeString(d.resolve(name + "__ModuleConfig.java"), ModuleConfigTemplate.create(m));
                Files.writeString(d.resolve(name + "__Inject.java"), StudioInjectControllerTemplate.create(m));
                int i = 0;
                for (var flow : m.getFlows()) {
                    String pkg = "org.ikasan.studio.boot.flow." + flow.getJavaPackageName();
                    Files.writeString(d.resolve(name + "__Flow" + i + ".java"), FlowTemplate.create(pkg, m, flow));
                    Files.writeString(d.resolve(name + "__Factory" + i + ".java"), FlowsComponentFactoryTemplate.create(pkg, m, flow));
                    i++;
                }
                notes.add(name + " flows=" + i);
            } catch (Throwable t) {
                notes.add(name + " EXCEPTION " + t);
            }
        }
        System.out.println("=====NOTES=====");
        notes.forEach(System.out::println);
    }
}
