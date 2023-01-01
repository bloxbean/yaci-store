package com.bloxbean.cardano.yaci.store.service;

import com.bloxbean.cardano.yaci.store.domain.Cursor;
import com.bloxbean.cardano.yaci.store.model.CursorEntity;
import com.bloxbean.cardano.yaci.store.repository.CursorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class CursorService {
    private final CursorRepository cursorRepository;

    @Value("${event.publisher.id:1}")
    private long eventPublisherId;

    public CursorService(CursorRepository cursorRepository) {
        this.cursorRepository = cursorRepository;
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
    }

    public Optional<Cursor> getCursor() {
        //Get last 50 blocks and select the lowest block number
        List<CursorEntity> cursorEntities =
                cursorRepository.findTop50ByIdOrderByBlockDesc(eventPublisherId);

        if (cursorEntities == null || cursorEntities.size() == 0)
            return Optional.empty();

        CursorEntity cursorEntity = cursorEntities.get(cursorEntities.size() - 1);

        return Optional.of(Cursor.builder()
                .slot(cursorEntity.getSlot())
                .blockHash(cursorEntity.getBlockHash())
                .block(cursorEntity.getBlock())
                .build());
    }

}
