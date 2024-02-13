package com.bloxbean.cardano.yaci.store.adapot.service;

import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;
import com.bloxbean.cardano.yaci.store.adapot.storage.AdaPotStorage;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Optional;

/**
 * Handles update to AdaPot
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdaPotService {
    private final AdaPotStorage adaPotStorage;

    public void updateAdaPotDeposit(EventMetadata metadata, AdaPot prevAdaPot, BigInteger totalDeposit, BigInteger totalFee, boolean isEpochBoundary) {
        var adaPot = AdaPot.builder()
                .deposit(prevAdaPot.getDeposit().add(totalDeposit))
                .fees(totalFee)
                .epoch(metadata.getEpochNumber())
                .epochBoundary(isEpochBoundary)
                .slot(metadata.getSlot())
                .blockNumber(metadata.getBlock())
                .blockTime(metadata.getBlockTime())
                .build();

        //Save adaPot
        adaPotStorage.save(adaPot);
    }

    public AdaPot getAdaPot(Integer epoch) {
        Optional<AdaPot> prevAdaPotOptional = adaPotStorage.findByEpoch(epoch);
        if (prevAdaPotOptional.isEmpty()) {
            prevAdaPotOptional = adaPotStorage.findByEpoch(epoch - 1);
        }

        var prevAdaPot = prevAdaPotOptional.orElse(
                AdaPot.builder()
                        .deposit(BigInteger.ZERO)
                        .fees(BigInteger.ZERO)
                        .epoch(epoch)
                        .build());
        return prevAdaPot;
    }

    public int rollbackAdaPot(long rollbackSlot) {
        return adaPotStorage.deleteBySlotGreaterThan(rollbackSlot);
    }

}
