package com.bloxbean.cardano.yaci.indexer.model;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Range {
    private Point from;
    private Point to;
}
