package org.ikasan.studio.core.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.maven.model.Dependency;
import org.ikasan.studio.core.model.ModelUtils;
import org.ikasan.studio.core.model.ikasan.instance.serialization.ModuleDeserializer;
import org.ikasan.studio.core.model.ikasan.instance.serialization.ModuleSerializer;
import org.ikasan.studio.core.model.ikasan.meta.ComponentPropertyMeta;
import org.ikasan.studio.core.model.ikasan.meta.IkasanComponentLibrary;

import java.util.*;

/**
 * This class holds all the information about the ikasan module flow.
 * It's a deliberate decision not to use components from within the ikasan framework itself in an attempt to insulate
 * from any changes to ikasan or componentDependencies on any particular ikasan version.
 */

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper=true)

@JsonSerialize(using = ModuleSerializer.class)
@JsonDeserialize(using = ModuleDeserializer.class)

public class Module extends BasicElement {
    @JsonPropertyOrder(alphabetic = true)
    @JsonSetter(nulls = Nulls.SKIP)   // If the supplied value is null, ignore it.
    private List<Flow> flows;

    public Module() {
        super (IkasanComponentLibrary.getModule(IkasanComponentLibrary.STD_IKASAN_PACK), null);
        flows = new ArrayList<>();
    }

    @Builder (builderMethodName = "moduleBuilder")
    public Module(String name,
                  String description,
                  String version,
                  String applicationPackageName,
                  String port,
                  String h2PortNumber,
                  String h2WebPortNumber,
                  List<Flow> flows) {
        super (IkasanComponentLibrary.getModule(IkasanComponentLibrary.STD_IKASAN_PACK), description);

        setVersion(version);
        setName(name);
        setApplicationPackageName(applicationPackageName);
        setPort(port);
        setH2DbPortNumber(h2PortNumber);
        setH2WebPortNumber(h2WebPortNumber);
        this.flows = Objects.requireNonNullElseGet(flows, ArrayList::new);
    }

    public boolean addFlow(Flow ikasanFlow) {
        return flows.add(ikasanFlow);
    }

    public String getApplicationPackageName() {
        return (String) getPropertyValue(ComponentPropertyMeta.APPLICATION_PACKAGE_NAME);
    }
    @JsonIgnore
    public void setApplicationPackageName(String applicationPackageName) {
        this.setPropertyValue(ComponentPropertyMeta.APPLICATION_PACKAGE_NAME, applicationPackageName);
    }
    public String getPort() {
        return (String) getPropertyValue(ComponentPropertyMeta.APPLICATION_PORT_NUMBER_NAME);
    }
    @JsonIgnore
    public void setPort(String portNumber) {
        this.setPropertyValue(ComponentPropertyMeta.APPLICATION_PORT_NUMBER_NAME, portNumber);
    }
    public String getH2PortNumber() {
        return (String) getPropertyValue(ComponentPropertyMeta.H2_DB_PORT_NUMBER_NAME);
    }
    @JsonIgnore
    public void setH2DbPortNumber(String portNumber) {
        this.setPropertyValue(ComponentPropertyMeta.H2_DB_PORT_NUMBER_NAME, portNumber);
    }
    public String getH2WebPortNumber() {
        return (String) getPropertyValue(ComponentPropertyMeta.H2_WEB_PORT_NUMBER_NAME);
    }
    @JsonIgnore
    public void setH2WebPortNumber(String portNumber) {
        this.setPropertyValue(ComponentPropertyMeta.H2_WEB_PORT_NUMBER_NAME, portNumber);
    }
//
//    /**
//     * Get the sorted set of all Jar dependencies for the module and all its components
//     * The set will be sorted on groupId, artifactID and version.
//     * The set will be de-duplicated based on groupId, artifactID and version.
//     * @return a set of jar Dependencies for the module
//     */
//    public Set<Dependency> getAllJarDependencies2() {
//        SortedSet<Dependency> allJarDepedenciesSorted =  new TreeSet<>(Comparator.comparing(Dependency::getGroupId).thenComparing(Dependency::getArtifactId).thenComparing(Dependency::getVersion));
//        Map<String, Dependency> allJarDepedencies =  new TreeMap<>();
//        this.getComponentMeta().getJarDependencies().stream().forEach(
//            dep -> allJarDepedencies.put(dep.getGroupId()+dep.getArtifactId()+dep.getVersion(), dep)
//        );
//
//         for(Flow flow : this.getFlows()) {
//            for (FlowElement flowElement : flow.ftlGetConsumerAndFlowElements()) {
//                if (flowElement.getComponentMeta().getJarDependencies() != null) {
//                    flowElement.getComponentMeta().getJarDependencies().forEach(
//                            dep -> allJarDepedencies.put(dep.getGroupId() + dep.getArtifactId() + dep.getVersion(), dep)
//                    );
//                }
//            }
//        }
//        allJarDepedenciesSorted.addAll(allJarDepedencies.values());
//        return allJarDepedenciesSorted;
//    }

    /**
     * Get the sorted set of all Jar dependencies for the module and all its components
     * The set will be sorted on groupId, artifactID and version.
     * The set will be de-duplicated based on groupId, artifactID and version.
     * @return a set of jar Dependencies for the module
     */
    public Set<Dependency> getAllUniqueSortedJarDependencies() {
        Set<Dependency> allJarDepedencies = new HashSet<>(this.getComponentMeta().getJarDependencies());

         for(Flow flow : this.getFlows()) {
             for (FlowElement flowElement : flow.ftlGetConsumerAndFlowElements()) {
                 if (flowElement.getComponentMeta().getJarDependencies() != null) {
                     allJarDepedencies.addAll(flowElement.getComponentMeta().getJarDependencies());
                 }
             }
        }
        return ModelUtils.getAllUniqueSortedDependenciesSet(allJarDepedencies);
    }


}