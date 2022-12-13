package com.bloxbean.cardano.yaci.store.domain;

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
}
