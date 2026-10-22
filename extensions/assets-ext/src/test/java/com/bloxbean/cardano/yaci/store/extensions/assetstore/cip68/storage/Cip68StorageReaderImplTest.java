package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68UserTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Cip68StorageReaderImpl serves a subject only when its user token was seen")
class Cip68StorageReaderImplTest {

    private static final String POLICY = "aa".repeat(28);
    private static final String REF_NAME = "000643b0" + "6162";
    private static final String FT = POLICY + "0014df10" + "6162";
    private static final String RFT = POLICY + "001bc280" + "6162";

    private Cip68TokenService tokenService;
    private Cip68UserTokenService userTokenService;
    private Cip68StorageReaderImpl reader;
    private final FungibleTokenMetadata metadata = new FungibleTokenMetadata(null, "d", null, "N", null, null, 1L);

    @BeforeEach
    void setUp() {
        tokenService = mock(Cip68TokenService.class);
        userTokenService = mock(Cip68UserTokenService.class);
        reader = new Cip68StorageReaderImpl(tokenService, userTokenService);
        when(tokenService.getReferenceNftSubject(anyString())).thenAnswer(i -> {
            String subject = i.getArgument(0);
            return Optional.of(new AssetType(subject.substring(0, 56), REF_NAME));
        });
        when(tokenService.findSubject(eq(POLICY), eq(REF_NAME), any())).thenReturn(Optional.of(metadata));
    }

    @Test
    @DisplayName("a subject whose token was seen is served")
    void seenIsServed() {
        when(userTokenService.exists(FT)).thenReturn(true);

        assertThat(reader.findBySubject(FT)).contains(metadata);
    }

    @Test
    @DisplayName("a subject whose token was never seen is not served, and the metadata is not even read")
    void unseenIsNotServed() {
        when(userTokenService.exists(FT)).thenReturn(false);

        assertThat(reader.findBySubject(FT)).isEmpty();
        verify(tokenService, never()).findSubject(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("the batch leaves out the subjects whose token was never seen, with one lookup of the tokens")
    void batchFilters() {
        when(userTokenService.existing(anyList())).thenReturn(Set.of(RFT));
        when(tokenService.findSubjects(anyList(), any())).thenReturn(Map.of(POLICY + REF_NAME, metadata));

        Map<String, FungibleTokenMetadata> result = reader.findBySubjects(List.of(FT, RFT), List.of());

        assertThat(result).containsOnlyKeys(RFT);
        verify(userTokenService, times(1)).existing(anyList());
    }

    @Test
    @DisplayName("the batch reads no metadata when no token of it was seen")
    void batchWithNothingSeen() {
        when(userTokenService.existing(anyList())).thenReturn(Set.of());

        assertThat(reader.findBySubjects(List.of(FT, RFT), List.of())).isEmpty();
        verify(tokenService, never()).findSubjects(anyList(), any());
    }

    @Test
    @DisplayName("the policy and name form is served when the 333 or the 444 token was seen")
    void policyAndNameForm() {
        when(userTokenService.existsAsFungibleOrRichFungible(POLICY, "6162")).thenReturn(false);
        assertThat(reader.findByPolicyIdAndAssetName(POLICY, REF_NAME)).isEmpty();

        when(userTokenService.existsAsFungibleOrRichFungible(POLICY, "6162")).thenReturn(true);
        assertThat(reader.findByPolicyIdAndAssetName(POLICY, REF_NAME)).contains(metadata);
    }
}
