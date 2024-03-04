package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.BlockHeader;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.configuration.EpochConfig;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.domain.CardanoEra;
import com.bloxbean.cardano.yaci.store.core.storage.api.CursorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.api.EraStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EraService {
    private final EraStorage eraStorage;
    private final CursorStorage cursorStorage;
    private final EpochConfig epochConfig;
    private final GenesisConfig genesisConfig;
    private final StoreProperties storeProperties;

    private Era prevEra;
    private long shelleyStartSlot = -1;

    //Don't use these variable directly. Use firstShelleySlot(), shelleyEraStartTime() method
    private long _firstShelleySlot;
    private long _shelleyStartTime;

    public boolean checkIfNewEra(Era era, BlockHeader blockHeader) {
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

        if (prevEra != null && prevEra != era) {
            log.info("Era change detected at block {} from {} to {}", blockHeader.getHeaderBody().getBlockNumber(), prevEra.getValue(), era.getValue());
            saveEra(era, blockHeader.getHeaderBody().getSlot(), blockHeader.getHeaderBody().getBlockHash(), blockHeader.getHeaderBody().getBlockNumber());
            prevEra = era;
            return true;
        } else
            return false;
    }

    public void saveEra(Era era, long slot, String blockHash, long blockNumber) {
        CardanoEra cardanoEra = CardanoEra.builder()
                .era(era)
                .startSlot(slot)
                .blockHash(blockHash)
                .block(blockNumber)
                .build();

        eraStorage.saveEra(cardanoEra);
    }

    public int getEpochNo(Era era, long slot) {
        if (shelleyStartSlot == -1) {
            shelleyStartSlot = eraStorage.findFirstNonByronEra().map(cardanoEra -> cardanoEra.getStartSlot()) //For local devenet, it could be babbage era
                    .orElseThrow(() -> new IllegalStateException("Shelley start slot not found"));
        }

        final int epochNumber = epochConfig.epochFromSlot(shelleyStartSlot, era, slot);
        return epochNumber;
    }

    public int getShelleyEpochSlot(long shelleyAbsoluteSlot) {
        if (shelleyStartSlot == -1) {
            shelleyStartSlot = eraStorage.findFirstNonByronEra().map(cardanoEra -> cardanoEra.getStartSlot()) //For local devenet, it could be babbage era
                    .orElseThrow(() -> new IllegalStateException("Shelley start slot not found"));
        }

        return epochConfig.shelleyEpochSlot(shelleyStartSlot, shelleyAbsoluteSlot);
    }

    public long shelleyEraStartTime() {
        if (_shelleyStartTime != 0)
            return _shelleyStartTime;
        firstShelleySlot();

        long startTime = genesisConfig.getStartTime(storeProperties.getProtocolMagic());
        _shelleyStartTime = startTime + firstShelleySlot() * (long)genesisConfig.slotDuration(Era.Byron);

        return _shelleyStartTime;
    }

    public long slotsPerEpoch(Era era) {
        return genesisConfig.slotsPerEpoch(era);
    }

    public long getFirstNonByronSlot() {
        return firstShelleySlot();
    }

    public Optional<Integer> getFirstNonByronEpoch() {
        var nonByronEra = eraStorage.findFirstNonByronEra();
        var epoch = nonByronEra.map(cardanoEra -> getEpochNo(cardanoEra.getEra(), cardanoEra.getStartSlot()));

        return epoch;
    }

    private long firstShelleySlot() {
        //calculate Byron Era last slot time
        if (_firstShelleySlot == 0) {
            _firstShelleySlot = eraStorage.findFirstNonByronEra().map(cardanoEra -> cardanoEra.getStartSlot())
                    .orElse(0L);

            if (_firstShelleySlot == -1) //Genesis block is already in shelley/post shelley era
                _firstShelleySlot = 0;
        }

        return _firstShelleySlot;
    }

    /**
     * Find the block time for a given slot for shelley and post shelley era
     * @param era
     * @param slot
     * @return block time
     */
    public long blockTime(Era era, long slot) {
        if (era == Era.Byron) {
            return byronBlockTime(slot);
        } else {
            long slotsFromShelleyStart = slot - firstShelleySlot();
            return (shelleyEraStartTime() + slotsFromShelleyStart * (long) genesisConfig.slotDuration(Era.Shelley));
        }
    }

    /**
     * Find the block time for a given slot for byron era
     * @param slot
     * @return block time
     */
    private long byronBlockTime(long slot) {
        long startTime = genesisConfig.getStartTime(storeProperties.getProtocolMagic());
        return startTime + slot * (long)genesisConfig.slotDuration(Era.Byron);
    }
}
