package com.bloxbean.cardano.yaci.store.blockfrost.block.storage.impl;

import com.bloxbean.cardano.yaci.store.blockfrost.block.storage.impl.model.BFBlockAddressTxRow;
import org.jooq.DSLContext;
import org.jooq.ExecuteContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.conf.StatementType;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultExecuteListener;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BFBlockAddressesTest {
    @Test
    void boundsOutputsByTheBlocksSlotAndKeepsOlderSpentOutputs() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID()
                + ";DATABASE_TO_LOWER=TRUE")) {
            List<String> statements = new ArrayList<>();
            DSLContext dsl = DSL.using(connection, SQLDialect.H2,
                    new Settings().withStatementType(StatementType.STATIC_STATEMENT));
            dsl.configuration().set(new DefaultExecuteListener() {
                @Override
                public void renderEnd(ExecuteContext context) {
                    statements.add(context.sql());
                }
            });
            dsl.execute("create table block (number bigint, slot bigint)");
            dsl.execute("create table address_utxo (owner_addr varchar, owner_addr_full varchar, "
                    + "tx_hash varchar, output_index int, block bigint, slot bigint)");
            dsl.execute("create table tx_input (tx_hash varchar, output_index int, "
                    + "spent_tx_hash varchar, spent_at_block bigint)");
            dsl.execute("create table transaction (tx_hash varchar, block bigint, tx_index int)");
            // Block height and absolute slot are different; spent outputs were created earlier.
            // Byron epoch boundary and main blocks can share a height but have different slots.
            dsl.execute("insert into block values (10, 499), (10, 500), (9, 400)");
            dsl.execute("insert into address_utxo values "
                    + "('alice', null, 'old', 0, 9, 400), "
                    + "('bob', null, 'new', 0, 10, 500), "
                    + "('bob', null, 'new', 1, 10, 500), "
                    + "('charlie', null, 'other', 0, 9, 400)");
            dsl.execute("insert into tx_input values ('old', 0, 'new', 10)");
            dsl.execute("insert into transaction values ('new', 10, 2), ('old', 9, 1)");

            statements.clear();
            var reader = new BFBlocksStorageReaderImpl(dsl);
            assertThat(reader.findBlockAddressTransactions(10, 0, 100)).containsExactly(
                    new BFBlockAddressTxRow("alice", "new", 2),
                    new BFBlockAddressTxRow("bob", "new", 2));
            assertThat(reader.findBlockAddressTransactions(10, 1, 1)).containsExactly(
                    new BFBlockAddressTxRow("bob", "new", 2));
            assertThat(reader.findBlockAddressTransactions(10, 2, 1)).isEmpty();
            // Keep an indexable slot bound even when no address_utxo(block) index exists.
            assertThat(statements.getFirst()).contains("\"output_utxo\".\"slot\" in (select");
        }
    }
}
