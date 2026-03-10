package com.bloxbean.cardano.yaci.store.blockfrost.pools.service;

import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.blockfrost.pools.dto.*;
import com.bloxbean.cardano.yaci.store.blockfrost.pools.mapper.BFPoolsMapper;
import com.bloxbean.cardano.yaci.store.blockfrost.pools.storage.BFPoolsStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.pools.storage.impl.model.BFPoolMetadata;
import com.bloxbean.cardano.yaci.store.blockfrost.pools.util.BFPoolIdUtil;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BFPoolsService {

    private final BFPoolsStorageReader storageReader;
    private final StoreProperties storeProperties;
    private final BFPoolsMapper mapper = BFPoolsMapper.INSTANCE;

    // Header bytes for reward (stake) addresses: 0xe0 = testnet key hash, 0xe1 = mainnet key hash
    private static final byte REWARD_KEY_TESTNET = (byte) 0xe0;
    private static final byte REWARD_KEY_MAINNET = (byte) 0xe1;

    public List<String> getPools(int page, int count, String order) {
        int p = page - 1;
        return storageReader.getPoolIds(p, count, BFPoolIdUtil.normalizeOrder(order));
    }

    public List<BFPoolRetireItemDto> getRetiredPools(int page, int count, String order) {
        int p = page - 1;
        return storageReader.getRetiredPools(p, count, BFPoolIdUtil.normalizeOrder(order))
                .stream().map(mapper::toBFPoolRetireItemDto).toList();
    }

    public List<BFPoolRetireItemDto> getRetiringPools(int page, int count, String order) {
        int p = page - 1;
        return storageReader.getRetiringPools(p, count, BFPoolIdUtil.normalizeOrder(order))
                .stream().map(mapper::toBFPoolRetireItemDto).toList();
    }

    public BFPoolDto getPool(String poolId) {
        String poolIdHex = normalizePoolHash(poolId);
        BFPoolDto dto = storageReader.getPoolDetail(poolIdHex)
                .map(mapper::toBFPoolDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pool not found: " + poolId));
        // Convert raw stake key hashes in owners list to bech32 stake addresses
        if (dto.getOwners() != null) {
            dto.setOwners(dto.getOwners().stream()
                    .map(this::stakeKeyHashToBech32)
                    .toList());
        }
        return dto;
    }

    public List<BFPoolUpdateDto> getPoolUpdates(String poolId, int page, int count, String order) {
        String poolIdHex = normalizePoolHash(poolId);
        int p = page - 1;
        return storageReader.getPoolUpdates(poolIdHex, p, count, BFPoolIdUtil.normalizeOrder(order))
                .stream().map(mapper::toBFPoolUpdateDto).toList();
    }

    public Object getPoolMetadata(String poolId) {
        String poolIdHex = normalizePoolHash(poolId);
        Optional<BFPoolMetadata> metadata = storageReader.getPoolMetadata(poolIdHex);
        if (metadata.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pool not found: " + poolId);
        }
        BFPoolMetadata raw = metadata.get();
        if (raw.metadataUrl() == null && raw.metadataHash() == null) {
            return Map.of();
        }
        return mapper.toBFPoolMetadataDto(raw);
    }

    public List<BFPoolRelayDto> getPoolRelays(String poolId) {
        String poolIdHex = normalizePoolHash(poolId);
        if (storageReader.getVrfKeyByPoolId(poolIdHex).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pool not found: " + poolId);
        }
        return storageReader.getPoolRelays(poolIdHex);
    }

    public List<String> getPoolBlocks(String poolId, int page, int count, String order) {
        String poolIdHex = normalizePoolHash(poolId);
        int p = page - 1;
        // Use getVrfKeyByPoolId only to verify pool exists (404 guard)
        if (storageReader.getVrfKeyByPoolId(poolIdHex).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pool not found: " + poolId);
        }
        // block.slot_leader = pool_id hex (not vrf_key) — pass poolIdHex directly
        return storageReader.getPoolBlockHashes(poolIdHex, p, count, BFPoolIdUtil.normalizeOrder(order));
    }

    public List<BFPoolVoteDto> getPoolVotes(String poolId, int page, int count, String order) {
        String poolIdHex = normalizePoolHash(poolId);
        int p = page - 1;
        return storageReader.getPoolVotes(poolIdHex, p, count, BFPoolIdUtil.normalizeOrder(order))
                .stream().map(mapper::toBFPoolVoteDto).toList();
    }

    public List<BFPoolListItemDto> getExtendedPools(int page, int count, String order) {
        int p = page - 1;
        return storageReader.getExtendedPools(p, count, BFPoolIdUtil.normalizeOrder(order))
                .stream().map(mapper::toBFPoolListItemDto).toList();
    }

    /**
     * Convert a 56-char hex stake key hash to a bech32 stake address.
     * Prepends the single-byte network header (0xe1 mainnet / 0xe0 testnet).
     */
    private String stakeKeyHashToBech32(String stakeKeyHashHex) {
        if (stakeKeyHashHex == null) return null;
        try {
            byte[] keyHashBytes = HexUtil.decodeHexString(stakeKeyHashHex);
            byte header = storeProperties.isMainnet() ? REWARD_KEY_MAINNET : REWARD_KEY_TESTNET;
            byte[] addressBytes = new byte[1 + keyHashBytes.length];
            addressBytes[0] = header;
            System.arraycopy(keyHashBytes, 0, addressBytes, 1, keyHashBytes.length);
            String hrp = storeProperties.isMainnet() ? "stake" : "stake_test";
            return Bech32.encode(addressBytes, hrp);
        } catch (Exception e) {
            log.warn("Failed to convert stake key hash to bech32: {}", stakeKeyHashHex, e);
            return stakeKeyHashHex;
        }
    }

    private String normalizePoolHash(String poolId) {
        if (poolId == null || poolId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or malformed pool ID.");
        }
        try {
            return BFPoolIdUtil.toHex(poolId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or malformed pool ID.");
        }
    }
}
