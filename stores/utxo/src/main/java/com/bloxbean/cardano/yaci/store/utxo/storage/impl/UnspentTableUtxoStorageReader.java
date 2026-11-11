package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressTransaction;
import com.bloxbean.cardano.yaci.store.utxo.domain.AssetTransaction;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import lombok.NonNull;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.SortField;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS_UTXO_UNSPENT;
import static org.jooq.impl.DSL.field;

/**
 * Serves the unspent-output queries from {@code address_utxo_unspent}, so an address's or an
 * asset's current outputs cost the unspent set, not every output the address or asset ever had.
 * Every other query goes to the wrapped reader. Same rows, order and pages as
 * {@link UtxoStorageReaderImpl}: slot, tx_hash, output_index, in the requested direction.
 *
 * <p>Active only with {@code store.utxo.unspent-table-read-enabled}, which should be turned on
 * only once the table is complete (maintained from the start of the sync, or backfilled and
 * reconciled).
 */
public class UnspentTableUtxoStorageReader implements UtxoStorageReader {

    private final UtxoStorageReader delegate;
    private final DSLContext dsl;

    public UnspentTableUtxoStorageReader(UtxoStorageReader delegate, DSLContext dsl) {
        this.delegate = delegate;
        this.dsl = dsl;
    }

    private static List<SelectFieldOrAsterisk> columns() {
        var fields = new ArrayList<SelectFieldOrAsterisk>(Arrays.asList(ADDRESS_UTXO_UNSPENT.fields()));
        //Workaround as in UtxoStorageReaderImpl: AddressUtxo.blockNumber vs the block column
        fields.add(ADDRESS_UTXO_UNSPENT.BLOCK.as("blockNumber"));
        return fields;
    }

    private static SortField<?>[] sort(Order order) {
        if (order.equals(Order.desc)) {
            return new SortField<?>[]{ADDRESS_UTXO_UNSPENT.SLOT.desc(), ADDRESS_UTXO_UNSPENT.TX_HASH.desc(), ADDRESS_UTXO_UNSPENT.OUTPUT_INDEX.desc()};
        }
        return new SortField<?>[]{ADDRESS_UTXO_UNSPENT.SLOT.asc(), ADDRESS_UTXO_UNSPENT.TX_HASH.asc(), ADDRESS_UTXO_UNSPENT.OUTPUT_INDEX.asc()};
    }

    private Condition unit(String unit) {
        if (dsl.dialect().family() == SQLDialect.POSTGRES) {
            return DSL.condition("{0} @> {1}::jsonb", ADDRESS_UTXO_UNSPENT.AMOUNTS, DSL.val("[{\"unit\": \"" + unit + "\"}]"));
        }
        return field(ADDRESS_UTXO_UNSPENT.AMOUNTS).cast(String.class).contains("\"unit\": \"" + unit + "\"")
                .or(field(ADDRESS_UTXO_UNSPENT.AMOUNTS).cast(String.class).contains("\"unit\":\"" + unit + "\""));
    }

    private List<AddressUtxo> page(Condition where, int page, int count, Order order) {
        return dsl.select(columns())
                .from(ADDRESS_UTXO_UNSPENT)
                .where(where)
                .orderBy(sort(order))
                .offset((long) page * count)
                .limit(count)
                .fetch()
                .into(AddressUtxo.class);
    }

    @Override
    public List<AddressUtxo> findUtxoByAddress(@NonNull String address, int page, int count, Order order) {
        return page(ADDRESS_UTXO_UNSPENT.OWNER_ADDR.eq(address), page, count, order);
    }

    @Override
    public List<AddressUtxo> findUtxosByAsset(String unit, int page, int count, Order order) {
        return page(unit(unit), page, count, order);
    }

    @Override
    public List<AddressUtxo> findUtxoByAddressAndAsset(String ownerAddress, String unit, int page, int count, Order order) {
        return page(ADDRESS_UTXO_UNSPENT.OWNER_ADDR.eq(ownerAddress).and(unit(unit)), page, count, order);
    }

    @Override
    public List<AddressUtxo> findUtxoByPaymentCredential(@NonNull String paymentCredential, int page, int count, Order order) {
        return page(ADDRESS_UTXO_UNSPENT.OWNER_PAYMENT_CREDENTIAL.eq(paymentCredential), page, count, order);
    }

    @Override
    public List<AddressUtxo> findUtxoByPaymentCredentialAndAsset(String paymentCredential, String unit, int page, int count, Order order) {
        return page(ADDRESS_UTXO_UNSPENT.OWNER_PAYMENT_CREDENTIAL.eq(paymentCredential).and(unit(unit)), page, count, order);
    }

    @Override
    public List<AddressUtxo> findUtxoByStakeAddress(@NonNull String stakeAddress, int page, int count, Order order) {
        return page(ADDRESS_UTXO_UNSPENT.OWNER_STAKE_ADDR.eq(stakeAddress), page, count, order);
    }

    @Override
    public List<AddressUtxo> findUtxoByStakeAddressAndAsset(@NonNull String stakeAddress, String unit, int page, int count, Order order) {
        return page(ADDRESS_UTXO_UNSPENT.OWNER_STAKE_ADDR.eq(stakeAddress.trim()).and(unit(unit)), page, count, order);
    }

    @Override
    public Optional<AddressUtxo> findById(String txHash, int outputIndex) {
        return delegate.findById(txHash, outputIndex);
    }

    @Override
    public List<AddressUtxo> findAllByIds(List<UtxoKey> utxoKeys) {
        return delegate.findAllByIds(utxoKeys);
    }

    @Override
    public List<AddressTransaction> findTransactionsByAddress(String address, int page, int count, Order order) {
        return delegate.findTransactionsByAddress(address, page, count, order);
    }

    @Override
    public List<AssetTransaction> findTransactionsByAsset(String unit, int page, int count, Order order) {
        return delegate.findTransactionsByAsset(unit, page, count, order);
    }
}
