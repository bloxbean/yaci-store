package com.bloxbean.cardano.yaci.store.events;

import com.bloxbean.cardano.yaci.store.events.domain.TxGovernance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GovernanceEvent {
    private EventMetadata metadata;
    private List<TxGovernance> txGovernanceList;
}
