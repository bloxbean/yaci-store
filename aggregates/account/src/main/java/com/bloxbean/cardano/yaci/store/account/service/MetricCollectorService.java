package com.bloxbean.cardano.yaci.store.account.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetricCollectorService {
    private final MeterRegistry meterRegistry;
    private Counter negativeBalanceAddressCounter;
    private Counter negativeBalanceStakeAddressCounter;
    private AtomicLong lastAccountBalanceProcessedBlock;

    public MetricCollectorService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        init();
    }

    private void init() {
        setupMetrics();
    }

    private void setupMetrics() {
        this.lastAccountBalanceProcessedBlock = new AtomicLong(0);

        negativeBalanceAddressCounter = Counter.builder("negative.balance.address.counter")
                .description("Counter for negative balance addresses")
                .register(meterRegistry);
        negativeBalanceStakeAddressCounter = Counter.builder("negative.balance.stakeaddress.counter")
                .description("Counter for negative balance stake addresses")
                .register(meterRegistry);
        Gauge.builder("last.account.balance.processed.block", lastAccountBalanceProcessedBlock, AtomicLong::get)
                .description("Last account balance processed block")
                .register(meterRegistry);
    }

    public void collectNegativeBalanceAddressMetric() {
        negativeBalanceAddressCounter.increment();
    }

    public void collectNegativeBalanceStakeAddressMetric() {
        negativeBalanceStakeAddressCounter.increment();
    }

    public void collectLastAccountBalanceProcessedBlockMetric(long blockNo) {
        lastAccountBalanceProcessedBlock.set(blockNo);
    }
}
