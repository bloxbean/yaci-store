package com.bloxbean.cardano.yaci.store.adapot.snapshot;

import com.bloxbean.cardano.yaci.store.adapot.AdaPotProperties;
import com.bloxbean.cardano.yaci.store.adapot.storage.PartitionManager;
import com.bloxbean.cardano.yaci.store.dbutils.index.util.DatabaseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class StakeSnapshotService {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AdaPotProperties adaPotProperties;
    private final PartitionManager partitionManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void takeStakeSnapshot(int epoch) {
        log.info("Taking stake snapshot for epoch : " + epoch);

        // Ensure partition exists for this epoch before taking snapshot
        partitionManager.ensureEpochStakePartition(epoch);

        String tableType = "";
        //If postgres, set synchronous_commit to off for rest of the transaction
        var dbType = DatabaseUtils.getDbType(jdbcTemplate.getJdbcTemplate().getDataSource()).orElse(null);
        if (dbType == DatabaseUtils.DbType.postgres) {
            jdbcTemplate.update("SET LOCAL synchronous_commit = off", Map.of());
            tableType = "UNLOGGED";
            log.info("Postgres detected. Using UNLOGGED table for temp tables");

            // Increase work_mem for complex snapshot queries with joins and window functions
            // This setting only affects the current transaction and automatically resets after
            // Only set if configured (null/empty means use PostgreSQL defaults)
            try {
                String workMem = adaPotProperties.getStakeSnapshotWorkMem();
                if (workMem != null && !workMem.isBlank()) {
                    jdbcTemplate.update("SET LOCAL work_mem = '" + workMem + "'", Map.of());
                    log.info("Set work_mem to {} for stake snapshot operations", workMem);
                }
            } catch (Exception e) {
                log.warn("Failed to set work_mem: {}. Continuing with default settings.", e.getMessage());
            }
        }

        jdbcTemplate.update("delete from epoch_stake where epoch = :epoch", new MapSqlParameterSource().addValue("epoch", epoch));

        // Drop temp tables in parallel
        List<String> dropQueries = List.of(
                "DROP TABLE IF EXISTS ss_last_withdrawal",
                "DROP TABLE IF EXISTS ss_max_slot_balances",
                "DROP TABLE IF EXISTS ss_ranked_delegations",
                "DROP TABLE IF EXISTS ss_pool_refund_rewards",
                "DROP TABLE IF EXISTS ss_pool_rewards",
                "DROP TABLE IF EXISTS ss_insta_spendable_rewards",
                "DROP TABLE IF EXISTS ss_spendable_reward_rest",
                "DROP TABLE IF EXISTS ss_pool_status"
        );


        for (String query : dropQueries) {
            jdbcTemplate.update(query, Map.of());
        }
        log.info("Dropped existing temp tables");

        String lastWithdrawalQuery = String.format("""
                    CREATE %s TABLE ss_last_withdrawal  AS
                        SELECT address, MAX(slot) AS max_slot, :epoch as mark_epoch
                        FROM withdrawal
                        WHERE epoch <= :epoch
                        GROUP BY address;
                """, tableType);

        var epochParam = new MapSqlParameterSource();
        epochParam.addValue("epoch", epoch);
        epochParam.addValue("snapshot_epoch", epoch + 1);
        epochParam.addValue("activeEpoch", epoch + 2);

        log.info(">> Creating temp tables for stake snapshot");
        jdbcTemplate.update(lastWithdrawalQuery, epochParam);
        jdbcTemplate.update("CREATE INDEX idx_ss_last_withdrawal_address ON ss_last_withdrawal(address)", Map.of());
        jdbcTemplate.update("CREATE INDEX idx_ss_last_withdrawal_address_max_slot on ss_last_withdrawal(address, max_slot)", Map.of());
        log.info(">> ss_last_withdrawal temp table created");


        String maxSlotBalancesQuery = String.format("""
                    CREATE %s TABLE ss_max_slot_balances  AS
                         SELECT
                                         address,
                                         MAX(slot) AS max_slot,
                                         :epoch as mark_epoch
                                     FROM
                                         stake_address_balance
                                     WHERE
                                             epoch <= :epoch
                                     GROUP BY
                                         address
                """, tableType);

        String rankedDelegationQuery = String.format("""
                CREATE %s TABLE  ss_ranked_delegations AS
                SELECT
                    address,
                    pool_id,
                    epoch,
                    slot,
                    tx_index,
                    cert_index,
                    ROW_NUMBER() OVER (
                        PARTITION BY address
                        ORDER BY slot DESC, tx_index DESC, cert_index DESC
                        ) AS rn,
                     :epoch as mark_epoch    
                FROM
                    delegation
                WHERE
                    epoch <= :epoch
                """, tableType);

        String poolRefundRewardsQuery = String.format("""
                        CREATE %s TABLE ss_pool_refund_rewards AS
                        SELECT r.address, SUM(r.amount) AS pool_refund_withdrawable_reward, :epoch as mark_epoch
                        FROM reward r
                                 LEFT JOIN ss_last_withdrawal lw ON r.address = lw.address
                        WHERE (lw.max_slot IS NULL OR r.slot > lw.max_slot)
                          AND r.spendable_epoch <= :epoch and r.type = 'refund'
                        GROUP BY r.address
                """, tableType);

        String poolRewardsQuery = String.format("""
                        CREATE %s TABLE ss_pool_rewards AS
                                SELECT r.address, SUM(r.amount) AS withdrawable_reward, :epoch as mark_epoch
                                FROM reward r
                                         LEFT JOIN ss_last_withdrawal lw ON r.address = lw.address
                                WHERE (lw.max_slot IS NULL OR r.slot > lw.max_slot)
                                  AND  r.earned_epoch <= :epoch
                                  AND  r.spendable_epoch <= :snapshot_epoch and r.type IN ('member', 'leader')
                                GROUP BY r.address
                """, tableType);

        String instaSpendableRewardsQuery = String.format("""
                        CREATE %s TABLE ss_insta_spendable_rewards AS
                                                  SELECT r.address, SUM(r.amount) AS insta_withdrawable_reward, :epoch as mark_epoch
                                                  FROM instant_reward r
                                                           LEFT JOIN ss_last_withdrawal lw ON r.address = lw.address
                                                  WHERE (lw.max_slot IS NULL OR r.slot > lw.max_slot)
                                                    AND r.spendable_epoch <= :snapshot_epoch
                                                  GROUP BY r.address
                """, tableType);

        String spendableRewardRestQuery = String.format("""
                        CREATE %s TABLE ss_spendable_reward_rest AS
                                                                     SELECT r.address, SUM(r.amount) AS withdrawable_reward_rest, :epoch as mark_epoch
                                                                            FROM reward_rest r
                                                                                     LEFT JOIN ss_last_withdrawal lw ON r.address = lw.address
                                                                            WHERE (lw.max_slot IS NULL OR r.slot > lw.max_slot)
                                                                              AND r.spendable_epoch <= :epoch
                                                                            GROUP BY r.address
                """, tableType);

        String poolStatusQuery = String.format("""
                        CREATE %s TABLE ss_pool_status AS
                                 SELECT
                                     pool_id,
                                     status,
                                     registration_slot,
                                     slot,
                                     ROW_NUMBER() OVER (
                                         PARTITION BY pool_id
                                         ORDER BY slot DESC, tx_index DESC, cert_index DESC
                                         ) AS rn,
                                     :epoch as mark_epoch    
                                 FROM
                                     pool
                                 WHERE
                                     epoch <= :epoch
                """, tableType);


        List<String> createTableQueries = List.of(
                maxSlotBalancesQuery,
                rankedDelegationQuery,
                poolRefundRewardsQuery,
                poolRewardsQuery,
                instaSpendableRewardsQuery,
                spendableRewardRestQuery,
                poolStatusQuery
        );

        long start = System.currentTimeMillis();
        for (String query : createTableQueries) {
            log.info("Executing query : " + query);
            jdbcTemplate.update(query, epochParam);
            log.info(">> Temp table created for query : " + query);
        }
        long end = System.currentTimeMillis();
        log.info(">> Created all temp tables << " + (end - start) + " ms");

        // Create indexes in parallel (if required, otherwise skip this part)
        List<String> createIndexQueries = List.of(
                "CREATE INDEX idx_ss_max_slot_balances_address ON ss_max_slot_balances(address)",
                "CREATE INDEX idx_ss_ranked_delegations_address ON ss_ranked_delegations(address)",
                "CREATE INDEX idx_ss_ranked_delegations_rn ON ss_ranked_delegations(rn)",
                "CREATE INDEX idx_ss_pool_refund_rewards_address ON ss_pool_refund_rewards(address)",
                "CREATE INDEX idx_ss_pool_rewards_address on ss_pool_rewards (address)",
                "CREATE INDEX idx_ss_insta_spendable_rewards_address ON ss_insta_spendable_rewards(address)",
                "CREATE INDEX idx_ss_spendable_reward_rest_address ON ss_spendable_reward_rest(address)",
                "CREATE INDEX idx_ss_pool_status_pool_id ON ss_pool_status(pool_id)",
                "CREATE INDEX idx_ss_pool_status_rn ON ss_pool_status(rn)"
        );

        start = System.currentTimeMillis();
        for (String indexQuery : createIndexQueries) {
            jdbcTemplate.update(indexQuery, Map.of());
        }
        end = System.currentTimeMillis();
        log.info(">> Indexes created for temp tables <<" + (end - start) + " ms");

        var query = """
                    insert into epoch_stake             
                    SELECT
                        :epoch,
                        d.address,
                        (COALESCE(s.quantity, 0) + COALESCE(r.withdrawable_reward, 0) + COALESCE(pr.pool_refund_withdrawable_reward, 0) 
                             + COALESCE(ir.insta_withdrawable_reward, 0) + COALESCE(rr.withdrawable_reward_rest, 0)),
                        d.pool_id,
                        d.epoch,
                        :activeEpoch,
                        now()
                    FROM
                        ss_ranked_delegations d
                            LEFT JOIN
                        ss_max_slot_balances msb ON d.address = msb.address
                            LEFT JOIN
                        stake_address_balance s ON msb.address = s.address AND msb.max_slot = s.slot
                            LEFT JOIN
                        ss_pool_rewards r ON d.address = r.address           
                            LEFT JOIN
                        ss_pool_refund_rewards pr ON d.address = pr.address
                            LEFT JOIN
                        ss_insta_spendable_rewards ir ON d.address = ir.address     
                            LEFT JOIN
                        ss_spendable_reward_rest rr ON d.address = rr.address
                            LEFT JOIN 
                        stake_registration sd
                                          ON sd.address = d.address
                                              AND sd.type = 'STAKE_DEREGISTRATION'
                                              AND sd.epoch <= :epoch
                                              AND (
                                                 sd.slot > d.slot OR
                                                 (sd.slot = d.slot AND sd.tx_index > d.tx_index) OR
                                                 (sd.slot = d.slot AND sd.tx_index = d.tx_index AND sd.cert_index > d.cert_index)
                                                 )
                    WHERE
                            d.rn = 1
                    and not exists(
                                    select 1 from ss_pool_status p
                                    where d.pool_id = p.pool_id and p.rn = 1 and (p.status = 'RETIRED' or d.slot < p.registration_slot)
                            )
                            AND sd.address IS NULL;
                """;

        var params = new MapSqlParameterSource();
        params.addValue("epoch", epoch);
        params.addValue("snapshot_epoch", epoch + 1);
        params.addValue("activeEpoch", epoch + 2);

        jdbcTemplate.update(query, params);

        log.info("Stake snapshot for epoch : {} is taken", epoch);
        log.info(">>>>>>>>>>>>>>>>>>>> Stake Snapshot taken for epoch : {} <<<<<<<<<<<<<<<<<<<<", epoch);
    }
}
