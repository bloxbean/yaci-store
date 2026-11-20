package com.bloxbean.cardano.yaci.store.script.processor;

import com.bloxbean.cardano.client.transaction.spec.script.ScriptPubkey;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.model.NativeScript;
import com.bloxbean.cardano.yaci.core.model.Redeemer;
import com.bloxbean.cardano.yaci.core.model.Witnesses;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.events.internal.BatchBlocksProcessedEvent;
import com.bloxbean.cardano.yaci.store.events.model.internal.BatchBlock;
import com.bloxbean.cardano.yaci.store.script.domain.Script;
import com.bloxbean.cardano.yaci.store.script.domain.ScriptType;
import com.bloxbean.cardano.yaci.store.script.helper.RedeemerDatumMatcher;
import com.bloxbean.cardano.yaci.store.script.helper.TxScriptFinder;
import com.bloxbean.cardano.yaci.store.script.storage.DatumStorage;
import com.bloxbean.cardano.yaci.store.script.storage.ScriptStorage;
import com.bloxbean.cardano.yaci.store.script.storage.TxScriptStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScriptRedeemerDatumProcessorTest {
    private static final String KEY_HASH = "ad7a7b87959173fc9eac9a85891cc93892f800dd45c0544128228884";
    private static final String NATIVE_SCRIPT_JSON = "{\"type\":\"sig\",\"keyHash\":\"" + KEY_HASH + "\"}";

    @Mock
    private TxScriptStorage txScriptStorage;
    @Mock
    private ScriptStorage scriptStorage;
    @Mock
    private DatumStorage datumStorage;
    @Mock
    private RedeemerDatumMatcher redeemerMatcher;
    @Mock
    private TxScriptFinder txScriptFinder;
    @Mock
    private ApplicationEventPublisher publisher;

    @Captor
    private ArgumentCaptor<List<Script>> scriptsCaptor;

    private ScriptRedeemerDatumProcessor processor;
    private String expectedScriptHash;

    @BeforeEach
    void setUp() throws Exception {
        processor = new ScriptRedeemerDatumProcessor(txScriptStorage, scriptStorage, datumStorage,
                redeemerMatcher, txScriptFinder, publisher);
        ReflectionTestUtils.setField(processor, "blockBatchPartitionSize", 10);
        expectedScriptHash = HexUtil.encodeHexString(new ScriptPubkey(KEY_HASH).getScriptHash());
    }

    @Test
    void nativeOnlyTx_sequential_shouldSaveNativeScript() {
        processor.handleScriptTransactionEvent(TransactionEvent.builder()
                .metadata(metadata(false))
                .transactions(List.of(tx(Collections.emptyList())))
                .build());

        assertNativeScriptSaved();
        verifyNoInteractions(txScriptStorage, redeemerMatcher, txScriptFinder);
    }

    @Test
    void nativeOnlyTx_nullRedeemers_shouldSaveNativeScript() {
        processor.handleScriptTransactionEvent(TransactionEvent.builder()
                .metadata(metadata(false))
                .transactions(List.of(tx(null)))
                .build());

        assertNativeScriptSaved();
    }

    @Test
    void nativeOnlyTx_parallel_shouldSaveNativeScript() {
        EventMetadata metadata = metadata(true);
        BatchBlock batchBlock = new BatchBlock(metadata, null, List.of(tx(Collections.emptyList())));

        processor.handleScriptTransactionEventForBlockCacheList(new BatchBlocksProcessedEvent(metadata, List.of(batchBlock)));

        assertNativeScriptSaved();
    }

    @Test
    void mixedNativeAndPlutusTx_shouldSaveNativeScriptOnce() {
        when(txScriptFinder.getScripts(any())).thenReturn(Collections.emptyMap());
        when(redeemerMatcher.findScriptsForRedeemers(any(), any())).thenReturn(Collections.emptyList());

        processor.handleScriptTransactionEvent(TransactionEvent.builder()
                .metadata(metadata(false))
                .transactions(List.of(tx(List.of(Redeemer.builder().build()))))
                .build());

        assertNativeScriptSaved();
    }

    @Test
    void invalidTx_shouldNotSaveNativeScript() {
        Transaction invalidTx = tx(Collections.emptyList());
        invalidTx.setInvalid(true);

        processor.handleScriptTransactionEvent(TransactionEvent.builder()
                .metadata(metadata(false))
                .transactions(List.of(invalidTx))
                .build());

        verifyNoInteractions(scriptStorage);
    }

    private void assertNativeScriptSaved() {
        verify(scriptStorage, times(1)).saveScripts(scriptsCaptor.capture());
        List<Script> scripts = scriptsCaptor.getValue();
        assertThat(scripts).hasSize(1);
        assertThat(scripts.get(0).getScriptHash()).isEqualTo(expectedScriptHash);
        assertThat(scripts.get(0).getScriptType()).isEqualTo(ScriptType.NATIVE_SCRIPT);
        assertThat(scripts.get(0).getSlot()).isEqualTo(1000L);
    }

    private EventMetadata metadata(boolean parallelMode) {
        return EventMetadata.builder()
                .slot(1000)
                .block(100)
                .blockHash("block-hash")
                .parallelMode(parallelMode)
                .build();
    }

    private Transaction tx(List<Redeemer> redeemers) {
        return Transaction.builder()
                .txHash("f90dce5765108da976abdbb9fc618f9a6ffd9fa4d93b2f288eed1808545424c9")
                .witnesses(Witnesses.builder()
                        .nativeScripts(List.of(new NativeScript(0, NATIVE_SCRIPT_JSON)))
                        .redeemers(redeemers)
                        .build())
                .build();
    }
}
