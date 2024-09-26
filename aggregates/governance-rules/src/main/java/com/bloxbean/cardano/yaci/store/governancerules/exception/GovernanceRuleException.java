package com.bloxbean.cardano.yaci.store.governancerules.exception;

public class GovernanceRuleException extends RuntimeException {
    public GovernanceRuleException(String message) {
        super(message);
    }

    public GovernanceRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
