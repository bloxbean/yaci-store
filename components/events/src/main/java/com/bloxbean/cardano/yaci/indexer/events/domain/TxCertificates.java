package com.bloxbean.cardano.yaci.indexer.events.domain;

import com.bloxbean.cardano.yaci.core.model.certs.Certificate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TxCertificates {
    private String txHash;
    private List<Certificate> certificates;
}
