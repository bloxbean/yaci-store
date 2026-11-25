package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.processor;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.Cip113Configuration;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.parser.Cip113RegistryNodeParser.ParseResult;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.parser.Cip113RegistryNodeParser.ParsedRegistryNode;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.parser.Cip113RegistryNodeParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.model.Cip113RegistryNode;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.repository.Cip113RegistryNodeRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.InvalidTokenReason;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.InvalidTokenRecorder;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.InvalidTokenReport;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Stores every version of the CIP-113 registry nodes (ADR 0011).
 * <p>
 * A registry node is an output holding one NFT of a configured registry policy. It is indexed straight from the block's
 * {@link AddressUtxoEvent}, not at the end of the batch as CIP-68 is: whether a node is stored does not depend on any
 * row stored before it, and two versions of a node are ordered by their own slot and {@code tx_index}, so the blocks of
 * a parallel sync can be indexed in any order. It does not read the utxo store's tables, so it works whatever the utxo
 * store's pruning, indexes or plugin filters.
 * <p>
 * An output is stored only if it is a real node: at the registry's own script address (its payment credential is the
 * registry policy ID, one script being both the NFT's minting policy and the nodes' spending validator) and holding the
 * registry NFT of its datum's key. Anyone can send an output with any datum to that address, but only the registry can
 * mint its NFT and put it there. What fails either check goes to {@code invalid_token} and is not stored:
 * {@code registry_node_unparseable} for a datum that is not a registry node, {@code registry_node_spoofed} for a node
 * that is not where, or with the NFT, it must be.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "store.assets.ext.cip113.enabled", havingValue = "true", matchIfMissing = true)
public class Cip113Processor {

    private static final int DATUM_PREFIX_CHARS = 256;

    private final Cip113Configuration cip113Configuration;
    private final Cip113RegistryNodeParser registryNodeParser;
    private final Cip113RegistryNodeRepository registryNodeRepository;
    private final InvalidTokenRecorder invalidTokenRecorder;
    private final Clock clock;

    @EventListener
    @Transactional
    public void processTransaction(AddressUtxoEvent addressUtxoEvent) {
        if (!cip113Configuration.isEnabled()) {
            return;
        }
        Long slot = addressUtxoEvent.getMetadata().getSlot();
        List<Cip113RegistryNode> nodes = new ArrayList<>();
        for (TxInputOutput txInputOutput : addressUtxoEvent.getTxInputOutputs()) {
            for (AddressUtxo output : txInputOutput.getOutputs()) {
                findRegistryNft(output).ifPresent(registryNft -> indexOutput(output, registryNft, slot).ifPresent(nodes::add));
            }
        }
        if (!nodes.isEmpty()) {
            registryNodeRepository.saveAll(nodes);
            nodes.forEach(node -> log.info("Indexed CIP-113 registry node: key={}, slot={}, tx={}#{}",
                    node.getKey(), node.getSlot(), node.getTxHash(), node.getOutputIndex()));
        }
    }

    /** The registry NFT the output holds: one unit of a configured registry policy. */
    private Optional<AssetType> findRegistryNft(AddressUtxo output) {
        if (output.getAmounts() == null) {
            return Optional.empty();
        }
        return output.getAmounts().stream()
                .filter(amt -> amt != null && amt.getUnit() != null && BigInteger.ONE.equals(amt.getQuantity())
                        && cip113Configuration.isRegistryPolicyId(amt.getPolicyId()))
                .map(Amt::getUnit)
                .map(unit -> AssetType.fromUnit(unit.toLowerCase(Locale.ROOT)))
                .findFirst();
    }

    private Optional<Cip113RegistryNode> indexOutput(AddressUtxo output, AssetType registryNft, Long slot) {
        try {
            ParseResult result = registryNodeParser.parseDetailed(output.getInlineDatum());
            ParsedRegistryNode parsed = result.node();
            if (parsed == null) {
                report(output, slot, registryNft, InvalidTokenReason.REGISTRY_NODE_UNPARSEABLE, result.rejection());
                return Optional.empty();
            }
            String spoofed = spoofReason(output, registryNft, parsed);
            if (spoofed != null) {
                log.warn("CIP-113 registry node {}#{} skipped: {}", output.getTxHash(), output.getOutputIndex(), spoofed);
                report(output, slot, registryNft, InvalidTokenReason.REGISTRY_NODE_SPOOFED, spoofed);
                return Optional.empty();
            }
            return Optional.of(toEntity(output, parsed, slot));
        } catch (RuntimeException e) {
            // never stop the sync for one output (ADR 0004): record it, as CIP-68 does
            log.warn("CIP-113 registry node {}#{} skipped: indexing it failed unexpectedly", output.getTxHash(),
                    output.getOutputIndex(), e);
            report(output, slot, registryNft, InvalidTokenReason.PROCESSING_FAILURE,
                    "indexing the registry node failed unexpectedly: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    /** Why the output is not a node of the registry whose NFT it holds, or {@code null} when it is one. */
    @Nullable
    private static String spoofReason(AddressUtxo output, AssetType registryNft, ParsedRegistryNode parsed) {
        String paymentCredential = output.getOwnerPaymentCredential();
        if (paymentCredential == null || !paymentCredential.equalsIgnoreCase(registryNft.policyId())) {
            return "the registry NFT is at payment credential " + paymentCredential
                    + ", not at the registry's script " + registryNft.policyId();
        }
        if (!registryNft.assetName().equals(parsed.key())) {
            return "the datum's key '" + parsed.key() + "' is not the token name '" + registryNft.assetName()
                    + "' of the registry NFT the output holds";
        }
        return null;
    }

    private Cip113RegistryNode toEntity(AddressUtxo output, ParsedRegistryNode parsed, Long slot) {
        return Cip113RegistryNode.builder()
                .key(parsed.key())
                .slot(slot)
                .txHash(output.getTxHash())
                .txIndex(output.getTxIndex())
                .outputIndex(output.getOutputIndex())
                .next(parsed.next())
                .mintingLogicScript(parsed.mintingLogicScript())
                .transferLogicScript(parsed.transferLogicScript())
                .thirdPartyTransferLogicScript(parsed.thirdPartyTransferLogicScript())
                .unfrackingLogicScript(parsed.unfrackingLogicScript())
                .globalStatePolicyId(parsed.globalStatePolicyId())
                .datum(output.getInlineDatum())
                .lastSyncedAt(LocalDateTime.now(clock))
                .build();
    }

    private void report(AddressUtxo output, Long slot, AssetType registryNft, String reasonCode, String reason) {
        Map<String, Object> details = new LinkedHashMap<>();
        String datum = output.getInlineDatum();
        details.put("datum_prefix", datum == null ? "" : datum.substring(0, Math.min(datum.length(), DATUM_PREFIX_CHARS)));
        invalidTokenRecorder.report(InvalidTokenReport.builder()
                .standard(InvalidTokenReason.CIP_113)
                .reasonCode(reasonCode)
                .reason(reason)
                .policyId(registryNft.policyId())
                .assetName(registryNft.assetName())
                .slot(slot)
                .txHash(output.getTxHash())
                .txIndex(output.getTxIndex())
                .outputIndex(output.getOutputIndex())
                .details(details)
                .build());
    }
}
