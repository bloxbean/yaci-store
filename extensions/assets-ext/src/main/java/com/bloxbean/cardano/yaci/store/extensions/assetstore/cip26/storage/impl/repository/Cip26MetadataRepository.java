package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.Cip26Metadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Cip26MetadataRepository extends JpaRepository<Cip26Metadata, String> {

    @Query("select t.subject from Cip26Metadata t")
    List<String> findAllSubjects();

}
