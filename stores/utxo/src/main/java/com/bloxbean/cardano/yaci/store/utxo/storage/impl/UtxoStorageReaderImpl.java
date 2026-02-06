package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressTransaction;
import com.bloxbean.cardano.yaci.store.utxo.domain.AssetTransaction;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.mapper.UtxoMapper;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.model.UtxoId;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS_UTXO;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.TX_INPUT;
import static org.jooq.impl.DSL.field;

@Slf4j
@RequiredArgsConstructor
public class UtxoStorageReaderImpl implements UtxoStorageReader {

    private final UtxoRepository utxoRepository;
    private final DSLContext dsl;
    private final UtxoMapper mapper = UtxoMapper.INSTANCE;

    @Override
    public Optional<AddressUtxo> findById(String txHash, int outputIndex) {
        return utxoRepository.findById(new UtxoId(txHash, outputIndex))
                .map(mapper::toAddressUtxo);
    }

    @Override
    public List<AddressUtxo> findUtxoByAddress(@NonNull String address, int page, int count, Order order) {
        Pageable pageable = getPageable(page, count, order);

        return utxoRepository.findUnspentByOwnerAddr(address, pageable)
                .stream()
                .flatMap(addressUtxoEntities -> addressUtxoEntities.stream().map(mapper::toAddressUtxo))
                .toList();
    }

    @Override
    public List<AddressUtxo> findUtxosByAsset(String unit, int page, int count, Order order) {
        Pageable pageable = getPageable(page, count, order);

        Condition unitCondition = getUnitCondition(unit);

        var fields = new ArrayList<>(Arrays.asList(ADDRESS_UTXO.fields()));
        fields.add(ADDRESS_UTXO.BLOCK.as("blockNumber")); //Workaround : due to mismatch field name in AddressUtxo.blockNumber and JOOQ model (block)

        var query = dsl
                .select(fields)
                .from(ADDRESS_UTXO)
                .leftJoin(TX_INPUT)
                .using(field(ADDRESS_UTXO.TX_HASH), field(ADDRESS_UTXO.OUTPUT_INDEX))
                .where(unitCondition)
                .and(TX_INPUT.TX_HASH.isNull())
                .orderBy(order.equals(Order.desc) ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc())  //TODO: Ordering
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        return query.fetch().into(AddressUtxo.class);
    }

    @Override
    public List<AddressUtxo> findUtxoByAddressAndAsset(String address, String unit, int page, int count, Order order) {
        Pageable pageable = getPageable(page, count, order);

        Condition unitCondition = getUnitCondition(unit);

        var fields = new ArrayList<>(Arrays.asList(ADDRESS_UTXO.fields()));
        fields.add(ADDRESS_UTXO.BLOCK.as("blockNumber")); //Workaround : due to mismatch field name in AddressUtxo.blockNumber and JOOQ model (block)

        var query = dsl
                .select(fields)
                .from(ADDRESS_UTXO)
                .leftJoin(TX_INPUT)
                .using(field(ADDRESS_UTXO.TX_HASH), field(ADDRESS_UTXO.OUTPUT_INDEX))
                .where(ADDRESS_UTXO.OWNER_ADDR.eq(address))
                .and(TX_INPUT.TX_HASH.isNull())
                .and(unitCondition)
                .orderBy(order.equals(Order.desc) ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc())  //TODO: Ordering
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        return query.fetch().into(AddressUtxo.class);
    }

    @Override
    public List<AddressUtxo> findUtxoByPaymentCredential(@NonNull String paymentCredential, int page, int count, Order order) {
        Pageable pageable = getPageable(page, count, order);

        return utxoRepository.findUnspentByOwnerPaymentCredential(paymentCredential, pageable)
                .stream()
                .flatMap(addressUtxoEntities -> addressUtxoEntities.stream().map(mapper::toAddressUtxo))
                .toList();
    }

    @Override
    public List<AddressUtxo> findUtxoByPaymentCredentialAndAsset(String paymentCredential, String unit, int page, int count, Order order) {
        Pageable pageable = getPageable(page, count, order);

        Condition unitCondition = getUnitCondition(unit);

        var fields = new ArrayList<>(Arrays.asList(ADDRESS_UTXO.fields()));
        fields.add(ADDRESS_UTXO.BLOCK.as("blockNumber")); //Workaround : due to mismatch field name in AddressUtxo.blockNumber and JOOQ model (block)

        var query = dsl
                .select(fields)
                .from(ADDRESS_UTXO)
                .leftJoin(TX_INPUT)
                .using(field(ADDRESS_UTXO.TX_HASH), field(ADDRESS_UTXO.OUTPUT_INDEX))
                .where(ADDRESS_UTXO.OWNER_PAYMENT_CREDENTIAL.eq(paymentCredential))
                .and(TX_INPUT.TX_HASH.isNull())
                .and(unitCondition)
                .orderBy(order.equals(Order.desc) ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc()) //TODO ordering
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        return query.fetch().into(AddressUtxo.class);
    }

    @Override
    public List<AddressUtxo> findUtxoByStakeAddress(@NonNull String stakeAddress, int page, int count, Order order) {
        Pageable pageable = getPageable(page, count, order);

        return utxoRepository.findUnspentByOwnerStakeAddr(stakeAddress, pageable)
                .stream()
                .flatMap(addressUtxoEntities -> addressUtxoEntities.stream().map(mapper::toAddressUtxo))
                .toList();
    }

