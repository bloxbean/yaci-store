package com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage;

import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model.BFDatum;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model.BFScript;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model.BFScriptListItem;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model.BFScriptRedeemer;

import java.util.List;
import java.util.Optional;

public interface BFScriptsStorageReader {

    /**
     * Returns a paginated list of all indexed scripts (hash only), ordered by slot.
     *
     * @param page  0-based page index
     * @param count page size
     * @param order "asc" or "desc"
     */
    List<BFScriptListItem> getScripts(int page, int count, String order);

    /**
     * Returns the full raw script row for the given hash, or empty if not found.
     */
    Optional<BFScript> getScript(String scriptHash);

    /**
     * Returns a paginated list of redeemers for the given script hash, ordered by slot.
     *
     * @param scriptHash script hash to filter by
     * @param page       0-based page index
     * @param count      page size
     * @param order      "asc" or "desc"
     */
    List<BFScriptRedeemer> getScriptRedeemers(String scriptHash, int page, int count, String order);

    /**
     * Returns the raw datum row for the given datum hash, or empty if not found.
     */
    Optional<BFDatum> getDatum(String datumHash);
}
