package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.domain.CardanoEra;
import com.bloxbean.cardano.yaci.store.core.storage.api.EraStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class InitService {
    private final StoreProperties storeProperties;
    private final EraStorage eraStorage;

    public void init() {
        findNetworkType();
        checkIfCustomStartPoint();
    }

    private void findNetworkType() {
        storeProperties.setMainnet(storeProperties.getProtocolMagic() == NetworkType.MAINNET.getProtocolMagic());
    }

    private void checkIfCustomStartPoint() {
        if (storeProperties.getSyncStartBlockhash() == null || storeProperties.getSyncStartBlockhash().isEmpty())
            return;

        Optional<CardanoEra> era = eraStorage.findFirstNonByronEra();
        if (era.isPresent())
            return;

        //Era is empty, let's fill shelley era start if it's a known network or configure from properties
        long shelleyStartSlot = storeProperties.getShelleyStartSlot();
        String shelleyStartBlockhash = storeProperties.getShelleyStartBlockhash();
        long shelleyStartBlock = storeProperties.getShelleyStartBlock();
        log.info("Trying to find shelley start block info from properties");

        if (shelleyStartSlot == 0 && shelleyStartBlockhash == null) {
            //set known network start slot
            CardanoEra cardanoEra = getShelleyStartPointForKnownNetwork();
            if (cardanoEra != null) {
                log.info("Shelley start point is set to : " + cardanoEra);
                eraStorage.saveEra(cardanoEra);
            } else
                throw new IllegalStateException("Shelley start point is not configured properly. " +
                        "For any custom sync start point, please configure shelley.start.slot, " +
                        "shelley.start.blockhash and shelley.start.block properties.");
        } else {
            CardanoEra cardanoEra = CardanoEra.builder()
                    .era(Era.Shelley)
                    .startSlot(shelleyStartSlot)
                    .blockHash(shelleyStartBlockhash)
                    .block(shelleyStartBlock)
                    .build();
            eraStorage.saveEra(cardanoEra);
            log.info("Shelley start point is set to : " + cardanoEra);
        }

    }

    private CardanoEra getShelleyStartPointForKnownNetwork() {
        if (NetworkType.MAINNET.getProtocolMagic() == storeProperties.getProtocolMagic()) {
            return CardanoEra.builder()
                    .era(Era.Shelley)
                    .startSlot(4492800)
                    .blockHash("aa83acbf5904c0edfe4d79b3689d3d00fcfc553cf360fd2229b98d464c28e9de")
                    .block(4490511)
                    .build();
        } else if (NetworkType.PREPROD.getProtocolMagic() == storeProperties.getProtocolMagic()) {
            return CardanoEra.builder()
                    .era(Era.Shelley)
                    .startSlot(86400)
                    .blockHash("c971bfb21d2732457f9febf79d9b02b20b9a3bef12c561a78b818bcb8b35a574")
                    .block(46)
                    .build();
        } else if (NetworkType.PREVIEW.getProtocolMagic() == storeProperties.getProtocolMagic()) {
            return CardanoEra.builder()
                    .era(Era.Alonzo)
                    .startSlot(0)
                    .blockHash("268ae601af8f9214804735910a3301881fbe0eec9936db7d1fb9fc39e93d1e37")
                    .block(0)
                    .build();
        } else
            return null;
    }

}
