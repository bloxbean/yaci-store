package com.bloxbean.cardano.yaci.store.governanceaggr.event;

import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.TxVote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VotingEvent {
    private EventMetadata metadata;
    private List<TxVote> txVotes;
}
