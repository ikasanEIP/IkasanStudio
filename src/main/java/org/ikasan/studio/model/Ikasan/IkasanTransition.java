package org.ikasan.studio.model.Ikasan;

public class IkasanTransition {
    private String from;
    private String to;
    private String name;

    public IkasanTransition(String from, String to, String name) {
        this.from = from;
        this.to = to;
        this.name = name;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
