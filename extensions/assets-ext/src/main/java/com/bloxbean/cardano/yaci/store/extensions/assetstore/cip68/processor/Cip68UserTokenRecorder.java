package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68UserToken;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68UserTokenRepository;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Remembers the CIP-68 fungible (333) and rich fungible (444) user tokens that appear in outputs, so that the API can serve a 333 or 444
 * subject only when the token exists (ADR 0008). It is called from the block transaction of {@link Cip68Processor}, where an exception stops
 * the whole sync (ADR 0004), so it is:
 * <ul>
 *   <li><b>bounded:</b> only a unit whose asset name starts with the 333 or 444 label and has a name after it is kept, and a unit is at most
 *       120 characters, so a row is small whatever is on chain;</li>
 *   <li><b>idempotent:</b> a token that is already recorded is left alone, so a replayed block or a rollback adds nothing twice;</li>
 *   <li><b>contained:</b> a failure of its own (reading or saving) is a warning, never thrown. A failure that only shows at flush or commit,
 *       inside the database, is not caught here (#1265).</li>
 * </ul>
 */
@Component
@Slf4j
public class Cip68UserTokenRecorder {

    @Nullable
    private final Cip68UserTokenRepository repository;

    public Cip68UserTokenRecorder(@Nullable Cip68UserTokenRepository repository) {
        this.repository = repository;
    }

    /** A recorder that records nothing, for code built without a database (tests). */
    public static Cip68UserTokenRecorder noop() {
        return new Cip68UserTokenRecorder(null);
    }

    /**
     * @param txHashByUnit the user tokens seen in a block, each with the hash of the first transaction of the block that showed it
     *                     (units in lower case hex)
     */
    public void record(Map<String, String> txHashByUnit, long slot) {
        if (repository == null || txHashByUnit.isEmpty()) {
            return;
        }
        try {
            Set<String> known = repository.findAllById(txHashByUnit.keySet()).stream()
                    .map(Cip68UserToken::getUnit).collect(Collectors.toSet());
            List<Cip68UserToken> added = new ArrayList<>();
            txHashByUnit.forEach((unit, txHash) -> {
                if (!known.contains(unit)) {
                    added.add(Cip68UserToken.builder().unit(unit).slot(slot).txHash(txHash).build());
                }
            });
            if (!added.isEmpty()) {
                repository.saveAll(added);
            }
        } catch (RuntimeException e) {
            log.warn("Could not record {} CIP-68 user token(s) of slot {}: {}: {}", txHashByUnit.size(), slot,
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
