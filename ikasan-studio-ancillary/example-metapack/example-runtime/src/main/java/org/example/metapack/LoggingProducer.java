package org.example.metapack;

/** Uses the application's logging configuration, without owning threads or resources. */
public final class LoggingProducer implements org.ikasan.spec.component.endpoint.Producer<String> {
    private static final System.Logger LOG = System.getLogger(LoggingProducer.class.getName());
    @Override public void invoke(String payload) { LOG.log(System.Logger.Level.INFO, payload); }
}
