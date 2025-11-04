package com.bloxbean.cardano.yaci.store.transaction.storage.impl;

import com.bloxbean.cardano.yaci.store.transaction.TransactionStoreProperties;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorageReader;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.mapper.TxnMapper;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.mapper.TxnMapperImpl;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.TxnCborRepository;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.TxnEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TransactionStorageReaderImplTest {

    @Autowired
    private TxnEntityRepository txnEntityRepository;

    @MockBean
    private TxnCborRepository txnCborRepository;

    private TransactionStorage transactionStorage;
    private TransactionStorageReader transactionStorageReader;

    private TxnMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TxnMapperImpl();
        transactionStorageReader = new TransactionStorageReaderImpl(txnEntityRepository, mapper, null);
        
        // Create TransactionStoreProperties
        TransactionStoreProperties properties = TransactionStoreProperties.builder()
                .saveCbor(false)
                .build();
        
        transactionStorage = new TransactionStorageImpl(
                txnEntityRepository, 
                txnCborRepository, 
                mapper, 
                null, // DSLContext not needed for this test
                properties
        );
    }

    @Test
    void getTotalDonation() {
        Txn txn1 = Txn.builder()
                .txHash("0000000100000")
                .epoch(4)
                .treasuryDonation(adaToLovelace(30))
                .build();

        Txn txn2 = Txn.builder()
                .txHash("1000000100000")
                .epoch(4)
                .treasuryDonation(adaToLovelace(90))
                .build();

        Txn txn3 = Txn.builder()
                .txHash("2000000100000")
                .epoch(4)
                .build();


        Txn txn4 = Txn.builder()
                .txHash("3000000100000")
                .epoch(5)
                .treasuryDonation(adaToLovelace(50))
                .build();

        Txn txn5 = Txn.builder()
                .txHash("4000000100000")
                .epoch(5)
                .treasuryDonation(adaToLovelace(10))
                .build();

        Txn txn6 = Txn.builder()
                .txHash("6000000100000")
                .epoch(6)
                .build();


        transactionStorage.saveAll(List.of(txn1, txn2, txn3, txn4, txn5, txn6));

        var epoch4Donation = transactionStorageReader.getTotalDonation(4);
        var epoch5Donation = transactionStorageReader.getTotalDonation(5);
        var epoch6Donation = transactionStorageReader.getTotalDonation(6);

        assertThat(epoch4Donation).isEqualTo(adaToLovelace(120));
        assertThat(epoch5Donation).isEqualTo(adaToLovelace(60));
        assertThat(epoch6Donation).isNull();

    }
}
