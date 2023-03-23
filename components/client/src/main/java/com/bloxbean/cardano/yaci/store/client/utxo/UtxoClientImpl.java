package com.bloxbean.cardano.yaci.store.client.utxo;

import com.bloxbean.carano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.carano.yaci.store.common.domain.UtxoKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnMissingBean(name = "utxoClient")
@Slf4j
public class UtxoClientImpl implements UtxoClient {
    private final RestTemplate restTemplate;

    @Value("${server.port:8080}")
    private int serverPort;
    @Value("${store.utxo.base.url:#{null}}")
    private String utxoStoreBaseUrl;

    public UtxoClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        log.info("Enabled Remote UtxoClient >>>");
    }

    @Override
    public List<AddressUtxo> getUtxosByIds(List<UtxoKey> utxoIds) {
        String url = getBaseUrl() + "/utxos";
        AddressUtxo[] utxos = restTemplate.postForObject(url, utxoIds, AddressUtxo[].class);
        return Arrays.asList(utxos);
    }

    public Optional<AddressUtxo> getUtxoById(UtxoKey utxoId) {
        String url = getBaseUrl() + "/utxos/" + utxoId.getTxHash() + "/" + utxoId.getOutputIndex();
        AddressUtxo utxo = restTemplate.getForObject(url, AddressUtxo.class);
        return Optional.ofNullable(utxo);
    }

    private String getBaseUrl() {
        if (utxoStoreBaseUrl == null)
            utxoStoreBaseUrl = "http://localhost:" + serverPort + "/api/v1";

        return utxoStoreBaseUrl;
    }
}
