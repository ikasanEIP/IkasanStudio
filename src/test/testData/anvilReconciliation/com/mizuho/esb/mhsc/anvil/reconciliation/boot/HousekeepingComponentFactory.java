package com.mizuho.esb.mhsc.anvil.reconciliation.boot;

import com.mizuho.esb.mhsc.anvil.reconciliation.housekeeping.producer.FileHouseKeepingProducer;
import com.mizuho.esb.mhsc.anvil.reconciliation.housekeeping.producer.configuration.FileHouseKeepingProducerConfiguration;
import com.mizuho.esb.mhsc.anvil.reconciliation.tradeextract.consumer.TimestampEventIdentifierService;
import org.ikasan.builder.BuilderFactory;
import org.ikasan.component.endpoint.quartz.consumer.MessageProvider;
import org.ikasan.component.endpoint.quartz.consumer.ScheduledConsumerConfiguration;
import org.ikasan.spec.component.endpoint.Consumer;
import org.ikasan.spec.component.endpoint.Producer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class HousekeepingComponentFactory {

    @Resource
    private ScheduledConsumerConfiguration fileHousekeepingScheduledConsumerConfiguration;

    @Resource
    private FileHouseKeepingProducerConfiguration fileHouseKeepingProducerConfiguration;

    @Value("${module.name}")
    private String moduleName;

    @Resource
    private BuilderFactory builderFactory;

    @Bean
    public Consumer<?,?> fileHouseKeepingScheduledConsumer()
    {
        return builderFactory.getComponentBuilder()
                .scheduledConsumer("fileHousekeeping")
                .setConfiguration(fileHousekeepingScheduledConsumerConfiguration)
                .setConfiguredResourceId(moduleName + "-fileHousekeepingScheduledConsumer")
                .setMessageProvider((MessageProvider<String>) jobExecutionContext -> "Executing anvil reconciliation file housekeeping!")
                .setManagedEventIdentifierService(new TimestampEventIdentifierService(moduleName
                        , "fileHouseKeepingFlow"))
                .build();
    }


    @Bean
    public Producer fileHousekeepingScheduledProducer() {
        FileHouseKeepingProducer fileHouseKeepingProducer = new FileHouseKeepingProducer();
        fileHouseKeepingProducer.setConfiguredResourceId(this.moduleName + "-fileHouseKeepingProducer");
        fileHouseKeepingProducer.setConfiguration(this.fileHouseKeepingProducerConfiguration);

        return fileHouseKeepingProducer;
    }
}
