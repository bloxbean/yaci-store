package com.bloxbean.cardano.yaci.store.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.domain.Cursor;
import com.bloxbean.cardano.yaci.store.model.CursorEntity;
import com.bloxbean.cardano.yaci.store.repository.CursorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class CursorService {
    private final CursorRepository cursorRepository;
    private AtomicLong count;

    @Value("${event.publisher.id:1}")
    private long eventPublisherId;

    private boolean syncMode;

    public CursorService(CursorRepository cursorRepository) {
        this.cursorRepository = cursorRepository;
        this.count = new AtomicLong(0);
    }

    public void setCursor(Cursor cursor) {
        if (cursor.getBlockHash() == null)
            throw new RuntimeException("BlockHash can't be null.");

        CursorEntity cursorEntity = CursorEntity
                .builder()
                .id(eventPublisherId)
                .slot(cursor.getSlot())
                .blockHash(cursor.getBlockHash())
                .block(cursor.getBlock())
                .build();

        cursorRepository.save(cursorEntity);
        printLog(cursor.getBlock(), cursor.getEra());
    }

    public void setByronEraCursor(String prevBlockHash, Cursor cursor) {
        if (cursor.getBlockHash() == null)
            throw new RuntimeException("BlockHash can't be null.");

        AtomicLong block = new AtomicLong();
        cursorRepository.findByBlockHash(prevBlockHash).ifPresent(preBlock -> {
            block.set(preBlock.getBlock() + 1);
        });

        CursorEntity cursorEntity = CursorEntity
                .builder()
                .id(eventPublisherId)
                .slot(cursor.getSlot())
                .blockHash(cursor.getBlockHash())
                .block(block.get())
                .build();

        cursorRepository.save(cursorEntity);
        printLog(block.get(), cursor.getEra());
    }

    public Optional<Cursor> getCursor() {
        //Get last 50 blocks and select the lowest block number
        List<CursorEntity> cursorEntities =
                cursorRepository.findTop50ByIdOrderBySlotDesc(eventPublisherId);

        if (cursorEntities == null || cursorEntities.size() == 0)
            return Optional.empty();

        CursorEntity cursorEntity = cursorEntities.get(cursorEntities.size() - 1);

        return Optional.of(Cursor.builder()
                .slot(cursorEntity.getSlot())
                .blockHash(cursorEntity.getBlockHash())
                .block(cursorEntity.getBlock())
                .build());
    }


    @Transactional
    public void rollback(long slot) {
        int count = cursorRepository.deleteBySlotGreaterThan(slot);
        log.info("Rollback -- {} cursor records", count);

        CursorEntity cursorEntity
                = cursorRepository.findTopByIdOrderBySlotDesc(eventPublisherId).orElse(new CursorEntity());
        log.info("Cursor : Slot=" + cursorEntity.getSlot() + ", Hash=" + cursorEntity.getBlockHash());
    }

    public boolean isSyncMode() {
        return syncMode;
    }

    public void setSyncMode(boolean syncMode) {
        this.syncMode = syncMode;
    }

    private void printLog(long block, Era era) {
        count.incrementAndGet();
        double val = count.get() % 100;

        if (!syncMode) {
            if (val == 0) {
                log.info("# of blocks written: " + count.get());
                log.info("Block No: " + block + "  , Era: " + era);
            }

        } else {
            log.info("# of blocks written: " + count.get());
            log.info("Block No: " + block);
        }
    }

}
