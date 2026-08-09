package com.bloxbean.cardano.yaci.store.submit.service;

/**
 * Transaction evaluation backend selected by {@code store.submit.tx-evaluator-mode}.
 */
public enum TxEvaluatorMode {
    /**
     * Delegate transaction evaluation to Ogmios.
     */
    OGMIOS,

    /**
     * Evaluate transactions locally with Scalus.
     */
    SCALUS
}
