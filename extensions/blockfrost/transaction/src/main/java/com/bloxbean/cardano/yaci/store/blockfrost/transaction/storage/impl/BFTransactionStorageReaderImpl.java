package com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage.impl;

import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.blockfrost.common.util.AmountsJsonUtil;
import com.bloxbean.cardano.yaci.store.blockfrost.common.util.BlockfrostDialectUtil;
import com.bloxbean.cardano.yaci.store.blockfrost.transaction.dto.*;
import com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage.BFTransactionStorageReader;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorageReader;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Select;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.assets.jooq.Tables.ASSETS;
import static com.bloxbean.cardano.yaci.store.script.jooq.Tables.TRANSACTION_SCRIPTS;
import static com.bloxbean.cardano.yaci.store.staking.jooq.Tables.*;
import static com.bloxbean.cardano.yaci.store.transaction.jooq.Tables.*;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class BFTransactionStorageReaderImpl implements BFTransactionStorageReader {

    private final DSLContext dsl;
    private final TransactionStorageReader transactionStorageReader;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<BFTransactionDto> findTransactionByHash(String txHash) {
        Field<Integer> withdrawalCount = DSL.selectCount()
                .from(WITHDRAWAL)
                .where(WITHDRAWAL.TX_HASH.eq(txHash))
                .asField("withdrawal_count");

        Field<Integer> delegationCount = DSL.selectCount()
                .from(DELEGATION)
                .where(DELEGATION.TX_HASH.eq(txHash))
                .asField("delegation_count");

        Field<Integer> stakeCertCount = DSL.selectCount()
                .from(STAKE_REGISTRATION)
                .where(STAKE_REGISTRATION.TX_HASH.eq(txHash))
                .asField("stake_cert_count");

        Field<Integer> poolUpdateCount = DSL.selectCount()
                .from(POOL_REGISTRATION)
                .where(POOL_REGISTRATION.TX_HASH.eq(txHash))
                .asField("pool_update_count");

        Field<Integer> poolRetireCount = DSL.selectCount()
                .from(POOL_RETIREMENT)
                .where(POOL_RETIREMENT.TX_HASH.eq(txHash))
                .asField("pool_retire_count");

        Field<Integer> redeemerCount = DSL.selectCount()
                .from(TRANSACTION_SCRIPTS)
                .where(TRANSACTION_SCRIPTS.TX_HASH.eq(txHash))
                .asField("redeemer_count");

        Field<Integer> assetMintCount = DSL.selectCount()
                .from(ASSETS)
                .where(ASSETS.TX_HASH.eq(txHash))
                .asField("asset_mint_count");

        Field<Integer> inputCount = DSL.selectCount()
                .from(TX_INPUT)
                .where(TX_INPUT.SPENT_TX_HASH.eq(txHash))
                .asField("input_count");

        Field<Integer> outputCount = DSL.selectCount()
                .from(ADDRESS_UTXO)
                .where(ADDRESS_UTXO.TX_HASH.eq(txHash))
                .and(ADDRESS_UTXO.IS_COLLATERAL_RETURN.isFalse().or(ADDRESS_UTXO.IS_COLLATERAL_RETURN.isNull()))
                .asField("output_count");

        return dsl.select(
                        TRANSACTION.TX_HASH,
                        TRANSACTION.BLOCK_HASH,
                        TRANSACTION.BLOCK,
                        TRANSACTION.BLOCK_TIME,
                        TRANSACTION.SLOT,
                        TRANSACTION.TX_INDEX,
                        TRANSACTION.FEE,
                        TRANSACTION.TTL,
                        TRANSACTION.VALIDITY_INTERVAL_START,
                        TRANSACTION.INVALID,
                        TRANSACTION_CBOR.CBOR_SIZE,
                        withdrawalCount,
                        delegationCount,
                        stakeCertCount,
                        poolUpdateCount,
                        poolRetireCount,
                        redeemerCount,
                        assetMintCount,
                        inputCount,
                        outputCount
                )
                .from(TRANSACTION)
                .leftJoin(TRANSACTION_CBOR).on(TRANSACTION_CBOR.TX_HASH.eq(TRANSACTION.TX_HASH))
                .where(TRANSACTION.TX_HASH.eq(txHash))
                .fetchOptional(record -> {
                    Boolean invalid = record.get(TRANSACTION.INVALID);

                    return BFTransactionDto.builder()
                            .hash(record.get(TRANSACTION.TX_HASH))
                            .block(record.get(TRANSACTION.BLOCK_HASH))
                            .blockHeight(record.get(TRANSACTION.BLOCK) != null ? record.get(TRANSACTION.BLOCK).intValue() : null)
                            .blockTime(record.get(TRANSACTION.BLOCK_TIME))
                            .slot(record.get(TRANSACTION.SLOT))
                            .index(record.get(TRANSACTION.TX_INDEX))
                            .fees(record.get(TRANSACTION.FEE) != null ? record.get(TRANSACTION.FEE).toString() : "0")
                            .size(record.get(TRANSACTION_CBOR.CBOR_SIZE))
                            .invalidBefore(record.get(TRANSACTION.VALIDITY_INTERVAL_START) != null
                                    && record.get(TRANSACTION.VALIDITY_INTERVAL_START) > 0
                                    ? record.get(TRANSACTION.VALIDITY_INTERVAL_START).toString() : null)
                            .invalidHereafter(record.get(TRANSACTION.TTL) != null
                                    ? record.get(TRANSACTION.TTL).toString() : null)
                            .withdrawalCount(record.get("withdrawal_count", Integer.class))
                            .delegationCount(record.get("delegation_count", Integer.class))
                            .stakeCertCount(record.get("stake_cert_count", Integer.class))
                            .poolUpdateCount(record.get("pool_update_count", Integer.class))
                            .poolRetireCount(record.get("pool_retire_count", Integer.class))
                            .redeemerCount(record.get("redeemer_count", Integer.class))
                            .assetMintOrBurnCount(record.get("asset_mint_count", Integer.class))
                            .utxoCount(Optional.ofNullable(record.get("input_count", Integer.class)).orElse(0)
                                    + Optional.ofNullable(record.get("output_count", Integer.class)).orElse(0))
                            .validContract(invalid == null || !invalid)
                            .build();
                });
    }

    @Override
    public Optional<BFTxUtxosDto> findTxUtxos(String txHash) {
        Optional<Txn> txnOpt = transactionStorageReader.getTransactionByTxHash(txHash);
        if (txnOpt.isEmpty()) {
            return Optional.empty();
        }
        Txn txn = txnOpt.get();

        Set<String> collateralKeys = buildUtxoKeySet(txn.getCollateralInputs());
        Set<String> referenceKeys = buildUtxoKeySet(txn.getReferenceInputs());

        // Inputs: UTXOs spent by this transaction
        List<BFTxInputDto> inputs = dsl.select(
                        ADDRESS_UTXO.TX_HASH,
                        ADDRESS_UTXO.OUTPUT_INDEX,
                        ADDRESS_UTXO.OWNER_ADDR,
                        ADDRESS_UTXO.LOVELACE_AMOUNT,
                        ADDRESS_UTXO.AMOUNTS.cast(String.class).as("amounts_str"),
                        ADDRESS_UTXO.DATA_HASH,
                        ADDRESS_UTXO.INLINE_DATUM,
                        ADDRESS_UTXO.REFERENCE_SCRIPT_HASH
                )
                .from(ADDRESS_UTXO)
                .join(TX_INPUT)
                .on(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH))
                .and(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX))
                .where(TX_INPUT.SPENT_TX_HASH.eq(txHash))
                .fetch(record -> {
                    String utxoTxHash = record.get(ADDRESS_UTXO.TX_HASH);
                    Integer outputIndex = record.get(ADDRESS_UTXO.OUTPUT_INDEX);
                    String utxoKey = utxoTxHash + ":" + outputIndex;

                    return BFTxInputDto.builder()
                            .address(record.get(ADDRESS_UTXO.OWNER_ADDR))
                            .amount(buildAmounts(record.get(ADDRESS_UTXO.LOVELACE_AMOUNT),
                                    record.get("amounts_str", String.class)))
                            .txHash(utxoTxHash)
                            .outputIndex(outputIndex)
                            .dataHash(record.get(ADDRESS_UTXO.DATA_HASH))
                            .inlineDatum(record.get(ADDRESS_UTXO.INLINE_DATUM))
                            .referenceScriptHash(record.get(ADDRESS_UTXO.REFERENCE_SCRIPT_HASH))
                            .collateral(collateralKeys.contains(utxoKey))
                            .reference(referenceKeys.contains(utxoKey))
                            .build();
                });

        // Outputs: UTXOs created by this transaction
        List<BFTxOutputDto> outputs = dsl.select(
                        ADDRESS_UTXO.TX_HASH,
                        ADDRESS_UTXO.OUTPUT_INDEX,
                        ADDRESS_UTXO.OWNER_ADDR,
                        ADDRESS_UTXO.LOVELACE_AMOUNT,
                        ADDRESS_UTXO.AMOUNTS.cast(String.class).as("amounts_str"),
                        ADDRESS_UTXO.DATA_HASH,
                        ADDRESS_UTXO.INLINE_DATUM,
                        ADDRESS_UTXO.REFERENCE_SCRIPT_HASH,
                        ADDRESS_UTXO.IS_COLLATERAL_RETURN,
                        TX_INPUT.SPENT_TX_HASH
                )
                .from(ADDRESS_UTXO)
                .leftJoin(TX_INPUT)
                .on(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH))
                .and(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX))
                .where(ADDRESS_UTXO.TX_HASH.eq(txHash))
                .fetch(record -> BFTxOutputDto.builder()
                        .address(record.get(ADDRESS_UTXO.OWNER_ADDR))
                        .amount(buildAmounts(record.get(ADDRESS_UTXO.LOVELACE_AMOUNT),
                                record.get("amounts_str", String.class)))
                        .outputIndex(record.get(ADDRESS_UTXO.OUTPUT_INDEX))
                        .dataHash(record.get(ADDRESS_UTXO.DATA_HASH))
                        .inlineDatum(record.get(ADDRESS_UTXO.INLINE_DATUM))
                        .referenceScriptHash(record.get(ADDRESS_UTXO.REFERENCE_SCRIPT_HASH))
                        .collateral(Boolean.TRUE.equals(record.get(ADDRESS_UTXO.IS_COLLATERAL_RETURN)))
                        .consumedByTx(record.get(TX_INPUT.SPENT_TX_HASH))
                        .build());

        return Optional.of(BFTxUtxosDto.builder()
                .hash(txHash)
                .inputs(inputs)
                .outputs(outputs)
                .build());
    }

    @Override
    public Optional<String> findTxCborHex(String txHash) {
        return dsl.select(TRANSACTION_CBOR.CBOR_DATA)
                .from(TRANSACTION_CBOR)
                .where(TRANSACTION_CBOR.TX_HASH.eq(txHash))
                .fetchOptional(record -> {
                    byte[] cborData = record.get(TRANSACTION_CBOR.CBOR_DATA);
                    return cborData != null ? HexUtil.encodeHexString(cborData) : null;
                });
    }

    @Override
    public List<BFTxRedeemerDto> findTxRedeemers(String txHash) {
        return dsl.select(
                        TRANSACTION_SCRIPTS.REDEEMER_INDEX,
                        TRANSACTION_SCRIPTS.PURPOSE,
                        TRANSACTION_SCRIPTS.SCRIPT_HASH,
                        TRANSACTION_SCRIPTS.REDEEMER_DATAHASH,
                        TRANSACTION_SCRIPTS.UNIT_MEM,
                        TRANSACTION_SCRIPTS.UNIT_STEPS
                )
                .from(TRANSACTION_SCRIPTS)
                .where(TRANSACTION_SCRIPTS.TX_HASH.eq(txHash))
                .orderBy(TRANSACTION_SCRIPTS.REDEEMER_INDEX.asc())
                .fetch(record -> BFTxRedeemerDto.builder()
                        .txIndex(record.get(TRANSACTION_SCRIPTS.REDEEMER_INDEX))
                        .purpose(record.get(TRANSACTION_SCRIPTS.PURPOSE) != null
                                ? record.get(TRANSACTION_SCRIPTS.PURPOSE).toLowerCase() : null)
                        .scriptHash(record.get(TRANSACTION_SCRIPTS.SCRIPT_HASH))
                        .redeemerDataHash(record.get(TRANSACTION_SCRIPTS.REDEEMER_DATAHASH))
                        .datumHash(record.get(TRANSACTION_SCRIPTS.REDEEMER_DATAHASH))
                        .unitMem(record.get(TRANSACTION_SCRIPTS.UNIT_MEM) != null
                                ? record.get(TRANSACTION_SCRIPTS.UNIT_MEM).toString() : null)
                        .unitSteps(record.get(TRANSACTION_SCRIPTS.UNIT_STEPS) != null
                                ? record.get(TRANSACTION_SCRIPTS.UNIT_STEPS).toString() : null)
                        .fee(null) // fee per redeemer not stored separately
                        .build());
    }

    @Override
    public List<BFTxStakeDto> findTxStakes(String txHash) {
        return dsl.select(
                        STAKE_REGISTRATION.CERT_INDEX,
                        STAKE_REGISTRATION.ADDRESS,
                        STAKE_REGISTRATION.TYPE
                )
                .from(STAKE_REGISTRATION)
                .where(STAKE_REGISTRATION.TX_HASH.eq(txHash))
                .orderBy(STAKE_REGISTRATION.CERT_INDEX.asc())
                .fetch(record -> BFTxStakeDto.builder()
                        .certIndex(record.get(STAKE_REGISTRATION.CERT_INDEX))
                        .address(record.get(STAKE_REGISTRATION.ADDRESS))
                        .registration("STAKE_REGISTRATION".equals(record.get(STAKE_REGISTRATION.TYPE)))
                        .build());
    }

    @Override
    public List<BFTxDelegationDto> findTxDelegations(String txHash) {
        return dsl.select(
                        DELEGATION.CERT_INDEX,
                        DELEGATION.ADDRESS,
                        DELEGATION.POOL_ID,
                        DELEGATION.EPOCH
                )
                .from(DELEGATION)
                .where(DELEGATION.TX_HASH.eq(txHash))
                .orderBy(DELEGATION.CERT_INDEX.asc())
                .fetch(record -> {
                    String poolIdHex = record.get(DELEGATION.POOL_ID);
                    String poolIdBech32 = toBech32PoolId(poolIdHex);
                    Integer epoch = record.get(DELEGATION.EPOCH);
                    Integer certIndex = record.get(DELEGATION.CERT_INDEX);
                    return BFTxDelegationDto.builder()
                            .index(certIndex)
                            .certIndex(certIndex)
                            .address(record.get(DELEGATION.ADDRESS))
                            .poolId(poolIdBech32)
                            .activeEpoch(epoch != null ? epoch + 2 : null)
                            .build();
                });
    }

    @Override
    public List<BFTxWithdrawalDto> findTxWithdrawals(String txHash) {
        return dsl.select(WITHDRAWAL.ADDRESS, WITHDRAWAL.AMOUNT)
                .from(WITHDRAWAL)
                .where(WITHDRAWAL.TX_HASH.eq(txHash))
                .fetch(record -> BFTxWithdrawalDto.builder()
                        .address(record.get(WITHDRAWAL.ADDRESS))
                        .amount(record.get(WITHDRAWAL.AMOUNT) != null
                                ? record.get(WITHDRAWAL.AMOUNT).toString() : "0")
                        .build());
    }

    @Override
    public List<BFTxPoolUpdateDto> findTxPoolUpdates(String txHash) {
        return dsl.select(
                        POOL_REGISTRATION.CERT_INDEX,
                        POOL_REGISTRATION.POOL_ID,
                        POOL_REGISTRATION.VRF_KEY,
                        POOL_REGISTRATION.PLEDGE,
                        POOL_REGISTRATION.MARGIN,
                        POOL_REGISTRATION.COST,
                        POOL_REGISTRATION.REWARD_ACCOUNT,
                        POOL_REGISTRATION.POOL_OWNERS,
                        POOL_REGISTRATION.RELAYS,
                        POOL_REGISTRATION.METADATA_URL,
                        POOL_REGISTRATION.METADATA_HASH,
                        POOL_REGISTRATION.EPOCH
                )
                .from(POOL_REGISTRATION)
                .where(POOL_REGISTRATION.TX_HASH.eq(txHash))
                .orderBy(POOL_REGISTRATION.CERT_INDEX.asc())
                .fetch(record -> {
                    String metadataUrl = record.get(POOL_REGISTRATION.METADATA_URL);
                    String metadataHash = record.get(POOL_REGISTRATION.METADATA_HASH);
                    BFTxPoolUpdateDto.PoolMetadata poolMetadata = null;
                    if (metadataUrl != null || metadataHash != null) {
                        poolMetadata = BFTxPoolUpdateDto.PoolMetadata.builder()
                                .url(metadataUrl)
                                .hash(metadataHash)
                                .build();
                    }

                    List<String> owners = parseJsonStringArray(record.get(POOL_REGISTRATION.POOL_OWNERS) != null
                            ? record.get(POOL_REGISTRATION.POOL_OWNERS).toString() : null);

                    List<BFTxPoolRelayDto> relays = parseRelays(record.get(POOL_REGISTRATION.RELAYS) != null
                            ? record.get(POOL_REGISTRATION.RELAYS).toString() : null);

                    Integer epoch = record.get(POOL_REGISTRATION.EPOCH);
                    return BFTxPoolUpdateDto.builder()
                            .certIndex(record.get(POOL_REGISTRATION.CERT_INDEX))
                            .poolId(toBech32PoolId(record.get(POOL_REGISTRATION.POOL_ID)))
                            .vrfKey(record.get(POOL_REGISTRATION.VRF_KEY))
                            .pledge(record.get(POOL_REGISTRATION.PLEDGE) != null
                                    ? record.get(POOL_REGISTRATION.PLEDGE).toString() : "0")
                            .marginCost(record.get(POOL_REGISTRATION.MARGIN))
                            .fixedCost(record.get(POOL_REGISTRATION.COST) != null
                                    ? record.get(POOL_REGISTRATION.COST).toString() : "0")
                            .rewardAccount(record.get(POOL_REGISTRATION.REWARD_ACCOUNT))
                            .owners(owners)
                            .relays(relays)
                            .metadata(poolMetadata)
                            .activeEpoch(epoch != null ? epoch + 1 : null)
                            .build();
                });
    }

    @Override
    public List<BFTxPoolRetireDto> findTxPoolRetires(String txHash) {
        return dsl.select(
                        POOL_RETIREMENT.CERT_INDEX,
                        POOL_RETIREMENT.POOL_ID,
                        POOL_RETIREMENT.RETIREMENT_EPOCH
                )
                .from(POOL_RETIREMENT)
                .where(POOL_RETIREMENT.TX_HASH.eq(txHash))
                .orderBy(POOL_RETIREMENT.CERT_INDEX.asc())
                .fetch(record -> BFTxPoolRetireDto.builder()
                        .certIndex(record.get(POOL_RETIREMENT.CERT_INDEX))
                        .poolId(toBech32PoolId(record.get(POOL_RETIREMENT.POOL_ID)))
                        .retiringEpoch(record.get(POOL_RETIREMENT.RETIREMENT_EPOCH))
                        .build());
    }

    @Override
    public Map<String, BigInteger> findTxOutputAmounts(String txHash) {
        if (BlockfrostDialectUtil.isPostgres(dsl)) {
            return findTxOutputAmountsPostgres(txHash);
        }
        return findTxOutputAmountsNonPostgres(txHash);
    }

    private Map<String, BigInteger> findTxOutputAmountsPostgres(String txHash) {
        Table<?> baseTable = dsl.select(
                        ADDRESS_UTXO.AMOUNTS.as("amounts"),
                        ADDRESS_UTXO.LOVELACE_AMOUNT.as("lovelace_amount")
                )
                .from(ADDRESS_UTXO)
                .where(ADDRESS_UTXO.TX_HASH.eq(txHash))
                .and(ADDRESS_UTXO.IS_COLLATERAL_RETURN.isFalse().or(ADDRESS_UTXO.IS_COLLATERAL_RETURN.isNull()))
                .asTable("base");

        Field<?> amountsField = DSL.field(DSL.name("base", "amounts"));
        Field<BigDecimal> lovelaceAmountField = DSL.field(DSL.name("base", "lovelace_amount"), BigDecimal.class);

        Select<Record2<String, BigDecimal>> lovelaceSelect = dsl.select(
                        DSL.inline("lovelace").as("unit"),
                        DSL.sum(lovelaceAmountField).cast(BigDecimal.class).as("quantity")
                )
                .from(baseTable)
                .where(lovelaceAmountField.isNotNull());

        Table<?> amountTable = DSL.table("jsonb_to_recordset({0}::jsonb) as amt(unit text, quantity numeric)", amountsField);
        Field<String> unitField = DSL.field("amt.unit", String.class);
        Field<BigDecimal> quantityField = DSL.field("amt.quantity", BigDecimal.class);

        Select<Record2<String, BigDecimal>> assetsSelect = dsl.select(unitField.as("unit"),
                        DSL.sum(quantityField).cast(BigDecimal.class).as("quantity"))
                .from(baseTable)
                .join(DSL.lateral(amountTable)).on(DSL.trueCondition())
                .where(amountsField.isNotNull())
                .and(unitField.ne("lovelace")
                        .or(lovelaceAmountField.isNull()
                                .or(lovelaceAmountField.eq(BigDecimal.ZERO))
                                .and(unitField.eq("lovelace"))))
                .groupBy(unitField);

        Table<?> combined = lovelaceSelect.unionAll(assetsSelect).asTable("combined");
        Field<String> combinedUnit = combined.field("unit", String.class);
        Field<BigDecimal> combinedQuantity = combined.field("quantity", BigDecimal.class);

        return dsl.select(combinedUnit, DSL.sum(combinedQuantity).as("quantity"))
                .from(combined)
                .groupBy(combinedUnit)
                .fetchMap(combinedUnit, r -> r.get("quantity", BigDecimal.class))
                .entrySet().stream()
                .filter(e -> e.getValue() != null)
                .collect(HashMap::new,
                        (map, e) -> map.put(e.getKey(), e.getValue().toBigInteger()),
                        HashMap::putAll);
    }

    private Map<String, BigInteger> findTxOutputAmountsNonPostgres(String txHash) {
        Map<String, BigInteger> totals = new HashMap<>();

        dsl.select(
                        ADDRESS_UTXO.LOVELACE_AMOUNT.cast(BigDecimal.class).as("lovelace_amount"),
                        ADDRESS_UTXO.AMOUNTS.cast(String.class).as("amounts_str")
                )
                .from(ADDRESS_UTXO)
                .where(ADDRESS_UTXO.TX_HASH.eq(txHash))
                .and(ADDRESS_UTXO.IS_COLLATERAL_RETURN.isFalse().or(ADDRESS_UTXO.IS_COLLATERAL_RETURN.isNull()))
                .fetch()
                .forEach(record -> {
                    BigDecimal lovelace = record.get("lovelace_amount", BigDecimal.class);
                    if (lovelace != null) {
                        totals.merge("lovelace", lovelace.toBigInteger(), BigInteger::add);
                    }
                    boolean includeJsonLovelace = lovelace == null || lovelace.compareTo(BigDecimal.ZERO) == 0;
                    Map<String, BigInteger> assetAmounts = AmountsJsonUtil.toQuantityByUnit(
                            record.get("amounts_str", String.class));
                    assetAmounts.forEach((unit, qty) -> {
                        if ("lovelace".equals(unit) && !includeJsonLovelace) return;
                        totals.merge(unit, qty, BigInteger::add);
                    });
                });

        return totals;
    }

    @Override
    public int countStakeRegistrations(String txHash) {
        return dsl.fetchCount(
                dsl.selectFrom(STAKE_REGISTRATION)
                        .where(STAKE_REGISTRATION.TX_HASH.eq(txHash))
                        .and(STAKE_REGISTRATION.TYPE.eq("STAKE_REGISTRATION"))
        );
    }

    @Override
    public int countPoolRegistrations(String txHash) {
        return dsl.fetchCount(
                dsl.selectFrom(POOL_REGISTRATION)
                        .where(POOL_REGISTRATION.TX_HASH.eq(txHash))
        );
    }

    // --- helpers ---

    private Set<String> buildUtxoKeySet(List<UtxoKey> keys) {
        if (keys == null) return Collections.emptySet();
        return keys.stream()
                .map(k -> k.getTxHash() + ":" + k.getOutputIndex())
                .collect(Collectors.toSet());
    }

    private List<BFAmountDto> buildAmounts(Long lovelaceAmount, String amountsJson) {
        Map<String, BigInteger> totals = new LinkedHashMap<>();

        if (lovelaceAmount != null && lovelaceAmount > 0) {
            totals.put("lovelace", BigInteger.valueOf(lovelaceAmount));
        }

        if (amountsJson != null) {
            AmountsJsonUtil.toQuantityByUnit(amountsJson).forEach((unit, qty) -> {
                if ("lovelace".equals(unit) && lovelaceAmount != null && lovelaceAmount > 0) return;
                totals.merge(unit, qty, BigInteger::add);
            });
        }

        if (totals.isEmpty() && (lovelaceAmount == null || lovelaceAmount == 0)) {
            totals.put("lovelace", BigInteger.ZERO);
        }

        return totals.entrySet().stream()
                .map(e -> BFAmountDto.builder()
                        .unit(e.getKey())
                        .quantity(e.getValue().toString())
                        .build())
                .collect(Collectors.toList());
    }

    private String toBech32PoolId(String poolIdHex) {
        if (poolIdHex == null || poolIdHex.isBlank()) return poolIdHex;
        if (poolIdHex.startsWith("pool1")) return poolIdHex;
        try {
            return Bech32.encode(HexUtil.decodeHexString(poolIdHex), "pool");
        } catch (Exception e) {
            log.warn("Failed to encode pool id to bech32: {}", poolIdHex, e);
            return poolIdHex;
        }
    }

    private List<String> parseJsonStringArray(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isArray()) return Collections.emptyList();
            List<String> result = new ArrayList<>();
            node.forEach(n -> result.add(n.asText()));
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse JSON string array: {}", json, e);
            return Collections.emptyList();
        }
    }

    private List<BFTxPoolRelayDto> parseRelays(String relaysJson) {
        if (relaysJson == null || relaysJson.isBlank()) return Collections.emptyList();
        try {
            JsonNode relaysNode = objectMapper.readTree(relaysJson);
            if (!relaysNode.isArray()) return Collections.emptyList();
            List<BFTxPoolRelayDto> result = new ArrayList<>();
            for (JsonNode relay : relaysNode) {
                result.add(BFTxPoolRelayDto.builder()
                        .ipv4(relay.path("ipv4").isNull() ? null : relay.path("ipv4").asText(null))
                        .ipv6(relay.path("ipv6").isNull() ? null : relay.path("ipv6").asText(null))
                        .dns(relay.path("dns").isNull() ? null : relay.path("dns").asText(null))
                        .dnsSrv(relay.path("dns_srv").isNull() ? null : relay.path("dns_srv").asText(null))
                        .port(relay.has("port") && !relay.path("port").isNull()
                                ? relay.path("port").asInt() : null)
                        .build());
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse relays JSON: {}", relaysJson, e);
            return Collections.emptyList();
        }
    }
}
