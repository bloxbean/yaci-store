package com.bloxbean.cardano.yaci.store.adapot;

import com.bloxbean.cardano.yaci.store.adapot.storage.AdaPotStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.DepositStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.AdaPotStorageImpl;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.DepositStorageImpl;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.AdaPotMapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.AdaPotRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.DepositRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnProperty(
        prefix = "store.adapot",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.adapot"})
@EnableJpaRepositories(basePackages = {"com.bloxbean.cardano.yaci.store.adapot"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.adapot"})
@EnableTransactionManagement
@EnableScheduling
public class AdaPotConfiguration {

    public DepositStorage depositStorage(DepositRepository depositRepository, AdaPotMapper adaPotMapper) {
        return new DepositStorageImpl(depositRepository, adaPotMapper);
    }

    public AdaPotStorage adaPotStorage(AdaPotRepository adaPotRepository, AdaPotMapper adaPotMapper) {
        return new AdaPotStorageImpl(adaPotRepository, adaPotMapper);
    }

}
