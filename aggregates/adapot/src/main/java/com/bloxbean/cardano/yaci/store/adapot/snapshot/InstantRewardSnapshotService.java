package com.bloxbean.cardano.yaci.store.adapot.snapshot;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.InstantRewardRepository;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class InstantRewardSnapshotService {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final InstantRewardRepository instantRewardRepository;

    @Transactional
    public void takeInstantRewardSnapshot(EventMetadata metadata, int epoch) {
        String deleteQuery = """
                delete from instant_reward
                where earned_epoch = :epoch
                """;

        String query = """
                insert into instant_reward
                select address as address,
                       pot as type,
                       sum(amount) as amount,
                       m.epoch as earned_epoch,
                       :spendable_epoch as spendable_epoch,
                       :current_slot as slot,               
                       now() as create_datetime
                from mir m
                where m.epoch = :epoch
                  and not exists(SELECT 1
                                 FROM stake_registration sd
                                 WHERE sd.address = m.address
                                   AND sd.type = 'STAKE_DEREGISTRATION'
                                   AND sd.epoch <= :epoch
                                   AND not exists(SELECT 1
                                                        FROM stake_registration sd2
                                                        WHERE sd2.address = sd.address
                                                        AND sd2.type = 'STAKE_REGISTRATION'
                                                        AND sd2.epoch <= :epoch
                                                        AND sd2.slot > sd.slot))
                group by address, pot, m.epoch
                """;

        //Only take the latest mir record for each address (Pre Alonzo)
        String preAlonzoQuery = """
                WITH LatestMIR AS (SELECT tx_hash,
                                          cert_index,
                                          pot,
                                          credential,
                                          address,
                                          amount,
                                          epoch,
                                          slot,
                                          block_hash,
                                          block,
                                          block_time,
                                          ROW_NUMBER() OVER (PARTITION BY address ORDER BY slot DESC) AS rn
                                   FROM mainnet.mir
                                   WHERE epoch = :epoch)
                insert into instant_reward
                select address          as address,
                       pot              as type,
                       amount           as amount,
                       m.epoch          as earned_epoch,
                       :spendable_epoch as spendable_epoch,
                       :current_slot    as slot,
                       now()            as create_datetime
                from LatestMIR m
                where m.rn = 1
                  and not exists(SELECT 1
                                 FROM stake_registration sd
                                 WHERE sd.address = m.address
                                   AND sd.type = 'STAKE_DEREGISTRATION'
                                   AND sd.epoch <= :epoch
                                   AND not exists(SELECT 1
                                                  FROM stake_registration sd2
                                                  WHERE sd2.address = sd.address
                                                    AND sd2.type = 'STAKE_REGISTRATION'
                                                    AND sd2.epoch <= :epoch
                                                    AND sd2.slot > sd.slot))
                """;

        if (metadata.getEra().getValue() < Era.Alonzo.getValue())
            query = preAlonzoQuery;

        var params = new MapSqlParameterSource();
        params.addValue("epoch", epoch);
        params.addValue("spendable_epoch", epoch + 1);
        params.addValue("current_slot", metadata.getSlot());

        jdbcTemplate.update(deleteQuery, params);
        jdbcTemplate.update(query, params);

        log.info("Stake snapshot for epoch : {} is taken", epoch);

    }

    @EventListener
    @Transactional
    public void handleRollback(RollbackEvent rollbackEvent) {
        int count = instantRewardRepository.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} instant reward records", count);
    }
}
