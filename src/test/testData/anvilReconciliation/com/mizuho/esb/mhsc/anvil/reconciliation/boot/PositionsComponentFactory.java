package com.mizuho.esb.mhsc.anvil.reconciliation.boot;

import com.mizuho.esb.mhi.api.ion.marketview.IonMarketViewObserver;
import com.mizuho.esb.mhi.api.ion.marketview.MarketViewSubscriberService;
import com.mizuho.esb.mhi.api.ion.marketview.consumer.MarketViewListenerFactoryConsumerImpl;
import com.mizuho.esb.mhi.connector.ion.endpoint.consumer.IonMarketViewChainConsumer;
import com.mizuho.esb.mhi.connector.ion.endpoint.consumer.IonMarketViewConfiguration;
import com.mizuho.esb.mhsc.anvil.reconciliation.positions.consumer.HouseKeepingDateMessageProvider;
import com.mizuho.esb.mhsc.anvil.reconciliation.positions.consumer.configuration.PositionHousekeepingScheduledConsumerConfiguration;
import com.mizuho.esb.mhsc.anvil.reconciliation.positions.dao.PositionDao;
import com.mizuho.esb.mhsc.anvil.reconciliation.positions.producer.PositionDbProducer;
import com.mizuho.esb.mhsc.anvil.reconciliation.positions.producer.PositionHousekeepingDbProducer;
import com.mizuho.esb.mhsc.anvil.reconciliation.positions.service.PositionService;
import com.mizuho.esb.mhsc.anvil.reconciliation.tradeextract.consumer.RequestFileMessageProvider;
import com.mizuho.esb.mhsc.anvil.reconciliation.tradeextract.consumer.TimestampEventIdentifierService;
import liquibase.integration.spring.SpringLiquibase;
import org.ikasan.builder.BuilderFactory;
import org.ikasan.spec.component.endpoint.Consumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Component
public class PositionsComponentFactory
{
    public static final String MHSC_EAI = "mhsc.eai";

    @Value("${module.name}")
    private String moduleName;

    @Value("${ion.chain.position.componentName}")
    private String componentName;


    @Resource
    private BuilderFactory builderFactory;

    @Resource
    private PositionDao xaPersonDao;

    @Resource
    private IonMarketViewConfiguration anvilPositionChainConsumerConfiguration;

    @Resource
    private PositionHousekeepingScheduledConsumerConfiguration positionHousekeepingScheduledConsumerConfiguration;

    @Bean
    private PositionService positionService() {
        return new PositionService(xaPersonDao);
    }

    @Bean
    public Consumer<?,?> houseKeepingScheduledConsumer()
    {
        return builderFactory.getComponentBuilder()
                .scheduledConsumer("positionHousekeeping")
                .setConfiguration(positionHousekeepingScheduledConsumerConfiguration)
                .setConfiguredResourceId(moduleName + "-positionHousekeepingConsumer")
                .setMessageProvider(new HouseKeepingDateMessageProvider())
                .setManagedEventIdentifierService(new TimestampEventIdentifierService(moduleName
                        , "positionHousekeepingFlow"))
                .build();
    }

    @Bean
    private PositionHousekeepingDbProducer positionHousekeepingDbProducer(PositionService positionService) {
        return new PositionHousekeepingDbProducer(positionService);
    }

    @Bean
    private PositionDbProducer positionDbProducer(PositionService positionService) {
        return new PositionDbProducer(positionService);
    }

    @Bean
    public IonMarketViewChainConsumer anvilPositionChainConsumer()
    {
        IonMarketViewChainConsumer consumer = new IonMarketViewChainConsumer();

        Map<String, String> chains = new HashMap<>();
        chains.put("bond", "ALL.ANVIL_POSITION.ANVIL_POSITION.Recon_Firm");
        chains.put("collateral", "ALL.ANVIL_POSITION.ANVIL_POSITION.Recon_Collateral");
        chains.put("repo", "ALL.ANVIL_POSITION.ANVIL_POSITION.Recon_Repo");
        consumer.setIonChainsMap(chains);
        consumer.setConfiguration(anvilPositionChainConsumerConfiguration);
        consumer.setConfiguredResourceId(moduleName + "-anvilPositionChainConsumer");
        return consumer;
    }

}
