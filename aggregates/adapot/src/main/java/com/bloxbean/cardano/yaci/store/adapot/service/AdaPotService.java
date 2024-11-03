package com.bloxbean.cardano.yaci.store.adapot.service;

import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;
import com.bloxbean.cardano.yaci.store.adapot.storage.AdaPotStorage;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Transactional
    public void createAdaPot(EventMetadata metadata) {
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
    }

    @Transactional
    public void updateEpochFee(int epoch, BigInteger fee) {
        adaPotStorage.findByEpoch(epoch)
                .ifPresentOrElse(adaPot -> {
                    adaPot.setFees(fee);
                    adaPotStorage.save(adaPot);
                }, () -> {
                    log.error("Epoch fee can't be updated in adapot. Recent not found for epoch : {}", epoch);
                });

        return;
    }

    @Transactional
    public void updateEpochUtxo(int epoch, BigInteger utxo) {
        adaPotStorage.findByEpoch(epoch)
                .ifPresentOrElse(adaPot -> {
                    adaPot.setUtxo(utxo);
                    adaPotStorage.save(adaPot);
                }, () -> {
                    log.error("Epoch utxo can't be updated in adapot. Recent not found for epoch : {}", epoch);
                });

        return;
    }

    @Transactional
    public void updateAdaPotDeposit(int epoch, BigInteger totalDeposit) {
        var prevAdaPot = adaPotStorage.findByEpoch(epoch - 1).orElse(null);

        var updatedDeposit = prevAdaPot != null && prevAdaPot.getDepositsStake() != null ? prevAdaPot.getDepositsStake().add(totalDeposit) : BigInteger.ZERO.add(totalDeposit);

        adaPotStorage.findByEpoch(epoch)
                .ifPresentOrElse(adaPot -> {
                    adaPot.setDepositsStake(updatedDeposit);
                    adaPotStorage.save(adaPot);
                }, () -> {
                    log.error("Updated deposit for epoch : {}", epoch);
                });
    }

    public AdaPot getAdaPot(Integer epoch) {
        Optional<AdaPot> prevAdaPotOptional = adaPotStorage.findByEpoch(epoch);
        if (prevAdaPotOptional.isEmpty()) {
            prevAdaPotOptional = adaPotStorage.findByEpoch(epoch - 1);
        }

        var prevAdaPot = prevAdaPotOptional.orElse(
                AdaPot.builder()
                        .depositsStake(BigInteger.ZERO)
                        .fees(BigInteger.ZERO)
                        .utxo(BigInteger.ZERO)
                        .epoch(epoch)
                        .build());
        return prevAdaPot;
    }

    @Transactional
    public AdaPot updateReserveAndTreasury(int epoch, BigInteger treasury, BigInteger reserves, BigInteger rewards) {
        adaPotStorage.findByEpoch(epoch)
                .ifPresentOrElse(adaPot -> {
                    adaPot.setTreasury(treasury);
                    adaPot.setReserves(reserves);
                    adaPot.setRewards(rewards);
                    adaPotStorage.save(adaPot);
                }, () -> {
                    log.error("Reserves and treasury can't be updated. Recent not found for epoch : {}", epoch);
                });

        return adaPotStorage.findByEpoch(epoch).orElse(null);
    }

    public int rollbackAdaPot(long rollbackSlot) {
        return adaPotStorage.deleteBySlotGreaterThan(rollbackSlot);
    }

}
