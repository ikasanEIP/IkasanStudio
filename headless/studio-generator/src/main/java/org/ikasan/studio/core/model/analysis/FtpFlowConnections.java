package org.ikasan.studio.core.model.analysis;

import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import org.ikasan.studio.core.model.ikasan.instance.Module;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Possible file hand-offs, inferred from configured endpoints; never executable flow routes. */
public final class FtpFlowConnections {
    private FtpFlowConnections() { }

    public record Endpoint(String protocol, String host, int port, String directory) {
        public String address() { return protocol + "://" + host + ":" + port + " " + directory; }
    }

    public record Link(FlowElement producer, FlowElement consumer, Endpoint endpoint) {
        public boolean isVisible(Object selection, boolean showAll) {
            return showAll || selection == producer || selection == consumer;
        }
    }

    public static List<Link> findMatchingLinks(Module module) {
        List<Link> links = new ArrayList<>();
        if (module == null || module.getFlows() == null) return links;
        Map<Endpoint, List<FlowElement>> producers = new LinkedHashMap<>();
        Map<Endpoint, List<FlowElement>> consumers = new LinkedHashMap<>();
        for (var flow : module.getFlows()) {
            if (flow == null || !flow.getPropertyValueAsString("testHarnessOwner").isBlank()) continue;
            for (FlowElement element : flow.ftlGetConsumerAndFlowElements()) {
                Endpoint endpoint = endpointOf(element);
                if (endpoint == null) continue;
                var target = element.getComponentMeta().isProducer() ? producers : consumers;
                target.computeIfAbsent(endpoint, ignored -> new ArrayList<>()).add(element);
            }
        }
        producers.forEach((endpoint, elements) -> {
            for (var producer : elements) {
                for (var consumer : consumers.getOrDefault(endpoint, List.of())) {
                    links.add(new Link(producer, consumer, endpoint));
                }
            }
        });
        return links;
    }

    private static Endpoint endpointOf(FlowElement element) {
        if (element == null || element.getComponentMeta() == null) return null;
        var meta = element.getComponentMeta();
        if (!"FTP Endpoint".equals(meta.getEndpointKey()) || (!meta.isProducer() && !meta.isConsumer())) return null;
        // A custom directory factory can ignore the configured source directory entirely.
        if (meta.isConsumer() && !element.getPropertyValueAsString("sourceDirectoryURLFactory").isBlank()) return null;
        String host = value(element, "remoteHost");
        String directory = value(element, meta.isProducer() ? "outputDirectory" : "sourceDirectory");
        String ftps = value(element, "ftps");
        if (host == null || directory == null || !("true".equals(ftps) || "false".equals(ftps))) return null;
        boolean secure = Boolean.parseBoolean(ftps);
        String implicit = secure ? value(element, "ftpsIsImplicit") : "false";
        if (!("true".equals(implicit) || "false".equals(implicit))) return null;
        String port = value(element, secure ? "ftpsPort" : "remotePort");
        try {
            int number = Integer.parseInt(port);
            if (number < 1 || number > 65535) return null;
            // Do not collapse '..', '.', repeated slashes or relative paths: server semantics may differ.
            while (directory.length() > 1 && directory.endsWith("/")) directory = directory.substring(0, directory.length() - 1);
            return new Endpoint(secure ? (Boolean.parseBoolean(implicit) ? "ftps-implicit" : "ftps") : "ftp",
                    host.toLowerCase(Locale.ROOT), number, directory);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String value(FlowElement element, String key) {
        Object value = element.getPropertyValue(key);
        if (value == null && element.getComponentMeta().getMetadata(key) != null) {
            value = element.getComponentMeta().getMetadata(key).getDefaultValue();
        }
        if (value == null) return null;
        String text = value.toString().trim();
        return text.isEmpty() || text.contains("${") || text.contains("#{") || text.contains("__") ? null : text;
    }
}