    @Override
    public List<AddressUtxo> findUtxoByStakeAddressAndAsset(@NonNull String stakeAddress, String unit, int page, int count, Order order) {
        stakeAddress = stakeAddress.trim();

        Pageable pageable = getPageable(page, count, order);

        Condition unitCondition = getUnitCondition(unit);

        var fields = new ArrayList<>(Arrays.asList(ADDRESS_UTXO.fields()));
        fields.add(ADDRESS_UTXO.BLOCK.as("blockNumber")); //Workaround : due to mismatch field name in AddressUtxo.blockNumber and JOOQ model (block)

        var query = dsl
                .select(fields)
                .from(ADDRESS_UTXO)
                .leftJoin(TX_INPUT)
                .using(field(ADDRESS_UTXO.TX_HASH), field(ADDRESS_UTXO.OUTPUT_INDEX))
                .where(ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(stakeAddress))
                .and(TX_INPUT.TX_HASH.isNull())
                .and(unitCondition)
                .orderBy(order.equals(Order.desc) ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc())  //TODO: ordering
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        return query.fetch().into(AddressUtxo.class);
    }

    @Override
    public List<AddressUtxo> findAllByIds(List<UtxoKey> utxoKeys) {
        List<UtxoId> utxoIds = utxoKeys.stream()
                .map(utxoKey -> new UtxoId(utxoKey.getTxHash(), utxoKey.getOutputIndex()))
                .toList();

        return utxoRepository.findAllById(utxoIds)
                .stream().map(mapper::toAddressUtxo)
                .toList();
    }

    @Override
    public List<AddressTransaction> findTransactionsByAddress(String address, int page, int count, Order order) {
        Pageable pageable = getPageable(page, count, order);

        // Define the CTEs using JOOQ
        Select<Record4<String, Long, Long, Long>> addressUtxoTx = dsl
                .select(ADDRESS_UTXO.TX_HASH, ADDRESS_UTXO.BLOCK, ADDRESS_UTXO.BLOCK_TIME, ADDRESS_UTXO.SLOT)
                .from(ADDRESS_UTXO)
                .where(ADDRESS_UTXO.OWNER_ADDR.eq(address));

        Select<Record4<String, Long, Long, Long>> spentTx = dsl
                .select(TX_INPUT.SPENT_TX_HASH.as("tx_hash"), TX_INPUT.SPENT_AT_BLOCK.as("block"), TX_INPUT.SPENT_BLOCK_TIME.as("block_time"), TX_INPUT.SPENT_AT_SLOT.as("slot"))
                .from(TX_INPUT)
                .join(ADDRESS_UTXO)
                .on(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH))
                .and(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX))
                .where(ADDRESS_UTXO.OWNER_ADDR.eq(address));

        // Use union to combine both CTEs
        Select<?> combinedTx = addressUtxoTx
                .union(spentTx);

        // Fetch distinct tx_hash results with pagination and order by slot in descending order
        List<AddressTransaction> result = dsl
                .selectDistinct(
                        DSL.field("tx_hash", String.class).as("txHash"),
                        DSL.field("block", Long.class).as("blockHeight"),
                        DSL.field("block", Long.class).as("blockNumber"),
                        DSL.field("block_time", Long.class).as("blockTime"),
                        DSL.field("slot", Long.class).as("slot")
                )
                .from(combinedTx.asTable("combined_tx"))
                .orderBy(order.equals(Order.desc) ? DSL.field("slot").desc() : DSL.field("slot").asc())
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .fetchInto(AddressTransaction.class);

        return result;
    }

    //This method is currently optimized only for Postgresql
    @Override
    public List<AssetTransaction> findTransactionsByAsset(String unit, int page, int count, Order order) {
        Pageable pageable = getPageable(page, count, order);
        Condition unitCondition = getUnitCondition(unit);

        // Query to fetch distinct transaction hashes
        Table<?> distinctTx = dsl
                .select(
                        ADDRESS_UTXO.TX_HASH.as("txHash"),
                        DSL.min(ADDRESS_UTXO.BLOCK).as("blockHeight"),
                        DSL.min(ADDRESS_UTXO.BLOCK).as("blockNumber"),    // Use MIN or MAX as block number is consistent
                        DSL.min(ADDRESS_UTXO.BLOCK_TIME).as("blockTime"), // Use MIN or MAX as block time is consistent,
                        DSL.min(ADDRESS_UTXO.SLOT).as("slot")
                )
                .from(ADDRESS_UTXO)
                .where(unitCondition)
                .groupBy(ADDRESS_UTXO.TX_HASH) // Group by transaction hash to get distinct results
                .orderBy(order.equals(Order.desc) ? DSL.field("slot").desc() : DSL.field("slot").asc())
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .asTable("distinct_tx");

        // Fetch results with pagination and sorting
        var query = dsl
                .select(
                        distinctTx.field("txHash", String.class),
                        distinctTx.field("blockHeight", Long.class),
                        distinctTx.field("blockNumber", Long.class),
                        distinctTx.field("blockTime", Long.class)
                )
                .from(distinctTx);

        if (log.isDebugEnabled())
            log.debug(query.toString());

        List<AssetTransaction> result = query.fetchInto(AssetTransaction.class);

        return result;
    }

    private Condition getUnitCondition(String unit) {
        SQLDialect dialect = dsl.dialect();
        Condition unitCondition;
        if (dialect.family() == SQLDialect.POSTGRES) {
            unitCondition = DSL.condition("amounts @> {0}::jsonb", DSL.val("[{\"unit\": \"" + unit + "\"}]"));
        } else {
            unitCondition = field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains("\"unit\": \""+ unit +"\"")
                    .or(field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains("\"unit\":\""+ unit +"\""));
        }
        return unitCondition;
    }

    private static PageRequest getPageable(int page, int count, Order order) {
        return PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");
    }
}
