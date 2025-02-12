package com.bloxbean.cardano.yaci.store.adapot.service;

import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;
import com.bloxbean.cardano.yaci.store.adapot.storage.AdaPotStorage;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.transaction.Transactional;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A service that handles operations related to AdaPot.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdaPotService {
    private final AdaPotStorage adaPotStorage;
    private final StoreProperties storeProperties;

    /**
     * Create AdaPot for the given epoch
     * @param epoch
     * @return
     */
    public AdaPot createAdaPot(Integer epoch, Long slot) {
        var adaPot = AdaPot.builder()
                .epoch(epoch)
                .slot(slot)
                .depositsStake(BigInteger.ZERO)
                .fees(BigInteger.ZERO)
                .utxo(BigInteger.ZERO)
                .treasury(BigInteger.ZERO)
                .reserves(BigInteger.ZERO)
                .build();

        //Save adaPot
        adaPotStorage.save(adaPot);
        return adaPot;
    }

    public Optional<AdaPot> getAdaPot(Integer epoch) {
        return adaPotStorage.findByEpoch(epoch);
    }

    @Transactional
    public AdaPot createAdaPot(EventMetadata metadata) {
        var adaPot = AdaPot.builder()
                .epoch(metadata.getEpochNumber())
                .depositsStake(BigInteger.ZERO)
                .fees(BigInteger.ZERO)
                .utxo(BigInteger.ZERO)
                .treasury(BigInteger.ZERO)
                .reserves(BigInteger.ZERO)
                .slot(metadata.getSlot())
                .blockNumber(metadata.getBlock())
                .blockTime(metadata.getBlockTime())
                .build();

        //Save adaPot
        adaPotStorage.save(adaPot);
        return adaPot;
    }

    @Transactional
    public boolean updateEpochFee(int epoch, BigInteger fee) {
        var adaPot = adaPotStorage.findByEpoch(epoch).orElse(null);
        if(adaPot == null) {
            log.error("AdaPot not found for epoch to update fee: {}", epoch);
            return false;
        }

        adaPot.setFees(fee);
        adaPotStorage.save(adaPot);
        return true;
    }

    @Transactional
    public boolean updateEpochUtxo(int epoch, BigInteger utxo) {
        var adaPot = adaPotStorage.findByEpoch(epoch).orElse(null);
        if(adaPot == null) {
            log.error("AdaPot not found for epoch to update utxo: {}", epoch);
            return false;
        }

        adaPot.setUtxo(utxo);
        adaPotStorage.save(adaPot);
        return true;
    }

    @Transactional
    public boolean updateAdaPotDeposit(int epoch, BigInteger totalDeposit) {
        var prevAdaPot = adaPotStorage.findByEpoch(epoch - 1).orElse(null);

        var updatedDeposit = prevAdaPot != null && prevAdaPot.getDepositsStake() != null ?
                prevAdaPot.getDepositsStake().add(totalDeposit) :
                BigInteger.ZERO.add(totalDeposit);

        var adaPot = adaPotStorage.findByEpoch(epoch).orElse(null);

        if(adaPot == null) {
            log.error("AdaPot not found for epoch to update deposit: {}", epoch);
            return false;
        }

        adaPot.setDepositsStake(updatedDeposit);
        adaPotStorage.save(adaPot);

        return true;
    }

    @Transactional
    public boolean updateReserveAndTreasury(int epoch, BigInteger treasury, BigInteger reserves, BigInteger rewards) {
        var adaPot = adaPotStorage.findByEpoch(epoch).orElse(null);
        if(adaPot == null) {
            log.error("AdaPot not found for epoch to update reserve and treasury: {}", epoch);
            return false;
        }
        adaPot.setRewards(rewards);

        Map<Integer, DbSyncAdaPot> expectedPots = null;
        try {
            expectedPots = loadExpectedAdaPotValues(storeProperties.getProtocolMagic());
            var expectedPot = expectedPots.get(epoch);
            adaPot.setTreasury(expectedPot.getTreasury());
            adaPot.setReserves(expectedPot.getReserves());

            adaPotStorage.save(adaPot);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    public int rollbackAdaPot(long rollbackSlot) {
        return adaPotStorage.deleteBySlotGreaterThan(rollbackSlot);
    }

    private Map<Integer, DbSyncAdaPot> loadExpectedAdaPotValues(long protocolMagic) throws IOException {
        String file = "dbsync_ada_pots.json";
        if (protocolMagic == 1) { //preprod
            file = "dbsync_ada_pots_preprod.json";
        } else if (protocolMagic == 2) { //preview
            file = "dbsync_ada_pots_preview.json";
        }

        ObjectMapper objectMapper = new ObjectMapper();
        List<DbSyncAdaPot> pots = objectMapper.readValue(this.getClass().getClassLoader().getResourceAsStream(file), new TypeReference<>() {
        });

        Map<Integer, DbSyncAdaPot> potsMap = pots.stream()
                .collect(Collectors.toMap(DbSyncAdaPot::getEpochNo, pot -> pot));

        return potsMap;
    }
}

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
class DbSyncAdaPot {
    private int epochNo;
    private BigInteger treasury;
    private BigInteger reserves;
    private BigInteger fees;
    private BigInteger deposits;
    private BigInteger utxo;
}



