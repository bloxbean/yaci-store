package com.bloxbean.cardano.yaci.store.epochnonce.service;

import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.Vrf;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.epochnonce.domain.EpochNonce;
import com.bloxbean.cardano.yaci.store.epochnonce.storage.EpochNonceStorage;
import com.bloxbean.cardano.yaci.store.epochnonce.util.EpochNonceConfig;
import com.bloxbean.cardano.yaci.store.epochnonce.util.NonceUtil;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.core.storage.impl.EraMapper;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EpochNonceService {
    // Praos starts at era >= Babbage (6).
    // Shelley(2), Allegra(3), Mary(4), Alonzo(5) use TPraos.

    private final BlockStorageReader blockStorageReader;
    private final EpochNonceStorage epochNonceStorage;
    private final EpochNonceConfig epochNonceConfig;
    private final StoreProperties storeProperties;
    private final ResourceLoader resourceLoader;

    public void computeEpochNonce(int newEpoch, Integer completedEpoch, EventMetadata metadata) {
        // For the very first Shelley epoch (completedEpoch is null or 0 for custom networks),
        // initialize from genesis
        EpochNonce prevState;
        if (completedEpoch == null) {
            prevState = initializeGenesisNonce();
        } else {
            prevState = epochNonceStorage.findByEpoch(completedEpoch)
                    .orElseGet(this::initializeGenesisNonce);
        }

        // Restore nonce state from previous record.
        // evolving/candidate are continuous across epochs — they are NEVER reset.
        byte[] evolvingNonce = decode(prevState.getEvolvingNonce());
        byte[] candidateNonce = decode(prevState.getCandidateNonce());
        byte[] labNonce = decode(prevState.getLabNonce());
        // ticknPrevHashNonce: the labNonce from the PREVIOUS epoch boundary,
        // used in the TICKN formula for the CURRENT epoch tick.
        byte[] ticknPrevHashNonce = decode(prevState.getLastEpochBlockNonce());

        // Query all blocks of completed epoch, sorted by slot
        if (completedEpoch != null) {
            List<Block> blocks = blockStorageReader.findBlocksByEpoch(completedEpoch);
            blocks.sort(Comparator.comparingLong(Block::getSlot));

            // Determine epoch era from first block to select the correct stability window
            Era epochEra = (!blocks.isEmpty() && blocks.get(0).getEra() != null)
                    ? EraMapper.intToEra(blocks.get(0).getEra())
                    : null;

            long stabilityWindow = epochNonceConfig.getStabilityWindow(epochEra);
            long epochLength = epochNonceConfig.getEpochLength();

            // Compute first slot of next epoch once (same for all blocks in this epoch)
            long firstSlotNextEpoch = !blocks.isEmpty()
                    ? (blocks.get(0).getSlot() - blocks.get(0).getEpochSlot()) + epochLength
                    : 0;

            log.debug("Epoch {} era={}, stabilityWindow={}", completedEpoch, epochEra, stabilityWindow);

            // Process each block — evolving/candidate update continuously
            for (Block block : blocks) {
                byte[] vrfOutput = getVrfOutputBytes(block);
                if (vrfOutput == null) {
                    continue;
                }

                // Era-aware VRF nonce derivation
                byte[] eta;
                if (block.getEra() != null && block.getEra() >= Era.Babbage.getValue()) {
                    eta = NonceUtil.vrfNonceValue(vrfOutput);        // Praos: Blake2b(Blake2b("N" || vrf))
                } else {
                    eta = NonceUtil.vrfNonceValueTPraos(vrfOutput);   // TPraos: Blake2b(vrf)
                }

                evolvingNonce = NonceUtil.combineNonces(evolvingNonce, eta);

                if (block.getSlot() + stabilityWindow < firstSlotNextEpoch) {
                    candidateNonce = evolvingNonce;
                }

                labNonce = NonceUtil.prevHashToNonce(block.getPrevHash());
            }
        }

        // TICKN: epochNonce = candidateNonce ⭒ ticknPrevHashNonce
        byte[] newEpochNonce = NonceUtil.combineNonces(candidateNonce, ticknPrevHashNonce);
        // Update ticknPrevHashNonce for the next epoch tick
        byte[] newTicknPrevHashNonce = labNonce;

        // Persist
        EpochNonce result = EpochNonce.builder()
                .epoch(newEpoch)
                .nonce(encode(newEpochNonce))
                .evolvingNonce(encode(evolvingNonce))
                .candidateNonce(encode(candidateNonce))
                .labNonce(encode(labNonce))
                .lastEpochBlockNonce(encode(newTicknPrevHashNonce))
                .slot(metadata.getSlot())
                .block(metadata.getBlock())
                .blockTime(metadata.getBlockTime())
                .build();

        epochNonceStorage.save(result);
        log.info("Epoch nonce computed for epoch {}: {}", newEpoch, result.getNonce());
    }

    public int rollback(long rollbackSlot) {
        int count = epochNonceStorage.deleteBySlotGreaterThan(rollbackSlot);
        log.info("Rollback -- {} epoch_nonce records deleted for slot > {}", count, rollbackSlot);
        return count;
    }

    private byte[] getVrfOutputBytes(Block block) {
        // Babbage+: vrfResult; Pre-Babbage: nonceVrf
        Vrf vrf = block.getVrfResult();
        if (vrf == null) {
            vrf = block.getNonceVrf();
        }
        if (vrf == null || vrf.getOutput() == null) {
            log.warn("Block at slot {} has no VRF output", block.getSlot());
            return null;
        }
        return HexUtil.decodeHexString(vrf.getOutput());
    }

    private EpochNonce initializeGenesisNonce() {
        byte[] genesisHash = computeShelleyGenesisHash();
        String genesisHashHex = HexUtil.encodeHexString(genesisHash);

        log.info("Initializing epoch nonce from genesis hash: {}", genesisHashHex);

        return EpochNonce.builder()
                .epoch(0)
                .nonce(genesisHashHex)
                .evolvingNonce(genesisHashHex)
                .candidateNonce(genesisHashHex)
                .labNonce(null)
                .lastEpochBlockNonce(null)
                .build();
    }

    private byte[] computeShelleyGenesisHash() {
        String shelleyGenesisFile = storeProperties.getShelleyGenesisFile();

        try {
            byte[] fileBytes;
            if (!StringUtil.isEmpty(shelleyGenesisFile)) {
                if (shelleyGenesisFile.startsWith("classpath:")) {
                    try (InputStream is = resourceLoader.getResource(shelleyGenesisFile).getInputStream()) {
                        fileBytes = is.readAllBytes();
                    }
                } else {
                    try (InputStream is = new FileInputStream(new File(shelleyGenesisFile))) {
                        fileBytes = is.readAllBytes();
                    }
                }
            } else {
                // Load from default classpath resources based on protocol magic
                String networkFolder = getNetworkFolder();
                String resourcePath = "store/networks/" + networkFolder + "/shelley-genesis.json";
                try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                    if (is == null) {
                        throw new IllegalStateException("Shelley genesis file not found at: " + resourcePath);
                    }
                    fileBytes = is.readAllBytes();
                }
            }

            return Blake2bUtil.blake2bHash256(fileBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute shelley genesis hash", e);
        }
    }

    private String getNetworkFolder() {
        long protocolMagic = storeProperties.getProtocolMagic();
        if (protocolMagic == 764824073) return "mainnet";
        if (protocolMagic == 1) return "preprod";
        if (protocolMagic == 2) return "preview";
        throw new IllegalStateException("Unknown network for protocol magic: " + protocolMagic
                + ". Please configure store.cardano.shelley-genesis-file");
    }

    private static byte[] decode(String hex) {
        if (hex == null || hex.isEmpty()) return null;
        return HexUtil.decodeHexString(hex);
    }

    private static String encode(byte[] bytes) {
        if (bytes == null) return null;
        return HexUtil.encodeHexString(bytes);
    }
}
