package com.bloxbean.cardano.yaci.store.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class RedisBaseEntity {

    private LocalDateTime createDateTime;

    private LocalDateTime updateDateTime;

}
