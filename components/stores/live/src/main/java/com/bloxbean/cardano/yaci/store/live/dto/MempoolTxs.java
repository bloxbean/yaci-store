package com.bloxbean.cardano.yaci.store.live.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MempoolTxs {
    @Builder.Default
    private ResType resType = ResType.MEMPOOL_TX_DATA;

    private List<MempoolTx> transactions;
}
