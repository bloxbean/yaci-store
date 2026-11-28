package com.bloxbean.cardano.yaci.store.admin.cli.snapshot;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.SourcePruning;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/** The CIP-113 settings a manifest records, and a target is checked against (cip113-registry-node must-match). */
class Cip113SettingsTest {

    @Test
    void defaultsAreOnWithTheBundledRegistries() {
        var settings = new SnapshotCliSupport(new MockEnvironment()).pruningSettings();
        assertThat(settings).containsEntry("store.assets.ext.cip113.enabled", "true")
                .containsEntry("store.assets.ext.cip113.registry-nft-policy-ids", "");
        assertThat(SourcePruning.enabled(settings)).isEmpty();
    }

    /** The same registries written differently compare equal. */
    @Test
    void registryIdsAreNormalized() {
        var a = new SnapshotCliSupport(new MockEnvironment()
                .withProperty("store.assets.ext.cip113.registry-nft-policy-ids", " BB , aa,bb")).pruningSettings();
        var b = new SnapshotCliSupport(new MockEnvironment()
                .withProperty("store.assets.ext.cip113.registry-nft-policy-ids[0]", "aa")
                .withProperty("store.assets.ext.cip113.registry-nft-policy-ids[1]", "bb")).pruningSettings();
        assertThat(a.get("store.assets.ext.cip113.registry-nft-policy-ids")).isEqualTo("aa,bb")
                .isEqualTo(b.get("store.assets.ext.cip113.registry-nft-policy-ids"));
    }
}
