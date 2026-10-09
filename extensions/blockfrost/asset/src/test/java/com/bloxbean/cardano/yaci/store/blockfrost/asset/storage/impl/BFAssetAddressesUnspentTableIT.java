package com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl;

import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFAssetAddress;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * /assets/{asset}/addresses read from address_utxo_unspent must equal the anti-join it replaces,
 * page by page and in both of Blockfrost's (asymmetric) orders. PostgreSQL only, as the query is.
 */
@Testcontainers(disabledWithoutDocker = true)
class BFAssetAddressesUnspentTableIT {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16");

    private static final String UNIT = "aa".repeat(28) + "01";
    private static BFAssetStorageReaderImpl antiJoin;
    private static BFAssetStorageReaderImpl unspentTable;

    @BeforeAll
    static void setUp() {
        DriverManagerDataSource ds = new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        Flyway.configure().dataSource(ds)
                .locations("filesystem:../../../stores/utxo/src/main/resources/db/store/postgresql",
                        "filesystem:../../../stores/transaction/src/main/resources/db/store/postgresql")
                .load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(ds);

        // 12 holders over 40 outputs: some hold several outputs, some outputs are spent, slots tie.
        for (int i = 0; i < 40; i++) {
            String txHash = String.format("%064x", 100 + i / 2);
            int index = i % 2;
            long slot = 1000L + (i / 4) * 10;
            String address = "addr_test1q" + (i % 12);
            jdbc.update("insert into transaction (tx_hash, slot, tx_index, block) values (?, ?, ?, ?) on conflict do nothing",
                    txHash, slot, (i / 2) % 2, slot / 10);
            jdbc.update("""
                    insert into address_utxo (tx_hash, output_index, slot, owner_addr, amounts)
                    values (?, ?, ?, ?, cast(? as jsonb))""",
                    txHash, index, slot, address,
                    "[{\"unit\": \"lovelace\", \"quantity\": 1}, {\"unit\": \"" + UNIT + "\", \"quantity\": " + (i + 1) + "}]");
            if (i % 7 == 3) {
                jdbc.update("insert into tx_input (tx_hash, output_index, spent_at_slot) values (?, ?, ?)", txHash, index, 9999L);
            }
        }
        jdbc.update("""
                insert into address_utxo_unspent select a.* from address_utxo a
                where not exists (select 1 from tx_input i where i.tx_hash = a.tx_hash and i.output_index = a.output_index)""");

        DSLContext dsl = DSL.using(ds, SQLDialect.POSTGRES);
        antiJoin = new BFAssetStorageReaderImpl(dsl, false);
        unspentTable = new BFAssetStorageReaderImpl(dsl, true);
    }

    @Test
    void everyPageEqualsTheAntiJoinInBothOrders() {
        assertThat(antiJoin.findAssetAddresses(UNIT, 0, 100, Order.asc)).hasSize(12);
        for (Order order : Order.values()) {
            for (int count : List.of(1, 5, 100)) {
                for (int page = 0; page < 14; page++) {
                    List<BFAssetAddress> expected = antiJoin.findAssetAddresses(UNIT, page, count, order);
                    assertThat(unspentTable.findAssetAddresses(UNIT, page, count, order))
                            .as("order=%s count=%d page=%d", order, count, page)
                            .isEqualTo(expected);
                }
            }
        }
    }
}
