package com.bloxbean.cardano.yaci.store.common.genesis;

import com.bloxbean.cardano.yaci.store.common.domain.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

import static com.bloxbean.cardano.yaci.store.common.genesis.util.PlutusKeys.PLUTUS_V3;

public class ConwayGenesis extends GenesisFile {
    private final static String POOL_VOTING_THRESHOLDS = "poolVotingThresholds";
    private final static String PVT_COMMITTEE_NORMAL = "committeeNormal";
    private final static String PVT_COMMITTEE_NO_CONFIDENCE = "committeeNoConfidence";
    private final static String PVT_HARD_FORK_INITIATION = "hardForkInitiation";
    private final static String PVT_MOTION_NO_CONFIDENCE = "motionNoConfidence";
    private final static String PVT_PP_SECURITY_GROUP = "ppSecurityGroup";

    private final static String D_REP_VOTING_THRESHOLDS = "dRepVotingThresholds";
    private final static String DVT_MOTION_NO_CONFIDENCE = "motionNoConfidence";
    private final static String DVT_COMMITTEE_NORMAL = "committeeNormal";
    private final static String DVT_COMMITTEE_NO_CONFIDENCE = "committeeNoConfidence";
    private final static String DVT_UPDATE_TO_CONSTITUTION = "updateToConstitution";
    private final static String DVT_HARD_FORK_INITIATION = "hardForkInitiation";
    private final static String DVT_PPNETWORK_GROUP = "ppNetworkGroup";
    private final static String DVT_PPECONOMIC_GROUP = "ppEconomicGroup";
    private final static String DVT_PPTECHNICAL_GROUP = "ppTechnicalGroup";
    private final static String DVT_PPGOV_GROUP = "ppGovGroup";
    private final static String DVT_TREASURY_WITHDRAWAL = "treasuryWithdrawal";

    private final static String COMMITTEE_MIN_SIZE = "committeeMinSize";
    private final static String COMMITTEE_MAX_TERM_LENGTH = "committeeMaxTermLength";
    private final static String GOV_ACTION_LIFETIME = "govActionLifetime";
    private final static String GOV_ACTION_DEPOSIT = "govActionDeposit";
    private final static String D_REP_DEPOSIT = "dRepDeposit";
    private final static String D_REP_ACTIVITY = "dRepActivity";
    private final static String MIN_FEE_REF_SCRIPT_COST_PER_BYTE = "minFeeRefScriptCostPerByte";
    private final static String CONSTITUTION = "constitution";
    private final static String ANCHOR = "anchor";
    private final static String URL = "url";
    private final static String DATA_HASH = "dataHash";
    private final static String SCRIPT = "script";
    private final static String COMMITTEE = "committee";
    private final static String MEMBERS = "members";
    private final static String THRESHOLD = "threshold";
    private final static String NUMERATOR = "numerator";
    private final static String DENOMINATOR = "denominator";
    private final static String PLUTUS_V3_COST_MODEL = "plutusV3CostModel";

    @Getter
    private List<GenesisCommitteeMember> committeeMembers;
    @Getter
    private BigDecimal committeeThreshold;
    @Getter
    private BigInteger committeeNumerator;
    @Getter
    private BigInteger committeeDenominator;
    @Getter
    private GenesisConstitution constitution;

    public ConwayGenesis(File file) {
        super(file);
    }

    public ConwayGenesis(InputStream is) {
        super(is);
    }

    public ConwayGenesis(long protocolMagic) {
        super(protocolMagic);
    }

