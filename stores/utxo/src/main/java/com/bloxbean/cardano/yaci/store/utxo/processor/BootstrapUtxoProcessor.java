package com.bloxbean.cardano.yaci.store.utxo.processor;

import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import org.springframework.stereotype.Component;

import static com.bloxbean.cardano.yaci.store.utxo.UtxoStoreConfiguration.STORE_UTXO_ENABLED;

@Component
@EnableIf(STORE_UTXO_ENABLED)
public class BootstrapUtxoProcessor {
    //TODO
}
