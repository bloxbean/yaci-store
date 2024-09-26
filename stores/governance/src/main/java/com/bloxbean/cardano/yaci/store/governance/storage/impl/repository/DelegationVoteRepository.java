package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DelegationVoteEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DelegationVoteId;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.projection.DelegationVoteProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface DelegationVoteRepository extends JpaRepository<DelegationVoteEntity, DelegationVoteId> {
    Slice<DelegationVoteEntity> findByDrepId(String dRepId, Pageable pageable);

    Slice<DelegationVoteEntity> findByAddress(String address, Pageable pageable);

    @Query(
            value =
                    "select dv.drepHash as drepHash,dv.address as address,dv.txHash as txHash from DelegationVoteEntity dv where dv.drepHash in :dRepHash")
    List<DelegationVoteProjection> findAllByDRepHashIn(@Param("dRepHash") Set<String> dRepHash);

    int deleteBySlotGreaterThan(long slot);
}
