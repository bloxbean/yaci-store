package com.bloxbean.cardano.yaci.store.blockfrost.address.service;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.blockfrost.address.storage.BFAddressStorageReader;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class BFAddressServiceTest {

    private static final String ADDRESS = "addr_test1vrxyxhvkzfu4j328whrs8n2xwn4aql6tms26kpmtun6hq7qm9u055";
    private static final String BYRON_ADDRESS = "Ae2tdPwUPEZFRbyhz3cpfC2CumGzNkFBN2L42rcUc2yjQpEkxDbkPodpMAi";

    private BFAddressStorageReader reader;
    private BFAddressService service;

    private final List<Consumer<String>> endpoints = List.of(
            a -> service.getAddressInfo(a),
            a -> service.getAddressTotal(a),
            a -> service.getAddressUtxos(a, 0, 10, Order.asc),
            a -> service.getAddressUtxosForAsset(a, "lovelace", 0, 10, Order.asc),
            a -> service.getAddressTransactions(a, 0, 10, Order.asc, null, null),
            a -> service.getAddressTxs(a, 0, 10, Order.asc));

    @BeforeEach
    void setUp() {
        reader = mock(BFAddressStorageReader.class);
        service = new BFAddressService(mock(UtxoStorageReader.class), reader, mock(AccountStoreProperties.class));
    }

    @Test
    void malformedAddress_returns400WithoutQuerying() {
        endpoints.forEach(endpoint -> assertStatus(() -> endpoint.accept("not_an_address"), HttpStatus.BAD_REQUEST));
        verifyNoInteractions(reader);
    }

    @Test
    void unusedAddress_returns404() {
        when(reader.addressExists(ADDRESS)).thenReturn(false);

        endpoints.forEach(endpoint -> assertStatus(() -> endpoint.accept(ADDRESS), HttpStatus.NOT_FOUND));
    }

    @Test
    void usedAddress_isServed() {
        when(reader.addressExists(ADDRESS)).thenReturn(true);

        endpoints.forEach(endpoint -> endpoint.accept(ADDRESS));
        assertThat(service.getAddressInfo(ADDRESS).getAddress()).isEqualTo(ADDRESS);
    }

    @Test
    void byronAddress_isValid() {
        when(reader.addressExists(BYRON_ADDRESS)).thenReturn(true);

        assertThat(service.getAddressInfo(BYRON_ADDRESS).getAddress()).isEqualTo(BYRON_ADDRESS);
    }

    private void assertStatus(Runnable call, HttpStatus status) {
        assertThatThrownBy(call::run)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(status));
    }
}
