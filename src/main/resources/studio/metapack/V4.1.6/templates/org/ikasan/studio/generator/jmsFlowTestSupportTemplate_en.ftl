package org.ikasan.studio.flowtests.support;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Queue/text-message helper. Use isolated test destinations, not production queues or topics. */
public final class JmsFlowTestSupport implements AutoCloseable {
    private final Connection connection;
    private final Session session;
    private final long deliveryTimeoutMillis;

    /**
     * Opens a connection using {@code studioTestJmsConnectionFactory} and the shared delivery timeout.
     * The caller must close this helper, preferably with try-with-resources.
     * @param context running test application containing the factory bean
     * @return a started JMS connection/session helper
     * @throws JMSException if connection/session creation fails
     */
    public static JmsFlowTestSupport from(ConfigurableApplicationContext context) throws JMSException {
        return new JmsFlowTestSupport(context.getBean("studioTestJmsConnectionFactory", ConnectionFactory.class),
                ModuleFlowTestSupport.deliveryTimeoutSeconds(context.getEnvironment()
                        .getProperty("test.delivery.timeout-seconds", "10")));
    }

    /** Opens a helper with a ten-second delivery timeout. The caller owns {@link #close()}. */
    public JmsFlowTestSupport(ConnectionFactory factory) throws JMSException {
        this(factory, 10);
    }

    /**
     * Opens and starts a connection and non-transacted session for queue-based text fixtures.
     * @param factory test broker connection factory
     * @param timeoutSeconds positive timeout for expected deliveries
     * @throws JMSException if the connection/session cannot be opened
     */
    public JmsFlowTestSupport(ConnectionFactory factory, int timeoutSeconds) throws JMSException {
        deliveryTimeoutMillis = 1000L * ModuleFlowTestSupport.deliveryTimeoutSeconds(String.valueOf(timeoutSeconds));
        connection = factory.createConnection();
        try {
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            connection.start();
        } catch (JMSException | RuntimeException failure) {
            try { connection.close(); } catch (JMSException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    /** Sends one text message to a queue; use test-owned destinations. Does not wait for flow completion. */
    public void sendText(String queue, String text) throws JMSException {
        MessageProducer producer = session.createProducer(session.createQueue(queue));
        try { producer.send(session.createTextMessage(text)); }
        finally { producer.close(); }
    }

    /**
     * Consumes one queue message within the configured timeout and compares its text exactly.
     * Receiving acknowledges the message; use a dedicated test queue with no competing consumers.
     * @throws AssertionError if no message arrives, its type is not TextMessage, or the body differs
     */
    public void assertText(String queue, String expected) throws JMSException {
        MessageConsumer consumer = session.createConsumer(session.createQueue(queue));
        try {
            Message message = consumer.receive(deliveryTimeoutMillis);
            assertTrue("Expected a text message from " + queue, message instanceof TextMessage);
            assertEquals(expected, ((TextMessage) message).getText());
        } finally { consumer.close(); }
    }

    /** Observes the queue for one second and fails if any message arrives; receipt consumes that message. */
    public void assertNoMessage(String queue) throws JMSException {
        MessageConsumer consumer = session.createConsumer(session.createQueue(queue));
        try { assertNull("Unexpected delivery to " + queue, consumer.receive(1000)); }
        finally { consumer.close(); }
    }

    /** Closes the owned connection and its sessions/producers/consumers. Never purges a shared queue. */
    @Override public void close() throws JMSException { connection.close(); }
}
