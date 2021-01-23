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
public class TradeFileRequestFlowFactory
{
    @Resource
    private BuilderFactory builderFactory;

    @Value("${module.name}")
    private String moduleName;

    @Resource
    private MatchingExceptionResolver anvilExceptionResolver;

    @Resource
    public Consumer tradeFileRequestConsumer;

    @Resource
    public Producer tradeFileRequestJmsProducer;

    @Resource
    public Broker tradeFileRequestBroker;

    @Resource
    private ApplicationContext applicationContext;

    @Bean
    public Flow tradeFileRequestFlow()
    {
        ModuleBuilder moduleBuilder = builderFactory.getModuleBuilder(moduleName);
        return moduleBuilder.getFlowBuilder("Trade File Request Flow")
                .withExceptionResolver(anvilExceptionResolver)
                .withMonitor(applicationContext.getBean("monitor", Monitor.class))
                .withDescription("Request the anvil trade file.")
                .consumer("Trade File Request Scheduled Consumer", tradeFileRequestConsumer)
                .broker("Trade File Request Broker", tradeFileRequestBroker)
                .producer("Trade File Request Producer", tradeFileRequestJmsProducer)
                .build();
    }
}
