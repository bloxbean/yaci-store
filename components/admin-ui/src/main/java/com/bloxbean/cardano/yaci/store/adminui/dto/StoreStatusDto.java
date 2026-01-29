package com.bloxbean.cardano.yaci.store.adminui.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreStatusDto {
    private String name;
    private boolean enabled;
    private boolean apiEnabled;
}
