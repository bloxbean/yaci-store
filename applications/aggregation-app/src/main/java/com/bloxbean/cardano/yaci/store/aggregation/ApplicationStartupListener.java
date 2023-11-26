package com.bloxbean.cardano.yaci.store.aggregation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationStartupListener implements
        ApplicationListener<ContextRefreshedEvent> {

    private final DataSource dataSource;

    @Value("${store.aggr.clean-db-before-start:false}")
    private boolean cleanDBBeforeStart;


    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!cleanDBBeforeStart)
            return;

        log.info("Deleting records from aggregation tables before sync >>>");
        cleanAggregationTables();
    }

    private void cleanAggregationTables() {
        try {
            String scriptPath = "sql/clean-db.sql";

            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScripts(
                    new ClassPathResource(scriptPath));
            populator.execute(this.dataSource);

            log.info("Aggregation records have been deleted successfully !!!");
        } catch (Exception e) {
            log.error("Aggregation records cleanup failed.", e);
        }
    }
}
