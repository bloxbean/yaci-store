package com.bloxbean.cardano.yaci.store.blockfrost.governance.service;

import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.dto.*;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.mapper.BFDRepMapper;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.mapper.BFProposalMapper;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.BFGovernanceStorageReader;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.common.util.GovUtil;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.GovActionId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
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

    public List<BFDRepListItemDto> getDReps(int page, int count, String order) {
        return storageReader.findAllDReps(page, count, toOrder(order))
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

    public List<BFDRepDelegatorDto> getDRepDelegators(String drepId, int page, int count, String order) {
        String drepHex = resolveDRepHex(drepId);
        return storageReader.findDRepDelegators(drepHex, page, count, toOrder(order))
                .stream()
                .map(dRepMapper::toDelegatorDto)
                .collect(Collectors.toList());
    }

    public List<BFDRepUpdateDto> getDRepUpdates(String drepId, int page, int count, String order) {
        String drepHex = resolveDRepHex(drepId);
        return storageReader.findDRepUpdates(drepHex, page, count, toOrder(order))
                .stream()
                .map(dRepMapper::toUpdateDto)
                .collect(Collectors.toList());
    }

    public List<BFDRepVoteDto> getDRepVotes(String drepId, int page, int count, String order) {
        String drepHex = resolveDRepHex(drepId);
        return storageReader.findDRepVotes(drepHex, page, count, toOrder(order))
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

    public List<BFProposalDto> getProposals(int page, int count, String order) {
        return storageReader.findAllProposals(page, count, toOrder(order))
                .stream()
                .map(proposalMapper::toDto)
                .collect(Collectors.toList());
    }

    public BFProposalDto getProposal(String txHash, int certIndex) {
        return storageReader.findProposalByTxHashAndIndex(txHash, certIndex)
                .map(proposalMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Proposal not found: " + txHash + "#" + certIndex));
    }

    public BFProposalDto getProposalByGovActionId(String govActionId) {
        GovActionId id = decodeGovActionId(govActionId);
        return getProposal(id.getTransactionId(), id.getGovActionIndex());
    }

    public BFProposalParametersDto getProposalParameters(String txHash, int certIndex) {
        return storageReader.findParameterChangeProposal(txHash, certIndex)
                .map(row -> {
                    BFProposalParametersDto dto = proposalMapper.toParametersDto(row);
                    dto.setParameters(transformProtocolParams(row.getDetails()));
                    return dto;
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Protocol parameter proposal not found: " + txHash + "#" + certIndex));
    }

    public BFProposalParametersDto getProposalParametersByGovActionId(String govActionId) {
        GovActionId id = decodeGovActionId(govActionId);
        return getProposalParameters(id.getTransactionId(), id.getGovActionIndex());
    }

    public List<BFProposalWithdrawalDto> getProposalWithdrawals(String txHash, int certIndex) {
        if (!storageReader.isWithdrawalProposal(txHash, certIndex)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Treasury withdrawal proposal not found: " + txHash + "#" + certIndex);
        }
        return storageReader.findProposalWithdrawals(txHash, certIndex)
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

    public List<BFProposalVoteDto> getProposalVotes(String txHash, int certIndex, int page, int count, String order) {
        return storageReader.findProposalVotes(txHash, certIndex, page, count, toOrder(order))
                .stream()
                .map(proposalMapper::toVoteDto)
                .collect(Collectors.toList());
    }

    public List<BFProposalVoteDto> getProposalVotesByGovActionId(String govActionId, int page, int count, String order) {
        GovActionId id = decodeGovActionId(govActionId);
        return getProposalVotes(id.getTransactionId(), id.getGovActionIndex(), page, count, order);
    }

    public BFProposalMetadataDto getProposalMetadata(String txHash, int certIndex) {
        return storageReader.findProposalMetadata(txHash, certIndex)
                .map(proposalMapper::toMetadataDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Proposal metadata not found: " + txHash + "#" + certIndex));
    }

    public BFProposalMetadataDto getProposalMetadataByGovActionId(String govActionId) {
        GovActionId id = decodeGovActionId(govActionId);
        return getProposalMetadata(id.getTransactionId(), id.getGovActionIndex());
    }

    // ────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────

    private Order toOrder(String order) {
        try {
            return Order.valueOf(order.toLowerCase());
        } catch (Exception e) {
            return Order.asc;
        }
    }

    /**
     * Resolves a DRep ID (bech32 or hex) to the raw 56-char hex used in the DB.
     * Handles:
     *  - 56-char hex: raw hash, return as-is
     *  - 66-char hex: CIP-129 with 1-byte credential header, strip first byte
     *  - bech32 drep1...: decoded bytes are 29 bytes (1 header + 28 hash), strip first byte
     */
    private String resolveDRepHex(String drepId) {
        if (drepId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DRep ID cannot be null");
        // 56-char hex: raw 28-byte hash stored in DB
        if (drepId.matches("[0-9a-fA-F]{56}")) {
            return drepId.toLowerCase();
        }
        // 58-char hex: CIP-129 "hex" field (1 header byte + 28 hash bytes), strip first byte
        if (drepId.matches("[0-9a-fA-F]{58}")) {
            return drepId.substring(2).toLowerCase();
        }
        // 66-char hex: older CIP-129 with 1-byte credential header, strip first byte
        if (drepId.matches("[0-9a-fA-F]{66}")) {
            return drepId.substring(2).toLowerCase();
        }
        // bech32 drep1... — decode and strip the 1-byte credential header
        try {
            Bech32.Bech32Data decoded = Bech32.decode(drepId);
            byte[] data = decoded.data;
            // 29 bytes = 1 header byte + 28 raw hash bytes
            if (data.length == 29) {
                return HexUtil.encodeHexString(Arrays.copyOfRange(data, 1, 29));
            }
            return HexUtil.encodeHexString(data);
        } catch (Exception e) {
            // Invalid bech32 — will yield a DB miss which the caller turns into 404
            log.debug("Could not bech32-decode DRep ID '{}': {}", drepId, e.getMessage());
            return drepId;
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
            if (node != null && !node.isNull()) out.set(e.getValue(), node);
        }
        return out;
    }

    /**
     * Converts a hex-encoded Cardano stake address to bech32 (stake_test1... or stake1...).
     * The first byte is the address header — bit 0 = 0 means testnet, 1 means mainnet.
     */
    private String hexToStakeAddress(String hex) {
        if (hex == null) return null;
        byte[] bytes = HexUtil.decodeHexString(hex);
        boolean isMainnet = bytes.length > 0 && (bytes[0] & 0x01) == 1;
        String hrp = isMainnet ? "stake" : "stake_test";
        return Bech32.encode(bytes, hrp);
    }
}
