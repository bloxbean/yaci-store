package com.bloxbean.cardano.yaci.store.service;

import com.bloxbean.cardano.yaci.store.domain.Cursor;
import com.bloxbean.cardano.yaci.store.model.CursorEntity;
import com.bloxbean.cardano.yaci.store.repository.CursorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CursorService {
    private final CursorRepository cursorRepository;

    public void setCursor(Cursor cursor) {
        if (cursor.getBlockHash() == null)
            throw new RuntimeException("BlockHash can't be null.");

        CursorEntity cursorEntity = CursorEntity
                .builder()
                .id(1L)
                .slot(cursor.getSlot())
                .blockHash(cursor.getBlockHash())
                .block(cursor.getBlock())
                .build();

        cursorRepository.save(cursorEntity);
    }

    public Optional<Cursor> getCursor() {
        return cursorRepository.findById(1L)
                .map(cursorEntity -> Cursor.builder()
                        .slot(cursorEntity.getSlot())
                        .blockHash(cursorEntity.getBlockHash())
                        .block(cursorEntity.getBlock())
                        .build());
    }
}
