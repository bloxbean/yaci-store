package com.bloxbean.cardano.yaci.store.adapot.service;

import com.bloxbean.cardano.yaci.store.adapot.storage.AdaPotStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.EpochStakeStorageReader;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.epoch.service.ProtocolParamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.rewards.calculation.domain.Epoch;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

/**
 * EpochInfoService is a component that provides information about epochs
 * in the blockchain, including block counts, fees, and active stake.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EpochInfoService {
    private final EraService eraService;
    private final BlockStorageReader blockStorageReader;
    private final AdaPotStorage adaPotStorage;
    private final BlockInfoService blockInfoService;
    private final EpochStakeStorageReader epochStakeStorage;
    private final ProtocolParamService protocolParamService;

    public Optional<Epoch> getEpochInfo(int epoch) {
        //Return empty if epoch is before the start of the Shelley era
        int shelleyEpoch = eraService.getFirstNonByronEpoch().orElse(0);
        log.info("Shelley epoch : {}", shelleyEpoch);
        if (epoch < shelleyEpoch) {
            log.warn("Epoch " + epoch + " is before the start of the Shelley era. No rewards were calculated in this epoch.");
            return Optional.empty();
        }

        var blocksCount = blockStorageReader.totalBlocksInEpoch(epoch);
        log.info("Blocks count for epoch {} : {}", epoch, blocksCount);

        var adaPot = adaPotStorage.findByEpoch(epoch + 1)//To get fee collected in epoch e
                .orElse(null);

        if (adaPot == null)
            log.error("AdaPot not found for epoch : {}", epoch);

        BigInteger fees = adaPot != null? adaPot.getFees(): BigInteger.ZERO;

        Epoch epochInfo = Epoch.builder()
                .number(epoch)
                .fees(fees)
                .blockCount(blocksCount)
                .build();

        var protocolParam = protocolParamService.getProtocolParam(epoch).orElse(null);
        if (protocolParam != null) {
            if (protocolParam.getDecentralisationParam() == null
                    || protocolParam.getDecentralisationParam() == BigDecimal.ZERO) { // When decentralisation is 0. No OBFT blocks
                epochInfo.setNonOBFTBlockCount(epochInfo.getBlockCount());
            } else if (protocolParam.getDecentralisationParam() == BigDecimal.ONE) { // When decentralisation is 1. All blocks are OBFT
                epochInfo.setNonOBFTBlockCount(0);
            } else { //When decentralisation is between 0 and 1, get the non-OBFT blocks
                Integer nonObftBlocks = blockInfoService.getNonOBFTBlocksInEpoch(epoch);
                epochInfo.setNonOBFTBlockCount(nonObftBlocks);
            }
        } else {
            log.error("Protocol parameters not found for epoch : {}", epoch);
            //TODO: Should we throw exception here ?
        }

        epochStakeStorage.getTotalActiveStakeByEpoch(epoch).ifPresent(stake -> {
            epochInfo.setActiveStake(stake);
        });

        if (epochInfo.getActiveStake() == null)
            epochInfo.setActiveStake(BigInteger.ZERO);

        return Optional.of(epochInfo);

    }
}
