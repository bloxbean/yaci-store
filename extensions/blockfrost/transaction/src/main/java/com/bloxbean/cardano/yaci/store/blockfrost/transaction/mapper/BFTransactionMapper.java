package com.bloxbean.cardano.yaci.store.blockfrost.transaction.mapper;

import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.util.Constants;
import com.bloxbean.cardano.yaci.store.blockfrost.common.util.AmountsJsonUtil;
import com.bloxbean.cardano.yaci.store.blockfrost.transaction.dto.*;
import com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage.impl.model.*;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
@Slf4j
public abstract class BFTransactionMapper {

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected StoreProperties storeProperties;

    // ============================================================
    // Abstract methods — MapStruct generates the implementation
    // ============================================================

    @Mapping(target = "registration",
            expression = "java(\"STAKE_REGISTRATION\".equals(raw.getType()))")
    public abstract BFTxStakeDto toStakeDto(TxStakeRaw raw);

    public abstract List<BFTxStakeDto> toStakeDtos(List<TxStakeRaw> raws);

    @Mapping(target = "amount", qualifiedByName = "bigIntegerToString")
    public abstract BFTxWithdrawalDto toWithdrawalDto(TxWithdrawalRaw raw);

    public abstract List<BFTxWithdrawalDto> toWithdrawalDtos(List<TxWithdrawalRaw> raws);

    @Mapping(target = "index", source = "certIndex")
    @Mapping(target = "poolId", source = "poolIdHex", qualifiedByName = "toBech32PoolId")
    @Mapping(target = "activeEpoch",
            expression = "java(raw.getEpoch() != null ? raw.getEpoch() + 2 : null)")
    public abstract BFTxDelegationDto toDelegationDto(TxDelegationRaw raw);

    public abstract List<BFTxDelegationDto> toDelegationDtos(List<TxDelegationRaw> raws);

    @Mapping(target = "poolId", source = "poolIdHex", qualifiedByName = "toBech32PoolId")
    @Mapping(target = "retiringEpoch", source = "retirementEpoch")
    public abstract BFTxPoolRetireDto toPoolRetireDto(TxPoolRetireRaw raw);

    public abstract List<BFTxPoolRetireDto> toPoolRetireDtos(List<TxPoolRetireRaw> raws);

    // ============================================================
    // Named conversion helpers used by MapStruct
    // ============================================================

    @Named("bigIntegerToString")
    public String bigIntegerToString(BigInteger value) {
        return value != null ? value.toString() : "0";
    }

    @Named("toBech32PoolId")
    public String toBech32PoolId(String poolIdHex) {
        if (poolIdHex == null || poolIdHex.isBlank()) return poolIdHex;
        if (poolIdHex.startsWith("pool1")) return poolIdHex;
        try {
            return Bech32.encode(HexUtil.decodeHexString(poolIdHex), "pool");
        } catch (Exception e) {
            log.warn("Failed to encode pool id to bech32: {}", poolIdHex, e);
            return poolIdHex;
        }
    }

    // ============================================================
    // Complex concrete methods — manual implementations kept here
    // because they involve multi-source params, JSON parsing,
    // fee computation, or StoreProperties-dependent logic.
    // ============================================================

    public BFTransactionDto toTransactionDto(TxRaw raw, List<BFAmountDto> outputAmounts,
                                             String deposit, int mirCertCount) {
        Boolean invalid = raw.getInvalid();
        int utxoCount = raw.getInputCount() + raw.getOutputCount()
                + (Boolean.TRUE.equals(invalid) ? raw.getCollateralReturnCount() : 0);

        return BFTransactionDto.builder()
                .hash(raw.getTxHash())
                .block(raw.getBlockHash())
                .blockHeight(raw.getBlockNumber() != null ? Math.max(0, raw.getBlockNumber().intValue()) : null)
                .blockTime(raw.getBlockTime())
                .slot(raw.getSlot() != null ? Math.max(0L, raw.getSlot()) : null)
                .index(raw.getTxIndex() != null ? raw.getTxIndex() : 0)
                .fees(raw.getFees() != null ? raw.getFees().toString() : "0")
                .deposit(deposit)
                .size(raw.getCborSize())
                .invalidBefore(raw.getValidityIntervalStart() != null && raw.getValidityIntervalStart() > 0
                        ? raw.getValidityIntervalStart().toString() : null)
                .invalidHereafter(raw.getTtl() != null && raw.getTtl() > 0
                        ? raw.getTtl().toString() : null)
                .outputAmount(outputAmounts)
                .utxoCount(utxoCount)
                .withdrawalCount(raw.getWithdrawalCount())
                .mirCertCount(mirCertCount)
                .delegationCount(raw.getDelegationCount())
                .stakeCertCount(raw.getStakeCertCount())
                .poolUpdateCount(raw.getPoolUpdateCount())
                .poolRetireCount(raw.getPoolRetireCount())
                .assetMintOrBurnCount(raw.getAssetMintOrBurnCount())
                .redeemerCount(raw.getRedeemerCount())
                .validContract(invalid == null || !invalid)
                .build();
    }

