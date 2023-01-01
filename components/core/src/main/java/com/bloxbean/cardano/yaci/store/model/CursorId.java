package com.bloxbean.cardano.yaci.store.model;

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
    @Column(name = "block_number")
    private Long block;
}
