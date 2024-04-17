package com.bloxbean.cardano.yaci.store.api.utxo.client;

import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Utxo;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Retryable Utxo client. Retries 5 times with backoff delay of 2 sec and multiplier of 2.
 * If the retry fails, it throws IllegalStateException
 */
@Component("retryableUtxoClient")
@Qualifier("retryableUtxoClient")
@Slf4j
public class RetryableUtxoClient implements UtxoClient {

    private final UtxoClient delegate;

    public RetryableUtxoClient(UtxoClient utxoClient) {
        this.delegate = utxoClient;
    }

    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 2))
    @Override
    public List<AddressUtxo> getUtxosByIds(List<UtxoKey> utxoIds) {
        if (log.isDebugEnabled()) {
            log.info("Trying to get utxo by ids.." + utxoIds);
        }

        var utxos = delegate.getUtxosByIds(utxoIds);

        if (utxos.size() != utxoIds.size()) {
            var fetchedUtxoKeys = utxos.stream().map(addressUtxo -> new UtxoKey(addressUtxo.getTxHash(), addressUtxo.getOutputIndex()))
                    .toList();
            var missingKeys = utxoIds.stream().filter(utxoKey -> !fetchedUtxoKeys.contains(utxoKey)).toList();

            throw new IllegalStateException("Utxos not found for all ids : " + missingKeys);
        } else
            return utxos;
    }

    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 2))
    @Override
    public Optional<AddressUtxo> getUtxoById(UtxoKey utxoId) {
        if (log.isDebugEnabled()) {
            log.info("Trying to get utxo by id.." + utxoId);
        }

        var utxo = delegate.getUtxoById(utxoId);
        if (utxo.isEmpty())
            throw new IllegalStateException("Utxo not found for id" + utxoId);
        else
            return utxo;
    }

    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 2))
    @Override
    public List<Utxo> getUtxoByAddress(String address, int page, int count) {
        return delegate.getUtxoByAddress(address, page, count);
    }
}
