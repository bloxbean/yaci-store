package com.bloxbean.cardano.yaci.store.adapot.service;

import com.bloxbean.cardano.yaci.store.adapot.storage.AdaPotStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.EpochStakeStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.rewards.calculation.domain.Epoch;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Optional;

//Get epoch info during reward calculation
@Component
@RequiredArgsConstructor
@Slf4j
public class EpochInfoService {
    private final EraService eraService;
    private final BlockStorage blockStorage;
    private final BlockStorageReader blockStorageReader;
    private final AdaPotService adaPotService;
    private final AdaPotStorage adaPotStorage;
    private final BlockInfoService blockInfoService;
    private final EpochStakeStorage epochStakeStorage;

    public Optional<Epoch> getEpochInfo(int epoch) {
        //Return null if epoch is before the start of the Shelley era

        int shelleyEpoch = eraService.getFirstNonByronEpoch().orElse(0);
        log.info("Shelley epoch : {}", shelleyEpoch);
        if (epoch < shelleyEpoch) {
            log.warn("Epoch " + epoch + " is before the start of the Shelley era. No rewards were calculated in this epoch.");
            return Optional.empty();
        }


        //TODO -- optimize this
//        var blocks = blockStorage.findBlocksByEpoch(epoch);
        var blocksCount = blockStorageReader.totalBlocksInEpoch(epoch);
        log.info("Blocks count for epoch {} : {}", epoch, blocksCount);

        //TODO -- Fee .. Can we get it from adapot or sum transaction fees from transaction table.
      //  var adaPot = adaPotService.getAdaPot(epoch);
        var adaPot = adaPotStorage.findByEpoch(epoch + 1)//To get fee collected in epoch e
                .orElse(null);

        log.error("AdaPot not found for epoch : {}", epoch);
        BigInteger fees = adaPot != null? adaPot.getFees(): BigInteger.ZERO;

        Epoch epochInfo = Epoch.builder()
                .number(epoch)
                .fees(fees)
                .blockCount(blocksCount)
                .build();

        //TODO -- Replace the harcoded values with network independent values using protocol params values
        if (epoch < 211) {
            epochInfo.setNonOBFTBlockCount(0);
        } else if (epoch > 256) {
            epochInfo.setNonOBFTBlockCount(epochInfo.getBlockCount());
        } else {
            Integer nonObftBlocks = blockInfoService.getNonOBFTBlocksInEpoch(epoch);
            epochInfo.setNonOBFTBlockCount(nonObftBlocks);
        }

//        BigInteger epochStake = dbSyncEpochStakeRepository.getEpochStakeByEpoch(epoch);
//        epochInfo.setFees(fees);
//        epochInfo.setActiveStake(epochStake);

        epochStakeStorage.getTotalActiveStakeByEpoch(epoch).ifPresent(stake -> {
            epochInfo.setActiveStake(stake);
        });

        return Optional.of(epochInfo);

    }
}
