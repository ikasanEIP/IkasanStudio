package org.ikasan.studio.model.ik;

import java.util.List;

public class Flow {
    String name;
    String configurationId;
    String flowStartupType;
    String flowStartupComment;
    FlowElement consumer;
    List<Transition> transitions;
    List<FlowElement> flowElements;
}
