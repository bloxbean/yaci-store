package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OwnedPartitionTest {
    private final SnapshotSpecRegistry registry = SnapshotSpecRegistry.builtIn();

    @Test
    void aBatchOfOneEpochFillsItsOwnTargetPartition() {
        assertThat(TableLoader.ownedPartition(batch("epoch-stake", "main/epoch_stake/epoch=455")))
                .isEqualTo("epoch_stake_p455");
    }

    @Test
    void aSingleBatchOverEveryEpochOwnsNoPartition() {
        assertThat(TableLoader.ownedPartition(batch("drep-dist", "main/drep_dist/epoch=600"))).isNull();
    }

    @Test
    void rewardIsPartitionedByAnotherEpochThanItsSource() {
        // Source partitions are earned epochs, target partitions spendable epochs.
        assertThat(TableLoader.ownedPartition(batch("reward", "main/reward/epoch=455"))).isNull();
    }

    @Test
    void onlyASingleWholeSourcePartitionOwnsItsTarget() {
        assertThat(TableLoader.ownedPartition(batch("epoch-stake", "epoch=1,epoch=2"))).isNull();
        assertThat(TableLoader.ownedPartition(batch("epoch-stake", null))).isNull();
        assertThat(TableLoader.ownedPartition(batch("block", "main/block/date=2026-01-01"))).isNull();
    }

    private ImportBatch batch(String specId, String partition) {
        return new ImportBatch("b", registry.byId(specId).orElseThrow(), List.of(), partition);
    }
}
