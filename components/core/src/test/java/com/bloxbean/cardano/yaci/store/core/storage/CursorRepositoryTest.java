package com.bloxbean.cardano.yaci.store.core.storage;

import com.bloxbean.cardano.yaci.store.core.storage.impl.CursorRepository;
import com.bloxbean.cardano.yaci.store.core.storage.impl.model.CursorEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The bulk cursor deletes must not depend on a transaction opened by the caller. In the native image the
 * {@code CursorStorage} bean is not proxied ({@code @Bean} returns the interface, so AOT sees no
 * {@code @Transactional}), and {@code CursorServiceImpl.getStartCursor} calls them with no transaction: every
 * restart on an existing database failed with {@code TransactionRequiredException} (#1205).
 * <p>
 * Runs without the transaction {@code @DataJpaTest} normally wraps each test in, to call the repository the
 * way the native build does.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CursorRepositoryTest {
    private static final long PUBLISHER_ID = 1L;

    @Autowired
    private CursorRepository cursorRepository;

    @AfterEach
    void cleanUp() {
        cursorRepository.deleteAll();
    }

    private void saveCursors(long... blocks) {
        for (long block : blocks) {
            cursorRepository.save(CursorEntity.builder()
                    .id(PUBLISHER_ID)
                    .blockHash("hash-" + block)
                    .block(block)
                    .slot(block * 10)
                    .prevBlockHash("hash-" + (block - 1))
                    .era(7)
                    .build());
        }
    }

    @Test
    void deleteByIdAndSlotGreaterThan_worksWithoutCallerTransaction() {
        saveCursors(1, 2, 3, 4, 5);

        int deleted = cursorRepository.deleteByIdAndSlotGreaterThan(PUBLISHER_ID, 30L);

        assertThat(deleted).isEqualTo(2);
        assertThat(cursorRepository.findTopByIdOrderBySlotDesc(PUBLISHER_ID))
                .hasValueSatisfying(cursor -> assertThat(cursor.getSlot()).isEqualTo(30L));
    }

    @Test
    void deleteByIdAndSlotGreaterThan_deletesNothingAtTheTip() {
        saveCursors(1, 2, 3);

        // What every restart does: the tip is valid, so nothing is newer
        int deleted = cursorRepository.deleteByIdAndSlotGreaterThan(PUBLISHER_ID, 30L);

        assertThat(deleted).isZero();
        assertThat(cursorRepository.count()).isEqualTo(3);
    }

    @Test
    void deleteByIdAndBlockLessThan_worksWithoutCallerTransaction() {
        saveCursors(1, 2, 3, 4, 5);

        int deleted = cursorRepository.deleteByIdAndBlockLessThan(PUBLISHER_ID, 3L);

        assertThat(deleted).isEqualTo(2);
        assertThat(cursorRepository.count()).isEqualTo(3);
    }
}
