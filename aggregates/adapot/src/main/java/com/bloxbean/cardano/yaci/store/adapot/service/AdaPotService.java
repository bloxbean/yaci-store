package com.bloxbean.cardano.yaci.store.adapot.service;

import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;
import com.bloxbean.cardano.yaci.store.adapot.storage.AdaPotStorage;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.rewards.calculation.domain.EpochCalculationResult;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Optional;

/**
 * A service that handles operations related to AdaPot.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdaPotService {
    private final AdaPotStorage adaPotStorage;

    /**
     * Create AdaPot for the given epoch
     * @param epoch
     * @return
     */
    @Transactional
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
    public boolean updateAdaPot(int epoch, EpochCalculationResult epochCalculationResult) {
        var adaPot = adaPotStorage.findByEpoch(epoch).orElse(null);
        if(adaPot == null) {
            log.error("AdaPot not found for epoch to update reserve and treasury: {}", epoch);
            return false;
        }

        adaPot.setTreasury(epochCalculationResult.getTreasury());
        adaPot.setReserves(epochCalculationResult.getReserves());
        adaPot.setCirculation(epochCalculationResult.getTotalAdaInCirculation());
        adaPot.setDistributedRewards(epochCalculationResult.getTotalDistributedRewards());
        adaPot.setUndistributedRewards(epochCalculationResult.getTotalUndistributedRewards());
        adaPot.setRewardsPot(epochCalculationResult.getTotalRewardsPot());
        adaPot.setPoolRewardsPot(epochCalculationResult.getTotalPoolRewardsPot());
        adaPotStorage.save(adaPot);

        return true;
    }

    public int rollbackAdaPot(long rollbackSlot) {
        return adaPotStorage.deleteBySlotGreaterThan(rollbackSlot);
    }

}
