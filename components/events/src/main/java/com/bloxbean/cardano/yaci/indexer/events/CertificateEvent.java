package com.bloxbean.cardano.yaci.indexer.events;

import com.bloxbean.cardano.yaci.indexer.events.domain.TxCertificates;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CertificateEvent {
    private EventMetadata metadata;
    private List<TxCertificates> txCertificatesList;
}
