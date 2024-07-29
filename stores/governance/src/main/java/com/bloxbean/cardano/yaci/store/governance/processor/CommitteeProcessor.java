
package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.genesis.ConwayGenesis;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.governance.domain.Committee;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeStorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommitteeProcessor {
    private final StoreProperties storeProperties;
    private final CommitteeStorage committeeStorage;
    private final CommitteeStorageReader committeeStorageReader;

    @EventListener
    @Transactional
    public void handleEpochChangeEvent(EpochChangeEvent epochChangeEvent) {
        Era prevEra = epochChangeEvent.getPreviousEra();
        Era newEra = epochChangeEvent.getEra();
        long protocolMagic = epochChangeEvent.getEventMetadata().getProtocolMagic();
        long slot = epochChangeEvent.getEventMetadata().getSlot();
        int epoch = epochChangeEvent.getEventMetadata().getEpochNumber();

        // store data from genesis file
        if (newEra.equals(Era.Conway) && prevEra != Era.Conway) {
            var latestCommitteeOpt = committeeStorageReader.findByMaxEpoch();
            if (latestCommitteeOpt.isPresent()) {
                log.info("Committee data already exists, skip storing constitution from genesis file");
                return;
            }

            var conwayGenesis = getConwayGenesis(protocolMagic);
            var numerator = conwayGenesis.getCommitteeNumerator();
            var denominator = conwayGenesis.getCommitteeDenominator();
            var threshold = conwayGenesis.getCommitteeThreshold();
            var govActionTxHash = "genesis.conway";
            Committee committee = buildCommittee(govActionTxHash, null, numerator, denominator, threshold, epoch, slot);
            committeeStorage.save(committee);
        }
    }

    private ConwayGenesis getConwayGenesis(long protocolMagic) {
        String conwayGenesisFile = storeProperties.getConwayGenesisFile();

        if (StringUtil.isEmpty(conwayGenesisFile))
            return new ConwayGenesis(protocolMagic);
        else
            return new ConwayGenesis(new File(conwayGenesisFile));
    }

    private Committee buildCommittee(String govActionTxHash, Integer govActionIndex,
                                     BigInteger thresholdNumerator, BigInteger thresholdDenominator, Double threshold,
                                     int epoch, long slot) {
        return Committee.builder()
                .govActionTxHash(govActionTxHash)
                .govActionIndex(govActionIndex)
                .thresholdNumerator(thresholdNumerator)
                .thresholdDenominator(thresholdDenominator)
                .threshold(threshold)
                .epoch(epoch)
                .slot(slot)
                .build();
    }

    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = committeeStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} committee records", count);
    }
}
