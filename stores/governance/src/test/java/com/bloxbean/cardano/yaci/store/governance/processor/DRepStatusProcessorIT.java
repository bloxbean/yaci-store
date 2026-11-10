package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreCommitEvent;
import com.bloxbean.cardano.yaci.store.governance.domain.DRepRegistration;
import com.bloxbean.cardano.yaci.store.governance.domain.DRepStatus;
import com.bloxbean.cardano.yaci.store.governance.domain.event.DRepRegistrationEvent;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DRepEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.DRepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DRepStatusProcessorIT {
    private static final String DREP_ID = "drep1y0lpypv3vgrg6scz4emwl5u9z5aq3m5z4m2x5zn0gguvu6sv8yks9";
    private static final String DREP_HASH = "fe1205916206868c2b8e5df753851741d0ee82aed46a0a6f423f8e6b";
    private static final int EPOCH = 284;

    @Autowired
    private DRepStatusProcessor processor;

    @Autowired
    private DRepRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void aRetirementCommittedWithItsRegistrationKeepsThatRegistrationSlot() {
        // Committed earlier: first registration and retirement
        commit(block(121168694L, "tx1", CertificateType.REG_DREP_CERT),
                block(121168720L, "tx2", CertificateType.UNREG_DREP_CERT));

        // Committed together (range sync): registration again, then retirement
        commit(block(121180520L, "tx3", CertificateType.REG_DREP_CERT),
                block(121180555L, "tx4", CertificateType.UNREG_DREP_CERT));

        assertThat(row(121180555L).getStatus()).isEqualTo(DRepStatus.RETIRED);
        assertThat(row(121180555L).getRegistrationSlot()).isEqualTo(121180520L);
        assertThat(row(121168720L).getRegistrationSlot()).isEqualTo(121168694L);
    }

    @Test
    void anUpdateCommittedWithItsRegistrationKeepsThatRegistrationSlot() {
        commit(block(121168694L, "tx1", CertificateType.REG_DREP_CERT),
                block(121168720L, "tx2", CertificateType.UNREG_DREP_CERT));

        commit(block(121180520L, "tx3", CertificateType.REG_DREP_CERT),
                block(121180530L, "tx4", CertificateType.UPDATE_DREP_CERT));
        // A later commit copies the value from the update row
        commit(block(121190000L, "tx5", CertificateType.UNREG_DREP_CERT));

        assertThat(row(121180530L).getStatus()).isEqualTo(DRepStatus.UPDATED);
        assertThat(row(121180530L).getRegistrationSlot()).isEqualTo(121180520L);
        assertThat(row(121190000L).getRegistrationSlot()).isEqualTo(121180520L);
    }

    @Test
    void replayingARetirementAfterLaterRowsWereStoredKeepsItsRegistrationSlot() {
        commit(block(121168694L, "tx1", CertificateType.REG_DREP_CERT));
        commit(block(121168720L, "tx2", CertificateType.UNREG_DREP_CERT));
        commit(block(121180520L, "tx3", CertificateType.REG_DREP_CERT));
        var first = rows();

        // A restart processes the retirement block again while the later registration is stored
        commit(block(121168720L, "tx2", CertificateType.UNREG_DREP_CERT));

        assertThat(row(121168720L).getRegistrationSlot()).isEqualTo(121168694L);
        assertThat(rows()).usingRecursiveFieldByFieldElementComparatorIgnoringFields("updateDateTime")
                .isEqualTo(first);
    }

    @Test
    void aBlockCachedBeforeARollbackIsNotSavedOrUsed() {
        commit(block(121168694L, "tx1", CertificateType.REG_DREP_CERT));

        // A block is processed but its batch is not committed, then the store rolls back before it
        processor.processDRepRegistrationEvent(block(121180520L, "tx3", CertificateType.REG_DREP_CERT));
        processor.handleRollbackEvent(RollbackEvent.builder()
                .rollbackTo(new Point(121170000L, "block121170000"))
                .currentPoint(new Point(121180520L, "block121180520"))
                .build());

        commit(block(121180555L, "tx4", CertificateType.UNREG_DREP_CERT));

        assertThat(rows()).extracting(DRepEntity::getSlot).containsExactly(121168694L, 121180555L);
        assertThat(row(121180555L).getRegistrationSlot()).isEqualTo(121168694L);
    }

    private void commit(DRepRegistrationEvent... blocks) {
        for (var block : blocks)
            processor.processDRepRegistrationEvent(block);
        processor.handleCommitEventToProcessDRepStatus(new PreCommitEvent(blocks[blocks.length - 1].getMetadata()));
    }

    private DRepRegistrationEvent block(long slot, String txHash, CertificateType type) {
        var metadata = EventMetadata.builder()
                .slot(slot)
                .block(slot)
                .blockHash("block" + slot)
                .epochNumber(EPOCH)
                .build();
        var cert = DRepRegistration.builder()
                .txHash(txHash)
                .certIndex(0)
                .txIndex(0)
                .type(type)
                .slot(slot)
                .deposit(type == CertificateType.UNREG_DREP_CERT ? BigInteger.valueOf(-500_000_000)
                        : type == CertificateType.REG_DREP_CERT ? BigInteger.valueOf(500_000_000) : null)
                .drepHash(DREP_HASH)
                .drepId(DREP_ID)
                .epoch(EPOCH)
                .build();
        return new DRepRegistrationEvent(metadata, new ArrayList<>(List.of(cert)));
    }

    private DRepEntity row(long slot) {
        return rows().stream().filter(r -> r.getSlot() == slot).findFirst().orElseThrow();
    }

    private List<DRepEntity> rows() {
        return repository.findAll().stream()
                .sorted(Comparator.comparingLong(DRepEntity::getSlot))
                .toList();
    }
}