    public BFTxUtxosDto toUtxosDto(String txHash, TxUtxosRaw raw) {
        boolean txInvalid = raw.isTxInvalid();

        List<BFTxInputDto> inputs = raw.getInputs().stream()
                .map(i -> BFTxInputDto.builder()
                        .address(i.getAddress())
                        .amount(i.isCollateral()
                                ? lovelaceOnlyAmount(i.getLovelaceAmount())
                                : buildAmounts(i.getLovelaceAmount(), i.getAmountsJson()))
                        .txHash(i.getTxHash())
                        .outputIndex(i.getOutputIndex())
                        .dataHash(i.getDataHash())
                        .inlineDatum(i.getInlineDatum())
                        .referenceScriptHash(i.getReferenceScriptHash())
                        .collateral(i.isCollateral())
                        .reference(i.isReference())
                        .build())
                .collect(Collectors.toList());

        List<BFTxOutputDto> outputs = raw.getOutputs().stream()
                .map(o -> BFTxOutputDto.builder()
                        .address(o.getAddress())
                        .amount(buildAmounts(o.getLovelaceAmount(), o.getAmountsJson()))
                        .outputIndex(o.getOutputIndex())
                        .dataHash(o.getDataHash())
                        .inlineDatum(o.getInlineDatum())
                        .referenceScriptHash(o.getReferenceScriptHash())
                        .collateral(!txInvalid && Boolean.TRUE.equals(o.getIsCollateralReturn()))
                        .consumedByTx(o.getConsumedByTx())
                        .build())
                .collect(Collectors.toList());

        // For valid txs: collateral return in TRANSACTION JSON columns — parse and append
        boolean hasCollateralOutput = txInvalid
                || outputs.stream().anyMatch(o -> Boolean.TRUE.equals(o.getCollateral()));
        if (!hasCollateralOutput
                && raw.getCollateralReturnRefJson() != null
                && raw.getCollateralReturnDataJson() != null) {
            parseCollateralReturnOutput(raw.getCollateralReturnRefJson(),
                    raw.getCollateralReturnDataJson(), txHash)
                    .ifPresent(outputs::add);
        }

        return BFTxUtxosDto.builder()
                .hash(txHash)
                .inputs(inputs)
                .outputs(outputs)
                .build();
    }

