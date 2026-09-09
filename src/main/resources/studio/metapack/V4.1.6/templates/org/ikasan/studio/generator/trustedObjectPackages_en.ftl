/** Adds the explicitly selected object packages before ActiveMQ creates any connections. */
public static class Trusted${flowElement.getJavaClassName()}ContextFactory
        extends org.apache.activemq.jndi.ActiveMQInitialContextFactory {
    public static String initialFactoryName(String configuredFactory) {
        if (!"org.apache.activemq.jndi.ActiveMQInitialContextFactory".equals(configuredFactory)) {
            throw new IllegalArgumentException("Trusted object packages requires the standard ActiveMQ initial context factory; configure trust in your custom provider instead.");
        }
        return Trusted${flowElement.getJavaClassName()}ContextFactory.class.getName();
    }

    public static jakarta.jms.ConnectionFactory configure(jakarta.jms.ConnectionFactory connectionFactory) {
        if (!(connectionFactory instanceof org.apache.activemq.ActiveMQConnectionFactory)) {
            throw new IllegalArgumentException("Trusted object packages requires an ActiveMQ connection factory; configure trust on the underlying factory if yours is wrapped.");
        }
        org.apache.activemq.ActiveMQConnectionFactory activeMq =
                (org.apache.activemq.ActiveMQConnectionFactory) connectionFactory;
        java.util.Set<String> packages = new java.util.LinkedHashSet<>(activeMq.getTrustedPackages());
        for (String packageName : "${trustedPackages?j_string}".split(",")) {
            packages.add(packageName.trim());
        }
        activeMq.setTrustedPackages(new java.util.ArrayList<>(packages));
        return activeMq;
    }

    @Override
    protected org.apache.activemq.ActiveMQConnectionFactory createConnectionFactory(java.util.Hashtable environment)
            throws java.net.URISyntaxException {
        // Let ActiveMQ preserve JNDI, broker URL, credentials and XA connection-factory selection.
        org.apache.activemq.ActiveMQConnectionFactory factory = super.createConnectionFactory(environment);
        configure(factory);
        return factory;
    }
}