    @Override
    protected void readGenesisData(JsonNode genesisJson) {
        committeeMembers = new ArrayList<>();

        var poolVotingThresholdsNode = genesisJson.get(POOL_VOTING_THRESHOLDS);

        PoolVotingThresholds poolVotingThresholds = null;
        if (poolVotingThresholdsNode != null) {
            var pvtCommitteeNormalNode = poolVotingThresholdsNode.get(PVT_COMMITTEE_NORMAL);
            BigDecimal pvtCommitteNormal = pvtCommitteeNormalNode.decimalValue();

            var pvtCommitteeNoConfidenceNode = poolVotingThresholdsNode.get(PVT_COMMITTEE_NO_CONFIDENCE);
            BigDecimal pvtCommitteeNoConfidence = pvtCommitteeNoConfidenceNode.decimalValue();

            var pvtHardForkInitiationNode = poolVotingThresholdsNode.get(PVT_HARD_FORK_INITIATION);
            BigDecimal pvtHardForkInitiation = pvtHardForkInitiationNode.decimalValue();

            var pvtMotionNoConfidenceNode = poolVotingThresholdsNode.get(PVT_MOTION_NO_CONFIDENCE);
            BigDecimal pvtMotionNoConfidence = pvtMotionNoConfidenceNode.decimalValue();

            var pvtPPSecurityGroup = poolVotingThresholdsNode.get(PVT_PP_SECURITY_GROUP);
            BigDecimal pvtSecurityGroup = pvtPPSecurityGroup.decimalValue();

            poolVotingThresholds = PoolVotingThresholds.builder()
                    .pvtCommitteeNormal(pvtCommitteNormal)
                    .pvtCommitteeNoConfidence(pvtCommitteeNoConfidence)
                    .pvtHardForkInitiation(pvtHardForkInitiation)
                    .pvtMotionNoConfidence(pvtMotionNoConfidence)
                    .pvtPPSecurityGroup(pvtSecurityGroup)
                    .build();
        }

        var dRepVotingThresholdsNode = genesisJson.get(D_REP_VOTING_THRESHOLDS);
        DrepVoteThresholds drepVoteThresholds = null;
        if (dRepVotingThresholdsNode != null) {
            var dvtMotionNoConfidenceNode = dRepVotingThresholdsNode.get(DVT_MOTION_NO_CONFIDENCE);
            BigDecimal dvtMotionNoConfidence = dvtMotionNoConfidenceNode.decimalValue();

            var dvtCommitteeNormalNode = dRepVotingThresholdsNode.get(DVT_COMMITTEE_NORMAL);
            BigDecimal dvtCommitteeNormal = dvtCommitteeNormalNode.decimalValue();

            var dvtCommitteeNoConfidenceNode = dRepVotingThresholdsNode.get(DVT_COMMITTEE_NO_CONFIDENCE);
            BigDecimal dvtCommitteeNoConfidence = dvtCommitteeNoConfidenceNode.decimalValue();

            var dvtUpdateToConstitutionNode = dRepVotingThresholdsNode.get(DVT_UPDATE_TO_CONSTITUTION);
            BigDecimal dvtUpdateToConstitution = dvtUpdateToConstitutionNode.decimalValue();

            var dvtHardForkInitiationNode = dRepVotingThresholdsNode.get(DVT_HARD_FORK_INITIATION);
            BigDecimal dvtHardForkInitiation = dvtHardForkInitiationNode.decimalValue();

            var dvtPPNetworkGroupNode = dRepVotingThresholdsNode.get(DVT_PPNETWORK_GROUP);
            BigDecimal dvtPPNetworkGroup = dvtPPNetworkGroupNode.decimalValue();

            var dvtPPEconomicGroupNode = dRepVotingThresholdsNode.get(DVT_PPECONOMIC_GROUP);
            BigDecimal dvtPPEconomicGroup = dvtPPEconomicGroupNode.decimalValue();

            var dvtPPTechnicalGroupNode = dRepVotingThresholdsNode.get(DVT_PPTECHNICAL_GROUP);
            BigDecimal dvtPPTechnicalGroup = dvtPPTechnicalGroupNode.decimalValue();

            var dvtPPGovGroupNode = dRepVotingThresholdsNode.get(DVT_PPGOV_GROUP);
            BigDecimal dvtPPGovGroup = dvtPPGovGroupNode.decimalValue();

            var dvtTreasuryWithdrawalNode = dRepVotingThresholdsNode.get(DVT_TREASURY_WITHDRAWAL);
            BigDecimal dvtTreasuryWithdrawal = dvtTreasuryWithdrawalNode.decimalValue();

            drepVoteThresholds = DrepVoteThresholds.builder()
                    .dvtMotionNoConfidence(dvtMotionNoConfidence)
                    .dvtCommitteeNormal(dvtCommitteeNormal)
                    .dvtCommitteeNoConfidence(dvtCommitteeNoConfidence)
                    .dvtUpdateToConstitution(dvtUpdateToConstitution)
                    .dvtHardForkInitiation(dvtHardForkInitiation)
                    .dvtPPNetworkGroup(dvtPPNetworkGroup)
                    .dvtPPEconomicGroup(dvtPPEconomicGroup)
                    .dvtPPTechnicalGroup(dvtPPTechnicalGroup)
                    .dvtPPGovGroup(dvtPPGovGroup)
                    .dvtTreasuryWithdrawal(dvtTreasuryWithdrawal)
                    .build();
        }

        var committeeMinSizeNode = genesisJson.get(COMMITTEE_MIN_SIZE);
        var committeeMinSize = committeeMinSizeNode != null? committeeMinSizeNode.asInt(): null;

        var committeeMaxTermLengthNode = genesisJson.get(COMMITTEE_MAX_TERM_LENGTH);
        var committeeMaxTermLength = committeeMaxTermLengthNode != null? committeeMaxTermLengthNode.asInt(): null;

        var govActionLifetimeNode = genesisJson.get(GOV_ACTION_LIFETIME);
        var govActionLifetime = govActionLifetimeNode != null? govActionLifetimeNode.asInt(): null;

        var govActionDepositNode = genesisJson.get(GOV_ACTION_DEPOSIT);
        var govActionDeposit = govActionDepositNode != null? govActionDepositNode.bigIntegerValue(): null;

        var dRepDepositNode = genesisJson.get(D_REP_DEPOSIT);
        var dRepDeposit = dRepDepositNode != null? dRepDepositNode.bigIntegerValue(): null;

        var dRepActivityNode = genesisJson.get(D_REP_ACTIVITY);
        var dRepActivity = dRepActivityNode != null? dRepActivityNode.asInt(): null;

        var minFeeRefScriptCostPerByteNode = genesisJson.get(MIN_FEE_REF_SCRIPT_COST_PER_BYTE);
        var minFeeRefScriptCostPerByte = minFeeRefScriptCostPerByteNode != null ? minFeeRefScriptCostPerByteNode.decimalValue() : null;

        protocolParams = ProtocolParams.builder()
                .poolVotingThresholds(poolVotingThresholds)
                .drepVotingThresholds(drepVoteThresholds)
                .committeeMinSize(committeeMinSize)
                .committeeMaxTermLength(committeeMaxTermLength)
                .govActionLifetime(govActionLifetime)
                .govActionDeposit(govActionDeposit)
                .drepDeposit(dRepDeposit)
                .drepActivity(dRepActivity)
                .minFeeRefScriptCostPerByte(minFeeRefScriptCostPerByte)
                .build();
        var committeeNode = genesisJson.get(COMMITTEE);

        if (committeeNode != null) {
            var membersNode = committeeNode.get(MEMBERS);

            if (membersNode != null) {
                Iterator<Map.Entry<String, JsonNode>> fields = membersNode.fields();

                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String key = field.getKey();
                    Integer expiredEpoch = field.getValue().asInt();
                    Boolean hasScript = key.contains("scriptHash");
                    String hash = key.split("-")[1];

                    GenesisCommitteeMember member = GenesisCommitteeMember.builder()
                            .hash(hash)
                            .hasScript(hasScript)
                            .expiredEpoch(expiredEpoch)
                            .build();
                    committeeMembers.add(member);
                }
            }

            var ccThresholdNode = committeeNode.get(THRESHOLD);
            if (ccThresholdNode != null) {
                if (ccThresholdNode.isObject()) {
                    var numeratorNode = ccThresholdNode.get(NUMERATOR);
                    committeeNumerator = numeratorNode != null ? numeratorNode.bigIntegerValue() : null;
                    var denominatorNode = ccThresholdNode.get(DENOMINATOR);
                    committeeDenominator = denominatorNode != null ? denominatorNode.bigIntegerValue() : null;

                    if (committeeNumerator != null && committeeDenominator != null && committeeDenominator.compareTo(BigInteger.ZERO) != 0) {
                        try {
                            committeeThreshold =  new BigDecimal(committeeNumerator).divide(new BigDecimal(committeeDenominator));
                        } catch (ArithmeticException e) { //set scale and try again
                            committeeThreshold =  new BigDecimal(committeeNumerator).divide(new BigDecimal(committeeDenominator), 10, RoundingMode.HALF_UP);
                        }
                    }
                } else {
                    committeeThreshold = ccThresholdNode.decimalValue();
                }
            }
        }

