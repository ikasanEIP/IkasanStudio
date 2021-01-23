package com.mizuho.esb.mhsc.anvil.reconciliation.boot;

import liquibase.integration.spring.SpringLiquibase;
import org.ikasan.builder.BuilderFactory;
import org.ikasan.spec.flow.Flow;
import org.ikasan.spec.module.Module;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * Module configuration.
 */
@Configuration
@ImportResource( {
        "classpath:exception-conf.xml",
        "classpath:h2-datasource-conf.xml",
        "classpath:transaction-conf.xml",
        "classpath:filetransfer-service-conf.xml",
        "classpath:ikasan-transaction-pointcut-jms.xml",
        "classpath:ikasan-transaction-pointcut-quartz.xml",
        "classpath:ion-transaction-pointcut-consumer.xml",
        "classpath:position-db-conf.xml",
        "classpath:position-tx-manager.xml"
} )
public class ModuleConfig
{
    @Value("${module.name}")
    private String moduleName;

    @Resource
    private BuilderFactory builderFactory;

    @Resource
    private Flow tradeFileRequestFlow;
    @Resource
    private Flow tradeFileRetrieveFlow;
    @Resource
    private Flow tradeFilePublishFlow;
    @Resource
    private Flow anvilPositionFlow;
    @Resource
    private Flow anvilPositionHousekeepingFlow;
    @Resource
    private Flow fileHousekeepingFlow;

    @Bean
    public org.ikasan.spec.module.Module<?> getModule()
    {
        return builderFactory.getModuleBuilder(moduleName)
                .withDescription("Anvil Reconciliation Module")
                .addFlow(anvilPositionFlow)
                .addFlow(tradeFileRequestFlow)
                .addFlow(tradeFileRetrieveFlow)
                .addFlow(tradeFilePublishFlow)
                .addFlow(anvilPositionHousekeepingFlow)
                .addFlow(fileHousekeepingFlow)
                .build();
    }

    @Bean
    public SpringLiquibase eaiLiquibase(@Qualifier("positionDataSource")DataSource positionDataSource) {
        SpringLiquibase springLiquibase = new SpringLiquibase();
        springLiquibase.setChangeLog("classpath:liquibase/db-eai-changelog.xml");
        springLiquibase.setDataSource(positionDataSource);

        return springLiquibase;
    }

    @Bean
    public SpringLiquibase liquibase(DataSource ikasanXADatasource) {
        SpringLiquibase springLiquibase = new SpringLiquibase();
        springLiquibase.setChangeLog("classpath:db-changelog-master.xml");
        springLiquibase.setDataSource(ikasanXADatasource);

        return springLiquibase;
    }
}
