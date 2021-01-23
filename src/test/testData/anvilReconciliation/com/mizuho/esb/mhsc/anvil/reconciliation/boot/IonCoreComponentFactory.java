package com.mizuho.esb.mhsc.anvil.reconciliation.boot;

import com.mizuho.bdm.jaxb.adapter.DateAdapter;
import com.mizuho.bdm.jaxb.adapter.DateTimeAdapter;
import com.mizuho.esb.mhi.api.ion.marketview.MarketViewSubscriberService;
import com.mizuho.esb.mhi.api.ion.marketview.consumer.MarketViewListenerFactoryConsumerImpl;
import com.mizuho.esb.mhi.connector.ion.endpoint.consumer.IonMarketViewChainConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.TimeZone;

@Component
public class IonCoreComponentFactory
{
    @Bean
    public MarketViewSubscriberService marketViewSubscriberService( IonMarketViewChainConsumer anvilPositionChainConsumer)
    {
        MarketViewSubscriberService marketViewSubscriberService =
                new MarketViewSubscriberService(new MarketViewListenerFactoryConsumerImpl(anvilPositionChainConsumer));
        marketViewSubscriberService.setDebug(true);

        anvilPositionChainConsumer.setMarketViewSubscriberService(marketViewSubscriberService);

        return marketViewSubscriberService;
    }

    @Bean
    public DateTimeAdapter jstDateTimeAdapter()
    {
        return new DateTimeAdapter(TimeZone.getTimeZone("JST"));
    }

    @Bean
    public DateAdapter jstDateAdapter()
    {
        return new DateAdapter(TimeZone.getTimeZone("JST"));
    }
}
