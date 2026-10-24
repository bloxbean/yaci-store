package com.bloxbean.cardano.yaci.store.blockfrost.address.storage.impl;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
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

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The address endpoints' current-output reads from address_utxo_unspent must equal the anti-join they replace:
 * /addresses/{address}/utxos and /utxos/{asset} page by page in both orders, and the unspent balance of
 * /addresses/{address}. Includes a Byron-length address, matched on owner_addr_full.
 */
@Testcontainers(disabledWithoutDocker = true)
class BFAddressUtxosUnspentTableIT {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16");

    private static final String UNIT = "aa".repeat(28) + "01";
    private static final String OTHER_UNIT = "bb".repeat(28) + "02";
    private static final String LONG_ADDRESS = "DdzFF" + "z".repeat(600);
    private static final List<String> ADDRESSES = List.of("addr_test1qa", "addr_test1qb", LONG_ADDRESS);

    private static BFAddressStorageReaderImpl antiJoin;
    private static BFAddressStorageReaderImpl unspentTable;

    @BeforeAll
    static void setUp() {
        DriverManagerDataSource ds = new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        Flyway.configure().dataSource(ds)
                .locations("filesystem:../../../stores/utxo/src/main/resources/db/store/postgresql")
                .load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(ds);

        // 60 outputs over 3 addresses: every third holds UNIT, every fifth OTHER_UNIT, some are spent, slots tie.
        for (int i = 0; i < 60; i++) {
            String txHash = String.format("%064x", 100 + i / 3);
            int index = i % 3;
            long slot = 1000L + (i / 6) * 10;
            String address = ADDRESSES.get(i % 3);
            String amounts = "[{\"unit\": \"lovelace\", \"quantity\": " + (1_000_000 + i) + "}"
                    + (i % 3 == 0 ? ", {\"unit\": \"" + UNIT + "\", \"quantity\": " + (i + 1) + "}" : "")
                    + (i % 5 == 0 ? ", {\"unit\": \"" + OTHER_UNIT + "\", \"quantity\": 7}" : "")
                    + "]";
            jdbc.update("""
                    insert into address_utxo (tx_hash, output_index, slot, block, owner_addr, owner_addr_full,
                                              lovelace_amount, amounts)
                    values (?, ?, ?, ?, ?, ?, ?, cast(? as jsonb))""",
                    txHash, index, slot, slot / 10, address.substring(0, Math.min(500, address.length())),
                    address.length() > 500 ? address : null, 1_000_000L + i, amounts);
            if (i % 7 == 2) {
                jdbc.update("insert into tx_input (tx_hash, output_index, spent_at_slot) values (?, ?, ?)", txHash, index, 9999L);
            }
        }
        jdbc.update("""
                insert into address_utxo_unspent select a.* from address_utxo a
                where not exists (select 1 from tx_input i where i.tx_hash = a.tx_hash and i.output_index = a.output_index)""");

        DSLContext dsl = DSL.using(ds, SQLDialect.POSTGRES);
        antiJoin = new BFAddressStorageReaderImpl(dsl, false);
        unspentTable = new BFAddressStorageReaderImpl(dsl, true);
    }

    @Test
    void everyUtxoPageEqualsTheAntiJoinInBothOrders() {
        for (String address : ADDRESSES) {
            assertThat(antiJoin.findAddressUtxos(address, 0, 100, Order.asc)).isNotEmpty();
            for (Order order : Order.values()) {
                for (int count : List.of(1, 4, 100)) {
                    for (int page = 0; page < 20; page++) {
                        List<AddressUtxo> expected = antiJoin.findAddressUtxos(address, page, count, order);
                        assertThat(unspentTable.findAddressUtxos(address, page, count, order))
                                .as("address=%s order=%s count=%d page=%d", address.substring(0, 12), order, count, page)
                                .usingRecursiveFieldByFieldElementComparator()
                                .isEqualTo(expected);
                    }
                }
            }
        }
    }

    @Test
    void everyAssetUtxoPageEqualsTheAntiJoinInBothOrders() {
        for (String address : ADDRESSES) {
            for (String unit : List.of(UNIT, OTHER_UNIT)) {
                for (Order order : Order.values()) {
                    for (int page = 0; page < 8; page++) {
                        List<AddressUtxo> expected = antiJoin.findAddressUtxosForAsset(address, unit, page, 2, order);
                        assertThat(unspentTable.findAddressUtxosForAsset(address, unit, page, 2, order))
                                .as("address=%s unit=%s order=%s page=%d", address.substring(0, 12), unit.substring(0, 4), order, page)
                                .usingRecursiveFieldByFieldElementComparator()
                                .isEqualTo(expected);
                    }
                }
            }
        }
    }

    @Test
    void theUnspentBalanceEqualsTheAntiJoin() {
        for (String address : ADDRESSES) {
            Map<String, BigInteger> expected = antiJoin.findUnspentAddressBalanceByUnit(address);
            assertThat(expected).containsKey("lovelace");
            assertThat(unspentTable.findUnspentAddressBalanceByUnit(address)).isEqualTo(expected);
        }
    }
}
