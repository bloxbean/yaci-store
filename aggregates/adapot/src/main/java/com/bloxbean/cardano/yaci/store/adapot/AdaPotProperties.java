package com.bloxbean.cardano.yaci.store.adapot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdaPotProperties {

    @Builder.Default
    private int updateRewardDbBatchSize = 200;

    @Builder.Default
    private boolean verifyAdapotCalcValues = true;
}
