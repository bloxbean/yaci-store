package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.common.Constants;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockFinderTest {
    protected String node = Constants.PREPROD_IOHK_RELAY_ADDR;
    protected int nodePort = Constants.PREPROD_IOHK_RELAY_PORT;
    protected long protocolMagic = Constants.PREPROD_PROTOCOL_MAGIC;

    @Test
    void blockNotExists() {
        BlockSync blockSync = new BlockSync(node, nodePort, protocolMagic, Constants.WELL_KNOWN_PREPROD_POINT);
        BlockFinder blockFinder = new BlockFinder(blockSync);

        Point from = new Point(38474115, "784e0c913ce6378208f4fc1abf2ce74e817048306247e0ecffa6dac676ce8c65");

        boolean exists = blockFinder.blockExists(from);
        System.out.println(exists);
        assertFalse(exists);
    }

    @Test
    void blockExists() {
        BlockSync blockSync = new BlockSync(node, nodePort, protocolMagic, Constants.WELL_KNOWN_PREPROD_POINT);
        BlockFinder blockFinder = new BlockFinder(blockSync);

        Point from = new Point(38481569, "cf884b8e61126190f6e59d71ad53c561f620cf65ed09aff468149b32b537a804");

        boolean exists = blockFinder.blockExists(from);
        System.out.println(exists);
        assertTrue(exists);
    }
}
