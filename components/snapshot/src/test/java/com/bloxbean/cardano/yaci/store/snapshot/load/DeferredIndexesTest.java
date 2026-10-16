package com.bloxbean.cardano.yaci.store.snapshot.load;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
    void convertsDuckDbMemoryLimitsToPostgresSettings() {
        assertThat(DeferredIndexes.postgresMemory("3GB")).isEqualTo("3072MB");
        assertThat(DeferredIndexes.postgresMemory("512MiB")).isEqualTo("512MB");
        assertThat(DeferredIndexes.postgresMemory("1.5gb")).isEqualTo("1536MB");
        assertThat(DeferredIndexes.postgresMemory("100KB")).isNull();
        assertThat(DeferredIndexes.postgresMemory("lots")).isNull();
        assertThat(DeferredIndexes.postgresMemory(null)).isNull();
    }
}
