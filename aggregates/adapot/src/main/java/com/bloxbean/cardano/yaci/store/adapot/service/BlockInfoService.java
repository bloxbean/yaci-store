package com.bloxbean.cardano.yaci.store.adapot.service;

import com.bloxbean.cardano.yaci.store.adapot.domain.EpochPoolBlocks;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BlockInfoService {
    private final JdbcTemplate jdbcTemplate; //TODO: Use JOOQ ?

    public int getNonOBFTBlocksInEpoch(int epoch) {
        String query = """
            select count(number) from block b
            where b.epoch = ? and b.slot_leader in (select pool_id from pool_registration)
        """;

        return jdbcTemplate.queryForObject(query, Integer.class, epoch);
    }

    public List<EpochPoolBlocks> getPoolBlockCount(int epoch) {
        String query = """
            select b.slot_leader as pool_id, count(number) as blocks
                 from block b
                 where epoch = ?
                    and b.slot_leader in (select pool_id from pool_registration)
                    group by b.slot_leader
        """;

        return jdbcTemplate.query(query, (rs, rowNum) -> EpochPoolBlocks.builder()
                .poolId(rs.getString("pool_id"))
                .blocks(rs.getInt("blocks"))
                .epoch(epoch)
                .build(), epoch);
    }
}
