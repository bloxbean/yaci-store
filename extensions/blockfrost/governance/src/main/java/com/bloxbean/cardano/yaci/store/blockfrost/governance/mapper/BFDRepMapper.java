package com.bloxbean.cardano.yaci.store.blockfrost.governance.mapper;

import com.bloxbean.cardano.yaci.store.blockfrost.governance.dto.BFDRepDelegatorDto;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.dto.BFDRepDto;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.dto.BFDRepListItemDto;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.dto.BFDRepMetadataDto;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.dto.BFDRepUpdateDto;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.dto.BFDRepVoteDto;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl.model.BFDRepDelegator;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl.model.BFDRep;
import com.bloxbean.cardano.yaci.store.governance.domain.DRepRegistration;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DRepEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface BFDRepMapper {

    BFDRepMapper INSTANCE = Mappers.getMapper(BFDRepMapper.class);

    // ── DRep list item ────────────────────────────────────────────────────

    @Mapping(target = "drepId", source = "drepId")
    @Mapping(target = "hex", expression = "java(hexFromDrepId(entity.getDrepId(), entity.getDrepHash()))")
    BFDRepListItemDto toListItemDto(DRepEntity entity);

    // ── DRep detail ───────────────────────────────────────────────────────

    @Mapping(target = "drepId", source = "drepId")
    @Mapping(target = "hex", expression = "java(addCip129Prefix(row.getDrepHash(), row.getHasScript()))")
    @Mapping(target = "amount", source = "amount", qualifiedByName = "longToString")
    @Mapping(target = "active", expression = "java(!\"RETIRED\".equalsIgnoreCase(row.getStatus()))")
    @Mapping(target = "activeEpoch", source = "epoch")
    @Mapping(target = "hasScript", source = "hasScript")
    @Mapping(target = "retired", expression = "java(\"RETIRED\".equalsIgnoreCase(row.getStatus()))")
    @Mapping(target = "expired", source = "expired")
    @Mapping(target = "lastActiveEpoch", source = "epoch")
    BFDRepDto toDto(BFDRep row);

    // ── DRep delegator ────────────────────────────────────────────────────

    @Mapping(target = "address", source = "address")
    @Mapping(target = "amount", source = "amount", qualifiedByName = "longToString")
    BFDRepDelegatorDto toDelegatorDto(BFDRepDelegator delegator);

    // ── DRep update ───────────────────────────────────────────────────────

    @Mapping(target = "txHash", source = "txHash")
    @Mapping(target = "certIndex", expression = "java((int) registration.getCertIndex())")
    @Mapping(target = "action", expression = "java(certTypeStringToAction(registration.getType() != null ? registration.getType().name() : null))")
    BFDRepUpdateDto toUpdateDto(DRepRegistration registration);

    // ── DRep vote ─────────────────────────────────────────────────────────

    @Mapping(target = "txHash", source = "txHash")
    @Mapping(target = "certIndex", expression = "java((int) vote.getIndex())")
    @Mapping(target = "proposalId", expression = "java(vote.getGovActionId())")
    @Mapping(target = "proposalTxHash", source = "govActionTxHash")
    @Mapping(target = "proposalCertIndex", source = "govActionIndex")
    @Mapping(target = "vote", source = "vote", qualifiedByName = "voteToLowerCase")
    BFDRepVoteDto toVoteDto(VotingProcedure vote);

    // ── DRep metadata ─────────────────────────────────────────────────────

    @Mapping(target = "drepId", source = "drepId")
    @Mapping(target = "hex", source = "drepHash")
    @Mapping(target = "url", source = "anchorUrl")
    @Mapping(target = "hash", source = "anchorHash")
    @Mapping(target = "jsonMetadata", ignore = true)
    @Mapping(target = "bytes", ignore = true)
    BFDRepMetadataDto toMetadataDto(DRepRegistration registration);

    // ── Named converters ──────────────────────────────────────────────────

    @Named("longToString")
    default String longToString(Long value) {
        return value != null ? String.valueOf(value) : "0";
    }

    @Named("certTypeToAction")
    default String certTypeStringToAction(String typeName) {
        if (typeName == null) return null;
        return switch (typeName) {
            case "REG_DREP_CERT" -> "registered";
            case "UNREG_DREP_CERT" -> "deregistered";
            case "UPDATE_DREP_CERT" -> "updated";
            default -> typeName.toLowerCase();
        };
    }

    @Named("voteToLowerCase")
    default String voteToLowerCase(com.bloxbean.cardano.yaci.core.model.governance.Vote vote) {
        return vote != null ? vote.name().toLowerCase() : null;
    }

    /**
     * Adds the CIP-129 one-byte credential type prefix to the raw 28-byte hash.
     * Key hash dreps get prefix "22", script hash dreps get prefix "23".
     * This matches the Blockfrost API hex format.
     */
    default String addCip129Prefix(String rawHex, Boolean hasScript) {
        if (rawHex == null) return null;
        String prefix = (hasScript != null && hasScript) ? "23" : "22";
        return prefix + rawHex;
    }

    /**
     * Derives the CIP-129 hex from drep_id bech32 (which encodes the header byte).
     * Falls back to adding "22" prefix if bech32 decode fails.
     */
    default String hexFromDrepId(String drepId, String rawHash) {
        if (rawHash == null) return null;
        if (drepId == null) return "22" + rawHash;
        try {
            com.bloxbean.cardano.client.crypto.Bech32.Bech32Data decoded =
                    com.bloxbean.cardano.client.crypto.Bech32.decode(drepId);
            byte[] data = decoded.data;
            if (data.length == 29) {
                // First byte is the CIP-129 header (0x22 or 0x23)
                return String.format("%02x", data[0]) + rawHash;
            }
        } catch (Exception ignored) {
        }
        return "22" + rawHash;
    }
}
