package com.bloxbean.cardano.yaci.store.api.mir.service;

import com.bloxbean.cardano.yaci.store.api.mir.storage.MIRReader;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousReward;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousRewardSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MIRService {
    private final MIRReader mirReader;

    public List<MoveInstataneousRewardSummary> getMoveInstataneousRewardSummary(int page, int count) {
        return mirReader.findMIRSummaries(page, count);
    }

    public List<MoveInstataneousReward> getMoveInstataneousRewardByTxHash(String txHash) {
        return mirReader.findMIRsByTxHash(txHash);
    }

}
