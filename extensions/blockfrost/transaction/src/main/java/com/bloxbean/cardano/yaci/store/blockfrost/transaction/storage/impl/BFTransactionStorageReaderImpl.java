package com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage.impl;

import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.blockfrost.common.util.AmountsJsonUtil;
import com.bloxbean.cardano.yaci.store.blockfrost.transaction.dto.*;
import com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage.BFTransactionStorageReader;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorageReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.assets.jooq.Tables.ASSETS;
import static com.bloxbean.cardano.yaci.store.epoch.jooq.Tables.EPOCH_PARAM;
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
    private final com.bloxbean.cardano.yaci.store.common.config.StoreProperties storeProperties;

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

        // For valid txs: exclude collateral return from output_count (it is not a "real" output)
        // For invalid txs: collateral return IS the real output — counted separately
        Field<Integer> outputCount = DSL.selectCount()
                .from(ADDRESS_UTXO)
                .where(ADDRESS_UTXO.TX_HASH.eq(txHash))
                .and(ADDRESS_UTXO.IS_COLLATERAL_RETURN.isFalse().or(ADDRESS_UTXO.IS_COLLATERAL_RETURN.isNull()))
                .asField("output_count");

        Field<Integer> collateralReturnCount = DSL.selectCount()
                .from(ADDRESS_UTXO)
                .where(ADDRESS_UTXO.TX_HASH.eq(txHash))
                .and(ADDRESS_UTXO.IS_COLLATERAL_RETURN.isTrue())
                .asField("collateral_return_count");

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
                        outputCount,
                        collateralReturnCount
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
                                    && record.get(TRANSACTION.TTL) > 0
                                    ? record.get(TRANSACTION.TTL).toString() : null)
                            .withdrawalCount(record.get("withdrawal_count", Integer.class))
                            .delegationCount(record.get("delegation_count", Integer.class))
                            .stakeCertCount(record.get("stake_cert_count", Integer.class))
                            .poolUpdateCount(record.get("pool_update_count", Integer.class))
                            .poolRetireCount(record.get("pool_retire_count", Integer.class))
                            .redeemerCount(record.get("redeemer_count", Integer.class))
                            .assetMintOrBurnCount(record.get("asset_mint_count", Integer.class))
                            .utxoCount(Optional.ofNullable(record.get("input_count", Integer.class)).orElse(0)
                                    + Optional.ofNullable(record.get("output_count", Integer.class)).orElse(0)
                                    // For invalid txs the collateral return is a real output — include it
                                    + (Boolean.TRUE.equals(invalid)
                                            ? Optional.ofNullable(record.get("collateral_return_count", Integer.class)).orElse(0)
                                            : 0))
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
        // For invalid (failed-script) transactions Blockfrost treats the collateral
        // inputs as the "real" inputs and the collateral return as the "real" output,
        // both shown with collateral=false. The yaci-store TX_INPUT table records the
        // consumed collateral inputs for invalid txs, so TX_INPUT already contains them.
        boolean txInvalid = Boolean.TRUE.equals(txn.getInvalid());

        // Inputs: UTXOs spent by this transaction (regular inputs for valid txs;
        // consumed collateral inputs for invalid txs — both stored in TX_INPUT).
        List<BFTxInputDto> inputs = new ArrayList<>(dsl.select(
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
                .fetch(record -> BFTxInputDto.builder()
                        .address(record.get(ADDRESS_UTXO.OWNER_ADDR))
                        .amount(buildAmounts(record.get(ADDRESS_UTXO.LOVELACE_AMOUNT),
                                record.get("amounts_str", String.class)))
                        .txHash(record.get(ADDRESS_UTXO.TX_HASH))
                        .outputIndex(record.get(ADDRESS_UTXO.OUTPUT_INDEX))
                        .dataHash(record.get(ADDRESS_UTXO.DATA_HASH))
                        .inlineDatum(record.get(ADDRESS_UTXO.INLINE_DATUM))
                        .referenceScriptHash(record.get(ADDRESS_UTXO.REFERENCE_SCRIPT_HASH))
                        .collateral(false)
                        .reference(false)
                        .build()));

        // For valid txs: collateral inputs are NOT in TX_INPUT (not consumed) — fetch
        // them separately. A UTXO that serves both roles appears twice (BF behaviour).
        // For invalid txs: collateral inputs are already in TX_INPUT — skip to avoid duplicates.
        if (!txInvalid && !collateralKeys.isEmpty()) {
            inputs.addAll(fetchUtxosByKeys(collateralKeys, true, false));
        }
        if (!referenceKeys.isEmpty()) {
            inputs.addAll(fetchUtxosByKeys(referenceKeys, false, true));
        }

        // Outputs: UTXOs created by this transaction.
        // For invalid txs, BF shows the collateral return with collateral=false.
        List<BFTxOutputDto> outputs = new ArrayList<>(dsl.select(
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
                        // invalid txs: collateral return shown as collateral=false (BF behaviour)
                        .collateral(!txInvalid && Boolean.TRUE.equals(record.get(ADDRESS_UTXO.IS_COLLATERAL_RETURN)))
                        .consumedByTx(record.get(TX_INPUT.SPENT_TX_HASH))
                        .build()));

        // For valid transactions the collateral return output is not in address_utxo —
        // it is stored in transaction.collateral_return_json. Add it if not already present.
        // (For invalid txs the collateral return IS in address_utxo, so skip this block.)
        boolean hasCollateralOutput = txInvalid || outputs.stream().anyMatch(o -> Boolean.TRUE.equals(o.getCollateral()));
        if (!hasCollateralOutput) {
            dsl.select(TRANSACTION.COLLATERAL_RETURN, TRANSACTION.COLLATERAL_RETURN_JSON)
                    .from(TRANSACTION)
                    .where(TRANSACTION.TX_HASH.eq(txHash))
                    .fetchOptional()
                    .ifPresent(record -> {
                        org.jooq.JSON returnRef = record.get(TRANSACTION.COLLATERAL_RETURN);
                        org.jooq.JSON returnJson = record.get(TRANSACTION.COLLATERAL_RETURN_JSON);
                        if (returnRef != null && returnJson != null) {
                            try {
                                JsonNode refNode = objectMapper.readTree(returnRef.toString());
                                JsonNode jsonNode = objectMapper.readTree(returnJson.toString());
                                Integer outputIndex = refNode.has("output_index")
                                        ? refNode.path("output_index").asInt() : null;
                                String address = jsonNode.path("address").asText(null);
                                List<BFAmountDto> amounts = parseCollateralReturnAmounts(jsonNode.path("amounts"));
                                String dataHash = jsonNode.path("dataHash").isNull() ? null : jsonNode.path("dataHash").asText(null);
                                String inlineDatum = jsonNode.path("inlineDatum").isNull() ? null : jsonNode.path("inlineDatum").asText(null);
                                String refScriptHash = jsonNode.path("referenceScriptHash").isNull() ? null : jsonNode.path("referenceScriptHash").asText(null);
                                outputs.add(BFTxOutputDto.builder()
                                        .address(address)
                                        .amount(amounts)
                                        .outputIndex(outputIndex)
                                        .dataHash(dataHash)
                                        .inlineDatum(inlineDatum)
                                        .referenceScriptHash(refScriptHash)
                                        .collateral(true)
                                        .consumedByTx(null)
                                        .build());
                            } catch (Exception e) {
                                log.warn("Failed to parse collateral_return_json for tx {}: {}", txHash, e.getMessage());
                            }
                        }
                    });
        }

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
        // Resolve execution unit prices from protocol params for this tx's epoch
        BigDecimal priceMem = null;
        BigDecimal priceStep = null;
        try {
            Integer epoch = dsl.select(TRANSACTION.EPOCH)
                    .from(TRANSACTION)
                    .where(TRANSACTION.TX_HASH.eq(txHash))
                    .fetchOne(TRANSACTION.EPOCH);
            if (epoch != null) {
                String paramsJson = dsl.select(EPOCH_PARAM.PARAMS)
                        .from(EPOCH_PARAM)
                        .where(EPOCH_PARAM.EPOCH.eq(epoch))
                        .fetchOne(r -> r.get(EPOCH_PARAM.PARAMS) != null
                                ? r.get(EPOCH_PARAM.PARAMS).toString() : null);
                if (paramsJson != null) {
                    JsonNode params = objectMapper.readTree(paramsJson);
                    JsonNode pMemNode = params.path("price_mem");
                    JsonNode pStepNode = params.path("price_step");
                    if (!pMemNode.isMissingNode() && pMemNode.has("numerator")) {
                        priceMem = BigDecimal.valueOf(pMemNode.path("numerator").asLong())
                                .divide(BigDecimal.valueOf(pMemNode.path("denominator").asLong()), 20, RoundingMode.HALF_UP);
                    }
                    if (!pStepNode.isMissingNode() && pStepNode.has("numerator")) {
                        priceStep = BigDecimal.valueOf(pStepNode.path("numerator").asLong())
                                .divide(BigDecimal.valueOf(pStepNode.path("denominator").asLong()), 20, RoundingMode.HALF_UP);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch protocol params for redeemer fee computation, tx {}: {}", txHash, e.getMessage());
        }

        final BigDecimal finalPriceMem = priceMem;
        final BigDecimal finalPriceStep = priceStep;

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
                .fetch(record -> {
                    Long unitMem = record.get(TRANSACTION_SCRIPTS.UNIT_MEM);
                    Long unitSteps = record.get(TRANSACTION_SCRIPTS.UNIT_STEPS);
                    String fee = null;
                    if (finalPriceMem != null && finalPriceStep != null
                            && unitMem != null && unitSteps != null) {
                        fee = finalPriceMem.multiply(BigDecimal.valueOf(unitMem))
                                .add(finalPriceStep.multiply(BigDecimal.valueOf(unitSteps)))
                                .setScale(0, RoundingMode.CEILING)
                                .toBigInteger()
                                .toString();
                    }
                    return BFTxRedeemerDto.builder()
                            .txIndex(record.get(TRANSACTION_SCRIPTS.REDEEMER_INDEX))
                            .purpose(record.get(TRANSACTION_SCRIPTS.PURPOSE) != null
                                    ? record.get(TRANSACTION_SCRIPTS.PURPOSE).toLowerCase() : null)
                            .scriptHash(record.get(TRANSACTION_SCRIPTS.SCRIPT_HASH))
                            .redeemerDataHash(record.get(TRANSACTION_SCRIPTS.REDEEMER_DATAHASH))
                            .datumHash(record.get(TRANSACTION_SCRIPTS.REDEEMER_DATAHASH))
                            .unitMem(unitMem != null ? unitMem.toString() : null)
                            .unitSteps(unitSteps != null ? unitSteps.toString() : null)
                            .fee(fee)
                            .build();
                });
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
                    // metadata is always returned as an object (fields null when not stored)
                    BFTxPoolUpdateDto.PoolMetadata poolMetadata = BFTxPoolUpdateDto.PoolMetadata.builder()
                            .url(record.get(POOL_REGISTRATION.METADATA_URL))
                            .hash(record.get(POOL_REGISTRATION.METADATA_HASH))
                            .ticker(null)
                            .name(null)
                            .description(null)
                            .homepage(null)
                            .build();

                    List<String> owners = parseOwners(record.get(POOL_REGISTRATION.POOL_OWNERS) != null
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
                            .activeEpoch(epoch != null ? epoch + 2 : null)
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
    public List<BFAmountDto> findTxOutputAmounts(String txHash) {
        // Blockfrost behaviour: lovelace is aggregated (single entry); non-lovelace tokens
        // are returned per-output — same token appearing in N outputs yields N entries.
        // For invalid txs the collateral return IS the real output — include it.
        boolean txInvalid = dsl.select(TRANSACTION.INVALID).from(TRANSACTION)
                .where(TRANSACTION.TX_HASH.eq(txHash))
                .fetchOne(r -> Boolean.TRUE.equals(r.get(TRANSACTION.INVALID)));

        BigInteger lovelaceTotal = BigInteger.ZERO;
        List<BFAmountDto> nonLovelace = new ArrayList<>();

        List<org.jooq.Record2<Long, String>> rows = dsl.select(
                        ADDRESS_UTXO.LOVELACE_AMOUNT,
                        ADDRESS_UTXO.AMOUNTS.cast(String.class).as("amounts_str")
                )
                .from(ADDRESS_UTXO)
                .where(ADDRESS_UTXO.TX_HASH.eq(txHash))
                // For valid txs: exclude collateral return; for invalid txs: include it (it's the real output)
                .and(txInvalid
                        ? DSL.trueCondition()
                        : ADDRESS_UTXO.IS_COLLATERAL_RETURN.isFalse().or(ADDRESS_UTXO.IS_COLLATERAL_RETURN.isNull()))
                .orderBy(ADDRESS_UTXO.OUTPUT_INDEX.asc())
                .fetch();

        for (org.jooq.Record2<Long, String> row : rows) {
            Long lovelace = row.value1();
            if (lovelace != null && lovelace > 0) {
                lovelaceTotal = lovelaceTotal.add(BigInteger.valueOf(lovelace));
            }
            String amountsJson = row.value2();
            if (amountsJson != null) {
                AmountsJsonUtil.toQuantityByUnit(amountsJson).entrySet().stream()
                        .filter(e -> !"lovelace".equals(e.getKey()))
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(e -> nonLovelace.add(
                                BFAmountDto.builder().unit(e.getKey()).quantity(e.getValue().toString()).build()));
            }
        }

        List<BFAmountDto> result = new ArrayList<>();
        result.add(BFAmountDto.builder().unit("lovelace").quantity(lovelaceTotal.toString()).build());
        result.addAll(nonLovelace);
        return result;
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

    private List<BFAmountDto> parseCollateralReturnAmounts(JsonNode amountsNode) {
        if (amountsNode == null || !amountsNode.isArray()) return Collections.emptyList();
        // Blockfrost only shows lovelace for the collateral return output
        for (JsonNode a : amountsNode) {
            String unit = a.path("unit").asText(null);
            if ("lovelace".equals(unit)) {
                return Collections.singletonList(
                        BFAmountDto.builder().unit("lovelace").quantity(String.valueOf(a.path("quantity").asLong(0))).build());
            }
        }
        return Collections.emptyList();
    }

    private List<BFTxInputDto> fetchUtxosByKeys(Set<String> utxoKeys, boolean collateral, boolean reference) {
        if (utxoKeys.isEmpty()) return Collections.emptyList();
        org.jooq.Condition condition = utxoKeys.stream()
                .map(key -> {
                    String[] parts = key.split(":");
                    return ADDRESS_UTXO.TX_HASH.eq(parts[0])
                            .and(ADDRESS_UTXO.OUTPUT_INDEX.eq(Integer.parseInt(parts[1])));
                })
                .reduce(DSL.noCondition(), org.jooq.Condition::or);
        return dsl.select(
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
                .where(condition)
                .fetch(record -> BFTxInputDto.builder()
                        .address(record.get(ADDRESS_UTXO.OWNER_ADDR))
                        // Blockfrost only shows lovelace for collateral inputs
                        .amount(collateral
                                ? lovelaceOnlyAmount(record.get(ADDRESS_UTXO.LOVELACE_AMOUNT))
                                : buildAmounts(record.get(ADDRESS_UTXO.LOVELACE_AMOUNT),
                                        record.get("amounts_str", String.class)))
                        .txHash(record.get(ADDRESS_UTXO.TX_HASH))
                        .outputIndex(record.get(ADDRESS_UTXO.OUTPUT_INDEX))
                        .dataHash(record.get(ADDRESS_UTXO.DATA_HASH))
                        .inlineDatum(record.get(ADDRESS_UTXO.INLINE_DATUM))
                        .referenceScriptHash(record.get(ADDRESS_UTXO.REFERENCE_SCRIPT_HASH))
                        .collateral(collateral)
                        .reference(reference)
                        .build());
    }

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

        // lovelace first, then assets sorted alphabetically by unit
        return totals.entrySet().stream()
                .sorted((a, b) -> {
                    if ("lovelace".equals(a.getKey())) return -1;
                    if ("lovelace".equals(b.getKey())) return 1;
                    return a.getKey().compareTo(b.getKey());
                })
                .map(e -> BFAmountDto.builder()
                        .unit(e.getKey())
                        .quantity(e.getValue().toString())
                        .build())
                .collect(Collectors.toList());
    }

    private List<BFAmountDto> lovelaceOnlyAmount(Long lovelaceAmount) {
        long qty = lovelaceAmount != null ? lovelaceAmount : 0L;
        return Collections.singletonList(BFAmountDto.builder().unit("lovelace").quantity(String.valueOf(qty)).build());
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

    // Pool owners are stored as raw stake key hashes; convert to bech32 stake addresses
    private List<String> parseOwners(String json) {
        List<String> hashes = parseJsonStringArray(json);
        boolean isMainnet = storeProperties.getProtocolMagic() == 764824073L;
        String prefix = isMainnet ? "stake" : "stake_test";
        byte headerByte = isMainnet ? (byte) 0xe1 : (byte) 0xe0;
        return hashes.stream()
                .map(hash -> {
                    try {
                        byte[] keyHash = HexUtil.decodeHexString(hash);
                        byte[] stakeAddrBytes = new byte[keyHash.length + 1];
                        stakeAddrBytes[0] = headerByte;
                        System.arraycopy(keyHash, 0, stakeAddrBytes, 1, keyHash.length);
                        return Bech32.encode(stakeAddrBytes, prefix);
                    } catch (Exception e) {
                        log.warn("Failed to encode pool owner to stake address: {}", hash, e);
                        return hash;
                    }
                })
                .collect(Collectors.toList());
    }

    private List<BFTxPoolRelayDto> parseRelays(String relaysJson) {
        if (relaysJson == null || relaysJson.isBlank()) return Collections.emptyList();
        try {
            JsonNode relaysNode = objectMapper.readTree(relaysJson);
            if (!relaysNode.isArray()) return Collections.emptyList();
            List<BFTxPoolRelayDto> result = new ArrayList<>();
            for (JsonNode relay : relaysNode) {
                // DB stores dnsName / dnsSrv; Blockfrost API uses dns / dns_srv
                String dns = nullIfEmpty(relay.path("dnsName").isNull() ? null : relay.path("dnsName").asText(null));
                String dnsSrv = nullIfEmpty(relay.path("dnsSrv").isNull() ? null : relay.path("dnsSrv").asText(null));
                Integer port = relay.has("port") && !relay.path("port").isNull() && relay.path("port").asInt() > 0
                        ? relay.path("port").asInt() : null;
                result.add(BFTxPoolRelayDto.builder()
                        .ipv4(nullIfEmpty(relay.path("ipv4").isNull() ? null : relay.path("ipv4").asText(null)))
                        .ipv6(nullIfEmpty(relay.path("ipv6").isNull() ? null : relay.path("ipv6").asText(null)))
                        .dns(dns)
                        .dnsSrv(dnsSrv)
                        .port(port)
                        .build());
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse relays JSON: {}", relaysJson, e);
            return Collections.emptyList();
        }
    }

    private String nullIfEmpty(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
