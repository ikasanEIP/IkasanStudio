package com.mizuho.esb.mhsc.anvil.reconciliation.boot.flow;

import com.mizuho.esb.mhi.connector.ion.endpoint.consumer.IonMarketViewChainConsumer;
import com.mizuho.esb.mhsc.anvil.reconciliation.positions.converter.AnvilChainToPositionPojoConverter;
import com.mizuho.esb.mhsc.anvil.reconciliation.positions.producer.PositionDbProducer;
import org.ikasan.builder.BuilderFactory;
import org.ikasan.builder.ModuleBuilder;
import org.ikasan.exceptionResolver.MatchingExceptionResolver;
import org.ikasan.spec.flow.Flow;
import org.ikasan.spec.monitor.Monitor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class PositionChainFlowFactory {
    @Resource
    private BuilderFactory builderFactory;

    @Value("${module.name}")
    private String moduleName;

    @Resource
    private MatchingExceptionResolver anvilExceptionResolver;

    @Resource
    public IonMarketViewChainConsumer anvilPositionChainConsumer;

    @Resource
    private PositionDbProducer positionDbProducer;

    @Resource
    private ApplicationContext applicationContext;

    @Bean
    public Flow anvilPositionFlow()
    {
        ModuleBuilder moduleBuilder = builderFactory.getModuleBuilder(moduleName);
        return moduleBuilder.getFlowBuilder("Anvil Position Flow")
                .withExceptionResolver(anvilExceptionResolver)
                .withMonitor(applicationContext.getBean("monitor", Monitor.class))
                .withDescription("Listen to position events on Anvil chains and publish to a database.")
                .consumer("Position Chain Consumer", anvilPositionChainConsumer)
                .converter("Position Chain Data to Position Converter", new AnvilChainToPositionPojoConverter())
                .producer("Position Database Producer", positionDbProducer)
                .build();
    }
}
