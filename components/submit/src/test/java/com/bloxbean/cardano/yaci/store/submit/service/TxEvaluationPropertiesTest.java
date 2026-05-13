package com.bloxbean.cardano.yaci.store.submit.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class TxEvaluationPropertiesTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void bindsScalusEvaluatorMode() {
        contextRunner
                .withPropertyValues("store.submit.tx-evaluator-mode=scalus")
                .run(context -> assertThat(context.getBean(TxEvaluationProperties.class).getTxEvaluatorMode())
                        .isEqualTo(TxEvaluatorMode.SCALUS));
    }

    @Test
    void invalidEvaluatorModeFailsAtStartup() {
        contextRunner
                .withPropertyValues("store.submit.tx-evaluator-mode=scalu")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("store.submit.tx-evaluator-mode")
                            .hasStackTraceContaining("scalu");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(TxEvaluationProperties.class)
    static class TestConfig {
    }
}
