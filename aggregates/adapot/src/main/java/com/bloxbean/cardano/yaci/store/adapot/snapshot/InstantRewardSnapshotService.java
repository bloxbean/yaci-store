package com.bloxbean.cardano.yaci.store.adapot.snapshot;

import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class InstantRewardSnapshotService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

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
                                   AND sd.slot > m.slot)
                group by address, pot, m.epoch
                """;

        var params = new MapSqlParameterSource();
        params.addValue("epoch", epoch);
        params.addValue("spendable_epoch", epoch + 1);
        params.addValue("current_slot", metadata.getSlot());

        jdbcTemplate.update(deleteQuery, params);
        jdbcTemplate.update(query, params);

        log.info("Stake snapshot for epoch : {} is taken", epoch);

    }
}
