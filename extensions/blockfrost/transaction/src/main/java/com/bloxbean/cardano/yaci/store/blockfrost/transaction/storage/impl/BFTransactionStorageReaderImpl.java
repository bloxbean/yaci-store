package com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage.impl;

import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.util.Constants;
import com.bloxbean.cardano.yaci.store.blockfrost.common.util.AmountsJsonUtil;
import com.bloxbean.cardano.yaci.store.blockfrost.transaction.dto.BFAmountDto;
import com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage.BFTransactionStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage.impl.model.*;
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
import static com.bloxbean.cardano.yaci.store.governance.jooq.Tables.DREP_REGISTRATION;
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
    public Optional<TxRaw> findTransactionByHash(String txHash) {
        Field<Integer> withdrawalCount = DSL.selectCount()
                .from(WITHDRAWAL).where(WITHDRAWAL.TX_HASH.eq(txHash))
                .asField("withdrawal_count");

        Field<Integer> delegationCount = DSL.selectCount()
                .from(DELEGATION).where(DELEGATION.TX_HASH.eq(txHash))
                .asField("delegation_count");

        Field<Integer> stakeCertCount = DSL.selectCount()
                .from(STAKE_REGISTRATION).where(STAKE_REGISTRATION.TX_HASH.eq(txHash))
                .asField("stake_cert_count");

        Field<Integer> poolUpdateCount = DSL.selectCount()
                .from(POOL_REGISTRATION).where(POOL_REGISTRATION.TX_HASH.eq(txHash))
                .asField("pool_update_count");

        Field<Integer> poolRetireCount = DSL.selectCount()
                .from(POOL_RETIREMENT).where(POOL_RETIREMENT.TX_HASH.eq(txHash))
                .asField("pool_retire_count");

        Field<Integer> redeemerCount = DSL.selectCount()
                .from(TRANSACTION_SCRIPTS).where(TRANSACTION_SCRIPTS.TX_HASH.eq(txHash))
                .asField("redeemer_count");

        Field<Integer> assetMintCount = DSL.selectCount()
                .from(ASSETS).where(ASSETS.TX_HASH.eq(txHash))
                .asField("asset_mint_count");

        Field<Integer> inputCount = DSL.selectCount()
                .from(TX_INPUT).where(TX_INPUT.SPENT_TX_HASH.eq(txHash))
                .asField("input_count");

        Field<Integer> outputCount = DSL.selectCount()
                .from(ADDRESS_UTXO)
                .where(ADDRESS_UTXO.TX_HASH.eq(txHash))
                .and(ADDRESS_UTXO.IS_COLLATERAL_RETURN.isFalse()
                        .or(ADDRESS_UTXO.IS_COLLATERAL_RETURN.isNull()))
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
                        TRANSACTION.EPOCH,
                        TRANSACTION.FEE,
                        TRANSACTION.TTL,
                        TRANSACTION.VALIDITY_INTERVAL_START,
                        TRANSACTION.INVALID,
                        TRANSACTION_CBOR.CBOR_SIZE,
                        withdrawalCount, delegationCount, stakeCertCount,
                        poolUpdateCount, poolRetireCount, redeemerCount,
                        assetMintCount, inputCount, outputCount, collateralReturnCount
                )
                .from(TRANSACTION)
                .leftJoin(TRANSACTION_CBOR).on(TRANSACTION_CBOR.TX_HASH.eq(TRANSACTION.TX_HASH))
                .where(TRANSACTION.TX_HASH.eq(txHash))
                .fetchOptional(record -> TxRaw.builder()
                        .txHash(record.get(TRANSACTION.TX_HASH))
                        .blockHash(record.get(TRANSACTION.BLOCK_HASH))
                        .blockNumber(record.get(TRANSACTION.BLOCK))
                        .blockTime(record.get(TRANSACTION.BLOCK_TIME))
                        .slot(record.get(TRANSACTION.SLOT))
                        .txIndex(record.get(TRANSACTION.TX_INDEX))
                        .epoch(record.get(TRANSACTION.EPOCH))
                        .fees(record.get(TRANSACTION.FEE) != null ? BigInteger.valueOf(record.get(TRANSACTION.FEE)) : null)
                        .ttl(record.get(TRANSACTION.TTL))
                        .validityIntervalStart(record.get(TRANSACTION.VALIDITY_INTERVAL_START))
                        .invalid(record.get(TRANSACTION.INVALID))
                        .cborSize(record.get(TRANSACTION_CBOR.CBOR_SIZE))
                        .withdrawalCount(orZero(record.get("withdrawal_count", Integer.class)))
                        .delegationCount(orZero(record.get("delegation_count", Integer.class)))
                        .stakeCertCount(orZero(record.get("stake_cert_count", Integer.class)))
                        .poolUpdateCount(orZero(record.get("pool_update_count", Integer.class)))
                        .poolRetireCount(orZero(record.get("pool_retire_count", Integer.class)))
                        .redeemerCount(orZero(record.get("redeemer_count", Integer.class)))
                        .assetMintOrBurnCount(orZero(record.get("asset_mint_count", Integer.class)))
                        .inputCount(orZero(record.get("input_count", Integer.class)))
                        .outputCount(orZero(record.get("output_count", Integer.class)))
                        .collateralReturnCount(orZero(record.get("collateral_return_count", Integer.class)))
                        .build());
    }

    @Override
    public Optional<TxUtxosRaw> findTxUtxos(String txHash) {
        Optional<Txn> txnOpt = transactionStorageReader.getTransactionByTxHash(txHash);
        if (txnOpt.isEmpty()) {
            return Optional.empty();
        }
        Txn txn = txnOpt.get();

        Set<String> collateralKeys = buildUtxoKeySet(txn.getCollateralInputs());
        Set<String> referenceKeys = buildUtxoKeySet(txn.getReferenceInputs());
        boolean txInvalid = Boolean.TRUE.equals(txn.getInvalid());

        // Regular inputs (and collateral inputs for invalid txs — already in TX_INPUT)
        List<TxInputRaw> inputs = new ArrayList<>(dsl.select(
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
                .fetch(record -> TxInputRaw.builder()
                        .txHash(record.get(ADDRESS_UTXO.TX_HASH))
                        .outputIndex(record.get(ADDRESS_UTXO.OUTPUT_INDEX))
                        .address(record.get(ADDRESS_UTXO.OWNER_ADDR))
                        .lovelaceAmount(record.get(ADDRESS_UTXO.LOVELACE_AMOUNT))
                        .amountsJson(record.get("amounts_str", String.class))
                        .dataHash(record.get(ADDRESS_UTXO.DATA_HASH))
                        .inlineDatum(record.get(ADDRESS_UTXO.INLINE_DATUM))
                        .referenceScriptHash(record.get(ADDRESS_UTXO.REFERENCE_SCRIPT_HASH))
                        .collateral(false)
                        .reference(false)
                        .build()));

        // Collateral inputs for valid txs (not in TX_INPUT — fetch by key set)
        if (!txInvalid && !collateralKeys.isEmpty()) {
            inputs.addAll(fetchInputsByKeys(collateralKeys, true, false));
        }
        // Reference inputs
        if (!referenceKeys.isEmpty()) {
            inputs.addAll(fetchInputsByKeys(referenceKeys, false, true));
        }

        // Outputs
        List<TxOutputRaw> outputs = new ArrayList<>(dsl.select(
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
                .fetch(record -> TxOutputRaw.builder()
                        .txHash(record.get(ADDRESS_UTXO.TX_HASH))
                        .outputIndex(record.get(ADDRESS_UTXO.OUTPUT_INDEX))
                        .address(record.get(ADDRESS_UTXO.OWNER_ADDR))
                        .lovelaceAmount(record.get(ADDRESS_UTXO.LOVELACE_AMOUNT))
                        .amountsJson(record.get("amounts_str", String.class))
                        .dataHash(record.get(ADDRESS_UTXO.DATA_HASH))
                        .inlineDatum(record.get(ADDRESS_UTXO.INLINE_DATUM))
                        .referenceScriptHash(record.get(ADDRESS_UTXO.REFERENCE_SCRIPT_HASH))
                        .isCollateralReturn(record.get(ADDRESS_UTXO.IS_COLLATERAL_RETURN))
                        .consumedByTx(record.get(TX_INPUT.SPENT_TX_HASH))
                        .build()));

        // Collateral return JSON from TRANSACTION table (for valid txs)
        String collateralReturnRefJson = null;
        String collateralReturnDataJson = null;
        var collateralRecord = dsl.select(TRANSACTION.COLLATERAL_RETURN, TRANSACTION.COLLATERAL_RETURN_JSON)
                .from(TRANSACTION)
                .where(TRANSACTION.TX_HASH.eq(txHash))
                .fetchOptional()
                .orElse(null);
        if (collateralRecord != null) {
            org.jooq.JSON returnRef = collateralRecord.get(TRANSACTION.COLLATERAL_RETURN);
            org.jooq.JSON returnJson = collateralRecord.get(TRANSACTION.COLLATERAL_RETURN_JSON);
            if (returnRef != null && returnJson != null) {
                collateralReturnRefJson = returnRef.toString();
                collateralReturnDataJson = returnJson.toString();
            }
        }

        return Optional.of(TxUtxosRaw.builder()
                .txInvalid(txInvalid)
                .inputs(inputs)
                .outputs(outputs)
                .collateralReturnRefJson(collateralReturnRefJson)
                .collateralReturnDataJson(collateralReturnDataJson)
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
    public List<TxRedeemerRaw> findTxRedeemers(String txHash) {
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
                .fetch(record -> TxRedeemerRaw.builder()
                        .txIndex(record.get(TRANSACTION_SCRIPTS.REDEEMER_INDEX))
                        .purpose(record.get(TRANSACTION_SCRIPTS.PURPOSE))
                        .scriptHash(record.get(TRANSACTION_SCRIPTS.SCRIPT_HASH))
                        .redeemerDatahash(record.get(TRANSACTION_SCRIPTS.REDEEMER_DATAHASH))
                        .unitMem(record.get(TRANSACTION_SCRIPTS.UNIT_MEM))
                        .unitSteps(record.get(TRANSACTION_SCRIPTS.UNIT_STEPS))
                        .build());
    }

    @Override
    public Optional<TxRedeemerPricesRaw> findRedeemerPrices(String txHash) {
        try {
            Integer epoch = dsl.select(TRANSACTION.EPOCH)
                    .from(TRANSACTION)
                    .where(TRANSACTION.TX_HASH.eq(txHash))
                    .fetchOne(TRANSACTION.EPOCH);
            if (epoch == null) return Optional.empty();

            String paramsJson = dsl.select(EPOCH_PARAM.PARAMS)
                    .from(EPOCH_PARAM)
                    .where(EPOCH_PARAM.EPOCH.eq(epoch))
                    .fetchOne(r -> r.get(EPOCH_PARAM.PARAMS) != null
                            ? r.get(EPOCH_PARAM.PARAMS).toString() : null);
            if (paramsJson == null) return Optional.empty();

            JsonNode params = objectMapper.readTree(paramsJson);
            BigDecimal priceMem = null;
            BigDecimal priceStep = null;

            JsonNode pMemNode = params.path("price_mem");
            if (!pMemNode.isMissingNode() && pMemNode.has("numerator")) {
                priceMem = new BigDecimal(new BigInteger(pMemNode.path("numerator").asText()))
                        .divide(new BigDecimal(new BigInteger(pMemNode.path("denominator").asText())),
                                20, RoundingMode.HALF_UP);
            }
            JsonNode pStepNode = params.path("price_step");
            if (!pStepNode.isMissingNode() && pStepNode.has("numerator")) {
                priceStep = new BigDecimal(new BigInteger(pStepNode.path("numerator").asText()))
                        .divide(new BigDecimal(new BigInteger(pStepNode.path("denominator").asText())),
                                20, RoundingMode.HALF_UP);
            }
            return Optional.of(TxRedeemerPricesRaw.builder()
                    .priceMem(priceMem).priceStep(priceStep).build());
        } catch (Exception e) {
            log.warn("Failed to fetch redeemer prices for tx {}: {}", txHash, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<TxStakeRaw> findTxStakes(String txHash) {
        return dsl.select(
                        STAKE_REGISTRATION.CERT_INDEX,
                        STAKE_REGISTRATION.ADDRESS,
                        STAKE_REGISTRATION.TYPE
                )
                .from(STAKE_REGISTRATION)
                .where(STAKE_REGISTRATION.TX_HASH.eq(txHash))
                .orderBy(STAKE_REGISTRATION.CERT_INDEX.asc())
                .fetch(record -> TxStakeRaw.builder()
                        .certIndex(record.get(STAKE_REGISTRATION.CERT_INDEX))
                        .address(record.get(STAKE_REGISTRATION.ADDRESS))
                        .type(record.get(STAKE_REGISTRATION.TYPE))
                        .build());
    }

    @Override
    public List<TxDelegationRaw> findTxDelegations(String txHash) {
        return dsl.select(
                        DELEGATION.CERT_INDEX,
                        DELEGATION.ADDRESS,
                        DELEGATION.POOL_ID,
                        DELEGATION.EPOCH
                )
                .from(DELEGATION)
                .where(DELEGATION.TX_HASH.eq(txHash))
                .orderBy(DELEGATION.CERT_INDEX.asc())
                .fetch(record -> TxDelegationRaw.builder()
                        .certIndex(record.get(DELEGATION.CERT_INDEX))
                        .address(record.get(DELEGATION.ADDRESS))
                        .poolIdHex(record.get(DELEGATION.POOL_ID))
                        .epoch(record.get(DELEGATION.EPOCH))
                        .build());
    }

    @Override
    public List<TxWithdrawalRaw> findTxWithdrawals(String txHash) {
        return dsl.select(WITHDRAWAL.ADDRESS, WITHDRAWAL.AMOUNT)
                .from(WITHDRAWAL)
                .where(WITHDRAWAL.TX_HASH.eq(txHash))
                .fetch(record -> TxWithdrawalRaw.builder()
                        .address(record.get(WITHDRAWAL.ADDRESS))
                        .amount(record.get(WITHDRAWAL.AMOUNT))
                        .build());
    }

    @Override
    public List<TxPoolUpdateRaw> findTxPoolUpdates(String txHash) {
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
                .fetch(record -> TxPoolUpdateRaw.builder()
                        .certIndex(record.get(POOL_REGISTRATION.CERT_INDEX))
                        .poolIdHex(record.get(POOL_REGISTRATION.POOL_ID))
                        .vrfKey(record.get(POOL_REGISTRATION.VRF_KEY))
                        .pledge(record.get(POOL_REGISTRATION.PLEDGE))
                        .margin(record.get(POOL_REGISTRATION.MARGIN))
                        .cost(record.get(POOL_REGISTRATION.COST))
                        .rewardAccount(record.get(POOL_REGISTRATION.REWARD_ACCOUNT))
                        .poolOwnersJson(record.get(POOL_REGISTRATION.POOL_OWNERS) != null
                                ? record.get(POOL_REGISTRATION.POOL_OWNERS).toString() : null)
                        .relaysJson(record.get(POOL_REGISTRATION.RELAYS) != null
                                ? record.get(POOL_REGISTRATION.RELAYS).toString() : null)
                        .metadataUrl(record.get(POOL_REGISTRATION.METADATA_URL))
                        .metadataHash(record.get(POOL_REGISTRATION.METADATA_HASH))
                        .epoch(record.get(POOL_REGISTRATION.EPOCH))
                        .build());
    }

    @Override
    public List<TxPoolRetireRaw> findTxPoolRetires(String txHash) {
        return dsl.select(
                        POOL_RETIREMENT.CERT_INDEX,
                        POOL_RETIREMENT.POOL_ID,
                        POOL_RETIREMENT.RETIREMENT_EPOCH
                )
                .from(POOL_RETIREMENT)
                .where(POOL_RETIREMENT.TX_HASH.eq(txHash))
                .orderBy(POOL_RETIREMENT.CERT_INDEX.asc())
                .fetch(record -> TxPoolRetireRaw.builder()
                        .certIndex(record.get(POOL_RETIREMENT.CERT_INDEX))
                        .poolIdHex(record.get(POOL_RETIREMENT.POOL_ID))
                        .retirementEpoch(record.get(POOL_RETIREMENT.RETIREMENT_EPOCH))
                        .build());
    }

    @Override
    public List<BFAmountDto> findTxOutputAmounts(String txHash) {
        Boolean txInvalidRaw = dsl.select(TRANSACTION.INVALID).from(TRANSACTION)
                .where(TRANSACTION.TX_HASH.eq(txHash))
                .fetchOne(r -> r.get(TRANSACTION.INVALID));
        boolean txInvalid = Boolean.TRUE.equals(txInvalidRaw);

        BigInteger lovelaceTotal = BigInteger.ZERO;
        List<BFAmountDto> nonLovelace = new ArrayList<>();

        List<org.jooq.Record2<Long, String>> rows = dsl.select(
                        ADDRESS_UTXO.LOVELACE_AMOUNT,
                        ADDRESS_UTXO.AMOUNTS.cast(String.class).as("amounts_str")
                )
                .from(ADDRESS_UTXO)
                .where(ADDRESS_UTXO.TX_HASH.eq(txHash))
                .and(txInvalid
                        ? DSL.trueCondition()
                        : ADDRESS_UTXO.IS_COLLATERAL_RETURN.isFalse()
                        .or(ADDRESS_UTXO.IS_COLLATERAL_RETURN.isNull()))
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
                        .filter(e -> !Constants.LOVELACE.equals(e.getKey()))
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(e -> nonLovelace.add(
                                BFAmountDto.builder().unit(e.getKey())
                                        .quantity(e.getValue().toString()).build()));
            }
        }

        List<BFAmountDto> result = new ArrayList<>();
        result.add(BFAmountDto.builder().unit(Constants.LOVELACE).quantity(lovelaceTotal.toString()).build());
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
    public int countStakeDeregistrations(String txHash) {
        return dsl.fetchCount(
                dsl.selectFrom(STAKE_REGISTRATION)
                        .where(STAKE_REGISTRATION.TX_HASH.eq(txHash))
                        .and(STAKE_REGISTRATION.TYPE.eq("STAKE_DEREGISTRATION"))
        );
    }

    @Override
    public int countPoolRegistrations(String txHash) {
        return dsl.fetchCount(
                dsl.selectFrom(POOL_REGISTRATION)
                        .where(POOL_REGISTRATION.TX_HASH.eq(txHash))
        );
    }

    @Override
    public BigInteger sumDrepDeposit(String txHash) {
        // deposit is positive for DRep registration, negative for DRep deregistration.
        return dsl.select(DREP_REGISTRATION.DEPOSIT)
                .from(DREP_REGISTRATION)
                .where(DREP_REGISTRATION.TX_HASH.eq(txHash))
                .fetch(DREP_REGISTRATION.DEPOSIT)
                .stream()
                .filter(java.util.Objects::nonNull)
                .map(BigInteger::valueOf)
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    // --- helpers ---

    private List<TxInputRaw> fetchInputsByKeys(Set<String> utxoKeys,
                                                boolean collateral, boolean reference) {
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
                .fetch(record -> TxInputRaw.builder()
                        .txHash(record.get(ADDRESS_UTXO.TX_HASH))
                        .outputIndex(record.get(ADDRESS_UTXO.OUTPUT_INDEX))
                        .address(record.get(ADDRESS_UTXO.OWNER_ADDR))
                        .lovelaceAmount(record.get(ADDRESS_UTXO.LOVELACE_AMOUNT))
                        .amountsJson(record.get("amounts_str", String.class))
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

    private int orZero(Integer value) {
        return value != null ? value : 0;
    }
}
