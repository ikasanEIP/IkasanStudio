package org.ikasan.studio.cli;

import org.junit.jupiter.api.Test;
import org.ikasan.studio.core.migration.*;
import org.ikasan.studio.core.persistence.json.StudioJson;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.StringReader;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class MigrationDependencyTest {
    private String source() throws Exception {
        var json=StudioJson.newObjectMapper();
        var model=json.readTree(Files.readString(Path.of("src/test/resources/org/ikasan/studio/populated_module.json")));
        var converter=(ObjectNode)model.path("flows").get(0).path("flowElements").get(0);
        String name=converter.path("componentName").asText(); converter.removeAll();
        converter.put("componentName",name);
        converter.put("componentType","org.ikasan.spec.component.transformation.Converter");
        converter.put("implementingClass","org.ikasan.builder.component.converter.ObjectToXmlStringConverterBuilder");
        converter.put("objectClass","java.lang.String"); converter.put("rootClassName","java.lang.String"); converter.put("rootName","order");
        return model.toString();
    }
    private String baseline(String source) throws Exception {
        return MigrationArtifacts.render(new ModelMigration.Plan("V3.3.9","V3.3.9",source,source,java.util.List.of()),
                Files.readString(Path.of("regression-tests/migration/project/pom.xml"))).get("pom.xml");
    }
    @Test void removesRetiredBomDependenciesOnUpgradeAndDowngrade() throws Exception {
        String source=source(), before=baseline(source);
        assertTrue(before.contains("javax.xml.bind")); assertTrue(before.contains("jaxb-impl"));
        var plan=ModelMigration.analyse(source,"V4.1.6");assertTrue(plan.canApply(),plan.report());
        String after=MigrationArtifacts.render(plan,before).get("pom.xml");
        assertFalse(after.contains("javax.xml.bind")); assertFalse(after.contains("jaxb-impl"));
        assertTrue(after.contains("jakarta.xml.bind"));assertTrue(after.contains("jaxb-runtime"));
        var back=ModelMigration.analyse(plan.targetJson(),"V3.3.9");
        String restored=MigrationArtifacts.render(back,after).get("pom.xml");
        assertTrue(restored.contains("javax.xml.bind"));assertTrue(restored.contains("jaxb-impl"));
        assertFalse(restored.contains("jakarta.xml.bind"));assertFalse(restored.contains("jaxb-runtime"));
    }
    @Test void preservesExplicitVersionsAndUnrelatedDependencies() throws Exception {
        String source=source(), before=baseline(source).replace("<artifactId>jaxb-api</artifactId>","<artifactId>jaxb-api</artifactId><version>2.3.1</version>");
        String after=MigrationArtifacts.render(ModelMigration.analyse(source,"V4.1.6"),before).get("pom.xml");
        var pom=new MavenXpp3Reader().read(new StringReader(after));
        assertTrue(pom.getDependencies().stream().anyMatch(d->d.getArtifactId().equals("jaxb-api")&&"2.3.1".equals(d.getVersion())));
        assertTrue(pom.getDependencies().stream().anyMatch(d->d.getGroupId().equals("junit")&&"4.13.2".equals(d.getVersion())));
    }
}
