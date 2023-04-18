package com.bloxbean.cardano.yaci.store.core.storage.impl.model;

import jakarta.persistence.Column;
import lombok.*;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class CursorId implements Serializable {
    @Column(name = "id")
    private Long id;
    @Column(name = "slot")
    private Long slot;
}
