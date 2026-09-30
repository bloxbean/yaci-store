package com.bloxbean.cardano.yaci.store.snapshot.manifest;

/**
 * The single immutable Cardano point a snapshot restores to: the last block of the newest epoch
 * every required table fully supports.
 *
 * @param ducklakeSnapshotId the pinned DuckLake catalog snapshot the file set was resolved against
 */
public record ConsistencyPoint(
        String network,
        long protocolMagic,
        int epoch,
        long slot,
        long blockNumber,
        String blockHash,
        String prevBlockHash,
        int era,
        long blockTime,
        long ducklakeSnapshotId
) {}
