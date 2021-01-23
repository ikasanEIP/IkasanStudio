package com.mizuho.esb.mhsc.anvil.reconciliation.boot;

import com.iontrading.mkv.exceptions.MkvException;
import com.iontrading.mkv.helper.MkvSupplyUtils;
import com.mizuho.esb.mhi.api.ion.marketview.MarketViewSubscriberService;
import com.mizuho.esb.mhi.api.ion.marketview.publisher.function.FunctionPublisher;
import com.mizuho.esb.mhi.api.ion.marketview.publisher.function.FunctionPublisherImpl;
import com.mizuho.esb.mhi.api.ion.marketview.publisher.function.FunctionResultHandler;
import com.mizuho.esb.mhi.component.factory.JmsComponentFactory;
import com.mizuho.esb.mhi.connector.ion.endpoint.broker.IonFunctionBroker;
import com.mizuho.esb.mhsc.anvil.reconciliation.tradeextract.broker.configuration.FileRequestBrokerConfiguration;
import com.mizuho.esb.mhsc.anvil.reconciliation.tradeextract.consumer.RequestFileMessageProvider;
import com.mizuho.esb.mhsc.anvil.reconciliation.tradeextract.consumer.TimestampEventIdentifierService;
import com.mizuho.esb.mhsc.anvil.reconciliation.tradeextract.consumer.configuration.RequestFileScheduledConsumerConfiguration;
import com.mizuho.esb.mhsc.anvil.reconciliation.tradeextract.util.ValueRecordSupplier;
import org.apache.commons.lang3.StringUtils;
import org.ikasan.builder.BuilderFactory;
import org.ikasan.component.endpoint.quartz.consumer.ScheduledConsumerConfiguration;
import org.ikasan.spec.component.endpoint.Consumer;
import org.ikasan.spec.component.endpoint.Producer;
import org.ikasan.spec.component.transformation.TransformationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Component
public class TradeFileRequestComponentFactory {
    public static final String MHSC_EAI = "mhsc.eai";

    @Resource
    private RequestFileScheduledConsumerConfiguration requestFileRetrieveScheduledConsumerConfiguration;

    @Resource
    private FileRequestBrokerConfiguration fileRequestBrokerConfiguration;

    @Value("${module.name}")
    private String moduleName;

    @Resource
    private BuilderFactory builderFactory;

    @Resource
    private JmsComponentFactory jmsComponentFactory;;

    @Value("${destination.eai.trade.file.request}")
    private String tradeFileRequestDestination;

    @Resource
    public MarketViewSubscriberService marketViewSubscriberService;

    @Bean
    public Consumer<?,?> tradeFileRequestConsumer()
    {
        return builderFactory.getComponentBuilder()
                .scheduledConsumer("tradeFileRequest")
                .setConfiguration(requestFileRetrieveScheduledConsumerConfiguration)
                .setConfiguredResourceId(moduleName + "-tradeFileRequestConsumer")
                .setMessageProvider(new RequestFileMessageProvider())
                .setManagedEventIdentifierService(new TimestampEventIdentifierService(moduleName
                        , "tradeFileRequestFlow"))
                .build();
    }

    @Bean
    public IonFunctionBroker tradeFileRequestBroker()
    {
        FunctionResultHandler handler = (functionResult, map) -> {
            try
            {
                String supplyResult = functionResult.getMkvSupply().getString(0);
                if (StringUtils.startsWith(supplyResult, "0:OK"))
                {
                    map.put(functionResult.getMkvSupply().getString(1), functionResult.getMkvSupply().getString(2));
                }
                else
                {
                    functionResult.setSuccessful(false);
                }
            }
            catch (MkvException e)
            {
                throw new TransformationException("Could not make " + this.fileRequestBrokerConfiguration.getFileRequestFunction()
                        + " function call: " + MkvSupplyUtils.toString(functionResult.getMkvSupply()));
            }
        };
        FunctionPublisher<Map<String, Object>> publisher = new FunctionPublisherImpl(fileRequestBrokerConfiguration.getFileRequestFunction(), 5L, false
                , handler, new ValueRecordSupplier(fileRequestBrokerConfiguration.getSubscriptionFields()));
        IonFunctionBroker broker = new IonFunctionBroker(marketViewSubscriberService, publisher);
        broker.setConfiguration(fileRequestBrokerConfiguration);
        broker.setConfiguredResourceId(moduleName + "-tradeFileRequestBroker");
        return broker;
    }


    @Bean
    public Producer tradeFileRequestJmsProducer() {
        return jmsComponentFactory.getJMSProducer("manualControlMessageProducer", tradeFileRequestDestination, MHSC_EAI);
    }
}
