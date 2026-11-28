package com.bloxbean.cardano.yaci.store.admin.cli.snapshot;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.SourcePruning;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/** The settings inspect/export check, and record in the manifest, for a source that deletes rows. */
class SourcePruningSettingsTest {

    @Test
    void accountHistoryCleanupBlocksTheSourceLikePruning() {
        var source = new SnapshotCliSupport(new MockEnvironment()
                .withProperty("store.account.history-cleanup-enabled", "true"));

        assertThat(source.pruningSettings()).containsEntry("store.account.history-cleanup-enabled", "true");
        assertThat(SourcePruning.enabled(source.pruningSettings()))
                .containsExactly("store.account.history-cleanup-enabled=true");
    }

    @Test
    void aSourceWithNothingOnIsNotBlocked() {
        var source = new SnapshotCliSupport(new MockEnvironment());
        assertThat(source.pruningSettings()).containsEntry("store.account.history-cleanup-enabled", "false");
        assertThat(SourcePruning.enabled(source.pruningSettings())).isEmpty();
    }
}
