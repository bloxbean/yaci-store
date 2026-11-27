package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.common.config.StoreModuleConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.OptionalInt;

/**
 * Reads the {@code epoch} rows that the epoch-aggr module computes, so that the epoch exporter can
 * tell whether the row of an epoch is final.
 *
 * <p>The epoch-aggr {@code EpochProcessor} writes the row of epoch N while N is the current epoch
 * and recomputes it once, at the first commit in epoch N+1. It runs only when both
 * {@code store.epoch-aggr.enabled} and {@code store.epoch-aggr.epoch-calculation-enabled} are
 * true (both are false in the shipped configuration).</p>
 */
@Component
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class EpochAggregateReader {

    static final String EPOCH_AGGR_ENABLED = "store.epoch-aggr.enabled";
    static final String EPOCH_CALCULATION_ENABLED = "store.epoch-aggr.epoch-calculation-enabled";

    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    public EpochAggregateReader(@Qualifier("dataSource") DataSource dataSource, Environment environment) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.environment = environment;
    }

    /**
     * Whether the epoch-aggr module computes epoch rows in this application.
     */
    public boolean isEpochCalculationEnabled() {
        boolean moduleEnabled = environment.getProperty(EPOCH_AGGR_ENABLED, Boolean.class,
                StoreModuleConfig.defaultEnabled("epoch-aggr"));
        boolean calculationEnabled = environment.getProperty(EPOCH_CALCULATION_ENABLED, Boolean.class, false);
        return moduleEnabled && calculationEnabled;
    }

    /**
     * Block count recorded in the {@code epoch} row of the given epoch, or empty when there is no row.
     */
    public OptionalInt findBlockCount(int epoch) {
        List<Integer> counts = jdbcTemplate.queryForList(
                "SELECT block_count FROM epoch WHERE number = ?", Integer.class, (long) epoch);
        return counts.isEmpty() || counts.get(0) == null ? OptionalInt.empty() : OptionalInt.of(counts.get(0));
    }
}
