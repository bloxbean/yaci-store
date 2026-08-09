package com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage;

import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model.BFDatum;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model.BFScript;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model.BFScriptListItem;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model.BFScriptRedeemer;
import com.bloxbean.cardano.yaci.store.common.model.Order;

import java.util.List;
import java.util.Optional;

public interface BFScriptsStorageReader {

    List<BFScriptListItem> getScripts(int page, int count, Order order);

    Optional<BFScript> getScript(String scriptHash);

    List<BFScriptRedeemer> getScriptRedeemers(String scriptHash, int page, int count, Order order);

    /**
     * Returns the raw datum row for the given datum hash, or empty if not found.
     */
    Optional<BFDatum> getDatum(String datumHash);
}
