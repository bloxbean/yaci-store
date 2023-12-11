package com.bloxbean.cardano.yaci.store.rocksdb;

import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.rocksdb.common.KeyValue;
import com.bloxbean.cardano.yaci.store.rocksdb.config.RocksDBConfig;
import com.bloxbean.cardano.yaci.store.rocksdb.config.RocksDBProperties;
import org.junit.jupiter.api.*;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class RocksDBRepositoryTest {
    private static RocksDBRepository rocksDBRepository;
    private static String tmpdir = System.getProperty("java.io.tmpdir");
    private static RocksDBConfig rocksDBConfig;
    private static RocksDBProperties rocksDBProperties;

    @BeforeAll
    static void setup() {
        rocksDBProperties = new RocksDBProperties();
        rocksDBProperties.setRocksDBBaseDir(tmpdir + File.separator + "rocksdb-test");
        rocksDBProperties.setColumnFamilies("test1,test2");

        rocksDBConfig = new RocksDBConfig(rocksDBProperties);
        rocksDBConfig.initDB();

        rocksDBRepository = new RocksDBRepository(rocksDBConfig, "test1");
    }

    @AfterAll
    static void tearDown() {
        if (rocksDBConfig != null)
            rocksDBConfig.closeDB();

        File rocksDBFolder = new File(tmpdir + File.separator + "rocks-test");
        deleteDirectory(rocksDBFolder);
    }

    static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    @Test
    void saveAndFind() {
        String policy = getRandomHash();
        AddressUtxo addressUtxo = AddressUtxo.builder()
                .txHash(getRandomHash())
                .outputIndex(0)
                .blockNumber(1000L)
                .amounts(List.of(new Amt(getRandomHash(), policy, "Asset1", new BigInteger("989"))))
                .build();

        rocksDBRepository.save(getKey(addressUtxo.getTxHash(), addressUtxo.getOutputIndex()), addressUtxo);

        var savedAddressUtxo = rocksDBRepository.find(getKey(addressUtxo.getTxHash(), addressUtxo.getOutputIndex()), AddressUtxo.class).get();

        assertThat(savedAddressUtxo).isEqualTo(addressUtxo);
    }


    @Test
    void saveBatch_findMulti() {
        List<AddressUtxo> addressUtxoList = new ArrayList<>();
        IntStream.range(0, 20)
                .forEach(value -> {
                    var addressUtxo = AddressUtxo.builder()
                            .txHash(getRandomHash())
                            .outputIndex(getRandomNumber())
                            .blockNumber((long) getRandomNumber())
                            .amounts(List.of(new Amt(getRandomHash(), getRandomHash(), "Asset1", BigInteger.valueOf(getRandomNumber()))))
                            .build();
                    addressUtxoList.add(addressUtxo);
                });

        var keyValues = addressUtxoList.stream().map(addressUtxo -> new KeyValue<String, AddressUtxo>(getKey(addressUtxo.getTxHash(), addressUtxo.getOutputIndex()), addressUtxo))
                .toList();

        rocksDBRepository.saveBatch(keyValues);

        var keys = keyValues.stream().map(keyValue -> keyValue.getKey())
                .toList();

        var savedAddressUtxos = rocksDBRepository.findMulti(keys, AddressUtxo.class);

        assertThat(savedAddressUtxos).isEqualTo(addressUtxoList);
        assertThat(savedAddressUtxos).hasSize(20);
    }

    @Test
    void saveBatch_findMulti_additionalKeys() {
        List<AddressUtxo> addressUtxoList = new ArrayList<>();
        IntStream.range(0, 20)
                .forEach(value -> {
                    var addressUtxo = AddressUtxo.builder()
                            .txHash(getRandomHash())
                            .outputIndex(getRandomNumber())
                            .blockNumber((long) getRandomNumber())
                            .amounts(List.of(new Amt(getRandomHash(), getRandomHash(), "Asset1", BigInteger.valueOf(getRandomNumber()))))
                            .build();
                    addressUtxoList.add(addressUtxo);
                });

        var keyValues = addressUtxoList.stream().map(addressUtxo -> new KeyValue<String, AddressUtxo>(getKey(addressUtxo.getTxHash(), addressUtxo.getOutputIndex()), addressUtxo))
                .toList();

        rocksDBRepository.saveBatch(keyValues);

        var keys = keyValues.stream().map(keyValue -> keyValue.getKey())
                .toList();

        List<String> updatedKeys = new ArrayList<>();
        updatedKeys.addAll(keys);
        updatedKeys.add(getKey(getRandomHash(), 0));

        var savedAddressUtxos = rocksDBRepository.findMulti(updatedKeys, AddressUtxo.class);

        assertThat(savedAddressUtxos).containsAll(addressUtxoList);
        assertThat(savedAddressUtxos).hasSize(21);
        assertThat(savedAddressUtxos.get(20)).isNull();
    }

    @Test
    void delete() {
        String policy = getRandomHash();
        AddressUtxo addressUtxo = AddressUtxo.builder()
                .txHash(getRandomHash())
                .outputIndex(0)
                .blockNumber(1000L)
                .amounts(List.of(new Amt(getRandomHash(), policy, "Asset1", new BigInteger("989"))))
                .build();

        rocksDBRepository.save(getKey(addressUtxo.getTxHash(), addressUtxo.getOutputIndex()), addressUtxo);

        var savedAddressUtxo = rocksDBRepository.find(getKey(addressUtxo.getTxHash(), addressUtxo.getOutputIndex()), AddressUtxo.class).get();

        rocksDBRepository.delete(getKey(addressUtxo.getTxHash(), addressUtxo.getOutputIndex()));

        var afterDelAddressUtxo = rocksDBRepository.find(getKey(addressUtxo.getTxHash(), addressUtxo.getOutputIndex()), AddressUtxo.class);

        assertThat(savedAddressUtxo).isEqualTo(addressUtxo);
        assertThat(afterDelAddressUtxo).isEmpty();

    }

    /**
    @Test
    void iterator() {
        String address1 = "addr1q92fjctpfsaw77phwl6xa2a04dxw6x9chq359vys4n54zjjh7ysd636eqtzhcnf78s3jnra97mev4lhs83vned624dksese0s6";
        String address2 = "addr1v90rtez0ytrmpdcadu4z4a8537a0qru7yudhhfhy6v2gj8q55ktm8";
        String address3 = "addr1qygcvg02gh368mzh3n7lfz36dhzk9tmcsymf8fs8y0xx67cvpgfpnfstp608z54a5h29a4dp5t9hykraafqmv4r5krwsxpp4dk";

        String addr1PaymentCred = HexUtil.encodeHexString(new Address(address1).getPaymentCredentialHash().get());
        String addr2PaymentCred = HexUtil.encodeHexString(new Address(address2).getPaymentCredentialHash().get());
        String addr3PaymentCred = HexUtil.encodeHexString(new Address(address3).getPaymentCredentialHash().get());

        for (int i = 0; i < 10; i++) {
            String policy = getRandomHash();
            AddressUtxo addressUtxo = AddressUtxo.builder()
                    .txHash(getRandomHash())
                    .outputIndex(i)
                    .ownerAddr(address1)
                    .ownerPaymentCredential(addr1PaymentCred)
                    .blockNumber((long) getRandomNumber())
                    .amounts(List.of(new Amt(getRandomHash(), policy, "Asset1", BigInteger.valueOf(getRandomNumber()))))
                    .build();

            rocksDBRepository.save(addressUtxo.getOwnerPaymentCredential() + "#" + i, addressUtxo);
        }

        for (int i = 0; i < 5; i++) {
            String policy = getRandomHash();
            AddressUtxo addressUtxo = AddressUtxo.builder()
                    .txHash(getRandomHash())
                    .outputIndex(i)
                    .ownerAddr(address2)
                    .ownerPaymentCredential(addr2PaymentCred)
                    .blockNumber((long) getRandomNumber())
                    .amounts(List.of(new Amt(getRandomHash(), policy, "Asset1", BigInteger.valueOf(getRandomNumber()))))
                    .build();

            rocksDBRepository.save(addressUtxo.getOwnerPaymentCredential() + "#" + i, addressUtxo);
        }

        for (int i = 0; i < 20; i++) {
            String policy = getRandomHash();
            AddressUtxo addressUtxo = AddressUtxo.builder()
                    .txHash(getRandomHash())
                    .outputIndex(i)
                    .ownerAddr(address3)
                    .ownerPaymentCredential(addr3PaymentCred)
                    .blockNumber((long) getRandomNumber())
                    .amounts(List.of(new Amt(getRandomHash(), policy, "Asset1", BigInteger.valueOf(getRandomNumber()))))
                    .build();

            rocksDBRepository.save(addressUtxo.getOwnerPaymentCredential() + "#" + i, addressUtxo);
        }


        List<KeyValue> keyValues = rocksDBRepository.findElements(addr2PaymentCred, 10, AddressUtxo.class);

        System.out.println(keyValues);

        assertThat(keyValues).hasSize(3);
        assertThat(keyValues.get(0).getKey()).isEqualTo(addr2PaymentCred + "#" + "0");
        assertThat(keyValues.get(0).getKey()).isEqualTo(addr2PaymentCred + "#" + "1");
        assertThat(keyValues.get(0).getKey()).isEqualTo(addr2PaymentCred + "#" + "2");
    }
    **/

    private String getRandomHash() {
        return HexUtil.encodeHexString(Blake2bUtil.blake2bHash256(UUID.randomUUID().toString().getBytes()));
    }

    private static int getRandomNumber() {
        Random random = new Random();
        return random.nextInt();
    }

    private String getKey(String txHash, int outputIndex) {
        return txHash + "#" + outputIndex;
    }
}
