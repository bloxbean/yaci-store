package com.bloxbean.cardano.yaci.store.api.blocks.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CborHexResponse {
    private String cborHex;
}
