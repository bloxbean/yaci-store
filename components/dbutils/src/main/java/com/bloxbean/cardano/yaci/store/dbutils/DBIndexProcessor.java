package com.bloxbean.cardano.yaci.store.dbutils;

import com.bloxbean.cardano.yaci.store.dbutils.index.model.IndexDefinition;
import com.bloxbean.cardano.yaci.store.dbutils.index.util.IndexLoader;
import com.bloxbean.cardano.yaci.store.dbutils.index.service.IndexService;
import com.bloxbean.cardano.yaci.store.events.BlockHeaderEvent;
import com.bloxbean.cardano.yaci.store.events.ByronMainBlockEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class DBIndexProcessor {
    public static final String STORE_AUTO_INDEX_MANAGEMENT = "store.auto-index-management";
    public static final int INDEX_APPLY_BLOCK_NO_THRESHOLD = 50000;
    private final Environment environment;
    private final DataSource dataSource;
    private final IndexService indexService;

    private AtomicBoolean indexApplied = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        log.info("<< Enable DBIndexService >> AutoIndexManagement: " + isAutoIndexManagement());
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @SneakyThrows
    public void handleFirstBlockEvent(ByronMainBlockEvent byronMainBlockEvent) {
        if (!isAutoIndexManagement())
            return;

        var eventMetadata = byronMainBlockEvent.getMetadata();
        handleIndexApplyIfRequired(eventMetadata);
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFirstBlockEvent(BlockHeaderEvent blockHeaderEvent) {
        if (!isAutoIndexManagement())
            return;

        var eventMetadata = blockHeaderEvent.getMetadata();
        handleIndexApplyIfRequired(eventMetadata);
    }

    private void handleIndexApplyIfRequired(EventMetadata eventMetadata) {
        if (indexApplied.get() || !eventMetadata.isSyncMode())
            return;

        if (eventMetadata.getBlock() > INDEX_APPLY_BLOCK_NO_THRESHOLD) {
            log.info("<< I can't manage the creation of automatic indexes because the number of actual blocks in database exceeds the # of blocks threshold for automatic index application >>");
            log.info("Please manually reapply the required indexes if not done yet. For more details, refer to the 'create-index.sql' file !!!");
            indexApplied.set(true);
            return;
        }

        try {
            log.info("Let's try to apply optional indexes ...");
            applyIndexes("index.yml");

            log.info("Applying extra indexes ...");
            applyIndexes("extra-index.yml");
        } catch (Exception e) {
            log.error("Failed to apply indexes.", e);
        }

        indexApplied.set(true);
    }

    private boolean applyIndexes(String indexFile) {
        IndexLoader indexLoader = new IndexLoader();
        List<IndexDefinition> indexDefinitionList = indexLoader.loadIndexes(indexFile);
        if (indexDefinitionList == null || indexDefinitionList.isEmpty()) {
            log.warn("No optional index found to apply");
            return true;
        }

        var result = indexService.applyIndexes(indexDefinitionList);

        if (!result.getFirst().isEmpty()) {
            log.info("Total number of indexes successfully applied : {}", result.getFirst().size());
        }

        if (!result.getSecond().isEmpty()) {
            log.warn("Total number of indexes that failed to apply : {}", result.getSecond().size());
        }
        return false;
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

    private boolean isAutoIndexManagement() {
        var prop = environment.getProperty(STORE_AUTO_INDEX_MANAGEMENT);
        if (prop == null)
            return Boolean.TRUE;
        else if ("true".equalsIgnoreCase(prop.trim()))
            return Boolean.TRUE;
        else
            return Boolean.FALSE;
    }
}
