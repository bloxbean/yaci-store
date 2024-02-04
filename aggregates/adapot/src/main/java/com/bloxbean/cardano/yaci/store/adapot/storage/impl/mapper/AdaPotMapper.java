package com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;
import com.bloxbean.cardano.yaci.store.adapot.domain.Deposit;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.AdaPotEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.DepositEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class AdaPotMapper {
    public abstract AdaPot toAdaPot(AdaPotEntity adaPotEntity);

    public abstract AdaPotEntity toAdaPotEntity(AdaPot adaPot);

    public abstract Deposit toDeposit(DepositEntity depositEntity);

    public abstract DepositEntity toDepositEntity(Deposit deposit);
}
