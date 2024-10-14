package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.account.storage.impl.model.StakeAddressBalanceId;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.LatestStakeAddressBalanceEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface LatestStakeAddressBalanceRepository
        extends JpaRepository<LatestStakeAddressBalanceEntity, StakeAddressBalanceId> {

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
            value = "REFRESH MATERIALIZED VIEW CONCURRENTLY stake_address_balance_view",
            nativeQuery = true)
    void refreshMaterializedView();

    @Query("SELECT sab FROM LatestStakeAddressBalanceEntity sab WHERE sab.address IN :addresses")
    List<LatestStakeAddressBalanceEntity> findAllByStakeAddressIn(
            @Param("addresses") Set<String> addresses);
}
