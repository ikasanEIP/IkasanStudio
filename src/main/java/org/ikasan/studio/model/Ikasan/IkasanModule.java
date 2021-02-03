package org.ikasan.studio.model.Ikasan;

import com.intellij.psi.PsiFile;
import org.ikasan.studio.ui.viewmodel.IkasanModuleViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * This class holds all the information about the Ikasan module flow.
 *
 * Its a deliberate decision not to use components from within the Ikasan framework itself in an attempt to insulate
 * from any changes to Ikasan or dependencies on any particular Ikasan version.
 */
public class IkasanModule extends IkasanComponent {
    private PsiFile moduleConfig;

    private String version;
    private List<IkasanFlow> flows = new ArrayList<>();

    public IkasanModule() {
        super();
        this.properties = new TreeMap<String, IkasanComponentProperty>();
        this.properties.put(IkasanComponentPropertyMeta.NAME, new IkasanComponentProperty(IkasanComponentPropertyMeta.STD_NAME_META_COMPONENT));
        this.properties.put(IkasanComponentPropertyMeta.DESCRIPTION, new IkasanComponentProperty(IkasanComponentPropertyMeta.STD_DESCIPTION_META_COMPONENT));
        this.viewHandler = new IkasanModuleViewHandler(this);
        setName("New Module");
        setDescription("New Module, please provide description");
    }

    /**
     * This will be called if the module is reloaded or re-initialised.
     */
    public void reset() {
        if (flows != null && flows.size() > 0) {
            flows = new ArrayList<>();
        }
        setName("My Ikasan Integration Module");
        setDescription("My Ikasan Integration Module");
    }

    /**
     * This will be called when we have a new anonymous flows to add
     * @param ikasanFlow
     */
    public boolean addAnonymousFlow(IkasanFlow ikasanFlow) {
        ikasanFlow.setName("newFlow" + (getFlows().size() + 1));
        return addFlow(ikasanFlow);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<IkasanFlow> getFlows() {
        return flows;
    }

    public IkasanFlow getFlow(IkasanFlow searchedFlow) {
        if (searchedFlow != null && flows != null && flows.size() > 0) {
            for (IkasanFlow currentFlow : getFlows()) {
                if (searchedFlow.equals(currentFlow)) {
                    return currentFlow;
                }
            }
        }
        return null;
    }

    public boolean addFlow(IkasanFlow ikasanFlow) {
        return flows.add(ikasanFlow);
    }

    public ViewHandler getViewHandler() {
        return viewHandler;
    }

    public void setViewHandler(IkasanModuleViewHandler viewHandler) {
        this.viewHandler = viewHandler;
    }

    @Override
    public String toString() {
        return "IkasanModule{" +
                "name='" + getName() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", version='" + version + '\'' +
                ", flowList=" + flows +
                '}';
    }

    public PsiFile getModuleConfig() {
        return moduleConfig;
    }

    public void setModuleConfig(PsiFile moduleConfig) {
        this.moduleConfig = moduleConfig;
    }
}