        var plutusV3CostModelNode = genesisJson.get(PLUTUS_V3_COST_MODEL);

        if (plutusV3CostModelNode != null && plutusV3CostModelNode.isArray()) {
            Map<String, long[]> costModelMap = new HashMap<>();

            List<Long> costModelList = new ArrayList<>();
            for (JsonNode node : plutusV3CostModelNode) {
                costModelList.add(node.longValue());
            }

            long[] plutusV3CostModelArray = costModelList.stream().mapToLong(Long::longValue).toArray();
            costModelMap.put(PLUTUS_V3, plutusV3CostModelArray);
            protocolParams.setCostModels(costModelMap);
            protocolParams.setCostModelsHash("genesis.conway");
        }

        var constitutionNode = genesisJson.get(CONSTITUTION);

        if (constitutionNode != null) {
            var anchorNode = constitutionNode.get(ANCHOR);
            if (anchorNode != null) {
                String anchorUrl = anchorNode.get(URL) != null ? anchorNode.get(URL).asText() : null;
                String anchorHash = anchorNode.get(DATA_HASH) != null ? anchorNode.get(DATA_HASH).asText() : null;
                constitution = GenesisConstitution.builder()
                        .anchorHash(anchorHash)
                        .anchorUrl(anchorUrl)
                        .build();
            }

            String script = constitutionNode.get(SCRIPT) != null ? constitutionNode.get("script").asText() : null;
            if (constitution != null) {
                constitution.setScript(script);
            }
        }
    }

    @Override
    protected String getFileName() {
        return "conway-genesis.json";
    }

}
