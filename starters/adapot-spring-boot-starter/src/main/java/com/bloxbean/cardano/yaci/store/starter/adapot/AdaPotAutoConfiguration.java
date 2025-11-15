package com.bloxbean.cardano.yaci.store.starter.adapot;

import com.bloxbean.cardano.yaci.store.adapot.AdaPotConfiguration;
import com.bloxbean.cardano.yaci.store.adapot.AdaPotProperties;
import com.bloxbean.cardano.yaci.store.api.adapot.AdaPotApiConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(AdaPotAutoConfigProperties.class)
@Import({AdaPotConfiguration.class, AdaPotApiConfiguration.class})
@Slf4j
public class AdaPotAutoConfiguration {

    @Autowired
    AdaPotAutoConfigProperties properties;

    @Bean
    public AdaPotProperties adaPotProperties() {
        AdaPotProperties adaPotProperties = new AdaPotProperties();
        adaPotProperties.setEnabled(properties.getAdaPot().isEnabled());
        adaPotProperties.setUpdateRewardDbBatchSize(properties.getAdaPot().getUpdateRewardDbBatchSize());
        adaPotProperties.setBulkUpdateReward(properties.getAdaPot().isBulkUpdateReward());
        adaPotProperties.setBulkUpdateRewardWithCopy(properties.getAdaPot().isBulkUpdateRewardWithCopy());

        adaPotProperties.setRewardBulkLoadWorkMem(properties.getAdaPot().getRewardBulkLoadWorkMem());
        adaPotProperties.setRewardBulkLoadMaintenanceWorkMem(properties.getAdaPot().getRewardBulkLoadMaintenanceWorkMem());
        adaPotProperties.setStakeSnapshotWorkMem(properties.getAdaPot().getStakeSnapshotWorkMem());

        adaPotProperties.setVerifyAdapotCalcValues(properties.getAdaPot().isVerifyAdapotCalcValues());

        // Set epoch stake pruning properties
        adaPotProperties.setEpochStakePruningEnabled(properties.getAdaPot().isEpochStakePruningEnabled());
        adaPotProperties.setEpochStakePruningInterval(properties.getAdaPot().getEpochStakePruningInterval());
        adaPotProperties.setEpochStakePruningSafeEpochs(properties.getAdaPot().getEpochStakeSafeEpochs());
        adaPotProperties.setEpochStakePruningBatchSize(properties.getAdaPot().getEpochStakePruningBatchSize());

        // Set reward pruning properties
        adaPotProperties.setRewardPruningEnabled(properties.getAdaPot().isRewardPruningEnabled());
        adaPotProperties.setRewardPruningInterval(properties.getAdaPot().getRewardPruningInterval());
        adaPotProperties.setRewardPruningSafeSlots(properties.getAdaPot().getRewardPruningSafeSlots());
        adaPotProperties.setRewardPruningBatchSize(properties.getAdaPot().getRewardPruningBatchSize());

        adaPotProperties.setMetricsEnabled(properties.getAdaPot().getMetrics().isEnabled());
        adaPotProperties.setMetricsUpdateInterval(properties.getAdaPot().getMetrics().getUpdateInterval());

        return adaPotProperties;
    }
}
