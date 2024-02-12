package org.ikasan.studio.model.ikasan.instance;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Transition {
    private String from;
    private String to;
    private String name;
}
