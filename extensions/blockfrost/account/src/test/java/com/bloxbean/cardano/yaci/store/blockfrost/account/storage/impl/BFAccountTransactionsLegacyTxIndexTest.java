package com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl;

import com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl.model.AccountTransaction;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.conf.StatementType;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code address_utxo.tx_index} is null on rows written before it was stored; the index then has to
 * come from {@code transaction} to order the outputs a transaction spent.
 */
class BFAccountTransactionsLegacyTxIndexTest {

    private static final String STAKE = "stake_test1legacy";
    private static final String ADDR_A = "addr_test1a";
    private static final String ADDR_B = "addr_test1b";
    private static final String TX_1 = "tx1";
    private static final String TX_2 = "tx2";
    private static final String SPEND_TX = "spend";

    private Connection conn;
    private DSLContext dsl;

    @BeforeEach
    void setUp() throws Exception {
        conn = DriverManager.getConnection("jdbc:h2:mem:bf_account_legacy;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        // H2 returns no rows for this query with bind values; inlined values run it as written.
        dsl = DSL.using(conn, SQLDialect.H2, new Settings().withStatementType(StatementType.STATIC_STATEMENT));
        dsl.execute("create table address_utxo (tx_hash varchar(64), output_index int, owner_addr varchar(255), "
                + "owner_stake_addr varchar(255), block bigint, tx_index int, block_time bigint)");
        dsl.execute("create table tx_input (tx_hash varchar(64), output_index int, spent_tx_hash varchar(64), "
                + "spent_at_block bigint, spent_block_time bigint)");
        dsl.execute("create table transaction (tx_hash varchar(64), tx_index int)");

        // A comes first on chain (block 10, tx 1, output 5), B second (block 10, tx 2, output 0);
        // both rows lack tx_index, the transaction table has it.
        dsl.execute("insert into address_utxo values (?, 5, ?, ?, 10, null, 1000)", TX_1, ADDR_A, STAKE);
        dsl.execute("insert into address_utxo values (?, 0, ?, ?, 10, null, 1000)", TX_2, ADDR_B, STAKE);
        dsl.execute("insert into transaction values (?, 1), (?, 2), (?, 0)", TX_1, TX_2, SPEND_TX);
        dsl.execute("insert into tx_input values (?, 5, ?, 11, 1100), (?, 0, ?, 11, 1100)",
                TX_1, SPEND_TX, TX_2, SPEND_TX);
    }

    @AfterEach
    void tearDown() throws Exception {
        dsl.execute("drop all objects");
        conn.close();
    }

    @Test
    void findTransactions_legacyNullTxIndex_ordersSpentOutputsByOriginalTxIndex() {
        assertThat(spendAddresses(Order.asc)).containsExactly(ADDR_A, ADDR_B);
        assertThat(spendAddresses(Order.desc)).containsExactly(ADDR_B, ADDR_A);
    }

    private List<String> spendAddresses(Order order) {
        var reader = new BFAccountStorageReaderImpl(dsl, UtxoStoreProperties.builder().build());
        return reader.findTransactions(STAKE, 0, 10, order, null, null).stream()
                .filter(tx -> tx.txHash().equals(SPEND_TX))
                .map(AccountTransaction::address)
                .toList();
    }
}
