package com.bloxbean.cardano.yaci.store.blockfrost.governance.mapper;

import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.core.model.governance.Vote;
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
    @Mapping(target = "activeEpoch", expression = "java(\"RETIRED\".equalsIgnoreCase(row.getStatus()) ? null : row.getActiveEpoch())")
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
    @Mapping(target = "deposit", expression = "java(registrationDeposit(registration))")
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
    @Mapping(target = "hex", expression = "java(addCip129PrefixFromDrepId(registration.getDrepId(), registration.getDrepHash()))")
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

    /**
     * Deposit is only meaningful on a registration. The store persists the refund amount on
     * UNREG_DREP_CERT rows, but Blockfrost reports deposit as null for deregistered and
     * updated actions, so only REG_DREP_CERT surfaces a value.
     */
    default String registrationDeposit(DRepRegistration registration) {
        if (registration == null || registration.getDeposit() == null) return null;
        return registration.getType() == CertificateType.REG_DREP_CERT
                ? registration.getDeposit().toString() : null;
    }

    @Named("voteToLowerCase")
    default String voteToLowerCase(Vote vote) {
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
     * Derives the CIP-129 hex for metadata using drep_id bech32 to determine credential type.
     * Falls back to "22" prefix (key hash) if bech32 decode fails.
     */
    default String addCip129PrefixFromDrepId(String drepId, String rawHash) {
        return hexFromDrepId(drepId, rawHash);
    }

    /**
     * Derives the CIP-129 hex from drep_id bech32 (which encodes the header byte).
     * Falls back to adding "22" prefix if bech32 decode fails.
     */
    default String hexFromDrepId(String drepId, String rawHash) {
        if (rawHash == null) return null;
        if (drepId == null) return "22" + rawHash;
        try {
            Bech32.Bech32Data decoded = Bech32.decode(drepId);
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
