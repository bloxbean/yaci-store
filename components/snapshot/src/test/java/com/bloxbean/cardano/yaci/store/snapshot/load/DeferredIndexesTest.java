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
}
