package com.bloxbean.cardano.yaci.store.adapot.snapshot;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.RequiredArgsConstructor;
import org.cardanofoundation.rewards.calculation.config.NetworkConfig;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class UtxoSnapshotService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final StoreProperties storeProperties;
    private final EraService eraService;

    public BigInteger getTotalUtxosInEpoch(int epoch, Long epochFirstSlot) {
        var era = eraService.getEraForEpoch(epoch);
        if (era == null)
            return BigInteger.ZERO;

        BigInteger bootstrapUtxos = BigInteger.ZERO;
        if (storeProperties.isMainnet() && era.value >= Era.Allegra.value) {
            bootstrapUtxos = NetworkConfig.getMainnetConfig().getBootstrapAddressAmount();
        }

        String utxoQuery = """
                SELECT (SUM(a.lovelace_amount)) AS total_lovelace
                 FROM address_utxo a
                          LEFT JOIN tx_input t
                                    ON a.tx_hash = t.tx_hash
                                        AND a.output_index = t.output_index
                                        AND t.spent_at_slot < :epochFirstSlot
                 WHERE a.slot < :epochFirstSlot
                   AND (t.tx_hash IS NULL OR t.spent_at_slot >= :epochFirstSlot);               
                """;

        Map param = new HashMap<>();
        param.put("epochFirstSlot", epochFirstSlot);

        var totalUtxos = jdbcTemplate.queryForObject(utxoQuery, param, BigInteger.class);
        totalUtxos = totalUtxos.subtract(bootstrapUtxos);

        return totalUtxos;
    }
}

