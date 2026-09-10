package com.bloxbean.cardano.yaci.store.blockfrost.governance.util;

import com.bloxbean.cardano.client.governance.GovId;
import com.bloxbean.cardano.client.util.HexUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BFDRepIdentityTest {

    private static final String SHARED_HASH = "e5ab37261b3d63600d566564879370aea031ea3108b0a6bd8cef58aa";

    @Test
    void parseKeyHashCip129Id() {
        String drepId = GovId.drepFromKeyHash(HexUtil.decodeHexString(SHARED_HASH));

        BFDRepIdentity identity = BFDRepIdentity.parse(drepId);

        assertThat(identity.drepId()).isEqualTo(drepId);
        assertThat(identity.hash()).isEqualTo(SHARED_HASH);
        assertThat(identity.credType()).isEqualTo("ADDR_KEYHASH");
        assertThat(identity.hasScript()).isFalse();
        assertThat(identity.voterType()).isEqualTo("DREP_KEY_HASH");
        assertThat(identity.isSpecial()).isFalse();
    }

    @Test
    void parseScriptHashCip129Id() {
        String drepId = GovId.drepFromScriptHash(HexUtil.decodeHexString(SHARED_HASH));

        BFDRepIdentity identity = BFDRepIdentity.parse(drepId);

        assertThat(identity.drepId()).isEqualTo(drepId);
        assertThat(identity.hash()).isEqualTo(SHARED_HASH);
        assertThat(identity.credType()).isEqualTo("SCRIPTHASH");
        assertThat(identity.hasScript()).isTrue();
        assertThat(identity.voterType()).isEqualTo("DREP_SCRIPT_HASH");
    }

    @Test
    void parseDoesNotConflateKeyAndScriptTwins() {
        BFDRepIdentity key = BFDRepIdentity.parse(GovId.drepFromKeyHash(HexUtil.decodeHexString(SHARED_HASH)));
        BFDRepIdentity script = BFDRepIdentity.parse(GovId.drepFromScriptHash(HexUtil.decodeHexString(SHARED_HASH)));

        assertThat(key.hash()).isEqualTo(script.hash());
        assertThat(key.drepId()).isNotEqualTo(script.drepId());
        assertThat(key.credType()).isNotEqualTo(script.credType());
        assertThat(key.voterType()).isNotEqualTo(script.voterType());
    }

    @Test
    void parseSpecialDRepsWithoutGovId() {
        BFDRepIdentity abstain = BFDRepIdentity.parse(BFDRepIdentity.ALWAYS_ABSTAIN);
        BFDRepIdentity noConfidence = BFDRepIdentity.parse(BFDRepIdentity.ALWAYS_NO_CONFIDENCE);

        assertThat(abstain.isSpecial()).isTrue();
        assertThat(abstain.credType()).isEqualTo("ABSTAIN");
        assertThat(abstain.voterType()).isNull();
        assertThat(noConfidence.isSpecial()).isTrue();
        assertThat(noConfidence.credType()).isEqualTo("NO_CONFIDENCE");
    }

    @Test
    void parseRejectsBlankAndNonCip129Values() {
        assertThatThrownBy(() -> BFDRepIdentity.parse(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BFDRepIdentity.parse(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BFDRepIdentity.parse(SHARED_HASH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BFDRepIdentity.parse("22" + SHARED_HASH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BFDRepIdentity.parse("not-a-drep-id"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
