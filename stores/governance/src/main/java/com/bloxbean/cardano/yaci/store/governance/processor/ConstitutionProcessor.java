package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.GenesisConstitution;
import com.bloxbean.cardano.yaci.store.common.genesis.ConwayGenesis;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.governance.domain.Constitution;
import com.bloxbean.cardano.yaci.store.governance.storage.ConstitutionStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.ConstitutionStorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConstitutionProcessor {
    private final StoreProperties storeProperties;
    private final ConstitutionStorage constitutionStorage;
    private final ConstitutionStorageReader constitutionStorageReader;

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
            boolean isConstitutionDataPresent = constitutionStorageReader.findCurrentConstitution().isPresent();
            if (isConstitutionDataPresent) {
                log.debug("Constitution already exists. Skipping storing constitution from genesis file");
                return;
            }
            var constitution = getGenesisConstitution(protocolMagic);
            if (constitution != null) {
                var constitutionToSave = buildConstitution(constitution, epoch, slot);
                constitutionStorage.save(constitutionToSave);
            }
        }
    }

    private GenesisConstitution getGenesisConstitution(long protocolMagic) {
        String conwayGenesisFile = storeProperties.getConwayGenesisFile();

        if (StringUtil.isEmpty(conwayGenesisFile))
            return new ConwayGenesis(protocolMagic).getConstitution();
        else
            return new ConwayGenesis(new File(conwayGenesisFile)).getConstitution();
    }

    private Constitution buildConstitution(GenesisConstitution genesisConstitution, int activeEpoch, long slot) {
        return Constitution.builder()
                .anchorUrl(genesisConstitution.getAnchorUrl())
                .anchorHash(genesisConstitution.getAnchorHash())
                .activeEpoch(activeEpoch)
                .script(genesisConstitution.getScript())
                .slot(slot)
                .build();
    }

    @EventListener
    @Transactional
    //TODO -- tests
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = constitutionStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} constitution records", count);
    }
}
