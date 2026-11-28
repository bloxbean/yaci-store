package com.bloxbean.cardano.yaci.store.snapshot.manifest;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SourcePruningTest {

    @Test
    void listsOnlyThePruningSettingsThatAreOn() {
        Map<String, String> settings = new LinkedHashMap<>();
        settings.put("store.utxo.pruning-enabled", "true");
        settings.put("store.transaction.pruning-enabled", "false");
        settings.put("store.blocks.cbor-pruning-enabled", " TRUE ");
        settings.put("store.transaction.save-cbor", "true");
        settings.put("store.account.pruning-enabled", null);

        assertThat(SourcePruning.enabled(settings))
                .containsExactly("store.blocks.cbor-pruning-enabled=true", "store.utxo.pruning-enabled=true");
    }

    @Test
    void accountHistoryCleanupCountsAsPruning() {
        assertThat(SourcePruning.enabled(Map.of("store.account.history-cleanup-enabled", "true")))
                .containsExactly("store.account.history-cleanup-enabled=true");
        assertThat(SourcePruning.enabled(Map.of("store.account.history-cleanup-enabled", "false"))).isEmpty();
    }

    @Test
    void aSourceWithoutPruningSettingsIsNotPruning() {
        assertThat(SourcePruning.enabled(null)).isEmpty();
        assertThat(SourcePruning.enabled(Map.of())).isEmpty();
        assertThat(SourcePruning.enabled(Map.of("store.utxo.pruning-enabled", "false"))).isEmpty();
    }
}
