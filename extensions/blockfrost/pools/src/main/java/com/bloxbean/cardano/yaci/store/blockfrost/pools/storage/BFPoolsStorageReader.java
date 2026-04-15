package com.bloxbean.cardano.yaci.store.blockfrost.pools.storage;

import com.bloxbean.cardano.yaci.store.blockfrost.pools.dto.BFPoolDelegatorDto;
import com.bloxbean.cardano.yaci.store.blockfrost.pools.dto.BFPoolHistoryDto;
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

    Optional<String> getLatestPoolStatus(String poolIdHex);

    List<String> getPoolBlockHashes(String vrfKeyHash, int page, int count, String order);

    List<BFPoolVote> getPoolVotes(String poolIdHex, int page, int count, String order);

    List<BFPoolRegistrationInfo> getExtendedPools(int page, int count, String order);

    /**
     * Returns per-epoch history from the block table only (adapot unavailable path).
     * active_stake, active_size, delegators_count, rewards will be null in every returned dto.
     * page is 0-based.
     */
    List<BFPoolHistoryDto> getPoolHistoryBase(String poolIdHex, int page, int count, String order);

    /**
     * Returns full per-epoch history joining epoch_stake + reward + block (adapot available path).
     * All fields will be populated.
     * page is 0-based.
     */
    List<BFPoolHistoryDto> getPoolHistoryFull(String poolIdHex, int page, int count, String order);

    /**
     * Returns current delegators from the delegation table (adapot unavailable path).
     * liveStake will be null in every returned dto.
     * page is 0-based.
     */
    List<BFPoolDelegatorDto> getPoolDelegatorsBase(String poolIdHex, int page, int count, String order);

    /**
     * Returns current delegators with live stake from epoch_stake (adapot available path).
     * page is 0-based.
     */
    List<BFPoolDelegatorDto> getPoolDelegatorsFull(String poolIdHex, int page, int count, String order);

    /**
     * Returns aggregated stake info for a single pool from the latest epoch_stake snapshot.
     * Includes live_stake, live_delegators, active_stake (latest-2 epoch), and saturation params.
     * Returns Optional.empty() if no epoch_stake data exists for this pool.
     */
    Optional<BFPoolStakeInfo> getPoolStakeInfo(String poolIdHex);

    /**
     * Returns aggregated stake info for multiple pools in a single query (batch lookup for extended list).
     * Returns a map of poolIdHex -> BFPoolStakeInfo.
     */
    java.util.Map<String, BFPoolStakeInfo> getPoolsStakeInfoBatch(List<String> poolIdHexes);
}
