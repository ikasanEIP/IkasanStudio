package org.ikasan.studio.core.generator;

import org.ikasan.studio.core.model.ikasan.instance.FlowElement;
import java.util.*;

/** Delivery-specific interpretation stays outside the general scaffold generator. */
final class FlowTestDelivery {
    enum Strategy { NONE, FILE, FTP, SFTP, SMTP }
    private final Strategy strategy;
    private final int recipientCount;

    private FlowTestDelivery(Strategy strategy, int recipientCount) {
        this.strategy = strategy;
        this.recipientCount = recipientCount;
    }

    static FlowTestDelivery forProducers(List<FlowElement> producers) {
        if (producers.size() != 1) return new FlowTestDelivery(Strategy.NONE, 0);
        var producer = producers.get(0);
        var meta = producer.getComponentMeta();
        String declared = meta.getFlowTestDeliveryStrategy();
        Strategy strategy;
        if (declared != null && !declared.isBlank()) {
            strategy = Strategy.valueOf(declared.toUpperCase(Locale.ROOT));
        } else {
            // Compatibility for previously published packs; bundled packs declare their strategy.
            strategy = meta.isFlowTestFileDelivery()
                    ? (meta.supportsTestSftpServer() ? Strategy.SFTP
                    : meta.supportsTestFtpServer() ? Strategy.FTP : Strategy.FILE)
                    : meta.supportsTestMailServer() ? Strategy.SMTP : Strategy.NONE;
        }
        return new FlowTestDelivery(strategy, strategy == Strategy.SMTP ? smtpRecipients(producer) : 0);
    }

    boolean usesResources() { return strategy != Strategy.NONE; }

    void contributeTo(Map<String, Object> values) {
        values.put("fileDelivery", strategy == Strategy.FILE || strategy == Strategy.FTP || strategy == Strategy.SFTP);
        values.put("smtpDelivery", strategy == Strategy.SMTP);
        values.put("sftpFileDelivery", strategy == Strategy.SFTP);
        values.put("ftpFileDelivery", strategy == Strategy.FTP);
        values.put("smtpRecipientCount", recipientCount);
    }

    private static int smtpRecipients(FlowElement producer) {
        var groups = producer.getComponentMeta().getFlowTestRecipientProperties();
        if (groups == null || groups.isEmpty()) return 0;
        Set<String> addresses = new HashSet<>();
        for (var group : groups) {
            int configured = 0;
            for (String property : group) {
                Object value = producer.getPropertyValue(property);
                if (value == null || value instanceof String && ((String) value).isBlank()
                        || value instanceof List && ((List<?>) value).isEmpty()) continue;
                // Multiple alternative setters have order-dependent behaviour: require review.
                if (++configured > 1) return 0;
                var entries = value instanceof List ? (List<?>) value : List.of(value);
                for (Object entry : entries) {
                    // Infer only simple literal addresses, not expressions, groups or display names.
                    if (!(entry instanceof String)) return 0;
                    String address = ((String) entry).trim();
                    if (!address.matches("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+")) return 0;
                    if (!addresses.add(address.toLowerCase(Locale.ROOT))) return 0;
                }
            }
        }
        return addresses.size();
    }
}
