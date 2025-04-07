package com.bloxbean.cardano.yaci.store.core.storage.impl;

import com.bloxbean.cardano.yaci.store.core.storage.impl.model.CursorEntity;
import jakarta.transaction.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Transactional
public class CursorRepositoryImpl implements CursorRepository {

    @PersistenceContext
    EntityManager em;

    @Override
    public void save(CursorEntity cursorEntity) {
        em.merge(cursorEntity);
    }

    @Override
    public Optional<CursorEntity> findTopByIdOrderBySlotDesc(Long id) {
        return em.createQuery("""
                SELECT c FROM CursorEntity c
                WHERE c.id = :id
                ORDER BY c.slot DESC
                """, CursorEntity.class)
                .setParameter("id", id)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<CursorEntity> findTopByIdAndSlotBeforeOrderBySlotDesc(Long id, Long slot) {
        return em.createQuery("""
                SELECT c FROM CursorEntity c
                WHERE c.id = :id AND c.slot < :slot
                ORDER BY c.slot DESC
                """, CursorEntity.class)
                .setParameter("id", id)
                .setParameter("slot", slot)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<CursorEntity> findByIdAndBlockHash(Long id, String blockHash) {
        return em.createQuery("""
                SELECT c FROM CursorEntity c
                WHERE c.id = :id AND c.blockHash = :blockHash
                """, CursorEntity.class)
                .setParameter("id", id)
                .setParameter("blockHash", blockHash)
                .getResultStream()
                .findFirst();
    }

    @Override
    public long deleteByIdAndSlotGreaterThan(Long id, Long slot) {
        return em.createQuery("""
                DELETE FROM CursorEntity c
                WHERE c.id = :id AND c.slot > :slot
                """)
                .setParameter("id", id)
                .setParameter("slot", slot)
                .executeUpdate();
    }

    @Override
    public long deleteByIdAndBlockLessThan(Long id, Long block) {
        return em.createQuery("""
                DELETE FROM CursorEntity c
                WHERE c.id = :id AND c.block < :block
                """)
                .setParameter("id", id)
                .setParameter("block", block)
                .executeUpdate();
    }
}
