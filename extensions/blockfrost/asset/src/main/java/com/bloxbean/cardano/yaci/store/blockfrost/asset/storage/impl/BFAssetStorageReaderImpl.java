package com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl;

import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.BFAssetStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFAssetAddress;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFAssetHistory;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFAssetInfo;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFAssetTransaction;
import com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model.BFPolicyAsset;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.Row2;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.assets.jooq.Tables.ASSETS;
import static com.bloxbean.cardano.yaci.store.transaction.jooq.Tables.TRANSACTION;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS_UTXO;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.TX_INPUT;

@Component
@RequiredArgsConstructor
@Slf4j
public class BFAssetStorageReaderImpl implements BFAssetStorageReader {
    private final DSLContext dsl;

    @Override
    public List<BFPolicyAsset> findAssets(int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        List<FirstSeenUnit> pageRows = order == Order.asc
                ? findFirstSeenUnitsPageAsc(offset, count)
                : findFirstSeenUnitsPageDistinctOn(offset, count, order);
        if (pageRows.isEmpty()) {
            return List.of();
        }

        List<String> pageUnits = pageRows.stream()
                .map(FirstSeenUnit::unit)
                .collect(Collectors.toList());

        // Phase 2: one aggregate query for current page units (no N+1, no UNION ALL)
        // Use (unit, policy) predicates to align with unit-first composite index.
        Map<String, String> policyByUnit = pageUnits.stream()
                .collect(Collectors.toMap(
                        unit -> unit,
                        this::extractPolicy,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<Row2<String, String>> unitPolicyPairs = policyByUnit.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> DSL.row(entry.getKey(), entry.getValue()))
                .toList();
        List<String> unitsWithoutPolicy = policyByUnit.entrySet().stream()
                .filter(entry -> entry.getValue() == null)
                .map(Map.Entry::getKey)
                .toList();

        Field<String> quantityUnitField = ASSETS.UNIT.as("unit");
        Field<BigDecimal> quantityField = DSL.sum(ASSETS.QUANTITY.cast(BigDecimal.class)).as("quantity");
        Condition pageUnitsCondition = DSL.falseCondition();
        if (!unitPolicyPairs.isEmpty()) {
            pageUnitsCondition = pageUnitsCondition.or(DSL.row(ASSETS.UNIT, ASSETS.POLICY).in(unitPolicyPairs));
        }
        if (!unitsWithoutPolicy.isEmpty()) {
            pageUnitsCondition = pageUnitsCondition.or(ASSETS.UNIT.in(unitsWithoutPolicy));
        }

        var quantityQuery = dsl.select(quantityUnitField, quantityField)
                .from(ASSETS)
                .where(pageUnitsCondition)
                .groupBy(ASSETS.UNIT);

        logQuery("findAssetsQuantities", quantityQuery);
        Map<String, BigInteger> quantityByUnit = quantityQuery.fetch().stream()
                .collect(Collectors.toMap(
                        record -> record.get(quantityUnitField),
                        record -> toBigInteger(record.get(quantityField))
                ));

        return pageRows.stream()
                .map(row -> new BFPolicyAsset(
                        row.unit(),
                        quantityByUnit.getOrDefault(row.unit(), BigInteger.ZERO),
                        row.slot(),
                        row.txHash()
                ))
                .toList();
    }

    @Override
    public Optional<BFAssetInfo> findAssetInfo(String unit) {
        Condition assetCondition = buildAssetCondition(unit);

        var initialMintTxQuery = dsl.select(ASSETS.TX_HASH)
                .from(ASSETS)
                .where(assetCondition)
                .orderBy(ASSETS.SLOT.asc().nullsLast(), ASSETS.TX_HASH.asc())
                .limit(1);
        logQuery("findAssetInfoInitialMintTx", initialMintTxQuery);

        String initialMintTxHash = initialMintTxQuery.fetchOne(ASSETS.TX_HASH);
        if (initialMintTxHash == null) {
            return Optional.empty();
        }

        Field<String> outPolicyField = DSL.min(ASSETS.POLICY).as("policy_id");
        Field<String> outAssetNameField = DSL.min(ASSETS.ASSET_NAME).as("asset_name");
        Field<String> outFingerprintField = DSL.min(ASSETS.FINGERPRINT).as("fingerprint");
        Field<BigDecimal> outQuantityField = DSL.sum(ASSETS.QUANTITY.cast(BigDecimal.class)).as("quantity");
        Field<Long> outMintBurnCountField = DSL.count().cast(Long.class).as("mint_or_burn_count");

        var aggregateQuery = dsl.select(
                        outPolicyField,
                        outAssetNameField,
                        outFingerprintField,
                        outQuantityField,
                        outMintBurnCountField
                )
                .from(ASSETS)
                .where(assetCondition);

        logQuery("findAssetInfoAggregate", aggregateQuery);
        var result = aggregateQuery.fetchOne();

        if (result == null || result.get(outMintBurnCountField) == null || result.get(outMintBurnCountField) == 0L) {
            return Optional.empty();
        }

        return Optional.of(new BFAssetInfo(
                unit,
                result.get(outPolicyField),
                result.get(outAssetNameField),
                result.get(outFingerprintField),
                toBigInteger(result.get(outQuantityField)),
                initialMintTxHash,
                result.get(outMintBurnCountField)
        ));
    }

    @Override
    public List<BFAssetHistory> findAssetHistory(String unit, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        Condition assetCondition = buildAssetCondition(unit);
        SortField<?> slotOrder = order == Order.desc ? ASSETS.SLOT.desc().nullsLast() : ASSETS.SLOT.asc().nullsLast();
        SortField<?> txOrder = order == Order.desc ? ASSETS.TX_HASH.desc() : ASSETS.TX_HASH.asc();

        var query = dsl.select(ASSETS.TX_HASH, ASSETS.MINT_TYPE, ASSETS.QUANTITY)
                .from(ASSETS)
                .where(assetCondition)
                .orderBy(slotOrder, txOrder)
                .limit(count)
                .offset(offset);

        logQuery("findAssetHistory", query);

        return query.fetch(record -> {
                    String mintType = record.get(ASSETS.MINT_TYPE);
                    String action = "BURN".equalsIgnoreCase(mintType) ? "burned" : "minted";
                    BigInteger amount = toBigInteger(record.get(ASSETS.QUANTITY)).abs();
                    return new BFAssetHistory(record.get(ASSETS.TX_HASH), action, amount);
                });
    }

    @Override
    public List<String> findAssetTxHashes(String unit, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        Table<?> distinctAssetTx = buildDistinctAssetTxTable(unit);
        Field<String> distinctTxHashField = distinctAssetTx.field("tx_hash", String.class);

        Field<String> txHashField = TRANSACTION.TX_HASH;
        Field<Integer> txIndexSortField = TRANSACTION.TX_INDEX;
        SortField<?> blockOrder = order == Order.desc
                ? TRANSACTION.BLOCK.desc().nullsLast()
                : TRANSACTION.BLOCK.asc().nullsLast();
        SortField<?> txIndexOrder = order == Order.desc
                ? txIndexSortField.desc().nullsLast()
                : txIndexSortField.asc().nullsFirst();
        SortField<?> txOrder = order == Order.desc ? txHashField.desc() : txHashField.asc();

        var query = dsl.select(txHashField)
                .from(distinctAssetTx)
                .join(TRANSACTION)
                .on(TRANSACTION.TX_HASH.eq(distinctTxHashField))
                .orderBy(blockOrder, txIndexOrder, txOrder)
                .limit(count)
                .offset(offset);

        logQuery("findAssetTxHashes", query);
        return query.fetchInto(String.class);
    }

    @Override
    public List<BFAssetTransaction> findAssetTransactions(String unit, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        Table<?> distinctAssetTx = buildDistinctAssetTxTable(unit);
        Field<String> distinctTxHashField = distinctAssetTx.field("tx_hash", String.class);

        Field<String> txHashField = TRANSACTION.TX_HASH;
        Field<Long> blockHeightField = TRANSACTION.BLOCK.as("block_height");
        Field<Long> blockTimeField = TRANSACTION.BLOCK_TIME.as("block_time");
        Field<Integer> txIndexSortField = TRANSACTION.TX_INDEX;
        Field<Long> txIndexField = DSL.coalesce(txIndexSortField, 0).cast(Long.class).as("tx_index");

        SortField<?> blockOrder = order == Order.desc
                ? TRANSACTION.BLOCK.desc().nullsLast()
                : TRANSACTION.BLOCK.asc().nullsLast();
        SortField<?> txIndexOrder = order == Order.desc
                ? txIndexSortField.desc().nullsLast()
                : txIndexSortField.asc().nullsFirst();
        SortField<?> txOrder = order == Order.desc ? txHashField.desc() : txHashField.asc();

        var query = dsl.select(txHashField, txIndexField, blockHeightField, blockTimeField)
                .from(distinctAssetTx)
                .join(TRANSACTION)
                .on(TRANSACTION.TX_HASH.eq(distinctTxHashField))
                .orderBy(blockOrder, txIndexOrder, txOrder)
                .limit(count)
                .offset(offset);

        logQuery("findAssetTransactions", query);

        return query.fetch(record -> new BFAssetTransaction(
                        record.get(txHashField),
                        record.get(txIndexField),
                        record.get(blockHeightField),
                        record.get(blockTimeField)
                ));
    }

    @Override
    public List<BFAssetAddress> findAssetAddresses(String unit, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;

        Table<?> candidateUtxo = dsl.select(
                        ADDRESS_UTXO.TX_HASH.as("tx_hash"),
                        ADDRESS_UTXO.OUTPUT_INDEX.as("output_index"),
                        ADDRESS_UTXO.SLOT.as("slot"),
                        ADDRESS_UTXO.OWNER_ADDR.as("owner_addr"),
                        ADDRESS_UTXO.OWNER_ADDR_FULL.as("owner_addr_full"),
                        ADDRESS_UTXO.AMOUNTS.as("amounts")
                )
                .from(ADDRESS_UTXO)
                .where(buildUnitCondition(unit))
                .andNotExists(
                        dsl.selectOne()
                                .from(TX_INPUT)
                                .where(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH))
                                .and(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX))
                )
                .asTable("candidate_utxo");

