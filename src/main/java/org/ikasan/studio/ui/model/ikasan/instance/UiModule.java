package org.ikasan.studio.ui.model.ikasan.instance;

import lombok.Builder;
import org.ikasan.studio.build.model.ikasan.instance.Flow;
import org.ikasan.studio.build.model.ikasan.instance.Module;
import org.ikasan.studio.ui.viewmodel.AbstractViewHandler;
import org.ikasan.studio.ui.viewmodel.ViewHandlerFactory;

import java.util.List;

//@Data
//@EqualsAndHashCode(callSuper=true)
//public class UiModule extends Module {
public class UiModule extends UiBasicElement {
    protected AbstractViewHandler viewHandler;
    private Module module;
    private List<UiFlow> uiFlows;
//    @Builder (builderMethodName = "uiModuleBuilder")
//    public UiModule(String name, String description, String version, String applicationPackageName, String port,
//                  String h2PortNumber, String h2WebPortNumber, List<Flow> flows) {
//        super(name, description, version, applicationPackageName, port, h2PortNumber, h2WebPortNumber, flows);
//        this.viewHandler = ViewHandlerFactory.getInstance(this);
//    }

    @Builder (builderMethodName = "uiModuleBuilder")
    public UiModule(Module module) {
        this.module = module;
        this.viewHandler = ViewHandlerFactory.getInstance(this.module);
        if (module.getFlows() != null && !module.getFlows().isEmpty()) {
            for(Flow flow : module.getFlows()) {
                uiFlows.add(new UiFlow(flow));
            }
        }
    }
}
