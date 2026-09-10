package com.bloxbean.cardano.yaci.store.adapot.snapshot;

import com.bloxbean.cardano.yaci.store.common.util.ErrorCode;
import com.bloxbean.cardano.yaci.store.events.ErrorEvent;
import com.bloxbean.cardano.yaci.store.staking.service.DepositParamService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final DepositParamService depositParamService;
    private final ApplicationEventPublisher publisher;

    public BigInteger getNetStakeDepositInEpoch(int epoch) {
        // GenesisPoolProcessor writes synthetic "Genesis" rows for devnet initial
        // stake/pool certificates. Those deposits are already seeded into epoch 0,
        // so regular epoch snapshots must exclude them to avoid double-counting.
        // Legacy deregistration certificates have no coin. For those rows, select the immediately
        // preceding lifecycle row first and only use its deposit when it is a registration; filtering
        // to registrations before ordering could incorrectly cross an intervening deregistration.
        String stakeRegDeRegAndPoolDepositQuery = """
                SELECT
                    stake.stake_deposits,
                    stake.unresolved_stake_deposits,
                
                    -- Net deposit from pool registrations in the specified epoch
                    COALESCE((
                        SELECT SUM(amount)
                        FROM pool
                        WHERE epoch = :epoch
                          AND status = 'REGISTRATION'
                          AND tx_hash <> :genesis_tx_hash
                    ), 0) AS pool_deposits,
                
                    -- Net deposit from pool retirements in the next epoch
                    COALESCE((
                        SELECT SUM(-amount)
                        FROM pool
                        WHERE epoch = :epoch + 1
                          AND status = 'RETIRED'
                    ), 0) AS pool_retires
                FROM (
                    SELECT
                        COALESCE(SUM(CASE
                            WHEN resolved_registration.type = 'STAKE_REGISTRATION'
                                THEN resolved_registration.deposit
                            WHEN resolved_registration.type = 'STAKE_DEREGISTRATION'
                                THEN -resolved_registration.deposit
                            ELSE 0
                        END), 0) AS stake_deposits,
                        COALESCE(SUM(CASE
                            WHEN resolved_registration.type = 'STAKE_DEREGISTRATION'
                                 AND resolved_registration.deposit IS NULL
                                THEN 1
                            ELSE 0
                        END), 0) AS unresolved_stake_deposits
                    FROM (
                        SELECT registration.type,
                               COALESCE(registration.deposit, CASE
                                   WHEN registration.type = 'STAKE_DEREGISTRATION' THEN (
                                       SELECT CASE
                                           WHEN lifecycle.type = 'STAKE_REGISTRATION' THEN lifecycle.deposit
                                           ELSE NULL
                                       END
                                       FROM stake_registration lifecycle
                                       WHERE lifecycle.address = registration.address
                                         AND (lifecycle.slot < registration.slot
                                           OR (lifecycle.slot = registration.slot
                                               AND lifecycle.tx_index < registration.tx_index)
                                           OR (lifecycle.slot = registration.slot
                                               AND lifecycle.tx_index = registration.tx_index
                                               AND lifecycle.cert_index < registration.cert_index))
                                       ORDER BY lifecycle.slot DESC, lifecycle.tx_index DESC, lifecycle.cert_index DESC
                                       LIMIT 1
                                   )
                                   ELSE NULL
                               END) AS deposit
                        FROM stake_registration registration
                        WHERE registration.epoch = :epoch
                          AND registration.tx_hash <> :genesis_tx_hash
                    ) resolved_registration
                ) stake;
                """;

        Map<String, Object> param = new HashMap<>();
        param.put("epoch", epoch);
        param.put("genesis_tx_hash", "Genesis");

        var stakeDeposit = jdbcTemplate.queryForObject(stakeRegDeRegAndPoolDepositQuery, param, new RowMapper<StakeDeposit>() {
            @Override
            public StakeDeposit mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new StakeDeposit(
                        rs.getBigDecimal("stake_deposits").toBigInteger(),
                        rs.getBigDecimal("pool_deposits").toBigInteger(),
                        rs.getBigDecimal("pool_retires").toBigInteger(),
                        rs.getLong("unresolved_stake_deposits")
                );
            }
        });

        BigInteger totalDeposit = BigInteger.ZERO;
        if (stakeDeposit != null) {
            if (stakeDeposit.unresolvedStakeDeposits() > 0) {
                long unresolvedCount = stakeDeposit.unresolvedStakeDeposits();
                BigInteger fallbackDeposit = depositParamService.getKeyDeposit(epoch)
                        .multiply(BigInteger.valueOf(unresolvedCount));
                String reason = "Unable to resolve " + unresolvedCount
                        + " legacy stake deregistration deposit(s) for epoch " + epoch;

                publisher.publishEvent(ErrorEvent.builder()
                        .errorCode(ErrorCode.DATA_MISSING_ERROR.name())
                        .reason(reason)
                        .details("Using protocol parameter key deposit fallback: " + fallbackDeposit)
                        .build());

                totalDeposit = totalDeposit.subtract(fallbackDeposit);
            }
            totalDeposit = totalDeposit.add(stakeDeposit.netStakeDepositAmount())
                    .add(stakeDeposit.poolDepositAmount())
                    .add(stakeDeposit.poolRetiredAmount());
        }

        return totalDeposit;
    }
}

record StakeDeposit(BigInteger netStakeDepositAmount,
                    BigInteger poolDepositAmount,
                    BigInteger poolRetiredAmount,
                    long unresolvedStakeDeposits) {
}
