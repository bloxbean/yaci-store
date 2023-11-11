package com.bloxbean.cardano.yaci.store.app;

import com.bloxbean.cardano.yaci.store.events.BlockHeaderEvent;
import com.bloxbean.cardano.yaci.store.events.ByronMainBlockEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "store.auto-index-management", havingValue = "true", matchIfMissing = false)
public class DBIndexService {
    private final DataSource dataSource;

    private AtomicBoolean indexRemoved = new AtomicBoolean(false);
    private AtomicBoolean indexApplied = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        log.info("<< Enable DBIndexService >>");
    }

    @EventListener
    @Async
    @Transactional
    public void handleFirstBlockEvent(BlockHeaderEvent blockHeaderEvent) {
        if (indexRemoved.get() || blockHeaderEvent.getMetadata().getBlock() > 1
                || blockHeaderEvent.getMetadata().isSyncMode())
            return;

        runDeleteIndexes();
    }

    @EventListener
    @Transactional
    public void handleFirstBlockEventToCreateIndex(BlockHeaderEvent blockHeaderEvent) {
        if (blockHeaderEvent.getMetadata().isSyncMode() && !indexApplied.get()) {
           reApplyIndexes();
        }
    }

    @EventListener
    @Transactional
    @Async
    @SneakyThrows
    public void handleFirstBlockEvent(ByronMainBlockEvent byronMainBlockEvent) {
        if (indexRemoved.get() || byronMainBlockEvent.getMetadata().getBlock() > 1
                || byronMainBlockEvent.getMetadata().isSyncMode())
            return;

        runDeleteIndexes();
    }

    private void runDeleteIndexes() {
        try {
            boolean isMysql = isMysql();
            String scriptPath;
            if (isMysql)
                scriptPath = "sql/mysql/drop-index.sql";
            else
                scriptPath = "sql/drop-index.sql";

            log.info("Deleting optional indexes to speed-up the sync process ..... " + scriptPath);
            indexRemoved.set(true);
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScripts(
                    new ClassPathResource(scriptPath));
            populator.execute(this.dataSource);

            log.info("Optional indexes have been removed successfully.");
        } catch (Exception e) {
            log.error("Index deletion failed.", e);
        }
    }

    private void reApplyIndexes() {
        try {
            boolean isMysql = isMysql();
            String scriptPath;
            if (isMysql)
                scriptPath = "sql/mysql/create-index.sql";
            else
                scriptPath = "sql/create-index.sql";

            log.info("Re-applying optional indexes after sync process ..... " + scriptPath);
            indexApplied.set(true);

            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScripts(
                    new ClassPathResource(scriptPath));
            populator.execute(this.dataSource);

            log.info("Optional indexes have been re-applied successfully.");
        } catch (Exception e) {
            log.error("Filed to re-apply indexes.", e);
        }
    }

    private boolean isMysql() throws SQLException {
        var vendor = dataSource.getConnection().getMetaData().getDatabaseProductName();
        if (vendor != null && vendor.toLowerCase().contains("mysql"))
            return true;
        else
            return false;
    }
}