        Field<String> ownerAddrField = candidateUtxo.field("owner_addr", String.class);
        Field<String> ownerAddrFullField = candidateUtxo.field("owner_addr_full", String.class);
        Field<Long> slotField = candidateUtxo.field("slot", Long.class);
        Field<?> amountsField = candidateUtxo.field("amounts");

        Field<String> addressField = DSL.coalesce(ownerAddrFullField, ownerAddrField).as("address");
        Table<?> amountTable = DSL.table(
                "(select (elem->>'quantity')::numeric as quantity " +
                        "from jsonb_array_elements({0}::jsonb) elem " +
                        "where elem->>'unit' = {1} " +
                        "limit 1) as amt",
                amountsField,
                DSL.val(unit)
        );
        Field<BigDecimal> amountQuantityField = DSL.field("amt.quantity", BigDecimal.class);
        Field<BigDecimal> quantitySumField = DSL.sum(amountQuantityField).as("quantity");
        Field<Long> firstSeenSlotField = DSL.min(slotField).as("first_seen_slot");

        SortField<?> slotOrder = order == Order.desc ? firstSeenSlotField.desc().nullsLast() : firstSeenSlotField.asc().nullsLast();
        SortField<?> addressOrder = order == Order.desc ? addressField.desc() : addressField.asc();

