package com.bloxbean.cardano.yaci.store.blockfrost.governance.service;

import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Number;
import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.governance.GovId;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.GovActionId;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.util.CborSerializationUtil;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.dto.*;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.mapper.BFDRepMapper;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.mapper.BFProposalMapper;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.BFGovernanceStorageReader;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.common.util.GovUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BFGovernanceService {

    private final BFGovernanceStorageReader storageReader;
    private final ObjectMapper objectMapper;

    private final BFDRepMapper dRepMapper = BFDRepMapper.INSTANCE;
    private final BFProposalMapper proposalMapper = BFProposalMapper.INSTANCE;

    // ────────────────────────────────────────────────────────────────────────
    // DRep endpoints
    // ────────────────────────────────────────────────────────────────────────

    public List<BFDRepListItemDto> getDReps(int page, int count, Order order) {
        return storageReader.findAllDReps(page, count, order)
                .stream()
                .map(dRepMapper::toListItemDto)
                .collect(Collectors.toList());
    }

    public BFDRepDto getDRep(String drepId) {
        String drepHex = resolveDRepHex(drepId);
        return storageReader.findDRepByHash(drepHex)
                .map(dRepMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "DRep not found: " + drepId));
    }

    public List<BFDRepDelegatorDto> getDRepDelegators(String drepId, int page, int count, Order order) {
        String drepHex = resolveDRepHex(drepId);
        return storageReader.findDRepDelegators(drepHex, page, count, order)
                .stream()
                .map(dRepMapper::toDelegatorDto)
                .collect(Collectors.toList());
    }

    public List<BFDRepUpdateDto> getDRepUpdates(String drepId, int page, int count, Order order) {
        String drepHex = resolveDRepHex(drepId);
        return storageReader.findDRepUpdates(drepHex, page, count, order)
                .stream()
                .map(dRepMapper::toUpdateDto)
                .collect(Collectors.toList());
    }

    public List<BFDRepVoteDto> getDRepVotes(String drepId, int page, int count, Order order) {
        String drepHex = resolveDRepHex(drepId);
        return storageReader.findDRepVotes(drepHex, page, count, order)
                .stream()
                .map(dRepMapper::toVoteDto)
                .collect(Collectors.toList());
    }

    public BFDRepMetadataDto getDRepMetadata(String drepId) {
        String drepHex = resolveDRepHex(drepId);
        return storageReader.findDRepMetadata(drepHex)
                .map(dRepMapper::toMetadataDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "DRep metadata not found: " + drepId));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Proposal endpoints
    // ────────────────────────────────────────────────────────────────────────

    public List<BFProposalListItemDto> getProposals(int page, int count, Order order) {
        return storageReader.findAllProposals(page, count, order)
                .stream()
                .map(proposalMapper::toListItemDto)
                .collect(Collectors.toList());
    }

    public BFProposalDto getProposal(String txHash, int index) {
        return storageReader.findProposalByTxHashAndIndex(txHash, index)
                .map(row -> {
                    BFProposalDto dto = proposalMapper.toDto(row);
                    dto.setGovernanceDescription(transformGovernanceDescription(row.getType(), row.getDetails()));
                    return dto;
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Proposal not found: " + txHash + "#" + index));
    }

    public BFProposalDto getProposalByGovActionId(String govActionId) {
        GovActionId id = decodeGovActionId(govActionId);
        return getProposal(id.getTransactionId(), id.getGovActionIndex());
    }

    public BFProposalParametersDto getProposalParameters(String txHash, int index) {
        return storageReader.findParameterChangeProposal(txHash, index)
                .map(row -> {
                    BFProposalParametersDto dto = proposalMapper.toParametersDto(row);
                    dto.setParameters(transformProtocolParams(row.getDetails()));
                    return dto;
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Protocol parameter proposal not found: " + txHash + "#" + index));
    }

    public BFProposalParametersDto getProposalParametersByGovActionId(String govActionId) {
        GovActionId id = decodeGovActionId(govActionId);
        return getProposalParameters(id.getTransactionId(), id.getGovActionIndex());
    }

    public List<BFProposalWithdrawalDto> getProposalWithdrawals(String txHash, int index) {
        if (!storageReader.isWithdrawalProposal(txHash, index)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Treasury withdrawal proposal not found: " + txHash + "#" + index);
        }
        return storageReader.findProposalWithdrawals(txHash, index)
                .stream()
                .map(d -> BFProposalWithdrawalDto.builder()
                        .stakeAddress(hexToStakeAddress(d.getAddress()))
                        .amount(d.getAmount() != null ? String.valueOf(d.getAmount()) : "0")
                        .build())
                .collect(Collectors.toList());
    }

    public List<BFProposalWithdrawalDto> getProposalWithdrawalsByGovActionId(String govActionId) {
        GovActionId id = decodeGovActionId(govActionId);
        return getProposalWithdrawals(id.getTransactionId(), id.getGovActionIndex());
    }

    public List<BFProposalVoteDto> getProposalVotes(String txHash, int index, int page, int count, Order order) {
        return storageReader.findProposalVotes(txHash, index, page, count, order)
                .stream()
                .map(proposalMapper::toVoteDto)
                .collect(Collectors.toList());
    }

    public List<BFProposalVoteDto> getProposalVotesByGovActionId(String govActionId, int page, int count, Order order) {
        GovActionId id = decodeGovActionId(govActionId);
        return getProposalVotes(id.getTransactionId(), id.getGovActionIndex(), page, count, order);
    }

    public BFProposalMetadataDto getProposalMetadata(String txHash, int index) {
        return storageReader.findProposalMetadata(txHash, index)
                .map(proposalMapper::toMetadataDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Proposal metadata not found: " + txHash + "#" + index));
    }

    public BFProposalMetadataDto getProposalMetadataByGovActionId(String govActionId) {
        GovActionId id = decodeGovActionId(govActionId);
        return getProposalMetadata(id.getTransactionId(), id.getGovActionIndex());
    }

    // ────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Resolves a DRep ID (bech32 or hex) to the raw 56-char hex used in the DB.
     * Accepts a 56-character raw hash or a CIP-129 bech32 DRep ID.
     */
    private String resolveDRepHex(String drepId) {
        if (drepId == null || drepId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DRep ID cannot be null or blank");
        }

        if (drepId.matches("[0-9a-fA-F]{56}")) {
            return drepId.toLowerCase(Locale.ROOT);
        }

        try {
            return GovId.toDrep(drepId).getHash();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid DRep ID: " + drepId, e);
        }
    }

    /**
     * Decodes a CIP-129 bech32 gov_action_id, throwing HTTP 400 for invalid input.
     */
    private GovActionId decodeGovActionId(String govActionId) {
        try {
            return GovUtil.toGovActionIdFromBech32(govActionId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid gov_action_id: " + govActionId);
        }
    }

    /**
     * Blockfrost exposes the cardano-ledger Aeson encoding of GovAction, while
     * the store persists the Yaci domain model's JSON representation.
     */
    private JsonNode transformGovernanceDescription(String type, JsonNode details) {
        String safeType = type != null ? type : "";
        ObjectNode description = objectMapper.createObjectNode();
        description.put("tag", switch (safeType) {
            case "PARAMETER_CHANGE_ACTION" -> "ParameterChange";
            case "HARD_FORK_INITIATION_ACTION" -> "HardForkInitiation";
            case "TREASURY_WITHDRAWALS_ACTION" -> "TreasuryWithdrawals";
            case "NO_CONFIDENCE" -> "NoConfidence";
            case "UPDATE_COMMITTEE" -> "UpdateCommittee";
            case "NEW_CONSTITUTION" -> "NewConstitution";
            case "INFO_ACTION" -> "InfoAction";
            default -> safeType;
        });

        if ("INFO_ACTION".equals(type)) {
            return description;
        }

        JsonNode safeDetails = details != null ? details : objectMapper.nullNode();
        JsonNode contents = switch (safeType) {
            case "PARAMETER_CHANGE_ACTION" -> parameterChangeContents(safeDetails);
            case "HARD_FORK_INITIATION_ACTION" -> hardForkContents(safeDetails);
            case "TREASURY_WITHDRAWALS_ACTION" -> treasuryWithdrawalContents(safeDetails);
            case "NO_CONFIDENCE" -> transformGovActionId(safeDetails.get("govActionId"));
            case "UPDATE_COMMITTEE" -> updateCommitteeContents(safeDetails);
            case "NEW_CONSTITUTION" -> newConstitutionContents(safeDetails);
            default -> safeDetails.deepCopy();
        };
        description.set("contents", contents);
        return description;
    }

    private ArrayNode parameterChangeContents(JsonNode details) {
        ArrayNode contents = objectMapper.createArrayNode();
        contents.add(transformGovActionId(details.get("govActionId")));
        contents.add(transformProtocolParamUpdate(details.get("protocolParamUpdate")));
        contents.add(copyOrNull(details.get("policyHash")));
        return contents;
    }

    private ArrayNode hardForkContents(JsonNode details) {
        ArrayNode contents = objectMapper.createArrayNode();
        contents.add(transformGovActionId(details.get("govActionId")));

        JsonNode protocolVersion = details.get("protocolVersion");
        ObjectNode version = objectMapper.createObjectNode();
        if (protocolVersion != null && !protocolVersion.isNull()) {
            version.set("major", copyOrNull(firstPresent(protocolVersion, "major", "_1")));
            version.set("minor", copyOrNull(firstPresent(protocolVersion, "minor", "_2")));
        }
        contents.add(version);
        return contents;
    }

    private ArrayNode treasuryWithdrawalContents(JsonNode details) {
        ArrayNode contents = objectMapper.createArrayNode();
        ArrayNode withdrawals = objectMapper.createArrayNode();
        JsonNode withdrawalMap = details.get("withdrawals");

        if (withdrawalMap != null && withdrawalMap.isObject()) {
            List<java.util.Map.Entry<String, JsonNode>> entries = new ArrayList<>();
            withdrawalMap.fields().forEachRemaining(entries::add);
            entries.sort(Comparator.comparing(entry -> rewardAccountOrder(entry.getKey())));
            entries.forEach(entry -> {
                ArrayNode withdrawal = objectMapper.createArrayNode();
                withdrawal.add(transformRewardAccount(entry.getKey()));
                withdrawal.add(entry.getValue().deepCopy());
                withdrawals.add(withdrawal);
            });
        }

        contents.add(withdrawals);
        contents.add(copyOrNull(details.get("policyHash")));
        return contents;
    }

    private String rewardAccountOrder(String rewardAccount) {
        try {
            int header = Integer.parseInt(rewardAccount.substring(0, 2), 16);
            int network = header & 0x0f;
            int credentialType = (header & 0xf0) == 0xf0 ? 0 : 1;
            return network + ":" + credentialType + ":" + rewardAccount.substring(2).toLowerCase(Locale.ROOT);
        } catch (RuntimeException e) {
            return "2:2:" + rewardAccount;
        }
    }

    private ObjectNode transformRewardAccount(String rewardAccount) {
        ObjectNode account = objectMapper.createObjectNode();
        ObjectNode credential = objectMapper.createObjectNode();

        try {
            int header = Integer.parseInt(rewardAccount.substring(0, 2), 16);
            String credentialField = (header & 0xf0) == 0xf0 ? "scriptHash" : "keyHash";
            credential.put(credentialField, rewardAccount.substring(2).toLowerCase(Locale.ROOT));
            account.put("network", (header & 0x0f) == 1 ? "Mainnet" : "Testnet");
        } catch (RuntimeException e) {
            credential.put("keyHash", rewardAccount);
            account.put("network", "Testnet");
        }

        account.set("credential", credential);
        return account;
    }

    private ArrayNode updateCommitteeContents(JsonNode details) {
        ArrayNode contents = objectMapper.createArrayNode();
        contents.add(transformGovActionId(details.get("govActionId")));

        ArrayNode membersForRemoval = objectMapper.createArrayNode();
        JsonNode removals = details.get("membersForRemoval");
        if (removals != null && removals.isArray()) {
            List<JsonNode> credentials = new ArrayList<>();
            removals.forEach(credentials::add);
            credentials.sort(Comparator
                    .comparingInt(this::credentialTypeOrder)
                    .thenComparing(node -> node.path("hash").asText()));
            credentials.forEach(credential -> membersForRemoval.add(transformCredential(credential)));
        }
        contents.add(membersForRemoval);

        ObjectNode newMembersAndTerms = objectMapper.createObjectNode();
        JsonNode members = details.get("newMembersAndTerms");
        if (members != null && members.isObject()) {
            members.fields().forEachRemaining(entry -> {
                String credentialKey = transformCredentialKey(entry.getKey());
                newMembersAndTerms.set(credentialKey, entry.getValue().deepCopy());
            });
        }
        contents.add(newMembersAndTerms);
        contents.add(copyOrNull(details.get("threshold")));
        return contents;
    }

    private int credentialTypeOrder(JsonNode credential) {
        return credential.path("type").asText().contains("SCRIPT") ? 0 : 1;
    }

    private String transformCredentialKey(String key) {
        if (key.startsWith("keyHash-") || key.startsWith("scriptHash-")) {
            return key;
        }
        try {
            JsonNode credential = objectMapper.readTree(key);
            String prefix = credential.path("type").asText().contains("SCRIPT") ? "scriptHash-" : "keyHash-";
            return prefix + credential.path("hash").asText();
        } catch (Exception e) {
            return key;
        }
    }

    private ObjectNode transformCredential(JsonNode credential) {
        ObjectNode result = objectMapper.createObjectNode();
        String field = credential.path("type").asText().contains("SCRIPT") ? "scriptHash" : "keyHash";
        result.set(field, copyOrNull(credential.get("hash")));
        return result;
    }

    private ArrayNode newConstitutionContents(JsonNode details) {
        ArrayNode contents = objectMapper.createArrayNode();
        contents.add(transformGovActionId(details.get("govActionId")));

        JsonNode source = details.get("constitution");
        ObjectNode constitution = objectMapper.createObjectNode();
        ObjectNode anchor = objectMapper.createObjectNode();
        if (source != null && !source.isNull()) {
            JsonNode sourceAnchor = source.get("anchor");
            if (sourceAnchor != null && !sourceAnchor.isNull()) {
                anchor.set("url", copyOrNull(firstPresent(sourceAnchor, "url", "anchor_url")));
                anchor.set("dataHash", copyOrNull(firstPresent(sourceAnchor, "dataHash", "anchor_data_hash")));
            }
            constitution.set("script", copyOrNull(firstPresent(source, "script", "scripthash", "scriptHash")));
        }
        constitution.set("anchor", anchor);
        contents.add(constitution);
        return contents;
    }

    private JsonNode transformGovActionId(JsonNode source) {
        if (source == null || source.isNull()) {
            return objectMapper.nullNode();
        }
        ObjectNode result = objectMapper.createObjectNode();
        result.set("govActionIx", copyOrNull(firstPresent(source, "govActionIx", "gov_action_index")));
        result.set("txId", copyOrNull(firstPresent(source, "txId", "transactionId")));
        return result;
    }

    private ObjectNode transformProtocolParamUpdate(JsonNode source) {
        ObjectNode result = objectMapper.createObjectNode();
        if (source == null || source.isNull()) {
            return result;
        }

        String[][] directMappings = {
                {"minFeeA", "txFeePerByte"}, {"minFeeB", "txFeeFixed"},
                {"maxBlockSize", "maxBlockBodySize"}, {"maxTxSize", "maxTxSize"},
                {"maxBlockHeaderSize", "maxBlockHeaderSize"}, {"keyDeposit", "stakeAddressDeposit"},
                {"poolDeposit", "stakePoolDeposit"}, {"maxEpoch", "poolRetireMaxEpoch"},
                {"minPoolCost", "minPoolCost"}, {"maxValSize", "maxValueSize"},
                {"collateralPercent", "collateralPercentage"}, {"maxCollateralInputs", "maxCollateralInputs"},
                {"committeeMinSize", "committeeMinSize"},
                {"committeeMaxTermLength", "committeeMaxTermLength"},
                {"govActionLifetime", "govActionLifetime"}, {"govActionDeposit", "govActionDeposit"},
                {"drepDeposit", "dRepDeposit"}, {"drepActivity", "dRepActivity"}
        };
        for (String[] mapping : directMappings) {
            copyField(source, mapping[0], result, mapping[1]);
        }

        JsonNode nOpt = firstPresent(source, "nOpt", "nopt");
        if (nOpt != null && !nOpt.isNull()) result.set("stakePoolTargetNum", nOpt.deepCopy());
        copyRationalField(source, "poolPledgeInfluence", result, "poolPledgeInfluence");
        copyRationalField(source, "expansionRate", result, "monetaryExpansion");
        copyRationalField(source, "treasuryGrowthRate", result, "treasuryCut");
        copyRationalField(source, "minFeeRefScriptCostPerByte", result, "minFeeRefScriptCostPerByte");

        JsonNode poolThresholds = source.get("poolVotingThresholds");
        if (poolThresholds != null && poolThresholds.isObject()) {
            result.set("poolVotingThresholds", transformVotingThresholds(poolThresholds, new String[][]{
                    {"pvtMotionNoConfidence", "motionNoConfidence"},
                    {"pvtCommitteeNormal", "committeeNormal"},
                    {"pvtCommitteeNoConfidence", "committeeNoConfidence"},
                    {"pvtHardForkInitiation", "hardForkInitiation"},
                    {"pvtPPSecurityGroup", "ppSecurityGroup"}
            }));
        }

        JsonNode dRepThresholds = source.get("drepVotingThresholds");
        if (dRepThresholds != null && dRepThresholds.isObject()) {
            result.set("dRepVotingThresholds", transformVotingThresholds(dRepThresholds, new String[][]{
                    {"dvtMotionNoConfidence", "motionNoConfidence"},
                    {"dvtCommitteeNormal", "committeeNormal"},
                    {"dvtCommitteeNoConfidence", "committeeNoConfidence"},
                    {"dvtUpdateToConstitution", "updateToConstitution"},
                    {"dvtHardForkInitiation", "hardForkInitiation"},
                    {"dvtPPNetworkGroup", "ppNetworkGroup"},
                    {"dvtPPEconomicGroup", "ppEconomicGroup"},
                    {"dvtPPTechnicalGroup", "ppTechnicalGroup"},
                    {"dvtPPGovGroup", "ppGovGroup"},
                    {"dvtTreasuryWithdrawal", "treasuryWithdrawal"}
            }));
        }

        JsonNode utxoCost = firstPresent(source, "coinsPerUtxoByte", "adaPerUtxoByte");
        if (utxoCost != null) result.set("utxoCostPerByte", utxoCost.deepCopy());

        JsonNode costModels = source.get("costModels");
        if (costModels != null && costModels.isObject()) {
            ObjectNode mappedCostModels = objectMapper.createObjectNode();
            costModels.fields().forEachRemaining(entry -> mappedCostModels.set(switch (entry.getKey()) {
                case "0" -> "PlutusV1";
                case "1" -> "PlutusV2";
                case "2" -> "PlutusV3";
                default -> entry.getKey();
            }, transformCostModel(entry.getValue())));
            result.set("costModels", mappedCostModels);
        }

        ObjectNode prices = objectMapper.createObjectNode();
        JsonNode priceMemory = firstPresent(source, "priceMem");
        JsonNode priceSteps = firstPresent(source, "priceStep");
        JsonNode sourcePrices = source.get("executionUnitPrices");
        if (sourcePrices != null && !sourcePrices.isNull()) {
            if (priceMemory == null) priceMemory = firstPresent(sourcePrices, "priceMemory", "memory");
            if (priceSteps == null) priceSteps = firstPresent(sourcePrices, "priceSteps", "steps");
        }
        if (priceMemory != null) prices.set("priceMemory", transformRational(priceMemory));
        if (priceSteps != null) prices.set("priceSteps", transformRational(priceSteps));
        if (!prices.isEmpty()) result.set("executionUnitPrices", prices);

        ObjectNode maxTxUnits = executionUnits(source, "maxTxExUnits", "maxTxExMem", "maxTxExSteps");
        if (!maxTxUnits.isEmpty()) result.set("maxTxExecutionUnits", maxTxUnits);
        ObjectNode maxBlockUnits = executionUnits(source, "maxBlockExUnits", "maxBlockExMem", "maxBlockExSteps");
        if (!maxBlockUnits.isEmpty()) result.set("maxBlockExecutionUnits", maxBlockUnits);
        return result;
    }

    private ObjectNode transformVotingThresholds(JsonNode source, String[][] mappings) {
        ObjectNode result = objectMapper.createObjectNode();
        for (String[] mapping : mappings) {
            JsonNode value = source.get(mapping[0]);
            if (value != null && !value.isNull()) {
                result.set(mapping[1], transformRational(value));
            }
        }
        return result;
    }

    private JsonNode transformCostModel(JsonNode source) {
        if (source == null || !source.isTextual()) {
            return copyOrNull(source);
        }

        try {
            DataItem dataItem = CborSerializationUtil.deserializeOne(HexUtil.decodeHexString(source.asText()));
            if (dataItem instanceof Array cborArray) {
                ArrayNode result = objectMapper.createArrayNode();
                for (DataItem item : cborArray.getDataItems()) {
                    if (item instanceof Number number) {
                        result.add(number.getValue());
                    }
                }
                return result;
            }
        } catch (RuntimeException e) {
            log.debug("Unable to decode governance proposal cost model", e);
        }
        return source.deepCopy();
    }

    private void copyRationalField(JsonNode source, String sourceField, ObjectNode target, String targetField) {
        JsonNode value = source.get(sourceField);
        if (value != null && !value.isNull()) {
            target.set(targetField, transformRational(value));
        }
    }

    private JsonNode transformRational(JsonNode source) {
        if (source == null || !source.isObject()) {
            return copyOrNull(source);
        }

        JsonNode numerator = source.get("numerator");
        JsonNode denominator = source.get("denominator");
        if (numerator == null || denominator == null || denominator.asText().equals("0")) {
            return source.deepCopy();
        }

        BigDecimal ratio = new BigDecimal(numerator.asText())
                .divide(new BigDecimal(denominator.asText()), MathContext.DECIMAL128)
                .stripTrailingZeros();
        return objectMapper.valueToTree(ratio);
    }

    private ObjectNode executionUnits(JsonNode source, String nestedField, String memoryField, String stepsField) {
        ObjectNode result = objectMapper.createObjectNode();
        JsonNode memory = source.get(memoryField);
        JsonNode steps = source.get(stepsField);
        JsonNode nested = source.get(nestedField);
        if (nested != null && !nested.isNull()) {
            if (memory == null) memory = firstPresent(nested, "memory", "exUnitsMem");
            if (steps == null) steps = firstPresent(nested, "steps", "exUnitsSteps");
        }
        if (memory != null && !memory.isNull()) result.set("memory", memory.deepCopy());
        if (steps != null && !steps.isNull()) result.set("steps", steps.deepCopy());
        return result;
    }

    private void copyField(JsonNode source, String sourceField, ObjectNode target, String targetField) {
        JsonNode value = source.get(sourceField);
        if (value != null && !value.isNull()) {
            target.set(targetField, value.deepCopy());
        }
    }

    private JsonNode firstPresent(JsonNode source, String... fields) {
        if (source == null || source.isNull()) return null;
        for (String field : fields) {
            JsonNode value = source.get(field);
            if (value != null) return value;
        }
        return null;
    }

    private JsonNode copyOrNull(JsonNode value) {
        return value == null || value.isNull() ? objectMapper.nullNode() : value.deepCopy();
    }

    /**
     * Transforms raw DB protocolParamUpdate JSON (camelCase) to Blockfrost's
     * snake_case null-padded protocol parameters object.
     */
    private JsonNode transformProtocolParams(JsonNode details) {
        ObjectNode out = objectMapper.createObjectNode();
        // All BF fields initialized to null
        String[] fields = {
                "min_fee_a","min_fee_b","max_block_size","max_tx_size","max_block_header_size",
                "key_deposit","pool_deposit","e_max","n_opt","a0","rho","tau",
                "decentralisation_param","extra_entropy","protocol_major_ver","protocol_minor_ver",
                "min_utxo","min_pool_cost","cost_models","price_mem","price_step",
                "max_tx_ex_mem","max_tx_ex_steps","max_block_ex_mem","max_block_ex_steps",
                "max_val_size","collateral_percent","max_collateral_inputs","coins_per_utxo_size",
                "coins_per_utxo_word","pvt_motion_no_confidence","pvt_committee_normal",
                "pvt_committee_no_confidence","pvt_hard_fork_initiation","dvt_motion_no_confidence",
                "dvt_committee_normal","dvt_committee_no_confidence","dvt_update_to_constitution",
                "dvt_hard_fork_initiation","dvt_p_p_network_group","dvt_p_p_economic_group",
                "dvt_p_p_technical_group","dvt_p_p_gov_group","dvt_treasury_withdrawal",
                "committee_min_size","committee_max_term_length","gov_action_lifetime",
                "gov_action_deposit","drep_deposit","drep_activity","min_fee_ref_script_cost_per_byte",
                "pvtpp_security_group","pvt_p_p_security_group","epoch"
        };
        for (String f : fields) out.putNull(f);

        if (details == null) return out;

        // Extract protocolParamUpdate sub-object
        JsonNode ppu = details.get("protocolParamUpdate");
        if (ppu == null || ppu.isNull()) return out;

        // Map camelCase DB field names -> snake_case BF field names
        java.util.Map<String,String> mapping = new java.util.LinkedHashMap<>();
        mapping.put("minFeeA","min_fee_a"); mapping.put("minFeeB","min_fee_b");
        mapping.put("maxBlockSize","max_block_size"); mapping.put("maxTxSize","max_tx_size");
        mapping.put("maxBlockHeaderSize","max_block_header_size");
        mapping.put("keyDeposit","key_deposit"); mapping.put("poolDeposit","pool_deposit");
        mapping.put("maxEpoch","e_max"); mapping.put("nOpt","n_opt");
        mapping.put("poolPledgeInfluence","a0"); mapping.put("expansionRate","rho");
        mapping.put("treasuryGrowthRate","tau");
        mapping.put("decentralisationParam","decentralisation_param");
        mapping.put("extraEntropy","extra_entropy");
        mapping.put("protocolMajorVer","protocol_major_ver"); mapping.put("protocolMinorVer","protocol_minor_ver");
        mapping.put("minUtxoValue","min_utxo"); mapping.put("minPoolCost","min_pool_cost");
        mapping.put("costModels","cost_models"); mapping.put("executionUnitPrices.priceMemory","price_mem");
        mapping.put("executionUnitPrices.priceSteps","price_step");
        // Handle both nested format (maxTxExUnits.exUnitsMem) and flat format (maxTxExMem) from DB
        mapping.put("maxTxExUnits.exUnitsMem","max_tx_ex_mem"); mapping.put("maxTxExUnits.exUnitsSteps","max_tx_ex_steps");
        mapping.put("maxBlockExUnits.exUnitsMem","max_block_ex_mem"); mapping.put("maxBlockExUnits.exUnitsSteps","max_block_ex_steps");
        mapping.put("maxTxExMem","max_tx_ex_mem"); mapping.put("maxTxExSteps","max_tx_ex_steps");
        mapping.put("maxBlockExMem","max_block_ex_mem"); mapping.put("maxBlockExSteps","max_block_ex_steps");
        mapping.put("maxValueSize","max_val_size"); mapping.put("collateralPercentage","collateral_percent");
        mapping.put("maxCollateralInputs","max_collateral_inputs");
        mapping.put("coinsPerUtxoByte","coins_per_utxo_size"); mapping.put("adaPerUtxoByte","coins_per_utxo_size");
        mapping.put("poolVotingThresholds.motionNoConfidence","pvt_motion_no_confidence");
        mapping.put("poolVotingThresholds.committeeNormal","pvt_committee_normal");
        mapping.put("poolVotingThresholds.committeeNoConfidence","pvt_committee_no_confidence");
        mapping.put("poolVotingThresholds.hardForkInitiation","pvt_hard_fork_initiation");
        mapping.put("poolVotingThresholds.ppSecurityGroup","pvtpp_security_group");
        mapping.put("drepVotingThresholds.motionNoConfidence","dvt_motion_no_confidence");
        mapping.put("drepVotingThresholds.committeeNormal","dvt_committee_normal");
        mapping.put("drepVotingThresholds.committeeNoConfidence","dvt_committee_no_confidence");
        mapping.put("drepVotingThresholds.updateToConstitution","dvt_update_to_constitution");
        mapping.put("drepVotingThresholds.hardForkInitiation","dvt_hard_fork_initiation");
        mapping.put("drepVotingThresholds.ppNetworkGroup","dvt_p_p_network_group");
        mapping.put("drepVotingThresholds.ppEconomicGroup","dvt_p_p_economic_group");
        mapping.put("drepVotingThresholds.ppTechnicalGroup","dvt_p_p_technical_group");
        mapping.put("drepVotingThresholds.ppGovGroup","dvt_p_p_gov_group");
        mapping.put("drepVotingThresholds.treasuryWithdrawal","dvt_treasury_withdrawal");
        mapping.put("committeeMinSize","committee_min_size");
        mapping.put("committeeMaxTermLength","committee_max_term_length");
        mapping.put("govActionLifetime","gov_action_lifetime"); mapping.put("govActionDeposit","gov_action_deposit");
        mapping.put("dRepDeposit","drep_deposit"); mapping.put("dRepActivity","drep_activity");
        mapping.put("minFeeRefScriptCostPerByte","min_fee_ref_script_cost_per_byte");

        for (java.util.Map.Entry<String,String> e : mapping.entrySet()) {
            String[] parts = e.getKey().split("\\.");
            JsonNode node = ppu;
            for (String part : parts) { if (node == null || node.isNull()) break; node = node.get(part); }
            if (node != null && !node.isNull()) {
                // Blockfrost returns all values as strings; convert numbers accordingly
                if (node.isNumber()) {
                    out.put(e.getValue(), node.isIntegralNumber() ? node.asText() : node.toString());
                } else {
                    out.set(e.getValue(), node);
                }
            }
        }
        return out;
    }

    /**
     * Converts a hex-encoded Cardano stake address to bech32.
     */
    private String hexToStakeAddress(String hex) {
        if (hex == null) return null;
        return new Address(HexUtil.decodeHexString(hex)).toBech32();
    }
}
