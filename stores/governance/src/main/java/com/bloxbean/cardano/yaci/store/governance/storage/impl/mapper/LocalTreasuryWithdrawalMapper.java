package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.LocalTreasuryWithdrawal;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalTreasuryWithdrawalEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class LocalTreasuryWithdrawalMapper {
    public abstract LocalTreasuryWithdrawalEntity toLocalTreasuryWithdrawalEntity(LocalTreasuryWithdrawal localTreasuryWithdrawal);

    public abstract LocalTreasuryWithdrawal toLocalTreasuryWithdrawal(LocalTreasuryWithdrawalEntity localTreasuryWithdrawalEntity);

}
