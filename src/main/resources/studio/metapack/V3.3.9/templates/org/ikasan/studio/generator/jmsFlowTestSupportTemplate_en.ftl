package org.ikasan.studio.flowtests;

import javax.jms.*;
import org.springframework.context.ConfigurableApplicationContext;

/** Queue/text-message helper. Use isolated test destinations, not production queues or topics. */
public final class JmsFlowTestSupport implements AutoCloseable {
    private final Connection connection;
    private final Session session;

    public static JmsFlowTestSupport from(ConfigurableApplicationContext context) throws JMSException {
        return new JmsFlowTestSupport(context.getBean("studioTestJmsConnectionFactory", ConnectionFactory.class));
    }

    public JmsFlowTestSupport(ConnectionFactory factory) throws JMSException {
        connection = factory.createConnection();
        try {
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            connection.start();
        } catch (JMSException | RuntimeException failure) {
            try { connection.close(); } catch (JMSException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    public void sendText(String queue, String text) throws JMSException {
        MessageProducer producer = session.createProducer(session.createQueue(queue));
        try { producer.send(session.createTextMessage(text)); }
        finally { producer.close(); }
    }

    /** Checks actual receiver-side delivery; call for each batch, not just producer invocation. */
    public void assertText(String queue, String expected) throws JMSException {
        MessageConsumer consumer = session.createConsumer(session.createQueue(queue));
        try {
            Message message = consumer.receive(10000);
            org.junit.Assert.assertTrue("Expected a text message from " + queue, message instanceof TextMessage);
            org.junit.Assert.assertEquals(expected, ((TextMessage) message).getText());
        } finally { consumer.close(); }
    }

    /** Bounded observation for a deliberately filtered/rejected input on an isolated queue. */
    public void assertNoMessage(String queue) throws JMSException {
        MessageConsumer consumer = session.createConsumer(session.createQueue(queue));
        try { org.junit.Assert.assertNull("Unexpected delivery to " + queue, consumer.receive(1000)); }
        finally { consumer.close(); }
    }

    // Closing a connection closes its sessions/producers/consumers. Never purge shared queues.
    @Override public void close() throws JMSException { connection.close(); }
}
