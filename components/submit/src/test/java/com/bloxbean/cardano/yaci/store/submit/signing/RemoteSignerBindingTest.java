package com.bloxbean.cardano.yaci.store.submit.signing;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.client.transaction.spec.TransactionBody;
import com.bloxbean.cardano.client.transaction.spec.TransactionInput;
import com.bloxbean.cardano.client.transaction.spec.TransactionOutput;
import com.bloxbean.cardano.client.transaction.spec.Value;
import com.bloxbean.cardano.yaci.store.submit.config.SubmitSignerRegistryProperties;
import com.bloxbean.cardano.yaci.store.submit.signing.remote.HttpRemoteSignerClient;
import com.bloxbean.cardano.yaci.store.submit.signing.remote.RemoteSignerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RemoteSignerBindingTest {

    private MockRestServiceServer server;
    private RemoteSignerClient client;

    @BeforeEach
    void setup() {
        RestTemplate restTemplate = new RestTemplate();
        // Avoid 404 for any unexpected path; explicitly match in test
        restTemplate.setInterceptors(List.of((ClientHttpRequestInterceptor) (request, body, execution) -> execution.execute(request, body)));
        server = MockRestServiceServer.createServer(restTemplate);
        client = new HttpRemoteSignerClient(restTemplate);
    }

    @Test
    void shouldAddWitnessFromRemoteSigner() {
        String endpoint = "http://localhost:8089/sign";
        String signatureHex = "a1b2";
        String vkeyHex = "0011";

        server.expect(once(), requestTo(endpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"signature":"%s","verificationKey":"%s"}
                        """.formatted(signatureHex, vkeyHex), MediaType.APPLICATION_JSON));

        SubmitSignerRegistryProperties.RemoteSignerProperties props = SubmitSignerRegistryProperties.RemoteSignerProperties.builder()
                .endpoint(endpoint)
                .authToken("token")
                .keyId("ops-key-1")
                .verificationKey(vkeyHex)
                .timeoutMs(2000)
                .build();

        RemoteSignerBinding binding = new RemoteSignerBinding(
                "remote://ops",
                Set.of("payment"),
                props,
                client
        );

        Transaction tx = Transaction.builder()
                .body(buildBody())
                .build();

        binding.signerFor("payment").sign(null, tx);

        assertThat(tx.getWitnessSet()).isNotNull();
        assertThat(tx.getWitnessSet().getVkeyWitnesses()).hasSize(1);
        assertThat(tx.getWitnessSet().getVkeyWitnesses().get(0).getSignature()).isNotEmpty();
        assertThat(tx.getWitnessSet().getVkeyWitnesses().get(0).getVkey()).containsExactly(0x00, 0x11);

        server.verify();
    }

    private TransactionBody buildBody() {
        TransactionInput input = TransactionInput.builder()
                .transactionId("0".repeat(64))
                .index(0)
                .build();

        String addr = new Account(Networks.testnet()).baseAddress();
        TransactionOutput output = TransactionOutput.builder()
                .address(addr)
                .value(Value.builder().coin(BigInteger.valueOf(1_000_000)).build())
                .build();

        return TransactionBody.builder()
                .inputs(List.of(input))
                .outputs(List.of(output))
                .fee(BigInteger.valueOf(170000))
                .build();
    }
}
