package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.Extension;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.PolicyResponse;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.PolicyTokenSummary;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.Cip113StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.Cip26StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.MetadataReferenceNft;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.Cip68StorageReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for policy-level queries — aggregates all tokens and extensions for a minting policy.
 */
@Service
@Slf4j
public class PolicyService {

    private final Cip26StorageReader cip26StorageReader;
    private final Cip68StorageReader cip68StorageReader;
    private final Optional<Cip113StorageReader> cip113StorageReader;

    @Autowired
    public PolicyService(Cip26StorageReader cip26StorageReader,
                         Cip68StorageReader cip68StorageReader,
                         @Autowired(required = false) Cip113StorageReader cip113StorageReader) {
        this.cip26StorageReader = cip26StorageReader;
        this.cip68StorageReader = cip68StorageReader;
        this.cip113StorageReader = Optional.ofNullable(cip113StorageReader);
    }

    /**
     * Look up a single policy: aggregates all CIP-26 and CIP-68 tokens plus extensions.
     */
    public Optional<PolicyResponse> findByPolicyId(String policyId) {
        List<PolicyTokenSummary> tokens = buildTokenSummaries(policyId);
        Map<String, Extension> extensions = buildExtensions(policyId);

        if (tokens.isEmpty() && extensions.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new PolicyResponse(policyId, tokens, extensions));
    }

    /**
     * Batch lookup of policies.
     */
    public List<PolicyResponse> findByPolicyIds(Collection<String> policyIds) {
        if (policyIds.isEmpty()) {
            return List.of();
        }

        List<String> policyIdList = List.copyOf(policyIds);
        Map<String, List<TokenMetadata>> cip26ByPolicy = cip26StorageReader.findByPolicies(policyIdList)
                .stream()
                .collect(Collectors.groupingBy(TokenMetadata::getPolicy));

        Map<String, List<MetadataReferenceNft>> cip68ByPolicy = cip68StorageReader.findLatestByPolicyIds(policyIdList)
                .stream()
                .collect(Collectors.groupingBy(MetadataReferenceNft::getPolicyId));

        Map<String, ProgrammableTokenCip113> cip113ByPolicy = cip113StorageReader
                .map(reader -> reader.findByPolicyIds(policyIdList))
                .orElse(Map.of());

        List<PolicyResponse> results = new ArrayList<>();
        for (String policyId : policyIdList) {
            List<PolicyTokenSummary> tokens = mergeTokenSummaries(
                    cip26ByPolicy.getOrDefault(policyId, List.of()),
                    cip68ByPolicy.getOrDefault(policyId, List.of())
            );
            Map<String, Extension> extensions = buildExtensionsFromBatch(policyId, cip113ByPolicy);

            if (!tokens.isEmpty() || !extensions.isEmpty()) {
                results.add(new PolicyResponse(policyId, tokens, extensions));
            }
        }
        return results;
    }

    private Map<String, Extension> buildExtensions(String policyId) {
        Map<String, Extension> extensions = new LinkedHashMap<>();
        cip113StorageReader.flatMap(reader -> reader.findByPolicyId(policyId))
                .ifPresent(cip113 -> extensions.put(ProgrammableTokenCip113.EXTENSION_KEY, cip113));
        return extensions;
    }

    private Map<String, Extension> buildExtensionsFromBatch(String policyId,
                                                             Map<String, ProgrammableTokenCip113> cip113ByPolicy) {
        Map<String, Extension> extensions = new LinkedHashMap<>();
        ProgrammableTokenCip113 cip113 = cip113ByPolicy.get(policyId);
        if (cip113 != null) {
            extensions.put(ProgrammableTokenCip113.EXTENSION_KEY, cip113);
        }
        return extensions;
    }

    private List<PolicyTokenSummary> buildTokenSummaries(String policyId) {
        List<TokenMetadata> cip26Tokens = cip26StorageReader.findByPolicy(policyId);
        List<MetadataReferenceNft> cip68Tokens = cip68StorageReader.findLatestByPolicyIds(List.of(policyId));
        return mergeTokenSummaries(cip26Tokens, cip68Tokens);
    }

    /**
     * Merges CIP-26 and CIP-68 tokens into a deduplicated list of summaries.
     * CIP-68 takes precedence when the same subject exists in both.
     */
    private List<PolicyTokenSummary> mergeTokenSummaries(List<TokenMetadata> cip26Tokens,
                                                          List<MetadataReferenceNft> cip68Tokens) {
        Map<String, PolicyTokenSummary> bySubject = new LinkedHashMap<>();

        for (TokenMetadata token : cip26Tokens) {
            bySubject.put(token.getSubject(), new PolicyTokenSummary(
                    token.getSubject(),
                    token.getName(),
                    token.getTicker(),
                    token.getDecimals(),
                    "CIP_26"
            ));
        }

        for (MetadataReferenceNft nft : cip68Tokens) {
            String subject = nft.getPolicyId() + nft.getAssetName();
            bySubject.put(subject, new PolicyTokenSummary(
                    subject,
                    nft.getName(),
                    nft.getTicker(),
                    nft.getDecimals(),
                    "CIP_68"
            ));
        }

        return List.copyOf(bySubject.values());
    }
}
