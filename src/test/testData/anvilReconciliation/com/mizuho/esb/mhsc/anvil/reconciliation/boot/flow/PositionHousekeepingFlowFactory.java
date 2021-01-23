package com.mizuho.esb.mhsc.anvil.reconciliation.boot.flow;

import com.mizuho.esb.mhsc.anvil.reconciliation.positions.producer.PositionHousekeepingDbProducer;
import org.ikasan.builder.BuilderFactory;
import org.ikasan.builder.ModuleBuilder;
import org.ikasan.exceptionResolver.MatchingExceptionResolver;
import org.ikasan.spec.component.endpoint.Consumer;
import org.ikasan.spec.flow.Flow;
import org.ikasan.spec.monitor.Monitor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class PositionHousekeepingFlowFactory {
    @Resource
    private BuilderFactory builderFactory;

    @Value("${module.name}")
    private String moduleName;

    @Resource
    private MatchingExceptionResolver anvilExceptionResolver;

    @Resource
    public Consumer houseKeepingScheduledConsumer;

    @Resource
    private PositionHousekeepingDbProducer positionHousekeepingDbProducer;

    @Resource
    private ApplicationContext applicationContext;

    @Bean
    public Flow anvilPositionHousekeepingFlow()
    {
        ModuleBuilder moduleBuilder = builderFactory.getModuleBuilder(moduleName);
        return moduleBuilder.getFlowBuilder("Anvil Position Housekeeping Flow")
                .withExceptionResolver(anvilExceptionResolver)
                .withMonitor(applicationContext.getBean("monitor", Monitor.class))
                .withDescription("Remove expired positions from the database.")
                .consumer("Position Housekeeping Scheduled Consumer", houseKeepingScheduledConsumer)
                .producer("Position Housekeeping Database Producer", positionHousekeepingDbProducer)
                .build();
    }
}
