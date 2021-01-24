package org.ikasan.studio.model.Ikasan;

import com.intellij.psi.PsiFile;
import org.ikasan.studio.ui.viewmodel.IkasanModuleViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * This class holds all the information about the Ikasan module flow.
 *
 * Its a deliberate decision not to just use the one inside the Ikasan framework itself in an attempt to insulate us
 * from any changes to Ikasan or dependancies on any particualr Ikasan version.
 */
public class IkasanModule {
    private ViewHandler viewHandler;
    private PsiFile moduleConfig;

    private String name;
    private String description;
    private String version;
    private List<IkasanFlow> flows = new ArrayList<>();

    /**
     * This will be called if the module is reloaded or re-initialised.
     */
    public void reset() {
        if (flows != null && flows.size() > 0) {
            flows = new ArrayList<>();
        }
        this.name = "Reset";
        this.description = "Reset";
    }

    /**
     * This will be called when we have a new anonymous flows to add
     * @param ikasanFlow
     */
    public boolean addAnonymousFlow(IkasanFlow ikasanFlow) {
        // @todo we should look though any existing flows and ensure this name is unique ?
        ikasanFlow.setName("newFlow1");
        return addFlow(ikasanFlow);
    }


    public IkasanModule() {
        this.viewHandler = new IkasanModuleViewHandler(this);
        this.name = "New Module";
        this.description = "New Module, please provide description";
    }

    public IkasanModule(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
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
