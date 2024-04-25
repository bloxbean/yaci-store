package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import java.util.List;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

@SpringBootTest
@SqlGroup({
        @Sql(value = "classpath:scripts/address_utxo_data.sql", executionPhase = BEFORE_TEST_CLASS)
})
class UtxoStorageReaderIT {

    @Autowired
    UtxoStorageReader utxoStorageReader;


    @Test
    void givenFindUtxosByAsset_shouldNotEmpty() {
        List<AddressUtxo> addressUtxoList = utxoStorageReader.findUtxosByAsset("lovelace", 0, 10, Order.asc);
        Assertions.assertEquals(10, addressUtxoList.size());
    }

    @Test
    void givenFindUtxoByAddressAndAsset_shouldNotEmpty() {
        List<AddressUtxo> addressUtxoList = utxoStorageReader.findUtxoByAddressAndAsset("addr_test1qpu6f9988xfc0q9z9elj5xarhlyssxxhfwjdgurrl9n38lj28jlr8xfs0llar5yvc8wm4xyvh8dejzqyp8jfak9lpp5qy4pw4q","lovelace", 0, 10, Order.asc);
        Assertions.assertEquals(3, addressUtxoList.size());
    }

    @Test
    void givenFindUtxoByPaymentCredentialAndAsset_shouldNotEmpty() {
        List<AddressUtxo> addressUtxoList = utxoStorageReader.findUtxoByPaymentCredentialAndAsset("b2167ed79c541983b90dced683a971686e965fbbe089ae592341c43a","lovelace", 0, 10, Order.asc);
        Assertions.assertEquals(1, addressUtxoList.size());
    }


    @Test
    void givenFindUtxoByStakeAddressAndAsset_shouldNotEmpty() {
        List<AddressUtxo> addressUtxoList = utxoStorageReader.findUtxoByStakeAddressAndAsset("stake_test1up9kleffexw7xuw7vhlarjzurkqxcc4y4962kxsr5zd0saskdzuhp","lovelace", 0, 10, Order.asc);
        Assertions.assertEquals(1, addressUtxoList.size());
    }
}