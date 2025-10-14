package com.bloxbean.cardano.yaci.store.core.storage.impl;

import com.bloxbean.cardano.yaci.store.core.storage.impl.model.ErrorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ErrorRepository extends JpaRepository<ErrorEntity, Integer> {

    @Query("select e from ErrorEntity e order by e.id desc")
    List<ErrorEntity> findAllOrderById();
}
