package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.utxo.domain.AssetHolder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CustomUtxoRepositoryImpl implements CustomUtxoRepository {

    @Autowired
    private EntityManager entityManager;

    @Override
    public List<AssetHolder> findUtxosByUnit(String unit) {
        try {
            String databaseName = "PostgreSQL";

            String nativeSql;
            switch (databaseName) {
                case "PostgreSQL":
                    nativeSql = """
                        SELECT a.owner_addr, a.owner_stake_addr, sum(cast(amount_item->>'quantity' as int)) as quantity
                            FROM preprod.address_utxo a, jsonb_array_elements(a.amounts) as amount_item
                            WHERE amount_item->>'unit' = ?1
                            and a.spent is null
                            group by a.owner_addr, a.owner_stake_addr
                    """;
                    break;

//                case "MySQL":
//                    nativeSql = "..."; // MySQL-specific SQL
//                    break;
//                case "H2":
//                    nativeSql = "..."; // H2-specific SQL
//                    break;
                default:
                    throw new UnsupportedOperationException("Database not supported: " + databaseName);
            }

            Query query = entityManager.createNativeQuery(nativeSql, "AssetHolderMapping");
            query.setParameter(1, unit);
            return (List<AssetHolder>) query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute dialect-specific query", e);
        }
    }
}
