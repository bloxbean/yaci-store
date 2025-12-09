package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRepDelegationExclusion;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HardcodedDRepDelegationExclusionProviderTest {

    @Test
    void mainnetExclusions_shouldMatchDelegatorsForExcludingFile() throws IOException {
        HardcodedDRepDelegationExclusionProvider provider = new HardcodedDRepDelegationExclusionProvider();

        List<DRepDelegationExclusion> hardcoded =
                provider.getExclusionsForNetwork(Networks.mainnet().getProtocolMagic());

        List<DRepDelegationExclusion> expected = parseDelegatorsForExcludingFile(
                Path.of("/home/sotatek/Projects/yaci-store/delegators_for_exluding.txt"));

        Set<DRepDelegationExclusion> hardcodedSet = new HashSet<>(hardcoded);
        Set<DRepDelegationExclusion> expectedSet = new HashSet<>(expected);

        assertEquals(expectedSet, hardcodedSet,
                "Hardcoded mainnet exclusions should match delegators_for_exluding.txt");
    }

    private List<DRepDelegationExclusion> parseDelegatorsForExcludingFile(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);

        Pattern objectPattern = Pattern.compile("\\{([^}]*)}", Pattern.DOTALL);
        Matcher matcher = objectPattern.matcher(content);

        List<DRepDelegationExclusion> exclusions = new ArrayList<>();

        while (matcher.find()) {
            String objectBody = matcher.group(1);

            String address = extractStringField(objectBody, "\"address\"\\s*:\\s*\"([^\"]+)\"");
            String drepHash = extractStringField(objectBody, "\"drep_hash\"\\s*:\\s*\"([^\"]+)\"");
            String drepTypeStr = extractStringField(objectBody, "\"drep_type\"\\s*:\\s*\"([^\"]+)\"");
            Long slot = extractLongField(objectBody, "\"slot\"\\s*:\\s*(\\d+)");
            Integer txIndex = extractIntField(objectBody, "\"tx_index\"\\s*:\\s*(\\d+)");
            Integer certIndex = extractIntField(objectBody, "\"cert_index\"\\s*:\\s*(\\d+)");

            if (address == null || drepHash == null || slot == null || txIndex == null || certIndex == null) {
                continue;
            }

            DrepType drepType = null;
            if (drepTypeStr != null && !drepTypeStr.isBlank()) {
                drepType = DrepType.valueOf(drepTypeStr);
            }

            exclusions.add(DRepDelegationExclusion.builder()
                    .address(address)
                    .drepHash(drepHash)
                    .drepType(drepType)
                    .slot(slot)
                    .txIndex(txIndex)
                    .certIndex(certIndex)
                    .build());
        }

        return exclusions;
    }

    private String extractStringField(String objectBody, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(objectBody);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private Long extractLongField(String objectBody, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(objectBody);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return null;
    }

    private Integer extractIntField(String objectBody, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(objectBody);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }
}

