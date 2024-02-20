package com.bloxbean.cardano.yaci.store.adapot.snapshot;

import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.EpochStakeRepository;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class SnapshotService {
    private final StakeSnapshotService stakeSnapshotService;
    private final BlockStorageReader blockStorageReader;
    private final EpochStakeRepository stakeSnapshotRepository;
    private final EraService eraService;

    private Integer shelleyStartEpoch;

    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.SECONDS)
    public void takeSnapshot() {
        if (getShelleyStartEpoch() == null) {
            log.info("Shelley start epoch is not available yet");
            return;
        }

        var epoch = blockStorageReader.findRecentBlock()
                .map(Block::getEpochNumber)
                        .orElse(0);

        if (epoch == 0)
            return;

        var lastSnapshotEpoch = stakeSnapshotRepository.getMaxEpoch().orElse(null);

        if (lastSnapshotEpoch != null && lastSnapshotEpoch >= epoch - 1) {
            log.info("Stake snapshot for epoch : {} is already taken", epoch);
            return;
        }

        int epochToCalculate;
        if (lastSnapshotEpoch == null)
            epochToCalculate = getShelleyStartEpoch();
        else
            epochToCalculate = lastSnapshotEpoch + 1;

        if (epochToCalculate >= epoch - 1) {
            log.info("No new epoch to calculate");
            return;
        }

        //Take stake snapshot
        stakeSnapshotService.takeStakeSnapshot(epochToCalculate);
        //Take delegation snapshot
        //Take pool param snapshot
    }

    private Integer getShelleyStartEpoch() {
        if (shelleyStartEpoch == null)
            shelleyStartEpoch = eraService.getFirstNonByronEpoch().orElse(null);

        return shelleyStartEpoch;
    }
}
