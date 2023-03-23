package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa;

import com.bloxbean.carano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.carano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.mapper.UtxoMapper;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.AddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.UtxoId;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository.UtxoRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.api.UtxoStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UtxoStorageImpl implements UtxoStorage {
    private final UtxoRepository utxoRepository;
    private final UtxoMapper mapper;

    @Override
    public Optional<AddressUtxo> findById(String txHash, int outputIndex) {
        return utxoRepository.findById(new UtxoId(txHash, outputIndex))
                .map(entity -> mapper.toAddressUtxo(entity));
    }

    @Override
    public Optional<List<AddressUtxo>> findAddressUtxoByOwnerAddrAndSpent(String ownerAddress, Boolean spent, Pageable page) {
        List<AddressUtxo> addressUtxoList = utxoRepository.findAddressUtxoByOwnerAddrAndSpent(ownerAddress, spent, page)
                .stream()
                .flatMap(addressUtxoEntities -> addressUtxoEntities.stream().map(mapper::toAddressUtxo))
                .toList();

        return Optional.of(addressUtxoList);
    }

    @Override
    public List<AddressUtxo> findBySlot(Long slot) {
        return utxoRepository.findBySlot(slot)
                .stream().map(mapper::toAddressUtxo)
                .toList();
    }

    @Override
    public List<AddressUtxo> findAllByIds(List<UtxoKey> utxoKeys) {
        List<UtxoId> utxoIds = utxoKeys.stream()
                .map(utxoKey -> new UtxoId(utxoKey.getTxHash(), utxoKey.getOutputIndex()))
                .toList();

        return utxoRepository.findAllById(utxoIds)
                .stream().map(mapper::toAddressUtxo)
                .toList();
    }

    @Override
    public int deleteBySlotGreaterThan(Long slot) {
        return utxoRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public Optional<AddressUtxo> save(AddressUtxo addressUtxo) {
        AddressUtxoEntity addressUtxoEntity = utxoRepository.save(mapper.toAddressUtxoEntity(addressUtxo));
        return Optional.of(mapper.toAddressUtxo(addressUtxoEntity));
    }

    @Override
    public Optional<List<AddressUtxo>> saveAll(List<AddressUtxo> addressUtxoList) {
        List<AddressUtxoEntity> addressUtxoEntities = addressUtxoList.stream()
                .map(addressUtxo -> mapper.toAddressUtxoEntity(addressUtxo))
                .toList();
        addressUtxoEntities = utxoRepository.saveAll(addressUtxoEntities);
        return Optional.of(addressUtxoEntities.stream()
                .map(entity -> mapper.toAddressUtxo(entity))
                .toList());
    }
}
