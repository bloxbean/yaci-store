package com.bloxbean.cardano.yaci.store.app;

import com.bloxbean.cardano.yaci.store.events.BlockHeaderEvent;
import com.bloxbean.cardano.yaci.store.events.ByronMainBlockEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class DBIndexService {
    private final DataSource dataSource;

    private AtomicBoolean indexRemoved = new AtomicBoolean(false);
    private AtomicBoolean indexApplied = new AtomicBoolean(false);

    @Value("${store.auto-index-management:true}")
    private boolean autoIndexManagement;

    @PostConstruct
    public void init() {
        log.info("<< Enable DBIndexService >> AutoIndexManagement: " + autoIndexManagement);
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFirstBlockEvent(BlockHeaderEvent blockHeaderEvent) {
        if (!autoIndexManagement)
            return;

        if (indexRemoved.get() || blockHeaderEvent.getMetadata().getBlock() > 1
                || blockHeaderEvent.getMetadata().isSyncMode())
            return;

        runDeleteIndexes();
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFirstBlockEventToCreateIndex(BlockHeaderEvent blockHeaderEvent) {
        if (!autoIndexManagement)
            return;

        if (blockHeaderEvent.getMetadata().isSyncMode() && !indexApplied.get()) {
            if (blockHeaderEvent.getMetadata().getBlock() < 50000) {
                 reApplyIndexes();
            } else {
                log.info("<< I can't manage the creation of automatic indexes because the number of actual blocks in database exceeds the # of blocks threshold for automatic index application >>");
                log.info("Please manually reapply the required indexes if not done yet. For more details, refer to the 'create-index.sql' file !!!");
                indexApplied.set(true);
            }
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @SneakyThrows
    public void handleFirstBlockEvent(ByronMainBlockEvent byronMainBlockEvent) {
        if (!autoIndexManagement)
            return;

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
            executeSqlScript(scriptPath);

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

            executeSqlScript(scriptPath);

            //If h2 db, then try any additional indexes in h2 file
            if (isH2()) {
                log.info("Running additional indexes for H2 ...");
                String h2Script = "sql/extra-index-h2.sql";
                executeSqlScript(h2Script);
            } else if(isPostgres()) {
                log.info("Running additional indexes for Postgresql ...");
                String postgresScript = "sql/extra-index-postgresql.sql";
                executeSqlScript(postgresScript);
            }

            log.info("Optional indexes have been re-applied successfully.");
        } catch (Exception e) {
            log.error("Filed to re-apply indexes.", e);
        }
    }

    private void executeSqlScript(String scriptPath) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource(scriptPath));
        populator.execute(this.dataSource);
    }

    private boolean isMysql() throws SQLException {
        var vendor = dataSource.getConnection().getMetaData().getDatabaseProductName();
        if (vendor != null && vendor.toLowerCase().contains("mysql"))
            return true;
        else
            return false;
    }

    private boolean isH2() throws SQLException {
        var vendor = dataSource.getConnection().getMetaData().getDatabaseProductName();
        if (vendor != null && vendor.toLowerCase().contains("h2"))
            return true;
        else
            return false;
    }

    private boolean isPostgres() throws SQLException {
        var vendor = dataSource.getConnection().getMetaData().getDatabaseProductName();
        if (vendor != null && vendor.toLowerCase().contains("postgres"))
            return true;
        else
            return false;
    }
}
