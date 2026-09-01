package org.ikasan.studio.core.model.ikasan.instance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Groups every Email Producer across a Module's flows by its configured (mailSmtpHost, mailSmtpPort) address -
 * fills the same role for the canvas's shared "Test Mail Server" node/connector lines
 * (see {@code DesignerCanvas#paintTestMailServerNode}) that {@link JmsFlowConnections} fills for the JMS
 * shared-destination association lines, just many-producers-to-one-address rather than pairwise.
 * -
 * Framework-independent (no PSI/UI dependency), like {@link JmsFlowConnections}, so the grouping itself stays
 * plain-JUnit testable - actually probing "is anything really listening at that address" is a live TCP call
 * and stays in the ui layer (see {@code org.ikasan.studio.ui.actions.TestMailServerSupport}), since only
 * Start/Stop and the live canvas repaint loop ever need to perform it.
 */
public final class TestMailServerLinks {
    public static final String DEFAULT_SMTP_HOST = "127.0.0.1";
    public static final int DEFAULT_SMTP_PORT = 1025;

    private TestMailServerLinks() {}

    /** One shared address, and every Email Producer (possibly across several flows) configured to use it. */
    public record Link(String host, int port, List<FlowElement> producers) {
        public String address() {
            return host + ":" + port;
        }
    }

    private record Address(String host, int port) {}

    public static String resolveSmtpHost(FlowElement flowElement) {
        String mailSmtpHost = flowElement.getPropertyValueAsString("mailSmtpHost");
        if (mailSmtpHost != null && !mailSmtpHost.isBlank()) {
            return mailSmtpHost;
        }
        // mailSmtpHost overrides mailhost when set (see EmailProducer's own help text) - fall back the same way.
        String mailhost = flowElement.getPropertyValueAsString("mailhost");
        return (mailhost != null && !mailhost.isBlank()) ? mailhost : DEFAULT_SMTP_HOST;
    }

    public static int resolveSmtpPort(FlowElement flowElement) {
        Integer configured = toInteger(flowElement.getPropertyValue("mailSmtpPort"));
        return configured != null ? configured : DEFAULT_SMTP_PORT;
    }

    private static Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Integer.valueOf(stringValue.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * @param module to scan - the canvas only ever paints one Module at a time.
     * @return every Email Producer across every flow, grouped by its resolved SMTP address - usually a single
     * Link (one shared local test server), but more than one if producers are configured for different
     * hosts/ports. Empty if the module has no Email Producer at all.
     */
    public static List<Link> findLinks(Module module) {
        List<Link> links = new ArrayList<>();
        if (module == null || module.getFlows() == null) {
            return links;
        }

        Map<Address, List<FlowElement>> byAddress = new LinkedHashMap<>();
        for (Flow flow : module.getFlows()) {
            if (flow == null) {
                continue;
            }
            List<FlowElement> elements = flow.ftlGetConsumerAndFlowElements();
            if (elements == null) {
                continue;
            }
            for (FlowElement element : elements) {
                if (element != null && element.getComponentMeta() != null && element.getComponentMeta().supportsTestMailServer()) {
                    Address address = new Address(resolveSmtpHost(element), resolveSmtpPort(element));
                    byAddress.computeIfAbsent(address, k -> new ArrayList<>()).add(element);
                }
            }
        }
        for (Map.Entry<Address, List<FlowElement>> entry : byAddress.entrySet()) {
            links.add(new Link(entry.getKey().host(), entry.getKey().port(), entry.getValue()));
        }
        return links;
    }
}
