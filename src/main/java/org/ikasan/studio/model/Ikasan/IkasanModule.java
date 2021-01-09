package org.ikasan.studio.model.Ikasan;

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

    private String name;
    private String description;
    private String version;
    private List<IkasanFlow> flows = new ArrayList<>();

    public IkasanModule() {
        viewHandler = new IkasanModuleViewHandler(this);
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

//    public void setFlows(List<IkasanFlow> flows) {
//        this.flows = flows;
//    }

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
}
