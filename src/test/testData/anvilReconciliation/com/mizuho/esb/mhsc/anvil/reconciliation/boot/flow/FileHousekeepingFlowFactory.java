package com.mizuho.esb.mhsc.anvil.reconciliation.boot.flow;

import org.ikasan.builder.BuilderFactory;
import org.ikasan.builder.ModuleBuilder;
import org.ikasan.exceptionResolver.MatchingExceptionResolver;
import org.ikasan.spec.component.endpoint.Broker;
import org.ikasan.spec.component.endpoint.Consumer;
import org.ikasan.spec.component.endpoint.Producer;
import org.ikasan.spec.flow.Flow;
import org.ikasan.spec.monitor.Monitor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class FileHousekeepingFlowFactory
{
    @Resource
    private BuilderFactory builderFactory;

    @Value("${module.name}")
    private String moduleName;

    @Resource
    private MatchingExceptionResolver anvilExceptionResolver;

    @Resource
    public Consumer fileHouseKeepingScheduledConsumer;

    @Resource
    public Producer fileHousekeepingScheduledProducer;

    @Resource
    private ApplicationContext applicationContext;

    @Bean
    public Flow fileHousekeepingFlow()
    {
        ModuleBuilder moduleBuilder = builderFactory.getModuleBuilder(moduleName);
        return moduleBuilder.getFlowBuilder("File Housekeeping Flow")
                .withExceptionResolver(anvilExceptionResolver)
                .withMonitor(applicationContext.getBean("monitor", Monitor.class))
                .withDescription("House keep all anvil reconciliation files.")
                .consumer("File Housekeeping Scheduled Consumer", this.fileHouseKeepingScheduledConsumer)
                .producer("File Housekeeping Producer", this.fileHousekeepingScheduledProducer)
                .build();
    }
}
