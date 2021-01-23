package com.mizuho.esb.mhsc.anvil.reconciliation.boot.flow;

import com.mizuho.esb.mhsc.anvil.reconciliation.tradeextract.converter.FileContentsToPayloadConverter;
import com.mizuho.esb.mhsc.anvil.reconciliation.tradeextract.converter.PayloadToFileContentsConverter;
import org.ikasan.builder.BuilderFactory;
import org.ikasan.builder.ModuleBuilder;
import org.ikasan.exceptionResolver.MatchingExceptionResolver;
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
public class TradeFilePublishFlowFactory
{
    @Resource
    private BuilderFactory builderFactory;

    @Value("${module.name}")
    private String moduleName;

    @Resource
    private MatchingExceptionResolver anvilExceptionResolver;

    @Resource
    public Consumer tradeFileConsumer;

    @Resource
    public Producer tradeFileProducer;

    @Resource FileContentsToPayloadConverter fileContentsToPayloadConverter;

    @Resource
    private ApplicationContext applicationContext;

    @Bean
    public Flow tradeFilePublishFlow()
    {
        ModuleBuilder moduleBuilder = builderFactory.getModuleBuilder(moduleName);
        return moduleBuilder.getFlowBuilder("Trade File Publish Flow")
                .withExceptionResolver(anvilExceptionResolver)
                .withMonitor(applicationContext.getBean("monitor", Monitor.class))
                .withDescription("Publish the anvil trade file.")
                .consumer("Trade File Consumer", tradeFileConsumer)
                .converter("Trade File Contents to Payload Converter",fileContentsToPayloadConverter)
                .producer("Trade File Producer", tradeFileProducer)
                .build();
    }
}
