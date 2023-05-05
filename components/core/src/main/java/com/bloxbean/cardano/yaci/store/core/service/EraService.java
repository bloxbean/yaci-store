package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.BlockHeader;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.core.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.configuration.EpochConfig;
import com.bloxbean.cardano.yaci.store.core.domain.CardanoEra;
import com.bloxbean.cardano.yaci.store.core.storage.api.CursorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.api.EraStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EraService {
    private final EraStorage eraStorage;
    private final CursorStorage cursorStorage;
    private final EpochConfig epochConfig;
    private final StoreProperties storeProperties;

    private Era prevEra;
    private long shelleyStartSlot;

    public void checkIfNewEra(Era era, BlockHeader blockHeader) {
        if (prevEra == null) { //If prevEra is null, then try to find era of prevBlock
            long blockNumber = blockHeader.getHeaderBody().getBlockNumber();
            String prevBlockHash = blockHeader.getHeaderBody().getPrevHash();
            if (blockNumber > 0) {
                cursorStorage.findByBlockHash(storeProperties.getEventPublisherId(), prevBlockHash)
                        .ifPresent(prevBlock -> {
                            prevEra = prevBlock.getEra();
                        });
            }
        }

        if (prevEra == Era.Byron && era != Era.Byron) { //Save shelley slot info, it could be babbage for local devenet
            log.info("Era change detected at block {} from {} to {}", blockHeader.getHeaderBody().getBlockNumber(), prevEra.getValue(), era.getValue());
            CardanoEra cardanoEra = CardanoEra.builder()
                    .era(era)
                    .startSlot(blockHeader.getHeaderBody().getSlot())
                    .blockHash(blockHeader.getHeaderBody().getBlockHash())
                    .block(blockHeader.getHeaderBody().getBlockNumber())
                    .build();

            eraStorage.saveEra(cardanoEra);
            prevEra = era;
        }

        /** Just save shelley start point for now
        if (prevEra == null) {
            boolean eraChanged = cursorStorage.findByBlockHash(storeProperties.getEventPublisherId(),
                            blockHeader.getHeaderBody().getPrevHash())
                    .map(prevBlock -> {
                        if (era.getValue() > prevBlock.getEra().getValue()) {
                            log.info("Era change detected at block {} from {} to {}", blockHeader.getHeaderBody().getBlockNumber(), prevBlock.getEra(), era.getValue());
                            return true;
                        } else
                            return false;
                    }).orElse(false);

            if (eraChanged) {
                CardanoEra cardanoEra = CardanoEra.builder()
                        .era(era)
                        .startSlot(blockHeader.getHeaderBody().getSlot())
                        .blockHash(blockHeader.getHeaderBody().getBlockHash())
                        .block(blockHeader.getHeaderBody().getBlockNumber())
                        .build();
                eraStorage.saveEra(cardanoEra);
            }

            prevEra = era;
        } else if (era.getValue() > prevEra.getValue()) {
            log.info("Era change detected at block {} from {} to {}", blockHeader.getHeaderBody().getBlockNumber(), prevEra.getValue(), era.getValue());
            CardanoEra cardanoEra = CardanoEra.builder()
                    .era(era)
                    .startSlot(blockHeader.getHeaderBody().getSlot())
                    .blockHash(blockHeader.getHeaderBody().getBlockHash())
                    .block(blockHeader.getHeaderBody().getBlockNumber())
                    .build();

            eraStorage.saveEra(cardanoEra);
            prevEra = era;
        } **/
    }

    public int getEpochNo(Era era, long slot) {
        if (shelleyStartSlot == 0) {
            shelleyStartSlot = eraStorage.findFirstNonByronEra().map(cardanoEra -> cardanoEra.getStartSlot()) //For local devenet, it could be babbage era
                    .orElseThrow(() -> new IllegalStateException("Shelley start slot not found"));

            log.info("Shelley Start Slot : {}", shelleyStartSlot);
        }

        final int epochNumber = epochConfig.epochFromSlot(shelleyStartSlot, era, slot);
        return epochNumber;
    }
}
