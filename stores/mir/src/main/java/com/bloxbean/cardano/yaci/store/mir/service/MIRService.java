package com.bloxbean.cardano.yaci.store.mir.service;

import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousReward;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousRewardSummary;
import com.bloxbean.cardano.yaci.store.mir.storage.MIRStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MIRService {
    private final MIRStorage mirStorage;

    public List<MoveInstataneousRewardSummary> getMoveInstataneousRewardSummary(int page, int count) {
        return mirStorage.findMIRSummaries(page, count);
    }

    public List<MoveInstataneousReward> getMoveInstataneousRewardByTxHash(String txHash) {
        return mirStorage.findMIRsByTxHash(txHash);
    }

}
