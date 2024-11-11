package com.bloxbean.cardano.yaci.store.transaction.processor;

import com.bloxbean.cardano.yaci.core.model.Amount;
import com.bloxbean.cardano.yaci.core.model.TransactionBody;
import com.bloxbean.cardano.yaci.core.model.TransactionInput;
import com.bloxbean.cardano.yaci.core.model.TransactionOutput;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class FeeResolverTest {

    @Mock
    private UtxoClient utxoClient;

    @InjectMocks
    private FeeResolver feeResolver;

    @Test
    public void resolveFee_shouldReturnTransactionFee() {
        Transaction transaction = Transaction.builder()
                .body(TransactionBody.builder()
                        .fee(new BigInteger("1000"))
                        .build()).build();

        transaction.setInvalid(false);

        BigInteger fee = feeResolver.resolveFee(transaction);

        assertEquals(new BigInteger("1000"), fee);
    }

    @Test
    public void resolveFee_shouldReturnTotalCollateral() {
        Transaction transaction = Transaction.builder()
                .body(TransactionBody.builder()
                        .fee(new BigInteger("1000"))
                        .totalCollateral(new BigInteger("8000"))
                        .build()).build();

        transaction.setInvalid(true);

        BigInteger fee = feeResolver.resolveFee(transaction);

        assertEquals(new BigInteger("8000"), fee);
    }

    @Test
    public void resolveFee_shouldReturnCollateralInputMinusOutput() {
        Transaction transaction = Transaction.builder()
                .body(TransactionBody.builder()
                        .collateralInputs(Set.of(
                                new TransactionInput("tx1", 0),
                                new TransactionInput("tx2", 1))
                        )
                        .fee(new BigInteger("1000"))
                        //.totalCollateral(new BigInteger("8000"))
                        .collateralReturn(TransactionOutput.builder()
                                .amounts(List.of(
                                        Amount.builder()
                                                .unit("asset1")
                                                .quantity(new BigInteger("9000"))
                                                .build(),
                                        Amount.builder()
                                                .unit(LOVELACE)
                                                .quantity(new BigInteger("3000"))
                                                .build()
                                )).build())
                        .build()).build();
        transaction.setInvalid(true);

        //Total inputs = 13000 lovelace
        given(utxoClient.getUtxosByIds(anyList())).willReturn(List.of(
                AddressUtxo.builder()
                        .ownerAddr("addr1")
                        .lovelaceAmount(new BigInteger("5000")).build(),
                AddressUtxo.builder()
                        .ownerAddr("addr1")
                        .lovelaceAmount(new BigInteger("8000"))
                       .build()
        ));

        BigInteger fee = feeResolver.resolveFee(transaction);

        assertEquals(new BigInteger("10000"), fee);
    }

    @Test
    public void resolveFee_whenBothTotalCollateralAndCollateralReturn_shouldReturnTotalCollateral() {
        Transaction transaction = Transaction.builder()
                .body(TransactionBody.builder()
                        .collateralInputs(Set.of(
                                new TransactionInput("tx1", 0),
                                new TransactionInput("tx2", 1))
                        )
                        .fee(new BigInteger("1000"))
                        .totalCollateral(new BigInteger("8000"))
                        .collateralReturn(TransactionOutput.builder()
                                .amounts(List.of(
                                        Amount.builder()
                                                .unit("asset1")
                                                .quantity(new BigInteger("9000"))
                                                .build(),
                                        Amount.builder()
                                                .unit(LOVELACE)
                                                .quantity(new BigInteger("3000"))
                                                .build()
                                )).build())
                        .build()).build();
        transaction.setInvalid(true);


        BigInteger fee = feeResolver.resolveFee(transaction);

        assertEquals(new BigInteger("8000"), fee);
    }
}
