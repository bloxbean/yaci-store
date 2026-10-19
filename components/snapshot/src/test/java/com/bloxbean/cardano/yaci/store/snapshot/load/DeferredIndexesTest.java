package com.bloxbean.cardano.yaci.store.snapshot.load;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeferredIndexesTest {

    @Test
    void rebuildsAPartitionedIndexOnEveryPartition() {
        var parent = new DeferredIndexes.Entry("stake", "idx_stake_address", DeferredIndexes.INDEX, false,
                "CREATE INDEX idx_stake_address ON ONLY s.stake USING btree (address)", List.of(), false);
        assertThat(parent.createSql("s")).isEqualTo("CREATE INDEX idx_stake_address ON s.stake USING btree (address)");

        var plain = new DeferredIndexes.Entry("utxo", "idx_only", DeferredIndexes.INDEX, false,
                "CREATE INDEX idx_only ON s.utxo USING btree (slot) WHERE (note <> ' ON ONLY ')", List.of(), false);
        assertThat(plain.createSql("s")).isEqualTo(plain.definition());

        var quoted = new DeferredIndexes.Entry("stake", "Odd Name", DeferredIndexes.INDEX, true,
                "CREATE UNIQUE INDEX \"Odd Name\" ON ONLY s.stake USING btree (address)", List.of(), false);
        assertThat(quoted.createSql("s")).isEqualTo("CREATE UNIQUE INDEX \"Odd Name\" ON s.stake USING btree (address)");
    }

    @Test
    void constraintsAreAddedAndDroppedThroughTheirTable() {
        var key = new DeferredIndexes.Entry("utxo", "utxo_pkey", DeferredIndexes.CONSTRAINT, true,
                "PRIMARY KEY (tx_hash, output_index)", List.of("tx_hash", "output_index"), false);
        assertThat(key.createSql("s")).isEqualTo("ALTER TABLE \"s\".\"utxo\" ADD CONSTRAINT \"utxo_pkey\" PRIMARY KEY (tx_hash, output_index)");
        assertThat(key.dropSql("s")).isEqualTo("ALTER TABLE \"s\".\"utxo\" DROP CONSTRAINT IF EXISTS \"utxo_pkey\"");
    }

    @Test
    void buildsAPartitionsCopyUnnamedSoPostgresNamesItAsTheParentWould() {
        var index = new DeferredIndexes.Entry("stake", "Odd Name", DeferredIndexes.INDEX, true,
                "CREATE UNIQUE INDEX \"Odd Name\" ON ONLY \"my schema\".stake USING btree (address) WHERE (amount > 0)",
                List.of(), false);
        assertThat(index.partitionSql("s", "stake_1"))
                .isEqualTo("CREATE UNIQUE INDEX ON \"s\".\"stake_1\" USING btree (address) WHERE (amount > 0)");
        assertThat(DeferredIndexes.indexBody("CREATE UNIQUE INDEX stake_1_address_idx ON s.stake_1 USING btree (address)"
                + " WHERE (amount > 0)")).isEqualTo(DeferredIndexes.indexBody(index.definition()));

        var key = new DeferredIndexes.Entry("stake", "stake_pkey", DeferredIndexes.CONSTRAINT, true,
                "PRIMARY KEY (epoch, address)", List.of("epoch", "address"), false);
        assertThat(key.partitionSql("s", "stake_1")).isEqualTo("ALTER TABLE \"s\".\"stake_1\" ADD PRIMARY KEY (epoch, address)");
        var unique = new DeferredIndexes.Entry("stake", "stake_key", DeferredIndexes.CONSTRAINT, true,
                "UNIQUE NULLS NOT DISTINCT (address)", List.of(), false);
        assertThat(unique.partitionSql("s", "stake_1")).isEqualTo("ALTER TABLE \"s\".\"stake_1\" ADD UNIQUE NULLS NOT DISTINCT (address)");

        // Anything else is built from the parent, as before.
        var exclusion = new DeferredIndexes.Entry("stake", "stake_excl", DeferredIndexes.CONSTRAINT, true,
                "EXCLUDE USING gist (epoch WITH =)", List.of(), false);
        assertThat(exclusion.partitionSql("s", "stake_1")).isNull();
    }

    @Test
    void convertsDuckDbMemoryLimitsToPostgresSettings() {
        assertThat(DeferredIndexes.postgresMemory("3GB")).isEqualTo("3072MB");
        assertThat(DeferredIndexes.postgresMemory("512MiB")).isEqualTo("512MB");
        assertThat(DeferredIndexes.postgresMemory("1.5gb")).isEqualTo("1536MB");
        assertThat(DeferredIndexes.postgresMemory("100KB")).isNull();
        assertThat(DeferredIndexes.postgresMemory("lots")).isNull();
        assertThat(DeferredIndexes.postgresMemory(null)).isNull();
    }

    @Test
    void handsOutTheHeaviestTaskThatCanStartAndReadiesTheRestAsTheirPrerequisitesFinish() throws Exception {
        CountDownLatch txKey = new CountDownLatch(1);
        CountDownLatch rewardCopies = new CountDownLatch(2);
        var txPkey = task("transaction", null, List.of(), List.of(txKey), 60, 0);
        var reward1 = task("reward", "reward_1", List.of(), List.of(rewardCopies), 2, 1);
        var reward2 = task("reward", "reward_2", List.of(), List.of(rewardCopies), 1, 2);
        var rewardPkey = task("reward", null, List.of(rewardCopies), List.of(), Long.MAX_VALUE, 3);
        var txSlot = task("transaction", null, List.of(txKey), List.of(), 30, 4);
        var scheduler = new DeferredIndexes.Scheduler(List.of(txPkey, reward1, reward2, rewardPkey, txSlot));

        assertThat(scheduler.take()).isSameAs(txPkey);
        assertThat(scheduler.take()).isSameAs(reward1);
        scheduler.finished(txPkey);
        // The transaction index can start now and reads more than the reward partition left.
        assertThat(scheduler.take()).isSameAs(txSlot);
        assertThat(scheduler.take()).isSameAs(reward2);
        scheduler.finished(reward1);
        scheduler.finished(reward2);
        // The parent's key only attaches its partitions' copies, so it goes next.
        assertThat(scheduler.take()).isSameAs(rewardPkey);
        scheduler.finished(txSlot);
        scheduler.finished(rewardPkey);
        assertThat(scheduler.take()).isNull();
    }

    @Test
    void aConnectionWithNothingReadyWaitsInsteadOfTakingATaskItCannotStart() throws Exception {
        CountDownLatch key = new CountDownLatch(1);
        var pkey = task("utxo", null, List.of(), List.of(key), 10, 0);
        var index = task("utxo", null, List.of(key), List.of(), 5, 1);
        var scheduler = new DeferredIndexes.Scheduler(List.of(pkey, index));
        assertThat(scheduler.take()).isSameAs(pkey);

        var taken = new java.util.concurrent.CompletableFuture<DeferredIndexes.Task>();
        Thread second = Thread.ofPlatform().start(() -> {
            try {
                taken.complete(scheduler.take());
            } catch (InterruptedException e) {
                taken.completeExceptionally(e);
            }
        });
        Thread.sleep(100);
        assertThat(taken).isNotDone();
        scheduler.finished(pkey);
        assertThat(taken.get(5, java.util.concurrent.TimeUnit.SECONDS)).isSameAs(index);
        second.join();
    }

    @Test
    void afterAFailureNothingMoreIsHandedOutAndWhateverWaitedIsReleased() throws Exception {
        CountDownLatch key = new CountDownLatch(1);
        CountDownLatch indexDone = new CountDownLatch(1);
        var pkey = task("utxo", null, List.of(), List.of(key), 10, 0);
        var index = task("utxo", null, List.of(key), List.of(indexDone), 5, 1);
        var other = task("block", null, List.of(), List.of(), 1, 2);
        var scheduler = new DeferredIndexes.Scheduler(List.of(pkey, index, other));
        assertThat(scheduler.take()).isSameAs(pkey);

        scheduler.stop();
        scheduler.finished(pkey);

        assertThat(scheduler.take()).isNull();
        assertThat(indexDone.getCount()).isZero();
    }

    @Test
    void aTaskWhosePrerequisiteNeverRunsFailsInsteadOfHanging() {
        var orphan = task("utxo", null, List.of(new CountDownLatch(1)), List.of(), 5, 0);
        var scheduler = new DeferredIndexes.Scheduler(List.of(orphan));

        assertThatThrownBy(scheduler::take).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("index idx on utxo");
    }

    private static DeferredIndexes.Task task(String table, String partition, List<CountDownLatch> after,
                                             List<CountDownLatch> done, long weight, int order) {
        var entry = new DeferredIndexes.Entry(table, "idx", DeferredIndexes.INDEX, false,
                "CREATE INDEX idx ON s." + table + " USING btree (slot)", List.of(), false);
        return new DeferredIndexes.Task(table, partition, List.of(entry), after, done, weight, order);
    }
}
