package com.bloxbean.cardano.yaci.store.snapshot.spec;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A manifest pins each table's export contract at its {@code spec-version}, so a contract change
 * that keeps the version would make an importer reject, or worse accept, snapshots it does not
 * describe. {@code snapshot-contracts.txt} records the contract digest of every released
 * {@code id@spec-version}; a changed contract must come with a new spec-version and a new line.
 * Import-only changes (mappings, converters, SQL transforms, batching) leave it alone.
 */
class SpecContractVersionTest {

    @Test
    void aContractChangeComesWithANewSpecVersion() throws IOException {
        Map<String, String> recorded = recorded();
        List<String> changed = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        Map<String, String> current = new TreeMap<>();
        for (SnapshotTableSpec spec : SnapshotSpecRegistry.builtIn().all()) {
            String key = spec.key();
            current.put(key, spec.contractDigest());
            String expected = recorded.get(key);
            if (expected == null) {
                missing.add(key + "=" + spec.contractDigest());
            } else if (!expected.equals(spec.contractDigest())) {
                changed.add(key);
            }
        }
        assertThat(changed)
                .as("The export contract of these specifications changed at the same spec-version. Bump "
                        + "spec-version and add a line for it, or keep the change on the import side.")
                .isEmpty();
        assertThat(missing).as("New specification versions; add these lines to snapshot-contracts.txt").isEmpty();
        assertThat(current).isNotEmpty();
    }

    private static Map<String, String> recorded() throws IOException {
        Map<String, String> out = new TreeMap<>();
        try (InputStream in = SpecContractVersionTest.class.getResourceAsStream("/snapshot-contracts.txt")) {
            assertThat(in).as("snapshot-contracts.txt").isNotNull();
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                String l = line.trim();
                if (l.isEmpty() || l.startsWith("#")) {
                    continue;
                }
                int eq = l.indexOf('=');
                out.put(l.substring(0, eq), l.substring(eq + 1));
            }
        }
        return out;
    }
}
