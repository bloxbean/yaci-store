package com.bloxbean.cardano.yaci.store.submit.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for submit transaction evaluation.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "store.submit")
public class TxEvaluationProperties {
    /**
     * Backend used by the Blockfrost-compatible transaction evaluation endpoint.
     */
    private TxEvaluatorMode txEvaluatorMode = TxEvaluatorMode.SCALUS;
}
