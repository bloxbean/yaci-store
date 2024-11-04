package com.bloxbean.cardano.yaci.store.adapot.snapshot;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DepositSnapshotService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public BigInteger getNetStakeDepositInEpoch(int epoch) {
        String stakeRegDeRegAndPoolDepositQuery = """
                            SELECT
                -- Net deposit from stake registrations and deregistrations in the specified epoch
                COALESCE((
                             SELECT
                                 SUM(CASE
                                         WHEN type = 'STAKE_REGISTRATION' THEN :stake_reg_deposit
                                         WHEN type = 'STAKE_DEREGISTRATION' THEN -(:stake_reg_deposit)
                                         ELSE 0
                                     END) AS net_stake
                             FROM
                                 stake_registration
                             WHERE
                                 epoch = :epoch
                         ), 0) AS stake_deposits,
                
                -- Net deposit from pool registrations in the specified epoch
                COALESCE((
                             SELECT
                                 SUM(amount) AS net_pool_deposit
                             FROM
                                 pool
                             WHERE
                                 epoch = :epoch
                               AND status = 'REGISTRATION'
                         ), 0) AS pool_deposits,
                
                -- Net deposit from pool retirements in the next epoch
                COALESCE((
                             SELECT
                                 SUM(-amount) AS pool_retired
                             FROM
                                 pool
                             WHERE
                                 epoch = :epoch + 1
                               AND status = 'RETIRED'
                         ), 0) AS pool_retires;
                """;

        Map param = new HashMap<>();
        param.put("epoch", epoch);
        param.put("stake_reg_deposit", BigInteger.valueOf(2000000));

        var stakeDeposit = jdbcTemplate.queryForObject(stakeRegDeRegAndPoolDepositQuery, param, new RowMapper<StakeDeposit>() {
            @Override
            public StakeDeposit mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new StakeDeposit(
                        rs.getBigDecimal("stake_deposits").toBigInteger(),
                        rs.getBigDecimal("pool_deposits").toBigInteger(),
                        rs.getBigDecimal("pool_retires").toBigInteger()
                );
            }
        });

        BigInteger totalDeposit = BigInteger.ZERO;
        if(stakeDeposit != null) {
            totalDeposit = stakeDeposit.netStakeDepositAmount().add(stakeDeposit.poolDepositAmount()).add(stakeDeposit.poolRetiredAmount());
        }

        return totalDeposit;
    }
}

record StakeDeposit(BigInteger netStakeDepositAmount, BigInteger poolDepositAmount, BigInteger poolRetiredAmount) {
}
