package org.ikasan.studio.flowtests;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

/** Test-only ActiveMQ configuration; replace deliberately for another JMS provider. */
@Configuration
public class ModuleJmsTestConfig {
    @Bean
    public org.apache.activemq.ActiveMQConnectionFactory studioTestJmsConnectionFactory(
            @Value("${r"${"}test.jms.broker-url}") String brokerUrl,
            @Value("${r"${"}test.jms.username:}") String username,
            @Value("${r"${"}test.jms.password:}") String password) {
        return new org.apache.activemq.ActiveMQConnectionFactory(username, password, brokerUrl);
    }
}
