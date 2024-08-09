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

    public void updateAdaPotDeposit(EventMetadata metadata, AdaPot prevAdaPot, BigInteger totalDeposit, BigInteger totalFee, BigInteger netUtxo, boolean isEpochBoundary) {
        updateAdaPotDeposit(metadata, prevAdaPot, totalDeposit, totalFee, netUtxo, BigInteger.ZERO, isEpochBoundary);
    }

    public void updateAdaPotDeposit(EventMetadata metadata, AdaPot prevAdaPot, BigInteger totalDeposit, BigInteger totalFee, BigInteger netUtxo, BigInteger netTreasuryAmt, boolean isEpochBoundary) {
        var updatedDeposit = prevAdaPot.getDeposits() != null ? prevAdaPot.getDeposits().add(totalDeposit) : BigInteger.ZERO.add(totalDeposit);
        var updatedUtxo = prevAdaPot.getUtxo() != null ? prevAdaPot.getUtxo().add(netUtxo) : BigInteger.ZERO.add(netUtxo);
        var updatedTreasury = prevAdaPot.getTreasury() != null ? prevAdaPot.getTreasury().add(netTreasuryAmt) : BigInteger.ZERO.add(netTreasuryAmt);

        var adaPot = AdaPot.builder()
                .deposits(updatedDeposit)
                .fees(totalFee)
                .utxo(updatedUtxo)
                .treasury(updatedTreasury)
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
        Optional<AdaPot> prevAdaPotOptional = adaPotStorage.findRecentByEpoch(epoch);
        if (prevAdaPotOptional.isEmpty()) {
            prevAdaPotOptional = adaPotStorage.findRecentByEpoch(epoch - 1);
        }

        var prevAdaPot = prevAdaPotOptional.orElse(
                AdaPot.builder()
                        .deposits(BigInteger.ZERO)
                        .fees(BigInteger.ZERO)
                        .utxo(BigInteger.ZERO)
                        .epochBoundary(false)
                        .epoch(epoch)
                        .build());
        return prevAdaPot;
    }

    public AdaPot updateReserveAndTreasury(int epoch, BigInteger treasury, BigInteger reserves) {
        adaPotStorage.findByEpochAtEpochBoundary(epoch)
                .ifPresentOrElse(adaPot -> {
                    adaPot.setTreasury(treasury);
                    adaPot.setReserves(reserves);
                    adaPotStorage.save(adaPot);
                }, () -> {
                    log.error("Reserves and treasury can't be updated. AdaPot not found for epoch : {}", epoch);
                });

        adaPotStorage.findRecentByEpoch(epoch)
                .ifPresentOrElse(adaPot -> {
                    adaPot.setTreasury(treasury);
                    adaPot.setReserves(reserves);
                    adaPotStorage.save(adaPot);
                }, () -> {
                    log.error("Reserves and treasury can't be updated. Recent not found for epoch : {}", epoch);
                });

        return adaPotStorage.findByEpochAtEpochBoundary(epoch).orElse(null);
    }

    public int rollbackAdaPot(long rollbackSlot) {
        return adaPotStorage.deleteBySlotGreaterThan(rollbackSlot);
    }

}
