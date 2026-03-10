package com.bloxbean.cardano.yaci.store.blockfrost.pools.storage;

import com.bloxbean.cardano.yaci.store.blockfrost.pools.dto.BFPoolRelayDto;
import com.bloxbean.cardano.yaci.store.blockfrost.pools.storage.impl.model.*;

import java.util.List;
import java.util.Optional;

public interface BFPoolsStorageReader {

    List<String> getPoolIds(int page, int count, String order);

    List<BFPoolRetireItem> getRetiredPools(int page, int count, String order);

    List<BFPoolRetireItem> getRetiringPools(int page, int count, String order);

    Optional<BFPoolSummary> getPoolDetail(String poolIdHex);

    List<BFPoolUpdate> getPoolUpdates(String poolIdHex, int page, int count, String order);

    Optional<BFPoolMetadata> getPoolMetadata(String poolIdHex);

    List<BFPoolRelayDto> getPoolRelays(String poolIdHex);

    Optional<String> getVrfKeyByPoolId(String poolIdHex);

    List<String> getPoolBlockHashes(String vrfKeyHash, int page, int count, String order);

    List<BFPoolVote> getPoolVotes(String poolIdHex, int page, int count, String order);

    List<BFPoolRegistrationInfo> getExtendedPools(int page, int count, String order);
}
