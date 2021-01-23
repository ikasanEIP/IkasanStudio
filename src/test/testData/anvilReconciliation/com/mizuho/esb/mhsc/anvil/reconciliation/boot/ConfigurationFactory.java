package com.mizuho.esb.mhsc.anvil.reconciliation.boot;

import com.mizuho.esb.mhi.connector.ion.endpoint.consumer.IonMarketViewConfiguration;
import com.mizuho.esb.mhsc.anvil.reconciliation.housekeeping.producer.configuration.FileHouseKeepingProducerConfiguration;
import com.mizuho.esb.mhsc.anvil.reconciliation.positions.consumer.configuration.PositionHousekeepingScheduledConsumerConfiguration;
import com.mizuho.esb.mhsc.anvil.reconciliation.tradeextract.broker.configuration.FileRequestBrokerConfiguration;
import com.mizuho.esb.mhsc.anvil.reconciliation.tradeextract.consumer.configuration.RequestFileScheduledConsumerConfiguration;
import com.mizuho.esb.mhsc.anvil.reconciliation.tradeextract.converter.configuration.FileContentsToPayloadConverterConfiguration;
import org.ikasan.component.endpoint.quartz.consumer.ScheduledConsumerConfiguration;
import org.ikasan.endpoint.sftp.consumer.SftpConsumerConfiguration;
import org.ikasan.endpoint.sftp.producer.SftpProducerConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
public class ConfigurationFactory {

    @Bean
    @ConfigurationProperties(prefix = "sftp.publish.anvil.trade.file")
    public SftpProducerConfiguration publishAnvilTradeFileSftpConfiguration()
    {
        return new SftpProducerConfiguration();
    }

    @Bean
    @ConfigurationProperties(prefix = "ion.trade.file.request.schedule.comsumer")
    public RequestFileScheduledConsumerConfiguration requestFileRetrieveScheduledConsumerConfiguration() {
        return new RequestFileScheduledConsumerConfiguration();
    }

    @Bean
    @ConfigurationProperties(prefix = "ion.position.house.keep.schedule.comsumer")
    public PositionHousekeepingScheduledConsumerConfiguration positionHousekeepingScheduledConsumerConfiguration() {
        return new PositionHousekeepingScheduledConsumerConfiguration();
    }

    @Bean
    @ConfigurationProperties(prefix = "ion.trade.file.request")
    public FileRequestBrokerConfiguration fileRequestBrokerConfiguration() {
        return new FileRequestBrokerConfiguration();
    }

    @Bean
    @ConfigurationProperties(prefix = "sftp.anvil.trade.file")
    public SftpConsumerConfiguration fileRetrieveBrokerConfiguration() {
        return new SftpConsumerConfiguration();
    }

    @Bean
    @ConfigurationProperties(prefix = "sftp.anvil.trade.file.name")
    public FileContentsToPayloadConverterConfiguration fileContentsToPayloadConverterConfiguration() {
        return new FileContentsToPayloadConverterConfiguration();
    }

    @Bean
    @ConfigurationProperties(prefix = "ion.chain.position")
    public IonMarketViewConfiguration anvilInstrumentChainConsumerConfiguration()
    {
        return new IonMarketViewConfiguration();
    }

    @Bean
    @ConfigurationProperties(prefix = "file.house.keep.consumer")
    public ScheduledConsumerConfiguration fileHousekeepingScheduledConsumerConfiguration() {
        return new ScheduledConsumerConfiguration();
    }

    @Bean
    @ConfigurationProperties(prefix = "file.house.keep.producer")
    public FileHouseKeepingProducerConfiguration fileHouseKeepingProducerConfiguration() {
        return new FileHouseKeepingProducerConfiguration();
    }
}
