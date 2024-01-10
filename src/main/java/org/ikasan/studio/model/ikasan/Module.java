package org.ikasan.studio.model.ikasan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.intellij.psi.PsiFile;
import lombok.AllArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentPropertyMeta;
import org.ikasan.studio.model.ikasan.meta.IkasanComponentTypeMeta;
import org.ikasan.studio.ui.viewmodel.ViewHandlerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This class holds all the information about the ikasan module flow.
 * Its a deliberate decision not to use components from within the ikasan framework itself in an attempt to insulate
 * from any changes to ikasan or componentDependencies on any particular ikasan version.
 */
//@Data

@AllArgsConstructor
@Jacksonized
public class Module extends IkasanElement {
    @JsonPropertyOrder(alphabetic = true)
    @JsonIgnore
    private PsiFile moduleConfig;
    private String version;
    private List<Flow> flows = new ArrayList<>();

    public Module() {
        super(IkasanComponentTypeMeta.MODULE, IkasanComponentTypeMeta.MODULE.getMandatoryProperties());
        this.viewHandler = ViewHandlerFactory.getInstance(this);
    }

    /**
     * This will be called if the module is reloaded or re-initialised.
     */
    public void reset() {
        if (flows != null && !flows.isEmpty()) {
            flows = new ArrayList<>();
        }
        setComponentName("");
        setDescription("");
    }

    /**
     * Ensure any permissions to regenerate source is reset to false
     */
    public void resetRegenratePermissions() {
        for (Flow flow : flows) {
            for (IkasanElement component : flow.getFlowComponentList()) {
                component.resetUserImplementedClassPropertiesRegenratePermission();
            }
        }
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<Flow> getFlows() {
        return flows;
    }

    public Flow getFlow(Flow searchedFlow) {
        if (searchedFlow != null && flows != null && !flows.isEmpty()) {
            for (Flow currentFlow : getFlows()) {
                if (searchedFlow.equals(currentFlow)) {
                    return currentFlow;
                }
            }
        }
        return null;
    }

    public boolean addFlow(Flow ikasanFlow) {
        return flows.add(ikasanFlow);
    }

    @Override
    public String toString() {
        return "IkasanModule{" +
                "moduleConfig=" + moduleConfig +
                ", version='" + version + '\'' +
                ", flows=" + flows +
                ", Module type=" + ikasanComponentTypeMeta +
                ", Module properties=" + configuredProperties +
                '}';
    }

    @JsonIgnore
    public PsiFile getModuleConfig() {
        return moduleConfig;
    }
    public void setModuleConfig(PsiFile moduleConfig) {
        this.moduleConfig = moduleConfig;
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
