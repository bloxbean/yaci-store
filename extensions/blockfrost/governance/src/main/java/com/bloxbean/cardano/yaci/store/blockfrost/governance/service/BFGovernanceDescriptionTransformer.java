package com.bloxbean.cardano.yaci.store.blockfrost.governance.service;

import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Number;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.util.CborSerializationUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Adapts governance-action details stored as Yaci domain JSON to the generic Aeson encoding
 * exposed by cardano-ledger and expected by Blockfrost-compatible responses.
 *
 * <p>Constructor argument order must remain aligned with ledger's {@code GovAction} declaration.
 * Historical field aliases are intentionally accepted because persisted rows may have been
 * produced by different cardano-client-lib versions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class BFGovernanceDescriptionTransformer {

    private static final int MAX_DECIMALS_WORD64 = 19;

    private final ObjectMapper objectMapper;

    /**
     * Blockfrost exposes the cardano-ledger Aeson encoding of GovAction, while
     * the store persists the Yaci domain model's JSON representation.
     * Aeson keeps the Haskell constructor name in {@code tag} and encodes
     * non-record constructor arguments positionally in {@code contents}.
     * Unsupported action types retain their original tag and details so newer ledger actions
     * remain inspectable until an explicit mapping is added.
     *
     * @param type the stored Yaci governance-action type
     * @param details the stored Yaci governance-action details
     * @return the corresponding ledger Aeson representation
     *
     * @see <a href="https://github.com/IntersectMBO/cardano-ledger/blob/a624de4c8db7286a6c065da149679ea55f7d5629/eras/conway/impl/src/Cardano/Ledger/Conway/Governance/Procedures.hs#L815">
     * Ledger GovAction declaration</a>
     * @see <a href="https://github.com/IntersectMBO/cardano-ledger/blob/a624de4c8db7286a6c065da149679ea55f7d5629/eras/conway/impl/src/Cardano/Ledger/Conway/Governance/Procedures.hs#L874">
     * Ledger GovAction ToJSON instance</a>
     * @see <a href="https://hackage.haskell.org/package/aeson/docs/Data-Aeson-Types.html#v:defaultOptions">
     * Aeson default generic encoding options</a>
     */
    JsonNode transform(String type, JsonNode details) {
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

        // Aeson omits contents for this nullary constructor.
        if ("INFO_ACTION".equals(type)) {
            return description;
        }

        JsonNode safeDetails = details != null ? details : objectMapper.nullNode();
        // Each helper must preserve the argument order in ledger's GovAction declaration.
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

    // Example: {"govActionId":null,"protocolParamUpdate":{"minFeeA":44},"policyHash":null}
    //       -> [null,{"txFeePerByte":44},null]
    private ArrayNode parameterChangeContents(JsonNode details) {
        ArrayNode contents = objectMapper.createArrayNode();
        contents.add(transformGovActionId(details.get("govActionId")));
        contents.add(transformProtocolParamUpdate(details.get("protocolParamUpdate")));
        contents.add(copyOrNull(details.get("policyHash")));
        return contents;
    }

    // Example: {"govActionId":null,"protocolVersion":{"major":10,"minor":0}}
    //       -> [null,{"major":10,"minor":0}]
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

    // Example: {"withdrawals":{"e1ab...":1000},"policyHash":null}
    //       -> [[[{"network":"Mainnet","credential":{"keyHash":"ab..."}},1000]],null]
    private ArrayNode treasuryWithdrawalContents(JsonNode details) {
        ArrayNode contents = objectMapper.createArrayNode();
        ArrayNode withdrawals = objectMapper.createArrayNode();
        JsonNode withdrawalMap = details.get("withdrawals");

        if (withdrawalMap != null && withdrawalMap.isObject()) {
            // Aeson represents Map AccountAddress Coin as ordered [account, coin] pairs.
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

    /**
     * Recreates ledger's derived AccountAddress order: network, credential constructor, then hash.
     *
     * <p>Example: {@code e1ab... -> 1:1:ab...} for a Mainnet key credential and
     * {@code f0cd... -> 0:0:cd...} for a Testnet script credential.
     *
     * @see <a href="https://github.com/IntersectMBO/cardano-ledger/blob/a624de4c8db7286a6c065da149679ea55f7d5629/libs/cardano-ledger-core/src/Cardano/Ledger/Address.hs#L183">
     * AccountAddress declaration and derived ordering</a>
     * @see <a href="https://github.com/IntersectMBO/cardano-ledger/blob/a624de4c8db7286a6c065da149679ea55f7d5629/libs/cardano-ledger-core/src/Cardano/Ledger/BaseTypes.hs#L878">
     * Network declaration and derived ordering</a>
     * @see <a href="https://github.com/IntersectMBO/cardano-ledger/blob/a624de4c8db7286a6c065da149679ea55f7d5629/libs/cardano-ledger-core/src/Cardano/Ledger/Credential.hs#L98">
     * Credential declaration and derived ordering</a>
     */
    private String rewardAccountOrder(String rewardAccount) {
        try {
            int header = Integer.parseInt(rewardAccount.substring(0, 2), 16);
            int network = header & 0x0f;
            int credentialType = (header & 0xf0) == 0xf0 ? 0 : 1;
            return network + ":" + credentialType + ":" + rewardAccount.substring(2).toLowerCase(Locale.ROOT);
        } catch (RuntimeException e) {
            // Keep malformed values deterministic and ordered after valid ledger coordinates.
            return "2:2:" + rewardAccount;
        }
    }

    /**
     * Converts a reward-account hex value to ledger's AccountAddress JSON shape.
     *
     * <p>Example:</p>
     * {@code e1ab... -> {"network":"Mainnet","credential":{"keyHash":"ab..."}}}
     *
     * @see <a href="https://github.com/IntersectMBO/cardano-ledger/blob/a624de4c8db7286a6c065da149679ea55f7d5629/libs/cardano-ledger-core/src/Cardano/Ledger/Address.hs#L224">
     * AccountAddress JSON encoding</a>
     * @see <a href="https://github.com/IntersectMBO/cardano-ledger/blob/a624de4c8db7286a6c065da149679ea55f7d5629/libs/cardano-ledger-core/src/Cardano/Ledger/Credential.hs#L152">
     * Credential JSON encoding</a>
     */
    private ObjectNode transformRewardAccount(String rewardAccount) {
        ObjectNode account = objectMapper.createObjectNode();
        ObjectNode credential = objectMapper.createObjectNode();

        try {
            int header = Integer.parseInt(rewardAccount.substring(0, 2), 16);
            String credentialField = (header & 0xf0) == 0xf0 ? "scriptHash" : "keyHash";
            credential.put(credentialField, rewardAccount.substring(2).toLowerCase(Locale.ROOT));
            account.put("network", (header & 0x0f) == 1 ? "Mainnet" : "Testnet");
        } catch (RuntimeException e) {
            // Preserve the raw value in a stable shape instead of failing the entire proposal response.
            credential.put("keyHash", rewardAccount);
            account.put("network", "Testnet");
        }

        account.set("credential", credential);
        return account;
    }

    /**
     * Recreates ledger's positional UpdateCommittee contents and credential-set ordering.
     *
     * <p>Example:</p>
     * <pre>
     * {"govActionId":null,"membersForRemoval":[{"type":"SCRIPTHASH","hash":"aa..."}],
     *  "newMembersAndTerms":{"keyHash-bb...":500},"threshold":{"numerator":2,"denominator":3}}
     * ->
     * [null,[{"scriptHash":"aa..."}],{"keyHash-bb...":500},{"numerator":2,"denominator":3}]
     * </pre>
     *
     * @see <a href="https://github.com/IntersectMBO/cardano-ledger/blob/a624de4c8db7286a6c065da149679ea55f7d5629/eras/conway/impl/src/Cardano/Ledger/Conway/Governance/Procedures.hs#L839">
     * UpdateCommittee constructor</a>
     * @see <a href="https://github.com/IntersectMBO/cardano-ledger/blob/a624de4c8db7286a6c065da149679ea55f7d5629/libs/cardano-ledger-core/src/Cardano/Ledger/Credential.hs#L98">
     * Credential declaration and derived ordering</a>
     */
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
        contents.add(transformRational(details.get("threshold")));
        return contents;
    }

    private int credentialTypeOrder(JsonNode credential) {
        // Haskell's derived Ord follows declaration order: ScriptHashObj before KeyHashObj.
        return credential.path("type").asText().contains("SCRIPT") ? 0 : 1;
    }

    // Example: {"type":"ADDR_KEYHASH","hash":"aa"} -> keyHash-aa
    private String transformCredentialKey(String key) {
        if (key.startsWith("keyHash-") || key.startsWith("scriptHash-")) {
            return key;
        }
        try {
            JsonNode credential = objectMapper.readTree(key);
            String prefix = credential.path("type").asText().contains("SCRIPT") ? "scriptHash-" : "keyHash-";
            return prefix + credential.path("hash").asText();
        } catch (Exception e) {
            // Retain historical or unparseable keys losslessly when no conversion can be applied.
            return key;
        }
    }

    // Example: {"type":"SCRIPTHASH","hash":"aa"} -> {"scriptHash":"aa"}
    private ObjectNode transformCredential(JsonNode credential) {
        ObjectNode result = objectMapper.createObjectNode();
        String field = credential.path("type").asText().contains("SCRIPT") ? "scriptHash" : "keyHash";
        result.set(field, copyOrNull(credential.get("hash")));
        return result;
    }

    // Example: {"govActionId":null,"constitution":{"anchor":{"url":"https://example.com",
    //           "dataHash":"ab..."},"script":"cd..."}}
    //       -> [null,{"anchor":{"url":"https://example.com","dataHash":"ab..."},"script":"cd..."}]
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

    /**
     * Maps persisted GovActionId aliases to ledger's key-value JSON encoding.
     *
     * <p>Example:
     * {@code {"transactionId":"ab...","gov_action_index":2} -> {"txId":"ab...","govActionIx":2}}
     *
     * @see <a href="https://github.com/IntersectMBO/cardano-ledger/blob/a624de4c8db7286a6c065da149679ea55f7d5629/eras/conway/impl/src/Cardano/Ledger/Conway/Governance/Procedures.hs#L203">
     * GovActionId key-value encoding</a>
     */
    private JsonNode transformGovActionId(JsonNode source) {
        if (source == null || source.isNull()) {
            return objectMapper.nullNode();
        }
        ObjectNode result = objectMapper.createObjectNode();
        result.set("govActionIx", copyOrNull(firstPresent(source, "govActionIx", "gov_action_index")));
        result.set("txId", copyOrNull(firstPresent(source, "txId", "transactionId")));
        return result;
    }

    /**
     * Maps persisted protocol-parameter aliases to ledger's PParam names and JSON value shapes.
     *
     * <p>Example:
     * {@code {"minFeeA":44,"nopt":500} -> {"txFeePerByte":44,"stakePoolTargetNum":500}}
     *
     * @see <a href="https://github.com/IntersectMBO/cardano-ledger/blob/a624de4c8db7286a6c065da149679ea55f7d5629/libs/cardano-ledger-core/src/Cardano/Ledger/Core/PParams.hs#L733">
     * PParamsUpdate key-value encoding</a>
     * @see <a href="https://github.com/IntersectMBO/cardano-ledger/blob/a624de4c8db7286a6c065da149679ea55f7d5629/eras/conway/impl/src/Cardano/Ledger/Conway/PParams.hs#L1266">
     * Conway PParam definitions</a>
     */
    private ObjectNode transformProtocolParamUpdate(JsonNode source) {
        ObjectNode result = objectMapper.createObjectNode();
        if (source == null || source.isNull()) {
            return result;
        }

        // Ledger uses each PParam.ppName as the JSON key and omits absent updates.
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
            // Yaci stores cost vectors as CBOR hex; ledger Aeson emits language-keyed arrays.
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

    // Example: CBOR hex "9f0102ff" -> [1,2]
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
        // A failed CBOR conversion must not discard the persisted representation.
        return source.deepCopy();
    }

    private void copyRationalField(JsonNode source, String sourceField, ObjectNode target, String targetField) {
        JsonNode value = source.get(sourceField);
        if (value != null && !value.isNull()) {
            target.set(targetField, transformRational(value));
        }
    }

    // TODO: Re-verify this logic against ledger
    /**
     * Converts a persisted numerator/denominator object using ledger's bounded-ratio JSON rules.
     * A reduced ratio becomes a JSON number only when its decimal expansion terminates within
     * {@code maxDecimalsWord64} (19) places; otherwise it remains an exact ratio object.
     *
     * <p>Examples: {@code {"numerator":3,"denominator":10} -> 0.3},
     * {@code {"numerator":1,"denominator":3} -> {"numerator":1,"denominator":3}}
     *
     * @see <a href="https://github.com/IntersectMBO/cardano-ledger/blob/a624de4c8db7286a6c065da149679ea55f7d5629/libs/cardano-ledger-core/src/Cardano/Ledger/BaseTypes.hs#L208">
     * Ledger maxDecimalsWord64 definition</a>
     * @see <a href="https://github.com/IntersectMBO/cardano-ledger/blob/a624de4c8db7286a6c065da149679ea55f7d5629/libs/cardano-ledger-core/src/Cardano/Ledger/BaseTypes.hs#L429">
     * BoundedRatio JSON encoding</a>
     */
    private JsonNode transformRational(JsonNode source) {
        if (source == null || !source.isObject()) {
            return copyOrNull(source);
        }

        JsonNode numerator = source.get("numerator");
        JsonNode denominator = source.get("denominator");
        if (numerator == null || denominator == null) {
            return source.deepCopy();
        }

        try {
            BigInteger numeratorValue = new BigInteger(numerator.asText());
            BigInteger denominatorValue = new BigInteger(denominator.asText());
            if (numeratorValue.signum() < 0 || denominatorValue.signum() <= 0) {
                return source.deepCopy();
            }

            BigInteger gcd = numeratorValue.gcd(denominatorValue);
            BigInteger reducedNumerator = numeratorValue.divide(gcd);
            BigInteger reducedDenominator = denominatorValue.divide(gcd);

            try {
                // Exact division rejects repeating decimals; scale enforces ledger's 19-place limit.
                BigDecimal ratio = new BigDecimal(reducedNumerator)
                        .divide(new BigDecimal(reducedDenominator))
                        .stripTrailingZeros();
                if (ratio.scale() <= MAX_DECIMALS_WORD64) {
                    return objectMapper.valueToTree(ratio);
                }
            } catch (ArithmeticException ignored) {
                // A repeating decimal cannot be represented exactly by BigDecimal.
            }

            ObjectNode exactRatio = objectMapper.createObjectNode();
            exactRatio.put("numerator", reducedNumerator);
            exactRatio.put("denominator", reducedDenominator);
            return exactRatio;
        } catch (NumberFormatException e) {
            // Preserve malformed legacy values rather than changing their response shape.
            return source.deepCopy();
        }
    }

    // Example: {"maxTxExMem":17500000,"maxTxExSteps":10000000000}
    //       -> {"memory":17500000,"steps":10000000000}
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
}
