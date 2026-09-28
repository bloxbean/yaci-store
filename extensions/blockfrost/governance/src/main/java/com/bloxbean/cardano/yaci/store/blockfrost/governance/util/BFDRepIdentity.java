package com.bloxbean.cardano.yaci.store.blockfrost.governance.util;

import com.bloxbean.cardano.client.governance.GovId;
import com.bloxbean.cardano.client.transaction.spec.governance.DRep;
import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.bloxbean.cardano.yaci.core.model.governance.VoterType;

import java.util.Locale;

/**
 * Credential-aware DRep identity for Blockfrost lookups.
 * Regular DReps are identified by CIP-129 {@code drep_id} plus the raw hash and
 * credential type; a raw 28-byte hash alone is not a unique identity.
 */
public record BFDRepIdentity(String drepId, String hash, String credType) {

    public static final String ALWAYS_ABSTAIN = "drep_always_abstain";
    public static final String ALWAYS_NO_CONFIDENCE = "drep_always_no_confidence";

    public static boolean isSpecialId(String drepId) {
        return ALWAYS_ABSTAIN.equals(drepId) || ALWAYS_NO_CONFIDENCE.equals(drepId);
    }

    public static BFDRepIdentity special(String drepId) {
        if (!isSpecialId(drepId)) {
            throw new IllegalArgumentException("Not a special DRep ID: " + drepId);
        }
        String credType = ALWAYS_ABSTAIN.equals(drepId)
                ? DrepType.ABSTAIN.name()
                : DrepType.NO_CONFIDENCE.name();
        return new BFDRepIdentity(drepId, "", credType);
    }

    /**
     * Legacy raw-hash identity used only when the hash maps to no stored CIP-129 ID.
     */
    public static BFDRepIdentity rawHash(String hash) {
        if (hash == null || hash.isBlank()) {
            throw new IllegalArgumentException("DRep hash cannot be null or blank");
        }
        return new BFDRepIdentity(null, hash.toLowerCase(Locale.ROOT), null);
    }

    /**
     * Parses a CIP-129 bech32 DRep ID or a protocol-defined special ID.
     * Raw 56-character hashes are not accepted here; the service resolves those
     * only after checking they are unambiguous in storage.
     */
    public static BFDRepIdentity parse(String drepId) {
        if (drepId == null || drepId.isBlank()) {
            throw new IllegalArgumentException("DRep ID cannot be null or blank");
        }
        if (isSpecialId(drepId)) {
            return special(drepId);
        }

        DRep drep = GovId.toDrep(drepId);
        if (drep.getType() == null || drep.getHash() == null) {
            throw new IllegalArgumentException("Invalid DRep ID: " + drepId);
        }
        return new BFDRepIdentity(
                drepId,
                drep.getHash().toLowerCase(Locale.ROOT),
                drep.getType().name()
        );
    }

    public boolean isSpecial() {
        return isSpecialId(drepId);
    }

    public boolean isRawHashOnly() {
        return drepId == null && hash != null && !hash.isBlank();
    }

    public boolean hasScript() {
        return DrepType.SCRIPTHASH.name().equals(credType);
    }

    /**
     * Vote table {@code voter_type} for this identity, or {@code null} when the
     * credential type is unknown or the DRep is protocol-defined.
     */
    public String voterType() {
        if (DrepType.SCRIPTHASH.name().equals(credType)) {
            return VoterType.DREP_SCRIPT_HASH.name();
        }
        if (DrepType.ADDR_KEYHASH.name().equals(credType)) {
            return VoterType.DREP_KEY_HASH.name();
        }
        return null;
    }
}
