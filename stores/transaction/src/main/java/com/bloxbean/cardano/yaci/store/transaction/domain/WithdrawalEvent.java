package com.bloxbean.cardano.yaci.store.transaction.domain;

import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WithdrawalEvent {
    private EventMetadata metadata;
    private List<Withdrawal> withdrawals;
}
