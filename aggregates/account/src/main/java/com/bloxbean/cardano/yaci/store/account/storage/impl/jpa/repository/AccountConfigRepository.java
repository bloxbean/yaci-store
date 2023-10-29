package com.bloxbean.cardano.yaci.store.account.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.account.storage.impl.jpa.model.AccountConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountConfigRepository extends JpaRepository<AccountConfigEntity, String> {

}
