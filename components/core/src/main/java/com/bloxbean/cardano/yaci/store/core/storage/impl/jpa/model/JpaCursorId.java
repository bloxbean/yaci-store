package com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.model;

import jakarta.persistence.Column;
import lombok.*;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class JpaCursorId implements Serializable {
    @Column(name = "id")
    private Long id;
    @Column(name = "block_hash")
    private String blockHash;
}
