package com.bloxbean.cardano.yaci.store.adapot.snapshot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class StakeSnapshotService {
    private final DSLContext dsl;

    @Transactional
    public void takeStakeSnapshot(int epoch) {
        log.info("Taking stake snapshot for epoch : " + epoch);
        var query = dsl.query("""
                    WITH RankedDelegations AS (
                        SELECT
                            address,
                            pool_id,
                            epoch,
                            slot,
                            cert_index,
                            ROW_NUMBER() OVER (
                                PARTITION BY address
                                ORDER BY slot DESC, cert_index DESC
                                ) AS rn
                        FROM
                            delegation
                        WHERE
                                epoch <= ?
                    ),
                         MaxSlotBalances AS (
                             SELECT
                                 address,
                                 MAX(slot) AS max_slot
                             FROM
                                 stake_address_balance
                             WHERE
                                     epoch <= ?
                             GROUP BY
                                 address
                         ),
                        RewardBalanceAccount AS (
                            SELECT
                                address,
                                amount,
                                epoch,
                                slot,
                                ROW_NUMBER() OVER (
                                    PARTITION BY address
                                    ORDER BY slot DESC
                                    ) AS rn
                            FROM
                                reward_account
                            WHERE
                                    epoch <= ?
                        ),
                        PoolStatus AS (
                                 SELECT
                                     pool_id,
                                     status,
                                     slot,
                                     ROW_NUMBER() OVER (
                                         PARTITION BY pool_id
                                         ORDER BY slot DESC, cert_index DESC
                                         ) AS rn
                                 FROM
                                     pool
                                 WHERE
                                         epoch <= ?
                        )
                        
                    insert into epoch_stake                        
                    SELECT
                        ?,
                        d.address,
                        (COALESCE(s.quantity, 0) + COALESCE(r.amount, 0)),
                        d.pool_id,
                        d.epoch,
                        ?,
                        now()
                    FROM
                        RankedDelegations d
                            LEFT JOIN
                        MaxSlotBalances msb ON d.address = msb.address
                            LEFT JOIN
                        stake_address_balance s ON msb.address = s.address AND msb.max_slot = s.slot
                            LEFT JOIN
                        RewardBalanceAccount r ON d.address = r.address and r.rn = 1
                    WHERE
                            d.rn = 1      
                    and not exists(
                        select 1 from PoolStatus p
                        where d.pool_id = p.pool_id and p.rn = 1 and p.status = 'RETIRED'
                    )
                    and  NOT EXISTS (
                         SELECT 1
                            FROM stake_registration sd
                            WHERE sd.address = d.address
                                AND sd.type = 'STAKE_DEREGISTRATION'
                                AND sd.epoch <= d.epoch
                                AND sd.slot > d.slot
                    )               
                """, epoch, epoch, epoch, epoch, epoch, epoch + 2);

        //TODO -- Exclude de-registered stake address
        //TODO -- Exclude stake address delegated to retired pool

        log.info(query.getSQL());
        query.execute();
        log.info("Stake snapshot for epoch : {} is taken", epoch);

        //print log with ascii art of the epoch
        log.info(">>>>>>>>>>>>>>>>>>>> Stake Snapshot taken for epoch : {} <<<<<<<<<<<<<<<<<<<<", epoch);
    }
}
