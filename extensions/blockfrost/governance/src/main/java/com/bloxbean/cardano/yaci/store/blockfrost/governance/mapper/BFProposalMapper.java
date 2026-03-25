package com.bloxbean.cardano.yaci.store.blockfrost.governance.mapper;

import com.bloxbean.cardano.yaci.store.blockfrost.governance.dto.BFDRepDelegatorDto;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.dto.BFProposalDto;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.dto.BFProposalMetadataDto;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.dto.BFProposalParametersDto;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.dto.BFProposalVoteDto;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.dto.BFProposalWithdrawalDto;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl.model.BFDRepDelegator;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl.model.BFProposal;
import com.bloxbean.cardano.yaci.store.common.util.GovUtil;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface BFProposalMapper {

    BFProposalMapper INSTANCE = Mappers.getMapper(BFProposalMapper.class);

    // ── Proposal ──────────────────────────────────────────────────────────

    @Mapping(target = "id", expression = "java(com.bloxbean.cardano.yaci.store.common.util.GovUtil.toGovActionIdBech32(row.getTxHash(), row.getCertIndex()))")
    @Mapping(target = "txHash", source = "txHash")
    @Mapping(target = "certIndex", source = "certIndex")
    @Mapping(target = "governanceType", source = "type", qualifiedByName = "govActionTypeToString")
    @Mapping(target = "governanceDescription", source = "details")
    @Mapping(target = "deposit", source = "deposit", qualifiedByName = "longToString")
    @Mapping(target = "returnAddress", source = "returnAddress")
    @Mapping(target = "ratifiedEpoch", source = "ratifiedEpoch")
    @Mapping(target = "enactedEpoch", ignore = true)
    @Mapping(target = "droppedEpoch", expression = "java(row.getExpiredEpoch() != null ? row.getExpiredEpoch() + 1 : null)")
    @Mapping(target = "expiredEpoch", source = "expiredEpoch")
    @Mapping(target = "expiration", expression = "java(row.getEpoch() != null && row.getGovActionLifetime() != null && row.getGovActionLifetime() > 0 ? row.getEpoch() + row.getGovActionLifetime() + 1 : null)")
    BFProposalDto toDto(BFProposal row);

    // ── Proposal parameters ───────────────────────────────────────────────

    @Mapping(target = "id", expression = "java(com.bloxbean.cardano.yaci.store.common.util.GovUtil.toGovActionIdBech32(row.getTxHash(), row.getCertIndex()))")
    @Mapping(target = "txHash", source = "txHash")
    @Mapping(target = "certIndex", source = "certIndex")
    @Mapping(target = "parameters", source = "details")
    BFProposalParametersDto toParametersDto(BFProposal row);

    // ── Proposal withdrawal ───────────────────────────────────────────────
    // Note: built directly in BFGovernanceService (hex→bech32 conversion needed)

    // ── Proposal vote ─────────────────────────────────────────────────────

    @Mapping(target = "txHash", source = "txHash")
    @Mapping(target = "certIndex", expression = "java((int) vote.getIndex())")
    @Mapping(target = "voterRole", source = "voterType", qualifiedByName = "voterTypeToRole")
    @Mapping(target = "voter", expression = "java(mapVoterIdentifier(vote))")
    @Mapping(target = "vote", source = "vote", qualifiedByName = "voteToLowerCase")
    BFProposalVoteDto toVoteDto(VotingProcedure vote);

    // ── Proposal metadata ─────────────────────────────────────────────────

    @Mapping(target = "id", expression = "java(com.bloxbean.cardano.yaci.store.common.util.GovUtil.toGovActionIdBech32(row.getTxHash(), row.getCertIndex()))")
    @Mapping(target = "txHash", source = "txHash")
    @Mapping(target = "certIndex", source = "certIndex")
    @Mapping(target = "url", source = "anchorUrl")
    @Mapping(target = "hash", source = "anchorHash")
    @Mapping(target = "jsonMetadata", ignore = true)
    @Mapping(target = "bytes", ignore = true)
    BFProposalMetadataDto toMetadataDto(BFProposal row);

    // ── Named converters ──────────────────────────────────────────────────

    @Named("longToString")
    default String longToString(Long value) {
        return value != null ? String.valueOf(value) : "0";
    }

    @Named("govActionTypeToString")
    default String govActionTypeToString(String type) {
        if (type == null) return null;
        return switch (type) {
            case "PARAMETER_CHANGE_ACTION" -> "parameter_change";
            case "HARD_FORK_INITIATION_ACTION" -> "hard_fork_initiation";
            case "TREASURY_WITHDRAWALS_ACTION" -> "treasury_withdrawals";
            case "NO_CONFIDENCE" -> "no_confidence";
            case "UPDATE_COMMITTEE" -> "new_committee";
            case "NEW_CONSTITUTION" -> "new_constitution";
            case "INFO_ACTION" -> "info_action";
            default -> type.toLowerCase();
        };
    }

    @Named("voterTypeToRole")
    default String voterTypeToRole(com.bloxbean.cardano.yaci.core.model.governance.VoterType voterType) {
        if (voterType == null) return null;
        return switch (voterType) {
            case DREP_KEY_HASH, DREP_SCRIPT_HASH -> "drep";
            case STAKING_POOL_KEY_HASH -> "spo";
            default -> voterType.name().startsWith("CONSTITUTIONAL_COMMITTEE") ? "constitutional_committee" : voterType.name().toLowerCase();
        };
    }

    @Named("voteToLowerCase")
    default String voteToLowerCase(com.bloxbean.cardano.yaci.core.model.governance.Vote vote) {
        return vote != null ? vote.name().toLowerCase() : null;
    }

    default String mapVoterIdentifier(VotingProcedure vote) {
        if (vote == null || vote.getVoterHash() == null) return null;
        try {
            return switch (vote.getVoterType()) {
                case DREP_KEY_HASH -> com.bloxbean.cardano.client.governance.GovId.drepFromKeyHash(
                        com.bloxbean.cardano.client.util.HexUtil.decodeHexString(vote.getVoterHash()));
                case DREP_SCRIPT_HASH -> com.bloxbean.cardano.client.governance.GovId.drepFromScriptHash(
                        com.bloxbean.cardano.client.util.HexUtil.decodeHexString(vote.getVoterHash()));
                case STAKING_POOL_KEY_HASH -> com.bloxbean.cardano.yaci.store.common.util.PoolUtil.getBech32PoolId(vote.getVoterHash());
                default -> vote.getVoterHash();
            };
        } catch (Exception e) {
            return vote.getVoterHash();
        }
    }
}
