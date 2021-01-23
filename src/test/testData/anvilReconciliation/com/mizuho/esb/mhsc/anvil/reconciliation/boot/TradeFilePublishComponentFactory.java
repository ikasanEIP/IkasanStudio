package com.mizuho.esb.mhsc.anvil.reconciliation.boot;

import com.mizuho.esb.mhi.component.factory.JmsComponentFactory;
import com.mizuho.esb.mhsc.anvil.reconciliation.tradeextract.converter.FileContentsToPayloadConverter;
import com.mizuho.esb.mhsc.anvil.reconciliation.tradeextract.converter.PayloadToFileContentsConverter;
import com.mizuho.esb.mhsc.anvil.reconciliation.tradeextract.converter.configuration.FileContentsToPayloadConverterConfiguration;
import org.ikasan.builder.BuilderFactory;
import org.ikasan.endpoint.sftp.producer.SftpProducerConfiguration;
import org.ikasan.filetransfer.Payload;
import org.ikasan.spec.component.endpoint.Consumer;
import org.ikasan.spec.component.endpoint.Producer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class TradeFilePublishComponentFactory
{
    public static final String MHSC_EAI = "mhsc.eai";

    @Value("${module.name}")
    private String moduleName;

    @Resource
    private BuilderFactory builderFactory;

    @Resource
    private JmsComponentFactory jmsComponentFactory;;

    @Value("${destination.eai.trade.file.retrieve}")
    private String tradeFileRetrieveDestination;

    @Bean
    public Consumer tradeFileConsumer() {
        return jmsComponentFactory.getJMSConsumer("tradeFilePublishConsumer"
                , tradeFileRetrieveDestination, true, MHSC_EAI);
    }

    @Resource
    public SftpProducerConfiguration publishAnvilTradeFileSftpConfiguration;

    @Resource
    public FileContentsToPayloadConverterConfiguration fileContentsToPayloadConverterConfiguration;

    @Bean
    public FileContentsToPayloadConverter fileContentsToPayloadConverter(){
        FileContentsToPayloadConverter fileContentsToPayloadConverter = new FileContentsToPayloadConverter();
        fileContentsToPayloadConverter.setConfiguredResourceId(moduleName + "-payloadToFileContentsConverter");
        fileContentsToPayloadConverter.setConfiguration(fileContentsToPayloadConverterConfiguration);

        return fileContentsToPayloadConverter;
    }

    @Bean
    public Producer<Payload> tradeFileProducer()
    {
        return builderFactory.getComponentBuilder()
                .sftpProducer()
                .setConfiguration(publishAnvilTradeFileSftpConfiguration)
                .setConfiguredResourceId(moduleName + "-sodPositionsSftpProducer")
                .build();
    }

}