    public List<BFTxRedeemerDto> toRedeemerDtos(List<TxRedeemerRaw> raws,
                                                 BigDecimal priceMem, BigDecimal priceStep) {
        return raws.stream()
                .map(r -> {
                    Long unitMem = r.getUnitMem();
                    Long unitSteps = r.getUnitSteps();
                    String fee = null;
                    if (priceMem != null && priceStep != null && unitMem != null && unitSteps != null) {
                        fee = priceMem.multiply(BigDecimal.valueOf(unitMem))
                                .add(priceStep.multiply(BigDecimal.valueOf(unitSteps)))
                                .setScale(0, RoundingMode.CEILING)
                                .toBigInteger()
                                .toString();
                    }
                    return BFTxRedeemerDto.builder()
                            .txIndex(r.getTxIndex())
                            .purpose(mapRedeemerPurpose(r.getPurpose()))
                            .scriptHash(r.getScriptHash())
                            .redeemerDataHash(r.getRedeemerDatahash())
                            .datumHash(r.getRedeemerDatahash())
                            .unitMem(unitMem != null ? unitMem.toString() : null)
                            .unitSteps(unitSteps != null ? unitSteps.toString() : null)
                            .fee(fee)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<BFTxPoolUpdateDto> toPoolUpdateDtos(List<TxPoolUpdateRaw> raws) {
        return raws.stream()
                .map(r -> {
                    List<String> owners = parseOwners(r.getPoolOwnersJson());
                    List<BFTxPoolRelayDto> relays = parseRelays(r.getRelaysJson());
                    BFTxPoolUpdateDto.PoolMetadata metadata = BFTxPoolUpdateDto.PoolMetadata.builder()
                            .url(r.getMetadataUrl())
                            .hash(r.getMetadataHash())
                            .ticker(null).name(null).description(null).homepage(null)
                            .build();
                    return BFTxPoolUpdateDto.builder()
                            .certIndex(r.getCertIndex())
                            .poolId(toBech32PoolId(r.getPoolIdHex()))
                            .vrfKey(r.getVrfKey())
                            .pledge(r.getPledge() != null ? r.getPledge().toString() : "0")
                            .marginCost(r.getMargin())
                            .fixedCost(r.getCost() != null ? r.getCost().toString() : "0")
                            .rewardAccount(r.getRewardAccount())
                            .owners(owners)
                            .relays(relays)
                            .metadata(metadata)
                            .activeEpoch(r.getEpoch() != null ? r.getEpoch() + 2 : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ============================================================
    // Private helpers
    // ============================================================

    private String mapRedeemerPurpose(String purpose) {
        if (purpose == null) return null;
        return switch (purpose) {
            case "Voting" -> "vote";
            default -> purpose.toLowerCase();
        };
    }

    private List<BFAmountDto> buildAmounts(Long lovelaceAmount, String amountsJson) {
        Map<String, BigInteger> totals = new LinkedHashMap<>();
        if (lovelaceAmount != null && lovelaceAmount > 0) {
            totals.put(Constants.LOVELACE, BigInteger.valueOf(lovelaceAmount));
        }
        if (amountsJson != null) {
            AmountsJsonUtil.toQuantityByUnit(amountsJson).forEach((unit, qty) -> {
                if (Constants.LOVELACE.equals(unit) && lovelaceAmount != null && lovelaceAmount > 0) return;
                totals.merge(unit, qty, BigInteger::add);
            });
        }
        if (totals.isEmpty() && (lovelaceAmount == null || lovelaceAmount == 0)) {
            totals.put(Constants.LOVELACE, BigInteger.ZERO);
        }
        return totals.entrySet().stream()
                .sorted((a, b) -> {
                    if (Constants.LOVELACE.equals(a.getKey())) return -1;
                    if (Constants.LOVELACE.equals(b.getKey())) return 1;
                    return a.getKey().compareTo(b.getKey());
                })
                .map(e -> BFAmountDto.builder().unit(e.getKey()).quantity(e.getValue().toString()).build())
                .collect(Collectors.toList());
    }

    private List<BFAmountDto> lovelaceOnlyAmount(Long lovelaceAmount) {
        long qty = lovelaceAmount != null ? lovelaceAmount : 0L;
        return Collections.singletonList(
                BFAmountDto.builder().unit(Constants.LOVELACE).quantity(String.valueOf(qty)).build());
    }

    private Optional<BFTxOutputDto> parseCollateralReturnOutput(String refJson, String dataJson,
                                                                  String txHash) {
        try {
            JsonNode refNode = objectMapper.readTree(refJson);
            JsonNode jsonNode = objectMapper.readTree(dataJson);
            Integer outputIndex = refNode.has("output_index")
                    ? refNode.path("output_index").asInt() : null;
            String address = jsonNode.path("address").asText(null);
            List<BFAmountDto> amounts = parseCollateralReturnAmounts(jsonNode.path("amounts"));
            String dataHash = jsonNode.path("dataHash").isNull()
                    ? null : jsonNode.path("dataHash").asText(null);
            String inlineDatum = jsonNode.path("inlineDatum").isNull()
                    ? null : jsonNode.path("inlineDatum").asText(null);
            String refScriptHash = jsonNode.path("referenceScriptHash").isNull()
                    ? null : jsonNode.path("referenceScriptHash").asText(null);
            return Optional.of(BFTxOutputDto.builder()
                    .address(address).amount(amounts).outputIndex(outputIndex)
                    .dataHash(dataHash).inlineDatum(inlineDatum).referenceScriptHash(refScriptHash)
                    .collateral(true).consumedByTx(null)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to parse collateral_return_json for tx {}: {}", txHash, e.getMessage());
            return Optional.empty();
        }
    }

    private List<BFAmountDto> parseCollateralReturnAmounts(JsonNode amountsNode) {
        if (amountsNode == null || !amountsNode.isArray()) return Collections.emptyList();
        for (JsonNode a : amountsNode) {
            String unit = a.path("unit").asText(null);
            if (Constants.LOVELACE.equals(unit)) {
                return Collections.singletonList(BFAmountDto.builder()
                        .unit(Constants.LOVELACE)
                        .quantity(String.valueOf(a.path("quantity").asLong(0)))
                        .build());
            }
        }
        return Collections.emptyList();
    }

    private List<String> parseOwners(String json) {
        List<String> hashes = parseJsonStringArray(json);
        boolean isMainnet = storeProperties.getProtocolMagic() == NetworkType.MAINNET.getProtocolMagic();
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
                String dns = nullIfEmpty(
                        relay.path("dnsName").isNull() ? null : relay.path("dnsName").asText(null));
                String dnsSrv = nullIfEmpty(
                        relay.path("dnsSrv").isNull() ? null : relay.path("dnsSrv").asText(null));
                Integer port = relay.has("port") && !relay.path("port").isNull()
                        && relay.path("port").asInt() > 0
                        ? relay.path("port").asInt() : null;
                result.add(BFTxPoolRelayDto.builder()
                        .ipv4(nullIfEmpty(relay.path("ipv4").isNull()
                                ? null : relay.path("ipv4").asText(null)))
                        .ipv6(nullIfEmpty(relay.path("ipv6").isNull()
                                ? null : relay.path("ipv6").asText(null)))
                        .dns(dns).dnsSrv(dnsSrv).port(port)
                        .build());
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse relays JSON: {}", relaysJson, e);
            return Collections.emptyList();
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

    private String nullIfEmpty(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
