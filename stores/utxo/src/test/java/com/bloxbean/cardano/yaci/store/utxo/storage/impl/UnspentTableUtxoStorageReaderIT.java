package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link UnspentTableUtxoStorageReader} must answer every unspent query exactly as
 * {@link UtxoStorageReaderImpl} does (same rows, same order, same pages), reading
 * address_utxo_unspent instead of anti-joining address_utxo with tx_input.
 */
@SpringBootTest
class UnspentTableUtxoStorageReaderIT {

    private static final String ADDR_A = "addr_test1qaaaa";
    private static final String ADDR_B = "addr_test1qbbbb";
    private static final String UNIT_X = "0254a6ffa78edb03ea8933dbd4ca078758dbfc0fc6bb0d28b7a9c89f4c454e4649";
    private static final String UNIT_Y = "9954a6ffa78edb03ea8933dbd4ca078758dbfc0fc6bb0d28b7a9c89f4c454e4649";

    @Autowired
    private UtxoStorage utxoStorage;

    @Autowired
    private UtxoRepository utxoRepository;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private UnspentUtxoTableService service;

    @Autowired
    private JdbcTemplate jdbc;

    private UtxoStorageReaderImpl antiJoin;
    private UnspentTableUtxoStorageReader table;

    @BeforeEach
    void seed() {
        clean();   // the H2 database is shared with the module's other tests
        antiJoin = new UtxoStorageReaderImpl(utxoRepository, dsl);
        table = new UnspentTableUtxoStorageReader(antiJoin, dsl);

        List<AddressUtxo> outputs = new ArrayList<>();
        List<TxInput> spends = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            String txHash = String.format("%064x", 7000 + i / 3);   // three outputs per transaction
            int index = i % 3;
            long slot = 100L + (i / 6) * 10;                       // two transactions per slot
            String addr = i % 2 == 0 ? ADDR_A : ADDR_B;
            List<Amt> amounts = new ArrayList<>(List.of(Amt.builder().unit("lovelace").quantity(BigInteger.valueOf(1_000_000L + i)).build()));
            if (i % 3 != 2) amounts.add(Amt.builder().unit(UNIT_X).quantity(BigInteger.valueOf(i + 1)).build());
            if (i % 4 == 0) amounts.add(Amt.builder().unit(UNIT_Y).quantity(BigInteger.valueOf(5)).build());
            outputs.add(AddressUtxo.builder()
                    .txHash(txHash).outputIndex(index).slot(slot).blockNumber(slot / 10).blockTime(1_600_000L + slot)
                    .blockHash("bh" + slot).epoch(1).txIndex(i / 3)
                    .ownerAddr(addr).ownerStakeAddr("stake_test1" + addr.substring(10))
                    .ownerPaymentCredential("pc" + addr.substring(10)).ownerStakeCredential("sc" + addr.substring(10))
                    .lovelaceAmount(BigInteger.valueOf(1_000_000L + i)).amounts(amounts).isCollateralReturn(false)
                    .build());
            if (i % 5 == 1) {
                spends.add(TxInput.builder().txHash(txHash).outputIndex(index).spentAtSlot(999L).spentAtBlock(99L).spentTxHash("s").build());
            }
        }
        utxoStorage.saveUnspent(outputs);
        utxoStorage.saveSpent(spends);
        service.applyCommit(0, Long.MAX_VALUE);
    }

    @AfterEach
    void clean() {
        jdbc.update("delete from address_utxo_unspent");
        jdbc.update("delete from address_utxo");
        jdbc.update("delete from tx_input");
    }

    /** Every page of {@code read}, in both orders, from both readers. */
    private void samePages(String what, PageReader read) {
        assertThat(read.page(antiJoin, 0, 50, Order.asc)).as("%s has unspent rows to compare", what).isNotEmpty();
        for (Order order : Order.values()) {
            for (int count : List.of(1, 4, 50)) {
                for (int page = 0; page < 8; page++) {
                    List<String> expected = keys(read.page(antiJoin, page, count, order));
                    assertThat(keys(read.page(table, page, count, order)))
                            .as("%s order=%s count=%d page=%d", what, order, count, page)
                            .isEqualTo(expected);
                }
            }
        }
    }

    private static List<String> keys(List<AddressUtxo> utxos) {
        return utxos.stream().map(u -> u.getTxHash() + "#" + u.getOutputIndex() + "@" + u.getSlot()
                + " " + u.getOwnerAddr() + " " + u.getLovelaceAmount() + " " + u.getAmounts()).toList();
    }

    @FunctionalInterface
    interface PageReader {
        List<AddressUtxo> page(com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader reader, int page, int count, Order order);
    }

    @Test
    void everyUnspentQueryAnswersAsTheAntiJoinDoes() {
        samePages("address", (r, p, c, o) -> r.findUtxoByAddress(ADDR_A, p, c, o));
        samePages("asset", (r, p, c, o) -> r.findUtxosByAsset(UNIT_X, p, c, o));
        samePages("address+asset", (r, p, c, o) -> r.findUtxoByAddressAndAsset(ADDR_B, UNIT_X, p, c, o));
        samePages("payment credential", (r, p, c, o) -> r.findUtxoByPaymentCredential("pcqbbbb", p, c, o));
        samePages("payment credential+asset", (r, p, c, o) -> r.findUtxoByPaymentCredentialAndAsset("pcqaaaa", UNIT_Y, p, c, o));
        samePages("stake address", (r, p, c, o) -> r.findUtxoByStakeAddress("stake_test1qaaaa", p, c, o));
        samePages("stake address+asset", (r, p, c, o) -> r.findUtxoByStakeAddressAndAsset("stake_test1qbbbb", UNIT_X, p, c, o));
    }

    @Test
    void theRowMapsToTheSameAddressUtxoAsTheAntiJoin() {
        Function<com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader, AddressUtxo> first =
                r -> r.findUtxoByAddressAndAsset(ADDR_A, UNIT_Y, 0, 1, Order.asc).get(0);
        AddressUtxo expected = first.apply(antiJoin);
        AddressUtxo got = first.apply(table);
        assertThat(got).usingRecursiveComparison().ignoringFields("updateDateTime").isEqualTo(expected);
    }
}
