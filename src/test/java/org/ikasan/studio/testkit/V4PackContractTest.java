package org.ikasan.studio.testkit;

import org.ikasan.studio.core.TestFixtures;
import org.ikasan.studio.core.model.ikasan.instance.Module;
import java.util.List;

@org.junit.jupiter.api.Tag("packs")
class V4PackContractTest extends MetaPackContract {
    @Override protected String packId() { return "V4.1.6"; }
    @Override protected Module sampleModule() throws Exception {
        return TestFixtures.getMyFirstModuleIkasanModule(packId(),
                List.of(TestFixtures.getExceptionResolverFlow(packId())));
    }
}
