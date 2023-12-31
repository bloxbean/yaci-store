package com.bloxbean.cardano.yaci.store.aggregation.storage;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.util.AddressUtil;
import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import com.bloxbean.cardano.yaci.store.rocksdb.KeyReference;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.rocks.types.collection.RocksMultiMap;
import com.bloxbean.rocks.types.collection.RocksMultiSet;
import com.bloxbean.rocks.types.collection.RocksMultiZSet;
import com.bloxbean.rocks.types.collection.RocksZSet;
import com.bloxbean.rocks.types.common.Tuple;
import com.bloxbean.rocks.types.config.RocksDBConfig;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.rocksdb.WriteBatch;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class RocksDBAccountBalanceStorageImpl implements AccountBalanceStorage {
    private static final String ACCOUNT_BAL_COL_FAMILY = "account_balances";
    private static final String STAKE_BAL_COL_FAMILy = "stake_account_balances";

    private RocksDBConfig rocksDBConfig;

    private RocksMultiMap<byte[], AddressBalance> accountBalanceMap;
    private RocksMultiMap<byte[], StakeAddressBalance> stakeBalanceMap;

    private RocksMultiZSet<byte[]> accountBalanceZSet;
    private RocksMultiZSet<byte[]> stakeBalanceZSet;

    private RocksMultiSet<String> accountAssetsSet;

    private RocksZSet<byte[]> slotAccountAssetSet;
    private RocksZSet<byte[]> slotStakeAccountSet;

    @Value("${store.aggr.embedded-utxo-storage.write-batch-size:1000}")
    private int batchSize;

    public RocksDBAccountBalanceStorageImpl(RocksDBConfig rocksDBConfig) {
        this.rocksDBConfig = rocksDBConfig;

        this.accountBalanceMap = new RocksMultiMap<>(rocksDBConfig, ACCOUNT_BAL_COL_FAMILY, "account_bal_map", byte[].class, AddressBalance.class);
        this.accountBalanceZSet = new RocksMultiZSet<>(rocksDBConfig, ACCOUNT_BAL_COL_FAMILY, "account_bal", byte[].class);

        this.stakeBalanceMap = new RocksMultiMap<>(rocksDBConfig, STAKE_BAL_COL_FAMILy, "stake_bal_map", byte[].class, StakeAddressBalance.class);
        this.stakeBalanceZSet = new RocksMultiZSet<>(rocksDBConfig, STAKE_BAL_COL_FAMILy, "stake_bal", byte[].class);

        this.accountAssetsSet = new RocksMultiSet<>(rocksDBConfig, ACCOUNT_BAL_COL_FAMILY, "account_assets", String.class);

        this.slotAccountAssetSet = new RocksZSet<>(rocksDBConfig, ACCOUNT_BAL_COL_FAMILY, "slot_account_asset", byte[].class);
        this.slotStakeAccountSet = new RocksZSet<>(rocksDBConfig, STAKE_BAL_COL_FAMILy, "slot_stake_account", byte[].class);
    }

    @SneakyThrows
    @Override
    public Optional<AddressBalance> getAddressBalance(String address, String unit, long slot) {
        byte[] ns = getAddressAssetKey(address, unit);
        try (var iterator = accountBalanceZSet.membersInRangeReverseIterator(ns, slot, 0)) {
            var tuple = iterator.prev();
            return accountBalanceMap.get(ns, tuple._1);
        } catch (NoSuchElementException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<AddressBalance> getAddressBalanceByTime(String address, String unit, long time) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<AddressBalance> getAddressBalance(@NonNull String address) {
        byte[] addressNS = getAddressBytes(address);
        return accountAssetsSet.members(addressNS)
                .stream()
                .map(asset -> getAddressBalance(address, asset, Long.MAX_VALUE))
                .map(Optional::get)
                .toList();
    }

    @SneakyThrows
    @Override
    public void saveAddressBalances(@NonNull List<AddressBalance> addressBalances) {
        if (addressBalances.size() == 0)
            return;

        WriteBatch writeBatch = new WriteBatch();
        addressBalances
                .forEach(ab -> {
                    byte[] ns = getAddressAssetKey(ab.getAddress(), ab.getUnit());
                    long slot = ab.getSlot();
                    if (slot == -1)
                        slot = 0;

                    byte[] hash = getAddressBalanceHash(ab);
                    accountBalanceZSet.addBatch(ns, writeBatch, new Tuple<>(hash, slot));
                    accountBalanceMap.putBatch(ns, writeBatch, new Tuple<>(hash, ab));
                    byte[] addressNS = getAddressBytes(ab.getAddress());
                    accountAssetsSet.addBatch(addressNS, writeBatch, ab.getUnit());

                    //For slot index
                    slotAccountAssetSet.addBatch(writeBatch, new Tuple<>(serializeAccountAssetKeyForSlotSet(ab.getAddress(), ab.getUnit(), hash), slot));
                });

        rocksDBConfig.getRocksDB().write(new org.rocksdb.WriteOptions(), writeBatch);
    }

    @Override
    public int deleteAddressBalanceBeforeSlotExceptTop(String address, String unit, long slot) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @SneakyThrows
    @Override
    public int deleteAddressBalanceBySlotGreaterThan(Long slot) {
        WriteBatch writeBatch = new WriteBatch();
        int count = 0;
        try (var iterator = slotAccountAssetSet.membersInRangeIterable(slot + 1, Long.MAX_VALUE)) {
            while (iterator.hasNext()) {
                var tuple = iterator.next();
                KeyReference keyRef = deserializeAccountAssetKeyForSlot(tuple._1);
                if (keyRef != null) {
                    byte[] addressAssetNS = keyRef.getNs();
                    byte[] hash = keyRef.getKey();

                    accountBalanceZSet.removeBatch(addressAssetNS, writeBatch, hash);
                    accountBalanceMap.removeBatch(addressAssetNS, writeBatch, hash);
                    count++;
                }
            }
        }
        rocksDBConfig.getRocksDB().write(new org.rocksdb.WriteOptions(), writeBatch);
        return count;
    }

    @Override
    public int deleteAddressBalanceByBlockGreaterThan(Long block) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @SneakyThrows
    @Override
    public Optional<StakeAddressBalance> getStakeAddressBalance(String address, long slot) {
        byte[] ns = new Address(address).getBytes();
        try (var iterator = stakeBalanceZSet.membersInRangeReverseIterator(ns, slot, 0)) {
            var tuple = iterator.prev();
            return stakeBalanceMap.get(ns, tuple._1);
        } catch (NoSuchElementException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<StakeAddressBalance> getStakeAddressBalanceByTime(String address, long time) {
        return Optional.empty();
    }

    @Override
    public Optional<StakeAddressBalance> getStakeAddressBalance(String address) {
        return getStakeAddressBalance(address, Long.MAX_VALUE);
    }

    @SneakyThrows
    @Override
    public void saveStakeAddressBalances(List<StakeAddressBalance> stakeBalances) {
        if (stakeBalances.size() == 0)
            return;

        WriteBatch writeBatch = new WriteBatch();
        stakeBalances
                .forEach(stakeBalance -> {
                    byte[] ns = new Address(stakeBalance.getAddress()).getBytes();

                    long slot = stakeBalance.getSlot();
                    if (slot == -1)
                        slot = 0;

                    byte[] hash = getStakeAddressBalanceHash(stakeBalance);
                    stakeBalanceZSet.addBatch(ns, writeBatch, new Tuple<>(hash, slot));
                    stakeBalanceMap.putBatch(ns, writeBatch, new Tuple<>(hash, stakeBalance));

                    //For slot index
                    slotStakeAccountSet.addBatch(writeBatch,
                            new Tuple<>(serailizeStakeAccountKeyForSlot(stakeBalance.getAddress(), hash), slot));
                });

        rocksDBConfig.getRocksDB().write(new org.rocksdb.WriteOptions(), writeBatch);
    }

    @Override
    public int deleteStakeBalanceBeforeSlotExceptTop(String address, long slot) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @SneakyThrows
    @Override
    public int deleteStakeAddressBalanceBySlotGreaterThan(Long slot) {
        WriteBatch writeBatch = new WriteBatch();
        int count = 0;
        try (var iterator = slotStakeAccountSet.membersInRangeIterable(slot + 1, Long.MAX_VALUE)) {
            while (iterator.hasNext()) {
                var tuple = iterator.next();
                KeyReference keyRef = deserializeStakeAccountKeyForSlot(tuple._1);
                if (keyRef != null) {
                    byte[] stakeAddressNS = keyRef.getNs();
                    byte[] hash = keyRef.getKey();

                    stakeBalanceZSet.removeBatch(stakeAddressNS, writeBatch, hash);
                    stakeBalanceMap.removeBatch(stakeAddressNS, writeBatch, hash);
                    count++;
                }
            }
        }
        rocksDBConfig.getRocksDB().write(new org.rocksdb.WriteOptions(), writeBatch);
        return count;
    }

    @Override
    public int deleteStakeBalanceByBlockGreaterThan(Long block) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<AddressBalance> getAddressesByAsset(String unit, int page, int count, Order sort) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Long getBalanceCalculationBlock() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private byte[] getAddressAssetKey(String address, String unit) {
        return (address + "_" + unit).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] getAddressBalanceHash(AddressBalance addressBalance) {
        return Blake2bUtil.blake2bHash224((addressBalance.getAddress() + "_" + addressBalance.getUnit() + "_" + addressBalance.getSlot()).getBytes());
    }

    private byte[] getStakeAddressBalanceHash(StakeAddressBalance addressBalance) {
        return Blake2bUtil.blake2bHash224((addressBalance.getAddress() + "_" + addressBalance.getSlot()).getBytes());
    }

    private byte[] getAddressBytes(String address) {
        try {
            return AddressUtil.addressToBytes(address);
        } catch (Exception e) {
            return address.getBytes(StandardCharsets.UTF_8);
        }
    }

    //For slot index
    private byte[] serializeAccountAssetKeyForSlotSet(String address, String unit, byte[] hash) {
        var keyRef = new KeyReference(getAddressAssetKey(address, unit), hash);
        return keyRef.serialize();
    }

    private KeyReference deserializeAccountAssetKeyForSlot(byte[] slotAssetKey) {
        return KeyReference.deserialize(slotAssetKey);
    }

    private byte[] serailizeStakeAccountKeyForSlot(String stakeAddress, byte[] hash) {
        var keyRef = new KeyReference(new Address(stakeAddress).getBytes(), hash);
        return keyRef.serialize();
    }

    private KeyReference deserializeStakeAccountKeyForSlot(byte[] slotStakeKey) {
        return KeyReference.deserialize(slotStakeKey);
    }
}
