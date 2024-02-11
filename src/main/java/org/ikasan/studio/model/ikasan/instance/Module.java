package org.ikasan.studio.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.intellij.psi.PsiFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentLibrary;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentPropertyMeta;
import org.ikasan.studio.ui.viewmodel.ViewHandlerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.ikasan.studio.model.ikasan.meta.IkasanComponentLibrary.STD_IKASAN_PACK;

/**
 * This class holds all the information about the ikasan module flow.
 * Its a deliberate decision not to use components from within the ikasan framework itself in an attempt to insulate
 * from any changes to ikasan or componentDependencies on any particular ikasan version.
 */

@Data
@AllArgsConstructor
//@Jacksonized
@EqualsAndHashCode(callSuper=true)
@JsonDeserialize(using = ModuleDeserializer.class)
public class Module extends IkasanElement {
    @JsonPropertyOrder(alphabetic = true)
    @JsonIgnore
    private PsiFile moduleConfig;
    private String version;
    @JsonSetter(nulls = Nulls.SKIP)   // If the supplied value is null, ignore it.
    private List<Flow> flows;

    public Module() {
        super (IkasanComponentLibrary.getModule(STD_IKASAN_PACK));
        this.viewHandler = ViewHandlerFactory.getInstance(this);
        flows = new ArrayList<>();
    }

    @Builder (builderMethodName = "moduleBuilder")
    public Module(String name,
                  String description,
                  String version,
                  String applicationPackageName,
                  String port,
                  String h2PortNumber,
                  String h2WebPortNumber) {
        super (IkasanComponentLibrary.getModule(STD_IKASAN_PACK), description);
        this.viewHandler = ViewHandlerFactory.getInstance(this);
        flows = new ArrayList<>();
        this.version = version;
        setName(name);
        setApplicationPackageName(applicationPackageName);
        setPort(port);
        setH2DbPortNumber(h2PortNumber);
        setH2WebPortNumber(h2WebPortNumber);
    }

    public boolean addFlow(Flow ikasanFlow) {
        return flows.add(ikasanFlow);
    }

    public String getApplicationPackageName() {
        return (String) getPropertyValue(IkasanComponentPropertyMeta.APPLICATION_PACKAGE_NAME);
    }
    @JsonIgnore
    public void setApplicationPackageName(String applicationPackageName) {
//        this.setPropertyValue(IkasanComponentPropertyMeta.APPLICATION_PACKAGE_NAME, IkasanComponentPropertyMeta.STD_PACKAGE_NAME_META_COMPONENT, applicationPackageName);
        this.setPropertyValue(IkasanComponentPropertyMeta.APPLICATION_PACKAGE_NAME, applicationPackageName);
    }
    public String getPort() {
        return (String) getPropertyValue(IkasanComponentPropertyMeta.APPLICATION_PORT_NUMBER_NAME);
    }
    @JsonIgnore
    public void setPort(String portNumber) {
//        this.setPropertyValue(IkasanComponentPropertyMeta.APPLICATION_PORT_NUMBER_NAME, IkasanComponentPropertyMeta.STD_PORT_NUMBER_META_COMPONENT, portNumber);
        this.setPropertyValue(IkasanComponentPropertyMeta.APPLICATION_PORT_NUMBER_NAME, portNumber);
    }
    public String getH2PortNumber() {
        return (String) getPropertyValue(IkasanComponentPropertyMeta.H2_DB_PORT_NUMBER_NAME);
    }
    @JsonIgnore
    public void setH2DbPortNumber(String portNumber) {
//        this.setPropertyValue(IkasanComponentPropertyMeta.H2_DB_PORT_NUMBER_NAME, IkasanComponentPropertyMeta.STD_PORT_NUMBER_META_COMPONENT, portNumber);
        this.setPropertyValue(IkasanComponentPropertyMeta.H2_DB_PORT_NUMBER_NAME, portNumber);
    }
    public String getH2WebPortNumber() {
        return (String) getPropertyValue(IkasanComponentPropertyMeta.H2_WEB_PORT_NUMBER_NAME);
    }
    @JsonIgnore
    public void setH2WebPortNumber(String portNumber) {
//        this.setPropertyValue(IkasanComponentPropertyMeta.H2_WEB_PORT_NUMBER_NAME, IkasanComponentPropertyMeta.STD_PORT_NUMBER_META_COMPONENT, portNumber);
        this.setPropertyValue(IkasanComponentPropertyMeta.H2_WEB_PORT_NUMBER_NAME, portNumber);
    }

    public static IkasanBaseElement callBack(final ObjectMapper MAPPER, final String jsonString) throws JsonProcessingException {
        return MAPPER.readValue(jsonString, Module.class);
    }
}
