package org.ikasan.studio.model.ikasan.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.intellij.psi.PsiFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.jackson.Jacksonized;
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
//@Data

@Data
@AllArgsConstructor
@Jacksonized
@Builder
@EqualsAndHashCode(callSuper=true)
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

    public boolean addFlow(Flow ikasanFlow) {
        return flows.add(ikasanFlow);
    }


    public String getApplicationPackageName() {
        return (String) getPropertyValue(IkasanComponentPropertyMeta.APPLICATION_PACKAGE_NAME);
    }
    @JsonIgnore
    public void setApplicationPackageName(String applicationPackageName) {
        this.setPropertyValue(IkasanComponentPropertyMeta.APPLICATION_PACKAGE_NAME, IkasanComponentPropertyMeta.STD_PACKAGE_NAME_META_COMPONENT, applicationPackageName);
    }
    public String getApplicationPortNumber() {
        return (String) getPropertyValue(IkasanComponentPropertyMeta.APPLICATION_PORT_NUMBER_NAME);
    }
    @JsonIgnore
    public void setApplicationPortNumber(String applicationPortNumber) {
        this.setPropertyValue(IkasanComponentPropertyMeta.APPLICATION_PORT_NUMBER_NAME, IkasanComponentPropertyMeta.STD_PORT_NUMBER_META_COMPONENT, applicationPortNumber);
    }
    public String getH2PortNumber() {
        return (String) getPropertyValue(IkasanComponentPropertyMeta.H2_DB_PORT_NUMBER_NAME);
    }
    @JsonIgnore
    public void setH2DbPortNumber(String applicationPortNumber) {
        this.setPropertyValue(IkasanComponentPropertyMeta.H2_DB_PORT_NUMBER_NAME, IkasanComponentPropertyMeta.STD_PORT_NUMBER_META_COMPONENT, applicationPortNumber);
    }
    public String getH2WebPortNumber(String applicationPortNumber) {
        return (String) getPropertyValue(IkasanComponentPropertyMeta.H2_WEB_PORT_NUMBER_NAME);
    }    @JsonIgnore
    public void setH2WebPortNumber(String applicationPortNumber) {
        this.setPropertyValue(IkasanComponentPropertyMeta.H2_WEB_PORT_NUMBER_NAME, IkasanComponentPropertyMeta.STD_PORT_NUMBER_META_COMPONENT, applicationPortNumber);
    }
}
