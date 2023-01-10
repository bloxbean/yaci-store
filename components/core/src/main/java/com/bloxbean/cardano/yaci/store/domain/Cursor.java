package com.bloxbean.cardano.yaci.store.domain;

import com.bloxbean.cardano.yaci.core.model.Era;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class Cursor {
    private Long slot;
    private String blockHash;
    private Long block;
    private Era era;
}
