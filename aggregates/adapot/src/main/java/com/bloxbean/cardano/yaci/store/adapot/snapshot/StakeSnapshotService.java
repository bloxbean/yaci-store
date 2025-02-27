package com.bloxbean.cardano.yaci.store.adapot.snapshot;

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

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void takeStakeSnapshot(int epoch) {
        log.info("Taking stake snapshot for epoch : " + epoch);

        jdbcTemplate.update("delete from epoch_stake where epoch = :epoch", new MapSqlParameterSource().addValue("epoch", epoch));

        // Drop temp tables in parallel
        List<String> dropQueries = List.of(
                "DROP TABLE IF EXISTS last_withdrawal",
                "DROP TABLE IF EXISTS MaxSlotBalances",
                "DROP TABLE IF EXISTS RankedDelegations",
                "DROP TABLE IF EXISTS pool_refund_rewards",
                "DROP TABLE IF EXISTS pool_rewards",
                "DROP TABLE IF EXISTS insta_spendable_rewards",
                "DROP TABLE IF EXISTS spendable_reward_rest",
                "DROP TABLE IF EXISTS PoolStatus"
        );


        for (String query : dropQueries) {
            jdbcTemplate.update(query, Map.of());
        }
        log.info("Dropped existing temp tables");

        String lastWithdrawalQuery = """
                    CREATE TABLE last_withdrawal  AS
                        SELECT address, MAX(slot) AS max_slot
                        FROM withdrawal
                        WHERE epoch <= :epoch
                        GROUP BY address;
                """;

        var epochParam = new MapSqlParameterSource();
        epochParam.addValue("epoch", epoch);
        epochParam.addValue("snapshot_epoch", epoch + 1);
        epochParam.addValue("activeEpoch", epoch + 2);

        log.info(">> Creating temp tables for stake snapshot");
        jdbcTemplate.update(lastWithdrawalQuery, epochParam);
        jdbcTemplate.update("CREATE INDEX idx_last_withdrawal_address ON last_withdrawal(address)", Map.of());
        jdbcTemplate.update("CREATE INDEX idx_last_withdrawal_address_max_slot on last_withdrawal(address, max_slot)", Map.of());
        log.info(">> last_withdrawal temp table created");


        String maxSlotBalancesQuery = """
                    CREATE TABLE MaxSlotBalances  AS
                         SELECT
                                         address,
                                         MAX(slot) AS max_slot
                                     FROM
                                         stake_address_balance
                                     WHERE
                                             epoch <= :epoch
                                     GROUP BY
                                         address
                """;

        String rankedDelegationQuery = """
                CREATE TABLE  RankedDelegations AS
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
                        ) AS rn
                FROM
                    delegation
                WHERE
                    epoch <= :epoch
                """;

        String poolRefundRewardsQuery = """
                        CREATE TABLE pool_refund_rewards AS
                        SELECT r.address, SUM(r.amount) AS pool_refund_withdrawable_reward
                        FROM reward r
                                 LEFT JOIN last_withdrawal lw ON r.address = lw.address
                        WHERE (lw.max_slot IS NULL OR r.slot > lw.max_slot)
                          AND r.spendable_epoch <= :epoch and r.type = 'refund'
                        GROUP BY r.address
                """;

        String poolRewardsQuery = """
                        CREATE TABLE pool_rewards AS
                                SELECT r.address, SUM(r.amount) AS withdrawable_reward
                                FROM reward r
                                         LEFT JOIN last_withdrawal lw ON r.address = lw.address
                                WHERE (lw.max_slot IS NULL OR r.slot > lw.max_slot)
                                  AND  r.earned_epoch <= :epoch
                                  AND  r.spendable_epoch <= :snapshot_epoch and r.type IN ('member', 'leader')
                                GROUP BY r.address
                """;

        String instaSpendableRewardsQuery = """
                        CREATE TABLE insta_spendable_rewards AS
                                                  SELECT r.address, SUM(r.amount) AS insta_withdrawable_reward
                                                  FROM instant_reward r
                                                           LEFT JOIN last_withdrawal lw ON r.address = lw.address
                                                  WHERE (lw.max_slot IS NULL OR r.slot > lw.max_slot)
                                                    AND r.spendable_epoch <= :snapshot_epoch
                                                  GROUP BY r.address
                """;

        String spendableRewardRestQuery = """
                        CREATE TABLE spendable_reward_rest AS
                                                                     SELECT r.address, SUM(r.amount) AS withdrawable_reward_rest
                                                                            FROM reward_rest r
                                                                                     LEFT JOIN last_withdrawal lw ON r.address = lw.address
                                                                            WHERE (lw.max_slot IS NULL OR r.slot > lw.max_slot)
                                                                              AND r.spendable_epoch <= :epoch
                                                                            GROUP BY r.address
                """;

        String poolStatusQuery = """
                        CREATE TABLE PoolStatus AS
                                 SELECT
                                     pool_id,
                                     status,
                                     registration_slot,
                                     slot,
                                     ROW_NUMBER() OVER (
                                         PARTITION BY pool_id
                                         ORDER BY slot DESC, tx_index DESC, cert_index DESC
                                         ) AS rn
                                 FROM
                                     pool
                                 WHERE
                                     epoch <= :epoch
                """;


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
                "CREATE INDEX idx_MaxSlotBalances_address ON MaxSlotBalances(address)",
                "CREATE INDEX idx_RankedDelegations_address ON RankedDelegations(address)",
                "CREATE INDEX idx_RankedDelegations_rn ON RankedDelegations(rn)",
                "CREATE INDEX idx_pool_refund_rewards_address ON pool_refund_rewards(address)",
                "CREATE INDEX idx_pool_rewards_address on pool_rewards (address)",
                "CREATE INDEX idx_insta_spendable_rewards_address ON insta_spendable_rewards(address)",
                "CREATE INDEX idx_spendable_reward_rest_address ON spendable_reward_rest(address)",
                "CREATE INDEX idx_PoolStatus_pool_id ON PoolStatus(pool_id)",
                "CREATE INDEX idx_PoolStatus_rn ON PoolStatus(rn)"

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
                        RankedDelegations d
                            LEFT JOIN
                        MaxSlotBalances msb ON d.address = msb.address
                            LEFT JOIN
                        stake_address_balance s ON msb.address = s.address AND msb.max_slot = s.slot
                            LEFT JOIN
                        pool_rewards r ON d.address = r.address           
                            LEFT JOIN
                        pool_refund_rewards pr ON d.address = pr.address
                            LEFT JOIN
                        insta_spendable_rewards ir ON d.address = ir.address     
                            LEFT JOIN
                        spendable_reward_rest rr ON d.address = rr.address
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
                                    select 1 from PoolStatus p
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
