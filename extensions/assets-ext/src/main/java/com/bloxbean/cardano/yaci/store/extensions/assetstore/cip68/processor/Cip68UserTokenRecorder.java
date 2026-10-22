package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68UserToken;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68UserTokenRepository;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Remembers the CIP-68 fungible (333) and rich fungible (444) user tokens that appear in outputs, so that the API can serve a 333 or 444
 * subject only when the token exists (ADR 0008). It is called from the block transaction of {@link Cip68Processor}, where an exception stops
 * the whole sync (ADR 0004), so it is:
 * <ul>
 *   <li><b>bounded:</b> only a unit whose asset name starts with the 333 or 444 label and has a name after it is kept, and a unit is at most
 *       120 characters, so a row is small whatever is on chain;</li>
 *   <li><b>idempotent and safe in parallel:</b> a token already recorded is not written again, and the write itself is an upsert that the database
 *       resolves ({@link Cip68UserTokenWriter}), so two blocks processed in parallel that see the same new token do not fail with a duplicate key.
 *       The row keeps the smallest slot;</li>
 *   <li><b>best effort:</b> a failure of its own is a warning, not an exception. A failure that the database reports and that poisons the
 *       transaction (PostgreSQL) still reaches the commit, as in #1265.</li>
 * </ul>
 */
@Component
@Slf4j
public class Cip68UserTokenRecorder {

    @Nullable
    private final Cip68UserTokenRepository repository;
    @Nullable
    private final Cip68UserTokenWriter writer;

    public Cip68UserTokenRecorder(@Nullable Cip68UserTokenRepository repository, @Nullable Cip68UserTokenWriter writer) {
        this.repository = repository;
        this.writer = writer;
    }

    /** A recorder that records nothing, for code built without a database (tests). */
    public static Cip68UserTokenRecorder noop() {
        return new Cip68UserTokenRecorder(null, null);
    }

    /**
     * @param txHashByUnit the user tokens seen in a block, each with the hash of the first transaction of the block that showed it
     *                     (units in lower case hex)
     */
    public void recordSeen(Map<String, String> txHashByUnit, long slot) {
        if (repository == null || writer == null || txHashByUnit.isEmpty()) {
            return;
        }
        try {
            Map<String, Long> recordedSlot = repository.findAllById(txHashByUnit.keySet()).stream()
                    .collect(Collectors.toMap(Cip68UserToken::getUnit, Cip68UserToken::getSlot));
            // in a fixed order, so that two transactions that write the same tokens cannot wait for each other
            new TreeMap<>(txHashByUnit).forEach((unit, txHash) -> {
                // a token recorded at this slot or earlier is left alone; one recorded at a later slot (a later block committed first) is lowered
                Long recorded = recordedSlot.get(unit);
                if (recorded == null || recorded > slot) {
                    writer.upsertFirstSeen(unit, txHash, slot);
                }
            });
        } catch (RuntimeException e) {
            log.warn("Could not record {} CIP-68 user token(s) of slot {}: {}: {}", txHashByUnit.size(), slot,
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
