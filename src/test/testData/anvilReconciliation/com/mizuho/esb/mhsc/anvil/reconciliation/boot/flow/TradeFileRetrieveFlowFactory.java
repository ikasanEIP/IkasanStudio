package com.mizuho.esb.mhsc.anvil.reconciliation.boot.flow;

import com.mizuho.esb.mhsc.anvil.reconciliation.tradeextract.converter.PayloadToFileContentsConverter;
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
public class TradeFileRetrieveFlowFactory
{
    @Resource
    private BuilderFactory builderFactory;

    @Value("${module.name}")
    private String moduleName;

    @Resource
    private MatchingExceptionResolver anvilExceptionResolver;

    @Resource
    public Consumer tradeFileRetrieveConsumer;

    @Resource
    public Producer tradeFileRetrieveJmsProducer;

    @Resource
    public Broker tradeFileRetrieveBroker;

    @Resource
    private ApplicationContext applicationContext;

    @Bean
    public Flow tradeFileRetrieveFlow()
    {
        ModuleBuilder moduleBuilder = builderFactory.getModuleBuilder(moduleName);
        return moduleBuilder.getFlowBuilder("Trade File Retrieve Flow")
                .withExceptionResolver(anvilExceptionResolver)
                .withMonitor(applicationContext.getBean("monitor", Monitor.class))
                .withDescription("Retrieve the anvil trade file.")
                .consumer("Trade File Retrieve Consumer", tradeFileRetrieveConsumer)
                .broker("Trade File Retrieve Broker", tradeFileRetrieveBroker)
                .converter("Trade File Sftp Payload to File Contents Converter", new PayloadToFileContentsConverter())
                .producer("Trade File Contents Producer", tradeFileRetrieveJmsProducer)
                .build();
    }
}
