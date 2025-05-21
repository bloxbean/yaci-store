package com.bloxbean.cardano.yaci.store.account.storage.impl;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.config.JooqTestConfig;
import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import com.bloxbean.cardano.yaci.store.account.storage.impl.repository.AddressBalanceRepository;
import com.bloxbean.cardano.yaci.store.account.storage.impl.repository.StakeBalanceRepository;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;

import java.util.Optional;

@DataJpaTest
@Import({JooqTestConfig.class})
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/store/h2",
        "spring.flyway.out-of-order=true",
        "spring.datasource.url=jdbc:h2:mem:mydb",
        "spring.datasource.username=sa",
        "spring.datasource.password=password"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountBalanceStorageImplTest {

    @Autowired
    private AddressBalanceRepository addressBalanceRepository;

    @Autowired
    private StakeBalanceRepository stakeBalanceRepository;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private Flyway flyway;

    private StoreProperties storeProperties;
    private AccountStoreProperties accountStoreProperties;

    private AccountBalanceStorage accountBalanceStorage;

    @BeforeAll
    void migrate() {
        flyway.migrate();
    }

    @BeforeEach
    public void setUp() {
        storeProperties = new StoreProperties();
        accountStoreProperties = new AccountStoreProperties();

        accountBalanceStorage = new AccountBalanceStorageImpl(
                addressBalanceRepository,
                stakeBalanceRepository,
                dsl,
                storeProperties,
                accountStoreProperties
        );
    }

    @Test
    void testRefreshCurrentAddressBalance() {
        AddressBalance addressBalance11 = new AddressBalance();
        addressBalance11.setAddress("addr_test1vqvhvxjz4q7g5gw64jacxevgmzzlfju87ma0q4wcgv656vsvm7k08");
        addressBalance11.setUnit("lovelace");
        addressBalance11.setQuantity(new BigInteger("2000000"));
        addressBalance11.setSlot(1L);

        AddressBalance addressBalance21 = new AddressBalance();
        addressBalance21.setAddress("addr_test1vqvhvxjz4q7g5gw64jacxevgmzzlfju87ma0q4wcgv656vsvm7k08");
        addressBalance21.setUnit("a5968700149c46e87e13bd15e9a69dc5ef06d80b72dfad41523c78624d595f4e46545f31");
        addressBalance21.setQuantity(new BigInteger("1"));
        addressBalance21.setSlot(1L);

        AddressBalance addressBalance31 = new AddressBalance();
        addressBalance31.setAddress("addr_test1vqvhvxjz4q7g5gw64jacxevgmzzlfju87ma0q4wcgv656vsvm7k08");
        addressBalance31.setUnit("b6968700149c46e87e13bd15e9a69dc5ef06d80b72dfad41523c78624d595f4e46545f31");
        addressBalance31.setQuantity(new BigInteger("5"));
        addressBalance31.setSlot(1L);

        AddressBalance addressBalance12= new AddressBalance();
        addressBalance12.setAddress("addr_test1vqvhvxjz4q7g5gw64jacxevgmzzlfju87ma0q4wcgv656vsvm7k08");
        addressBalance12.setUnit("lovelace");
        addressBalance12.setQuantity(new BigInteger("3000000"));
        addressBalance12.setSlot(2L);

        AddressBalance addressBalance16= new AddressBalance();
        addressBalance16.setAddress("addr_test1vqvhvxjz4q7g5gw64jacxevgmzzlfju87ma0q4wcgv656vsvm7k08");
        addressBalance16.setUnit("lovelace");
        addressBalance16.setQuantity(new BigInteger("8000000"));
        addressBalance16.setSlot(6L);

        AddressBalance addressBalance26 = new AddressBalance();
        addressBalance26.setAddress("addr_test1vqvhvxjz4q7g5gw64jacxevgmzzlfju87ma0q4wcgv656vsvm7k08");
        addressBalance26.setUnit("b6968700149c46e87e13bd15e9a69dc5ef06d80b72dfad41523c78624d595f4e46545f31");
        addressBalance26.setQuantity(new BigInteger("56"));
        addressBalance26.setSlot(6L);

        AddressBalance addressBalance32 = new AddressBalance();
        addressBalance32.setAddress("addr_test1vqvhvxjz4q7g5gw64jacxevgmzzlfju87ma0q4wcgv656vsvm7k08");
        addressBalance32.setUnit("c8968700149c46e87e13bd15e9a69dc5ef06d80b72dfad41523c78624c595f4e46545f31");
        addressBalance32.setQuantity(new BigInteger("10"));
        addressBalance32.setSlot(3L);

        AddressBalance addressBalance41 = new AddressBalance();
        addressBalance41.setAddress("addr_test1vqvhvxjz4q7g5gw64jacxevgmzzlfju87ma0q4wcgv656vsvm7k08");
        addressBalance41.setUnit("d9968700149c46e87e13bd15e9a69dc5ef06d80b72dfad41523c78624d595f4e46545f31");
        addressBalance41.setQuantity(new BigInteger("20"));
        addressBalance41.setSlot(4L);

        AddressBalance newAddressBalance1 = new AddressBalance();
        newAddressBalance1.setAddress("addr_test1rpqmcdy3pklfwlptkzef77yj8sfvrlgqtdtvvhz989g3kkg6a2kwp");
        newAddressBalance1.setUnit("e0968700149c46e87e13bd15e9a69dc5ef06d80b72dfad41523c78624d595f4e46545f31");
        newAddressBalance1.setQuantity(new BigInteger("15"));
        newAddressBalance1.setSlot(3L);

        AddressBalance newAddressBalance2 = new AddressBalance();
        newAddressBalance2.setAddress("addr_test1qqpptk7uumhx3ppcwka362xycjyn23qkzd6w5y6pl3c7lg05u7sl4");
        newAddressBalance2.setUnit("f1968700149c46e87e13bd15e9a69dc5ef06d80b72dfad41523c78624d595f4e46545f31");
        newAddressBalance2.setQuantity(new BigInteger("25"));
        newAddressBalance2.setSlot(7L);

        accountBalanceStorage.saveAddressBalances(List.of(
                addressBalance11,
                addressBalance21,
                addressBalance31,
                addressBalance12,
                addressBalance16,
                addressBalance26,
                addressBalance32,
                addressBalance41,
                newAddressBalance1,
                newAddressBalance2
        ));
        accountBalanceStorage.saveCurrentAddressBalances(List.of(addressBalance21, addressBalance16, addressBalance26, addressBalance32, addressBalance41, newAddressBalance1, newAddressBalance2));

        accountBalanceStorage.refreshCurrentAddressBalance(addressBalance11.getAddress(), Set.of("lovelace", "b6968700149c46e87e13bd15e9a69dc5ef06d80b72dfad41523c78624d595f4e46545f31"), 4L);

        List<AddressBalance> addressBalances = accountBalanceStorage.getCurrentAddressBalance(addressBalance11.getAddress());
        assertEquals(5, addressBalances.size());
        assertEquals(new BigInteger("3000000"), addressBalances.stream().filter(ab -> ab.getUnit().equals("lovelace")).findFirst().get().getQuantity());
        assertEquals(new BigInteger("5"), addressBalances.stream().filter(ab -> ab.getUnit().equals("b6968700149c46e87e13bd15e9a69dc5ef06d80b72dfad41523c78624d595f4e46545f31"))
                .findFirst().get().getQuantity());
        assertEquals(new BigInteger("1"), addressBalances.stream().filter(ab -> ab.getUnit().equals("a5968700149c46e87e13bd15e9a69dc5ef06d80b72dfad41523c78624d595f4e46545f31"))
                .findFirst().get().getQuantity());
        assertEquals(new BigInteger("10"), addressBalances.stream().filter(ab -> ab.getUnit().equals("c8968700149c46e87e13bd15e9a69dc5ef06d80b72dfad41523c78624c595f4e46545f31"))
                .findFirst().get().getQuantity());
        assertEquals(new BigInteger("20"), addressBalances.stream().filter(ab -> ab.getUnit().equals("d9968700149c46e87e13bd15e9a69dc5ef06d80b72dfad41523c78624d595f4e46545f31"))
                .findFirst().get().getQuantity());
    }

    private static java.util.Map.Entry<String, String> generateRandomAddressAndUnit() {
        String randomAddress = "addr_test" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        String randomUnit = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        return java.util.Map.entry(randomAddress, randomUnit);
    }

    @Test
    void testRefreshCurrentAddressBalanceWithNewAddressOne() {
        var randomData = generateRandomAddressAndUnit();
        String newAddress = randomData.getKey();
        String newUnit = randomData.getValue();

        AddressBalance addressBalance = new AddressBalance();
        addressBalance.setAddress(newAddress);
        addressBalance.setUnit(newUnit);
        addressBalance.setQuantity(new BigInteger("1000000"));
        addressBalance.setSlot(10L);

        accountBalanceStorage.saveAddressBalances(List.of(addressBalance));
        accountBalanceStorage.saveCurrentAddressBalances(List.of(addressBalance));

        accountBalanceStorage.refreshCurrentAddressBalance(newAddress, Set.of(newUnit), 10L);

        List<AddressBalance> addressBalances = accountBalanceStorage.getCurrentAddressBalance(newAddress);
        assertEquals(1, addressBalances.size());
        assertEquals(new BigInteger("1000000"), addressBalances.stream().filter(ab -> ab.getUnit().equals(newUnit)).findFirst().get().getQuantity());
    }

    @Test
    void testRefreshCurrentAddressBalanceWithNewAddressOne_noBalanceAfterRollback() {
        var randomData = generateRandomAddressAndUnit();
        String newAddress = randomData.getKey();
        String newUnit = randomData.getValue();

        AddressBalance addressBalance = new AddressBalance();
        addressBalance.setAddress(newAddress);
        addressBalance.setUnit(newUnit);
        addressBalance.setQuantity(new BigInteger("1000000"));
        addressBalance.setSlot(10L);

        accountBalanceStorage.saveAddressBalances(List.of(addressBalance));
        accountBalanceStorage.saveCurrentAddressBalances(List.of(addressBalance));

        accountBalanceStorage.refreshCurrentAddressBalance(newAddress, Set.of(newUnit), 5L);

        List<AddressBalance> addressBalances = accountBalanceStorage.getCurrentAddressBalance(newAddress);
        assertEquals(0, addressBalances.size());
    }

    @Test
    void testRefreshCurrentAddressBalanceWithNewAddressTwo() {
        var randomData = generateRandomAddressAndUnit();
        String newAddress = randomData.getKey();
        String newUnit = randomData.getValue();

        AddressBalance addressBalance1 = new AddressBalance();
        addressBalance1.setAddress(newAddress);
        addressBalance1.setUnit(newUnit);
        addressBalance1.setQuantity(new BigInteger("2000000"));
        addressBalance1.setSlot(5L);

        AddressBalance addressBalance2 = new AddressBalance();
        addressBalance2.setAddress(newAddress);
        addressBalance2.setUnit(newUnit);
        addressBalance2.setQuantity(new BigInteger("3000000"));
        addressBalance2.setSlot(8L);

        accountBalanceStorage.saveAddressBalances(List.of(addressBalance1, addressBalance2));
        accountBalanceStorage.saveCurrentAddressBalances(List.of(addressBalance2));

        accountBalanceStorage.refreshCurrentAddressBalance(newAddress, Set.of(newUnit), 7L);

        List<AddressBalance> addressBalances = accountBalanceStorage.getCurrentAddressBalance(newAddress);
        assertEquals(1, addressBalances.size());
        assertEquals(new BigInteger("2000000"), addressBalances.stream().filter(ab -> ab.getUnit().equals(newUnit)).findFirst().get().getQuantity());
    }


    @Test
    void testRefreshCurrentStakeAddressBalanceWithStakeAddresses() {
        String stakeAddress1 = "stake_test1uqfu602nvxp7j5wkr2ngeq3pzusengw6anv9el6z4hqweuc4pvuq5";
        String stakeAddress2 = "stake_test1zp6cmg4vl8qkv6nvmcxyd7nvlj8q2htyj6nzemxadfqj8vspy8cck";

        StakeAddressBalance stakeBalance1 = new StakeAddressBalance();
        stakeBalance1.setAddress(stakeAddress1);
        stakeBalance1.setQuantity(new BigInteger("1000000"));
        stakeBalance1.setSlot(3L);

        StakeAddressBalance stakeBalance2 = new StakeAddressBalance();
        stakeBalance2.setAddress(stakeAddress2);
        stakeBalance2.setQuantity(new BigInteger("1500000"));
        stakeBalance2.setSlot(5L);

        accountBalanceStorage.saveStakeAddressBalances(List.of(stakeBalance1, stakeBalance2));
        accountBalanceStorage.saveCurrentStakeAddressBalances(List.of(stakeBalance1, stakeBalance2));

        accountBalanceStorage.refreshCurrentStakeAddressBalance(List.of(stakeAddress1, stakeAddress2), 4L);

        Optional<StakeAddressBalance> updatedBalance1 = accountBalanceStorage.getCurrentStakeAddressBalance(stakeAddress1);
        assertTrue(updatedBalance1.isPresent());
        assertEquals(new BigInteger("1000000"), updatedBalance1.get().getQuantity());

        Optional<StakeAddressBalance> updatedBalance2 = accountBalanceStorage.getCurrentStakeAddressBalance(stakeAddress2);
        assertFalse(updatedBalance2.isPresent());
    }

    @Test
    void testRefreshCurrentStakeAddressBalanceWithRollback() {
        String stakeAddress = "stake_test1zq5hdjkk9e6jxk6nlc4d57vnzej60eyzegn3fy0qcq7nm8slyp7qj";

        StakeAddressBalance stakeBalance = new StakeAddressBalance();
        stakeBalance.setAddress(stakeAddress);
        stakeBalance.setQuantity(new BigInteger("2000000"));
        stakeBalance.setSlot(6L);

        accountBalanceStorage.saveStakeAddressBalances(List.of(stakeBalance));
        accountBalanceStorage.saveCurrentStakeAddressBalances(List.of(stakeBalance));

        accountBalanceStorage.refreshCurrentStakeAddressBalance(List.of(stakeAddress), 3L);

        Optional<StakeAddressBalance> updatedBalance = accountBalanceStorage.getCurrentStakeAddressBalance(stakeAddress);
        assertTrue(updatedBalance.isEmpty());
    }

    @Test
    void testRefreshCurrentStakeAddressBalanceWithMultipleEntries() {
        String stakeAddress = "stake_test1uqfu602nvxp7j5wkr2ngeq3pzusengw6anv9el6z4hqweuc4pvuq5";
        String stakeAddress2 = "stake_test1zp6cmg4vl8qkv6nvmcxyd7nvlj8q2htyj6nzemxadfqj8vspy8cck";

        StakeAddressBalance stakeBalance1 = new StakeAddressBalance();
        stakeBalance1.setAddress(stakeAddress);
        stakeBalance1.setQuantity(new BigInteger("1000000"));
        stakeBalance1.setSlot(4L);

        StakeAddressBalance stakeBalance2 = new StakeAddressBalance();
        stakeBalance2.setAddress(stakeAddress2);
        stakeBalance2.setQuantity(new BigInteger("1500000"));
        stakeBalance2.setSlot(5L);

        StakeAddressBalance stakeBalance3 = new StakeAddressBalance();
        stakeBalance3.setAddress(stakeAddress);
        stakeBalance3.setQuantity(new BigInteger("3000000"));
        stakeBalance3.setSlot(6L);

        accountBalanceStorage.saveStakeAddressBalances(List.of(stakeBalance1, stakeBalance2, stakeBalance3));
        accountBalanceStorage.saveCurrentStakeAddressBalances(List.of(stakeBalance2, stakeBalance3));

        accountBalanceStorage.refreshCurrentStakeAddressBalance(List.of(stakeAddress, stakeAddress2), 5L);

        Optional<StakeAddressBalance> updatedBalance = accountBalanceStorage.getCurrentStakeAddressBalance(stakeAddress);
        assertTrue(updatedBalance.isPresent());
        assertEquals(new BigInteger("1000000"), updatedBalance.get().getQuantity());
    }
}