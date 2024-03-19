package com.bloxbean.cardano.yaci.store.core.storage.impl.model;

import jakarta.persistence.Column;
import lombok.*;

import java.io.Serializable;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CursorId implements Serializable {

    @Column(name = "id")
    private Long id;
    @Column(name = "block_hash")
    private String blockHash;
}