        var query = dsl.select(addressField, quantitySumField, firstSeenSlotField)
                .from(candidateUtxo)
                .join(DSL.lateral(amountTable))
                .on(DSL.trueCondition())
                .groupBy(addressField)
                .orderBy(slotOrder, addressOrder)
                .limit(count)
                .offset(offset);

        logQuery("findAssetAddresses", query);

        return query.fetch(record -> new BFAssetAddress(
                        record.get(addressField),
                        toBigInteger(record.get(quantitySumField)),
                        record.get(firstSeenSlotField)
                ));
    }

    @Override
    public List<BFPolicyAsset> findAssetsByPolicy(String policyId, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;

        Field<Integer> rnField = DSL.rowNumber()
                .over(DSL.partitionBy(ASSETS.UNIT)
                        .orderBy(ASSETS.SLOT.asc().nullsLast(), ASSETS.TX_HASH.asc()))
                .as("rn");

        Table<?> rankedAssets = dsl.select(
                        ASSETS.UNIT.as("unit"),
                        ASSETS.SLOT.as("slot"),
                        ASSETS.TX_HASH.as("tx_hash"),
                        rnField
                )
                .from(ASSETS)
                .where(ASSETS.POLICY.eq(policyId))
                .asTable("ranked_assets");

        Field<String> rankedUnitField = rankedAssets.field("unit", String.class);
        Field<Long> rankedSlotField = rankedAssets.field("slot", Long.class);
        Field<String> rankedTxHashField = rankedAssets.field("tx_hash", String.class);
        Field<Integer> rankedRnField = rankedAssets.field("rn", Integer.class);

        Table<?> firstSeenAssets = dsl.select(rankedUnitField, rankedSlotField, rankedTxHashField)
                .from(rankedAssets)
                .where(rankedRnField.eq(1))
                .asTable("first_seen_assets");

        Table<?> aggregatedQuantities = dsl.select(
                        ASSETS.UNIT.as("unit"),
                        DSL.sum(ASSETS.QUANTITY.cast(BigDecimal.class)).as("quantity")
                )
                .from(ASSETS)
                .where(ASSETS.POLICY.eq(policyId))
                .groupBy(ASSETS.UNIT)
                .asTable("aggregated_quantities");

        Field<String> unitField = firstSeenAssets.field("unit", String.class);
        Field<Long> slotField = firstSeenAssets.field("slot", Long.class);
        Field<String> txHashField = firstSeenAssets.field("tx_hash", String.class);
        Field<String> quantityUnitField = aggregatedQuantities.field("unit", String.class);
        Field<BigDecimal> quantityField = aggregatedQuantities.field("quantity", BigDecimal.class);

        SortField<?> slotOrder = order == Order.desc ? slotField.desc().nullsLast() : slotField.asc().nullsLast();
        SortField<?> txOrder = order == Order.desc ? txHashField.desc() : txHashField.asc();
        SortField<?> unitOrder = order == Order.desc ? unitField.desc() : unitField.asc();

        var query = dsl.select(unitField, quantityField, slotField, txHashField)
                .from(firstSeenAssets)
                .leftJoin(aggregatedQuantities)
                .on(quantityUnitField.eq(unitField))
                .orderBy(slotOrder, txOrder, unitOrder)
                .limit(count)
                .offset(offset);

        logQuery("findAssetsByPolicy", query);

        return query.fetch(record -> new BFPolicyAsset(
                        record.get(unitField),
                        toBigInteger(record.get(quantityField)),
                        record.get(slotField),
                        record.get(txHashField)
                ));
    }

    private Table<?> buildDistinctAssetTxTable(String unit) {
        return dsl.select(ADDRESS_UTXO.TX_HASH.as("tx_hash"))
                .from(ADDRESS_UTXO)
                .where(buildUnitCondition(unit))
                .groupBy(ADDRESS_UTXO.TX_HASH)
                .asTable("distinct_asset_tx");
    }

    private List<FirstSeenUnit> findFirstSeenUnitsPageAsc(int offset, int count) {
        int needed = offset + count;
        int candidateLimit = Math.max(needed * 50, 5000);
        int maxCandidateLimit = 200_000;

        while (true) {
            var candidatesQuery = dsl.select(ASSETS.UNIT, ASSETS.SLOT, ASSETS.TX_HASH)
                    .from(ASSETS)
                    .join(TRANSACTION)
                    .on(TRANSACTION.TX_HASH.eq(ASSETS.TX_HASH))
                    .where(ASSETS.MINT_TYPE.eq("MINT"))
                    .orderBy(
                            ASSETS.SLOT.asc().nullsLast(),
                            TRANSACTION.TX_INDEX.asc().nullsFirst(),
                            ASSETS.UNIT.asc()
                    )
                    .limit(candidateLimit);
            logQuery("findAssetsMintCandidates", candidatesQuery);

            var candidateRows = candidatesQuery.fetch();
            LinkedHashMap<String, FirstSeenUnit> uniqueByUnit = new LinkedHashMap<>();
            for (var row : candidateRows) {
                String unit = row.get(ASSETS.UNIT);
                uniqueByUnit.putIfAbsent(unit, new FirstSeenUnit(
                        unit,
                        row.get(ASSETS.SLOT),
                        row.get(ASSETS.TX_HASH)
                ));
            }

            if (uniqueByUnit.size() >= needed || candidateRows.size() < candidateLimit) {
                List<FirstSeenUnit> uniqueRows = new ArrayList<>(uniqueByUnit.values());
                if (offset >= uniqueRows.size()) {
                    return List.of();
                }
                return uniqueRows.subList(offset, Math.min(offset + count, uniqueRows.size()));
            }

            if (candidateLimit >= maxCandidateLimit) {
                return findFirstSeenUnitsPageDistinctOn(offset, count, Order.asc);
            }

            candidateLimit = Math.min(candidateLimit * 2, maxCandidateLimit);
        }
    }

    private List<FirstSeenUnit> findFirstSeenUnitsPageDistinctOn(int offset, int count, Order order) {
        Table<?> firstSeenUnits = DSL.table(
                "(with first_slots as materialized (" +
                        "select distinct on (unit) unit, slot " +
                        "from assets " +
                        "where mint_type = 'MINT' " +
                        "order by unit, slot asc nulls last" +
                        ") " +
                        "select fs.unit, fs.slot, picked.tx_hash, picked.tx_index " +
                        "from first_slots fs " +
                        "join lateral (" +
                        "select a.tx_hash, t.tx_index " +
                        "from assets a " +
                        "join transaction t on t.tx_hash = a.tx_hash " +
                        "where a.mint_type = 'MINT' " +
                        "and a.unit = fs.unit " +
                        "and a.slot = fs.slot " +
                        "order by t.tx_index asc nulls first, a.tx_hash asc " +
                        "limit 1" +
                        ") picked on true" +
                        ") as first_seen_units"
        );

        Field<String> unitField = DSL.field(DSL.name("first_seen_units", "unit"), String.class);
        Field<Long> slotField = DSL.field(DSL.name("first_seen_units", "slot"), Long.class);
        Field<String> txHashField = DSL.field(DSL.name("first_seen_units", "tx_hash"), String.class);
        Field<Integer> txIndexField = DSL.field(DSL.name("first_seen_units", "tx_index"), Integer.class);

        SortField<?> slotOrder = order == Order.desc ? slotField.desc().nullsLast() : slotField.asc().nullsLast();
        SortField<?> txIndexOrder = order == Order.desc ? txIndexField.desc().nullsLast() : txIndexField.asc().nullsFirst();
        SortField<?> unitOrder = order == Order.desc ? unitField.desc() : unitField.asc();

        var query = dsl.select(unitField, slotField, txHashField)
                .from(firstSeenUnits)
                .orderBy(slotOrder, txIndexOrder, unitOrder)
                .limit(count)
                .offset(offset);
        logQuery("findAssetsUnitsPage", query);

        return query.fetch(record -> new FirstSeenUnit(
                record.get(unitField),
                record.get(slotField),
                record.get(txHashField)
        ));
    }

    private Condition buildUnitCondition(String unit) {
        return DSL.condition("amounts @> {0}::jsonb", DSL.val("[{\"unit\": \"" + unit + "\"}]"));
    }

    private Condition buildAssetCondition(String unit) {
        if (unit == null || unit.length() < 56) {
            return ASSETS.UNIT.eq(unit);
        }

        return ASSETS.UNIT.eq(unit).and(ASSETS.POLICY.eq(extractPolicy(unit)));
    }

    private String extractPolicy(String unit) {
        if (unit == null || unit.length() < 56) {
            return null;
        }
        return unit.substring(0, 56);
    }

    private record FirstSeenUnit(String unit, Long slot, String txHash) {
    }

    private BigInteger toBigInteger(Object value) {
        if (value == null) {
            return BigInteger.ZERO;
        }
        if (value instanceof BigInteger bigInteger) {
            return bigInteger;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.toBigInteger();
        }
        if (value instanceof Number number) {
            return BigInteger.valueOf(number.longValue());
        }
        return new BigInteger(String.valueOf(value));
    }

    private void logQuery(String queryName, Query query) {
        log.info("[BFAssetStorageReader] {} SQL: {}", queryName, query.getSQL(ParamType.INLINED));
    }
}
