package com.mizuho.esb.mhsc.anvil.reconciliation.boot;

import com.mizuho.esb.mhi.component.factory.JmsComponentFactory;
import com.mizuho.esb.mhsc.anvil.reconciliation.tradeextract.broker.FileRetrieveBroker;
import org.ikasan.builder.BuilderFactory;
import org.ikasan.connector.base.command.TransactionalResourceCommandDAO;
import org.ikasan.connector.basefiletransfer.outbound.persistence.BaseFileTransferDao;
import org.ikasan.connector.util.chunking.model.dao.FileChunkDao;
import org.ikasan.endpoint.sftp.consumer.SftpConsumerConfiguration;
import org.ikasan.endpoint.sftp.consumer.SftpMessageProvider;
import org.ikasan.spec.component.endpoint.Broker;
import org.ikasan.spec.component.endpoint.Consumer;
import org.ikasan.spec.component.endpoint.Producer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.annotation.Resource;

@Component
public class TradeFileRetrieveComponentFactory
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

    @Value("${destination.eai.trade.file.request}")
    private String tradeFileRequestDestination;

    @Resource
    public SftpConsumerConfiguration fileRetrieveBrokerConfiguration;

    @Resource
    private JtaTransactionManager transactionManager;

    @Resource
    private BaseFileTransferDao baseFileTransferDao;

    @Resource
    private FileChunkDao fileChunkDao;

    @Resource
    private TransactionalResourceCommandDAO transactionalResourceCommandDAO;

    @Bean
    public SftpMessageProvider sftpMessageProvider() {
        return new SftpMessageProvider(transactionManager, baseFileTransferDao, fileChunkDao,
                transactionalResourceCommandDAO);
    }

    @Bean
    public Consumer tradeFileRetrieveConsumer() {
        return jmsComponentFactory.getJMSConsumer("tradeFileRetrieveConsumer"
                , tradeFileRequestDestination, true, MHSC_EAI);
    }

    @Bean
    public Producer tradeFileRetrieveJmsProducer() {
        return jmsComponentFactory.getJMSProducer("tradeFileRetrieveJmsProducer"
                , tradeFileRetrieveDestination, MHSC_EAI);
    }

    @Bean
    public Broker tradeFileRetrieveBroker(SftpMessageProvider sftpMessageProvider)
    {
        FileRetrieveBroker broker = new FileRetrieveBroker(sftpMessageProvider);
        broker.setConfiguredResourceId(moduleName + "-tradeFileRetrieveBroker");
        broker.setConfiguration(this.fileRetrieveBrokerConfiguration);
        return broker;
    }

}
