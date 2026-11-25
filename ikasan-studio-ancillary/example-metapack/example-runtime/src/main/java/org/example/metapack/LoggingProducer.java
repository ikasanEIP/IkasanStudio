package org.example.metapack;

import org.ikasan.spec.component.endpoint.Producer;

/** Uses the application's logging configuration, without owning threads or resources. */
public final class LoggingProducer implements Producer<String> {
    private static final System.Logger LOG = System.getLogger(LoggingProducer.class.getName());
    @Override public void invoke(String payload) { LOG.log(System.Logger.Level.INFO, payload); }
}
