package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.domain.AssetTransaction;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UtxoStorageReaderImplTest {

    @ParameterizedTest
    @EnumSource(Order.class)
    void assetHistoryPagesFollowTransactionOrderWithoutRepeats(Order order) throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:asset_history;DATABASE_TO_LOWER=TRUE")) {
            var dsl = DSL.using(connection, SQLDialect.H2);
            dsl.execute("""
                    create table address_utxo (
                        tx_hash varchar(64), output_index int, slot bigint, block bigint,
                        block_time bigint, tx_index int, amounts varchar(255),
                        primary key (tx_hash, output_index)
                    )
                    """);
            var expected = new ArrayList<String>();
            for (int i = 0; i < 600; i++) {
                // Hash order is deliberately opposite to transaction position within each slot.
                String hash = "%064x".formatted(600 - i);
                expected.add(hash);
                for (int output = 0; output < 2; output++) {
                    dsl.execute("insert into address_utxo values (?, ?, ?, ?, ?, ?, ?)",
                            hash, output, i / 7, i / 7, 1000 + i / 7, i % 7, "[{\"unit\":\"test\"}]");
                }
            }
            if (order == Order.desc)
                Collections.reverse(expected);

            var reader = new UtxoStorageReaderImpl(null, dsl);
            assertPages(reader, order, expected);

            // Older indexed data has no transaction position. Hashes still make pagination unique.
            dsl.execute("update address_utxo set tx_index = null");
            List<String> legacyExpected = dsl.fetch("select tx_hash from address_utxo group by tx_hash, slot "
                            + "order by slot " + order.name() + ", tx_hash " + order.name())
                    .getValues(0, String.class);
            assertPages(reader, order, legacyExpected);
        }
    }

    private void assertPages(UtxoStorageReaderImpl reader, Order order, List<String> expected) {
        var actual = new ArrayList<String>();
        for (int page = 0; page < 6; page++) {
            var rows = reader.findTransactionsByAsset("test", page, 100, order);
            assertThat(rows).hasSize(100);
            actual.addAll(rows.stream().map(AssetTransaction::getTxHash).toList());
        }
        assertThat(actual).doesNotHaveDuplicates().containsExactlyElementsOf(expected);
        assertThat(reader.findTransactionsByAsset("test", 6, 100, order)).isEmpty();
        assertThat(reader.findTransactionsByAsset("absent", 0, 100, order)).isEmpty();
    }
}
