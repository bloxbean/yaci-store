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

    /**
     * Returns a paginated asset list with first-seen mint tx (per unit) and aggregated quantity for each unit.
     * The method intentionally runs in two phases to avoid N+1:
     * 1) resolve page units with deterministic first-seen ordering
     * 2) aggregate quantities for only those page units.
     */
    @Override
    public List<BFPolicyAsset> findAssets(int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        List<FirstSeenUnit> pageRows = findFirstSeenUnitsPage(offset, count, order);
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

    /**
     * Resolves full asset information for a specific unit.
     * The earliest mint transaction hash is fetched separately from aggregates to keep both queries index-friendly.
     */
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

    /**
     * Returns mint/burn history for an asset unit in deterministic order by slot and tx hash.
     */
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

    /**
     * Returns transaction hashes related to the provided unit.
     * The tx hash set is pre-grouped from UTXO data and then ordered by block/tx_index/hash.
     */
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

    /**
     * Returns transaction references for the provided unit with block height/time and tx index.
     */
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

    /**
     * Returns current holder addresses for a unit with quantity and first-seen slot.
     * Only unspent UTXO rows are considered and quantity is extracted from the amounts JSON array.
     */
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
                """
                (
                    select (elem->>'quantity')::numeric as quantity
                    from jsonb_array_elements({0}::jsonb) elem
                    where elem->>'unit' = {1}
                    limit 1
                ) as amt
                """,
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

    /**
     * Returns policy assets with first-seen tx per unit and aggregated quantity for that policy.
     */
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

    /**
     * Builds a distinct tx hash table for a unit from UTXO amounts JSON.
     */
    private Table<?> buildDistinctAssetTxTable(String unit) {
        return dsl.select(ADDRESS_UTXO.TX_HASH.as("tx_hash"))
                .from(ADDRESS_UTXO)
                .where(buildUnitCondition(unit))
                .groupBy(ADDRESS_UTXO.TX_HASH)
                .asTable("distinct_asset_tx");
    }

    /**
     * Resolves the first mint row per unit for the requested page/order.
     * Strategy:
     * 1) build first mint slot per unit (distinct on unit),
     * 2) compute page boundary slots from a small ordered window,
     * 3) enrich only candidate rows inside boundary range,
     * 4) rank in final order and slice requested page.
     */
    private List<FirstSeenUnit> findFirstSeenUnitsPage(int offset, int count, Order order) {
        String slotOrder = order == Order.desc ? "desc nulls last" : "asc nulls last";
        String txIndexOrder = order == Order.desc ? "desc" : "asc";
        String unitOrder = order == Order.desc ? "desc" : "asc";

        // Boundary comparisons for slot range selection
        // For ASC: we want the page_window starting from offset, boundary is min/max of that window
        // For DESC: reversed
        String boundaryStartAgg = order == Order.desc ? "max" : "min";
        String boundaryEndAgg = order == Order.desc ? "min" : "max";

        // prefix counts rows strictly before the boundary slot
        String prefixCmp = order == Order.desc ? ">" : "<";
        // candidates are rows within the boundary slot range (inclusive)
        String candCmp1 = order == Order.desc ? "<=" : ">=";
        String candCmp2 = order == Order.desc ? ">=" : "<=";

        Table<?> firstSeenUnits = DSL.table(
                """
                (
                    with first_mints as materialized (
                        select distinct on (unit) unit as unit, slot::bigint as slot
                        from assets
                        where mint_type = 'MINT'
                        order by unit, slot asc nulls last
                    ),
                    page_window as materialized (
                        select unit, slot
                        from first_mints
                        order by slot %s, unit %s
                        offset {0} rows fetch next {1} rows only
                    ),
                    boundary as (
                        select %s(slot) as start_slot, %s(slot) as end_slot
                        from page_window
                    ),
                    prefix as (
                        select count(*) as cnt
                        from first_mints
                        where slot %s (select start_slot from boundary)
                    ),
                    candidates as (
                        select fm.unit, fm.slot
                        from first_mints fm, boundary b
                        where fm.slot %s b.start_slot
                          and fm.slot %s b.end_slot
                    ),
                    enriched as (
                        select c.unit, c.slot, picked.tx_hash, picked.tx_index
                        from candidates c
                        join lateral (
                            select a.tx_hash, t.tx_index
                            from assets a
                            join transaction t on t.tx_hash = a.tx_hash
                            where a.mint_type = 'MINT'
                              and a.unit = c.unit
                              and a.slot = c.slot
                            order by t.tx_index asc, a.tx_hash asc
                            fetch next 1 rows only
                        ) picked on true
                    ),
                    ranked as (
                        select unit, slot, tx_hash,
                               row_number() over (order by slot %s, tx_index %s, unit %s) as rn
                        from enriched
                    )
                    select unit, slot, tx_hash
                    from ranked
                    where rn > ({0} - (select cnt from prefix))
                      and rn <= ({0} - (select cnt from prefix) + {1})
                ) as first_seen_units
                """.formatted(
                        slotOrder, unitOrder,
                        boundaryStartAgg, boundaryEndAgg,
                        prefixCmp,
                        candCmp1, candCmp2,
                        slotOrder, txIndexOrder, unitOrder
                ),
                DSL.inline(offset), DSL.inline(count)
        );

        Field<String> unitField = DSL.field(DSL.name("first_seen_units", "unit"), String.class);
        Field<Long> slotField = DSL.field(DSL.name("first_seen_units", "slot"), Long.class);
        Field<String> txHashField = DSL.field(DSL.name("first_seen_units", "tx_hash"), String.class);

        var query = dsl.select(unitField, slotField, txHashField)
                .from(firstSeenUnits);
        logQuery("findAssetsUnitsPage", query);

        return query.fetch(record -> new FirstSeenUnit(
                record.get(unitField),
                record.get(slotField),
                record.get(txHashField)
        ));
    }

    /**
     * Builds JSONB containment condition for unit matching inside address_utxo.amounts.
     */
    private Condition buildUnitCondition(String unit) {
        return DSL.condition("amounts @> {0}::jsonb", DSL.val("[{\"unit\": \"" + unit + "\"}]"));
    }

    /**
     * Builds asset filter condition.
     * For valid unit strings (policy + asset name), it also adds policy equality for better index usage.
     */
    private Condition buildAssetCondition(String unit) {
        if (unit == null || unit.length() < 56) {
            return ASSETS.UNIT.eq(unit);
        }

        return ASSETS.UNIT.eq(unit).and(ASSETS.POLICY.eq(extractPolicy(unit)));
    }

    /**
     * Extracts policy id from a full unit (first 56 hex chars).
     */
    private String extractPolicy(String unit) {
        if (unit == null || unit.length() < 56) {
            return null;
        }
        return unit.substring(0, 56);
    }

    private record FirstSeenUnit(String unit, Long slot, String txHash) {
    }

    /**
     * Normalizes numeric results from jOOQ records into BigInteger.
     */
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

    /**
     * Emits full SQL with inlined parameters for debugging and plan analysis.
     */
    private void logQuery(String queryName, Query query) {
        log.debug("[BFAssetStorageReader] {} SQL: {}", queryName, query.getSQL(ParamType.INLINED));
    }
}
