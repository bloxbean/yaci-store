package com.bloxbean.cardano.yaci.store.script.processor;

import com.bloxbean.cardano.client.plutus.spec.ExUnits;
import com.bloxbean.cardano.client.plutus.spec.RedeemerTag;
import com.bloxbean.cardano.yaci.core.model.*;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.helper.model.Utxo;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.events.internal.BatchBlocksProcessedEvent;
import com.bloxbean.cardano.yaci.store.events.model.internal.BatchBlock;
import com.bloxbean.cardano.yaci.store.script.domain.Script;
import com.bloxbean.cardano.yaci.store.script.domain.ScriptType;
import com.bloxbean.cardano.yaci.store.script.domain.TxScript;
import com.bloxbean.cardano.yaci.store.script.domain.TxScriptEvent;
import com.bloxbean.cardano.yaci.store.script.helper.RedeemerDatumMatcher;
import com.bloxbean.cardano.yaci.store.script.helper.ScriptContext;
import com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil;
import com.bloxbean.cardano.yaci.store.script.helper.TxScriptFinder;
import com.bloxbean.cardano.yaci.store.script.storage.DatumStorage;
import com.bloxbean.cardano.yaci.store.script.storage.ScriptStorage;
import com.bloxbean.cardano.yaci.store.script.storage.TxScriptStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ScriptRedeemerDatumProcessorTest {

    @Mock
    private TxScriptStorage txScriptStorage;

    @Mock
    private TxScriptFinder txScriptFinder;

    @Mock
    private DatumStorage datumStorage;

    @Mock
    private RedeemerDatumMatcher redeemerMatcher;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private ScriptStorage scriptStorage;

    @InjectMocks
    private ScriptRedeemerDatumProcessor scriptRedeemerDatumProcessor;

    @Captor
    private ArgumentCaptor<List<com.bloxbean.cardano.yaci.store.script.domain.Datum>> datumArgCaptor;

    @Captor
    private ArgumentCaptor<List<Script>> scriptArgCaptor;

    @Captor
    private ArgumentCaptor<List<TxScript>> txScriptArgCaptor;

    @Captor
    private ArgumentCaptor<TxScriptEvent> txScriptPublisherArgCaptor;

    @Test
    void givenTransactionEvent_whenIsParallelModeTrue_shouldReturn() {
        TransactionEvent transactionEvent = TransactionEvent.builder()
                .metadata(EventMetadata.builder()
                        .parallelMode(true)
                        .build())
                .build();

        scriptRedeemerDatumProcessor.handleScriptTransactionEvent(transactionEvent);

        Mockito.verify(txScriptStorage, Mockito.never()).saveAll(Mockito.any());
    }

    @Test
    void givenTransactionEvent_whenRedeemersIsNull_shouldReturn() {
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(Transaction.builder()
                        .witnesses(Witnesses.builder()
                                .redeemers(null)
                                .build())
                .build());

        TransactionEvent transactionEvent = TransactionEvent.builder()
                .metadata(EventMetadata.builder()
                        .parallelMode(false)
                        .build())
                .transactions(transactions)
                .build();

        scriptRedeemerDatumProcessor.handleScriptTransactionEvent(transactionEvent);

        Mockito.verify(txScriptStorage, Mockito.never()).saveAll(Mockito.any());
    }

    @Test
    void givenTransactionEvent_whenRedeemersSizeIsZero_shouldReturn() {
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(Transaction.builder()
                .witnesses(Witnesses.builder()
                        .redeemers(new ArrayList<>())
                        .build())
                .build());

        TransactionEvent transactionEvent = TransactionEvent.builder()
                .metadata(EventMetadata.builder()
                        .parallelMode(false)
                        .build())
                .transactions(transactions)
                .build();

        scriptRedeemerDatumProcessor.handleScriptTransactionEvent(transactionEvent);

        Mockito.verify(txScriptStorage, Mockito.never()).saveAll(Mockito.any());
    }

    @Test
    void givenTransactionEvent_shouldPublishTxScriptEvent() {
        List<Transaction> transactions = new ArrayList<>();
        List<Amount> amounts = new ArrayList<>();
        amounts.add(Amount.builder()
                .unit("lovelace")
                .assetName("lovelace")
                .quantity(new BigInteger("9995000000"))
                .build());

        List<VkeyWitness> vkeyWitnesses = new ArrayList<>();
        vkeyWitnesses.add(VkeyWitness.builder()
                .key("096092b8515d75c2a2f75d6aa7c5191996755840e81deaa403dba5b690f091b6")
                .signature("6df908dc905c533ee2f8997675a3907a7ad36022e45f32a79d5834e0342ec850161eee69cfe58314f2735d97d5f298acf59692bedf31fef7a2a3cadfe1f70a0d")
                .build());
        List<Redeemer> redeemers = new ArrayList<>();
        redeemers.add(Redeemer.builder()
                .tag(RedeemerTag.Mint)
                .index(0)
                .data(Datum.builder().build())
                .exUnits(new ExUnits(new BigInteger("881262"), new BigInteger("270069846")))
                .cbor("840100d87980821a000d726e1a1018f056")
                .build());
        List<PlutusScript> plutusV2Scripts = new ArrayList<>();
        plutusV2Scripts.add(PlutusScript.builder()
                .type("2")
                .content("590c7e590c7b01000033232323232323232323233223233223232323232323322323232323232323232323232323232323232323232323232323232323355501722232325335330053333573466e1cd55ce9baa0044800080c88c98c80c8cd5ce00c8190181999ab9a3370e6aae7540092000233221233001003002323232323232323232323232323333573466e1cd55cea8062400046666666666664444444444442466666666666600201a01801601401201000e00c00a00800600466a0320346ae854030cd4064068d5d0a80599a80c80d9aba1500a3335501d75ca0386ae854024ccd54075d7280e1aba1500833501902235742a00e666aa03a046eb4d5d0a8031919191999ab9a3370e6aae75400920002332212330010030023232323333573466e1cd55cea8012400046644246600200600466a05aeb4d5d0a80118171aba135744a004464c6409066ae700bc1201184d55cf280089baa00135742a0046464646666ae68cdc39aab9d5002480008cc8848cc00400c008cd40b5d69aba15002302e357426ae8940088c98c8120cd5ce01782402309aab9e5001137540026ae84d5d1280111931902219ab9c02b044042135573ca00226ea8004d5d0a80299a80cbae35742a008666aa03a03e40026ae85400cccd54075d710009aba150023021357426ae8940088c98c8100cd5ce01382001f09aba25001135744a00226ae8940044d5d1280089aba25001135744a00226ae8940044d5d1280089aba25001135744a00226aae7940044dd50009aba150023011357426ae8940088c98c80c8cd5ce00c819018081889a817a4810350543500135573ca00226ea8004cd55405c888c8c8c94cd4c008c8d4004888888888888031400454cd4cd540a0cd5406809cd401888004cd540a08004c025400454cd54cd4cd540a0cd5407009d400cc8004c0254004854cd4ccc88cd54008c8cd409088ccd400c88008008004d40048800448cc004894cd4008400440c40c0004c08048004cd5540788cc0a000520022350012200100113355301c12001235001220020011302e4984c0b5261302a4988854cd400454cd4cc0a4008c8d400488008c848cc0040f0008cdc5a41fc0666e2940d540d44cd540a894cd400440bc4cd5ce249256572726f7220276d6b44734b6579506f6c6963792720696c6c6567616c206f7574707574730002e5335323335530221200133502322533500221003100150262533533320015025300c001300e302200a13502800115027001323500122222222222200a500321335502b335501d02a5006335502b2001300a001102d1302c498884c0b926130294988854cd400440b8884c0b52613500322002320013550372253350011502f2322321533533320015025300c5003300e302200a15335335502c335502002b500732001300b500321533500113002498884d400888cd40dc008c02c01c4c00526130014988c0180084d4004880044d400488cccd40048c98c80c8cd5ce2481024c680003220012326320323357389201024c68000322326320323357389201024c6800032232323333573466e1cd55cea801240004664424660020060046eb8d5d0a8011bae357426ae8940088c98c80c0cd5ce00b81801709aab9e50011375400246a002444400646a002444400846a0024444444444440104660326038002a0342464460046eb0004c8004d540bc88cccd55cf80092814119a81398021aba1002300335744004054464646666ae68cdc39aab9d5002480008cc8848cc00400c008c028d5d0a80118029aba135744a004464c6405466ae700440a80a04d55cf280089baa0012323232323333573466e1cd55cea8022400046666444424666600200a0080060046464646666ae68cdc39aab9d5002480008cc07cc04cd5d0a80119a8068091aba135744a004464c6405e66ae700580bc0b44d55cf280089baa00135742a008666aa010eb9401cd5d0a8019919191999ab9a3370ea0029002119091118010021aba135573ca00646666ae68cdc3a80124004464244460020086eb8d5d09aab9e500423333573466e1d400d20002122200323263203133573803006205e05c05a26aae7540044dd50009aba1500233500975c6ae84d5d1280111931901599ab9c01202b029135744a00226ae8940044d55cf280089baa0011335500175ceb44488c88c008dd5800990009aa81611191999aab9f00225026233502533221233001003002300635573aa004600a6aae794008c010d5d100181409aba100112232323333573466e1d400520002350193005357426aae79400c8cccd5cd19b87500248008940648c98c80a0cd5ce00781401301289aab9d500113754002464646666ae68cdc3a800a400c46424444600800a600e6ae84d55cf280191999ab9a3370ea004900211909111180100298049aba135573ca00846666ae68cdc3a801a400446424444600200a600e6ae84d55cf280291999ab9a3370ea00890001190911118018029bae357426aae7940188c98c80a0cd5ce00781401301281201189aab9d500113754002464646666ae68cdc39aab9d5002480008cc8848cc00400c008c014d5d0a8011bad357426ae8940088c98c8090cd5ce00581201109aab9e5001137540024646666ae68cdc39aab9d5001480008dd71aba135573ca004464c6404466ae700240880804dd5000919191919191999ab9a3370ea002900610911111100191999ab9a3370ea004900510911111100211999ab9a3370ea00690041199109111111198008048041bae35742a00a6eb4d5d09aba2500523333573466e1d40112006233221222222233002009008375c6ae85401cdd71aba135744a00e46666ae68cdc3a802a400846644244444446600c01201060186ae854024dd71aba135744a01246666ae68cdc3a8032400446424444444600e010601a6ae84d55cf280591999ab9a3370ea00e900011909111111180280418071aba135573ca018464c6405666ae700480ac0a40a009c09809409008c4d55cea80209aab9e5003135573ca00426aae7940044dd50009191919191999ab9a3370ea002900111999110911998008028020019bad35742a0086eb4d5d0a8019bad357426ae89400c8cccd5cd19b875002480008c8488c00800cc020d5d09aab9e500623263202433573801604804404226aae75400c4d5d1280089aab9e500113754002464646666ae68cdc3a800a4004460266eb8d5d09aab9e500323333573466e1d400920002321223002003375c6ae84d55cf280211931901099ab9c00802101f01e135573aa00226ea8004488c8c8cccd5cd19b87500148010848880048cccd5cd19b875002480088c84888c00c010c018d5d09aab9e500423333573466e1d400d20002122200223263202233573801204404003e03c26aae7540044dd50009191999ab9a3370ea0029001100b91999ab9a3370ea0049000100b91931900f19ab9c00501e01c01b135573a6ea800524010350543100112232230020013200135502122533500110152213500222533533008002007101a130060033200135501e22112253350011501822133501930040023355300612001004001112232230020013200135501f2253350011500b22135002225335330080020071350100011300600311122230033002001235001220023200135501a221122253350011350032200122133350052200230040023335530071200100500400112212330010030021223500222350032232335005233500425335333573466e3c00800405004c5400c404c804c8cd4010804c94cd4ccd5cd19b8f002001014013150031013153350032153350022133500223350022335002233500223301200200120162335002201623301200200122201622233500420162225335333573466e1c01800c06406054cd4ccd5cd19b870050020190181330130040011018101810111533500121011101122123300100300212122300200311220012122300100322333573466e1c00800402001c88ccd5cd19b8f0020010070061122300200123500849012f6572726f7220276d6b44734b6579506f6c696379272062616420696e7075747320696e207472616e73616374696f6e002350074901266572726f7220276d6b44734b6579506f6c696379272062616420696e697469616c206d696e74001220021220012350044901286572726f7220276d6b44734b6579506f6c696379273a20626164206d696e74656420746f6b656e730011220021221223300100400312326320033357380020069309000899b8a50015001133714a002a002266e29400540044cdc52800a800899b8b483f80c005220100112323001001223300330020020014c140d8799f581c4aa93779bf0953b3d43adfa530fa090f928bc06541b8be5b039221d9581c1cf569e1ec3e0fee92f1f5002bfd4213b796c151c708db46e6e2d3a4ff0001")
                .build());
        List<NativeScript> nativeScripts = new ArrayList<>();
        nativeScripts.add(NativeScript.builder()
                .content("{\n" +
                        "  \"type\" : \"sig\",\n" +
                        "  \"keyHash\" : \"998cb92c7751cddd467e485865fb9374c03f734b2d374938e10436ce\"\n" +
                        "}")
                .type(0)
                .build());
        transactions.add(Transaction.builder()
                .blockNumber(178279)
                .slot(10684581)
                .txHash("d5fed4dca96c7efda3d1b0897c91c0eee8f8c6a18e5931dfbd56b6ec7a5951a1")
                .body(mockBody())
                .utxos(new ArrayList<>())
                .collateralReturnUtxo(Utxo.builder()
                        .txHash("d5fed4dca96c7efda3d1b0897c91c0eee8f8c6a18e5931dfbd56b6ec7a5951a1")
                        .index(4)
                        .address("addr_test1vq85t2h3k22emdh9l72dhv0cywlj2a5qc0rj8tpdf8uh23st77ahh")
                        .amounts(amounts)
                        .build())
                .witnesses(Witnesses.builder()
                        .vkeyWitnesses(vkeyWitnesses)
                        .nativeScripts(nativeScripts)
                        .bootstrapWitnesses(new ArrayList<>())
                        .plutusV1Scripts(new ArrayList<>())
                        .datums(new ArrayList<>())
                        .redeemers(redeemers)
                        .plutusV2Scripts(plutusV2Scripts)
                        .plutusV3Scripts(new ArrayList<>())
                        .build())
                .invalid(false)
                .build());

        TransactionEvent transactionEvent = TransactionEvent.builder()
                .metadata(EventMetadata.builder()
                        .mainnet(false)
                        .protocolMagic(1)
                        .era(Era.Babbage)
                        .slotLeader("12946a3fe080dd99af599bfff10a05cd3de19bd38ed85b25dee35dd5")
                        .epochNumber(28)
                        .block(47)
                        .blockHash("47ce1d79ffd414412071bf172f78efee48c634fac48668073ef63c58e51131b0")
                        .blockTime(1666367781)
                        .prevBlockHash("e82c0484f433cceeaf63e2b1ef1e8c2f89f3a23efce8e4623d400db4616a5eee")
                        .slot(10684581)
                        .epochSlot(230181)
                        .noOfTxs(1)
                        .syncMode(false)
                        .remotePublish(false)
                        .parallelMode(false)
                        .build())
                .transactions(transactions)
                .build();

        Map<String, PlutusScript> scriptMap = new HashMap<>();
        scriptMap.put(ScriptUtil.getPlutusScriptHash(transactions.get(0).getWitnesses().getPlutusV2Scripts().get(0)),
                        transactions.get(0).getWitnesses().getPlutusV2Scripts().get(0));

        List<ScriptContext> scriptContexts = new ArrayList<>();
        ScriptContext scriptContext = new ScriptContext();
        scriptContext.setPlutusScript(scriptMap.get(transactions.get(0).getBody().getMint().get(0).getPolicyId()));
        scriptContext.setRedeemer(transactions.get(0).getWitnesses().getRedeemers().get(0));
        scriptContexts.add(scriptContext);


        Mockito.when(txScriptFinder.getScripts(transactions.get(0))).thenReturn(scriptMap);
        Mockito.when(redeemerMatcher.findScriptsForRedeemers(transactions.get(0), scriptMap)).thenReturn(scriptContexts);

        scriptRedeemerDatumProcessor.handleScriptTransactionEvent(transactionEvent);

        Mockito.verify(scriptStorage, Mockito.times(2)).saveScripts(scriptArgCaptor.capture());
        List<Script> scripts = scriptArgCaptor.getValue();

        assertThat(scripts.get(0).getScriptHash()).isEqualTo("ebe691a343665487dbca5d1853cfb798b68e5ab3e1f699240e2cf999");
        assertThat(scripts.get(0).getScriptType()).isEqualTo(ScriptType.NATIVE_SCRIPT);
        assertThat(scripts.get(0).getContent()).isEqualTo(JsonUtil.getJson(NativeScript.builder()
                .content("{\n" +
                        "  \"type\" : \"sig\",\n" +
                        "  \"keyHash\" : \"998cb92c7751cddd467e485865fb9374c03f734b2d374938e10436ce\"\n" +
                        "}")
                .type(0)
                .build()));

        Mockito.verify(datumStorage, Mockito.times(1)).saveAll(datumArgCaptor.capture());
        List<com.bloxbean.cardano.yaci.store.script.domain.Datum> datumList = datumArgCaptor.getValue();

        assertThat(datumList.get(0).getCreatedAtTx()).isEqualTo("d5fed4dca96c7efda3d1b0897c91c0eee8f8c6a18e5931dfbd56b6ec7a5951a1");

        Mockito.verify(txScriptStorage, Mockito.times(1)).saveAll(txScriptArgCaptor.capture());
        List<TxScript> txScriptList = txScriptArgCaptor.getValue();

        assertThat(txScriptList.get(0).getTxHash()).isEqualTo("d5fed4dca96c7efda3d1b0897c91c0eee8f8c6a18e5931dfbd56b6ec7a5951a1");
        assertThat(txScriptList.get(0).getSlot()).isEqualTo(10684581);
        assertThat(txScriptList.get(0).getBlockHash()).isEqualTo("47ce1d79ffd414412071bf172f78efee48c634fac48668073ef63c58e51131b0");
        assertThat(txScriptList.get(0).getRedeemerCbor()).isEqualTo("840100d87980821a000d726e1a1018f056");
        assertThat(txScriptList.get(0).getUnitMem()).isEqualTo(new BigInteger("881262"));
        assertThat(txScriptList.get(0).getUnitSteps()).isEqualTo(new BigInteger("270069846"));
        assertThat(txScriptList.get(0).getPurpose()).isEqualTo(RedeemerTag.Mint);
        assertThat(txScriptList.get(0).getRedeemerIndex()).isEqualTo(0);
        assertThat(txScriptList.get(0).getBlockNumber()).isEqualTo(47);
        assertThat(txScriptList.get(0).getBlockTime()).isEqualTo(1666367781);

        Mockito.verify(publisher, Mockito.times(1)).publishEvent(txScriptPublisherArgCaptor.capture());
        TxScriptEvent txScriptEvent = txScriptPublisherArgCaptor.getValue();

        assertThat(txScriptEvent.getEventMetadata().isMainnet()).isEqualTo(false);
        assertThat(txScriptEvent.getEventMetadata().getProtocolMagic()).isEqualTo(1);
        assertThat(txScriptEvent.getEventMetadata().getEra()).isEqualTo(Era.Babbage);
        assertThat(txScriptEvent.getEventMetadata().getSlotLeader()).isEqualTo("12946a3fe080dd99af599bfff10a05cd3de19bd38ed85b25dee35dd5");
        assertThat(txScriptEvent.getEventMetadata().getEpochNumber()).isEqualTo(28);
        assertThat(txScriptEvent.getEventMetadata().getBlock()).isEqualTo(47);
        assertThat(txScriptEvent.getEventMetadata().getBlockHash()).isEqualTo("47ce1d79ffd414412071bf172f78efee48c634fac48668073ef63c58e51131b0");
        assertThat(txScriptEvent.getEventMetadata().getBlockTime()).isEqualTo(1666367781);
        assertThat(txScriptEvent.getEventMetadata().getPrevBlockHash()).isEqualTo("e82c0484f433cceeaf63e2b1ef1e8c2f89f3a23efce8e4623d400db4616a5eee");
        assertThat(txScriptEvent.getEventMetadata().getSlot()).isEqualTo(10684581);
        assertThat(txScriptEvent.getEventMetadata().getEpochSlot()).isEqualTo(230181);
        assertThat(txScriptEvent.getEventMetadata().getNoOfTxs()).isEqualTo(1);
        assertThat(txScriptEvent.getEventMetadata().isSyncMode()).isEqualTo(false);
        assertThat(txScriptEvent.getEventMetadata().isParallelMode()).isEqualTo(false);
        assertThat(txScriptEvent.getEventMetadata().isRemotePublish()).isEqualTo(false);
        assertThat(txScriptEvent.getTxScriptList().get(0).getTxHash()).isEqualTo("d5fed4dca96c7efda3d1b0897c91c0eee8f8c6a18e5931dfbd56b6ec7a5951a1");
        assertThat(txScriptEvent.getTxScriptList().get(0).getSlot()).isEqualTo(10684581);
        assertThat(txScriptEvent.getTxScriptList().get(0).getBlockHash()).isEqualTo("47ce1d79ffd414412071bf172f78efee48c634fac48668073ef63c58e51131b0");
        assertThat(txScriptEvent.getTxScriptList().get(0).getRedeemerCbor()).isEqualTo("840100d87980821a000d726e1a1018f056");
        assertThat(txScriptEvent.getTxScriptList().get(0).getUnitMem()).isEqualTo(new BigInteger("881262"));
        assertThat(txScriptEvent.getTxScriptList().get(0).getUnitSteps()).isEqualTo(new BigInteger("270069846"));
        assertThat(txScriptEvent.getTxScriptList().get(0).getPurpose()).isEqualTo(RedeemerTag.Mint);
        assertThat(txScriptEvent.getTxScriptList().get(0).getRedeemerIndex()).isEqualTo(0);
        assertThat(txScriptEvent.getTxScriptList().get(0).getBlockNumber()).isEqualTo(47);
        assertThat(txScriptEvent.getTxScriptList().get(0).getBlockTime()).isEqualTo(1666367781);
    }

    private TransactionBody mockBody() {

        Set<TransactionInput> inputs = new HashSet<>();
        inputs.add(TransactionInput.builder()
                        .transactionId("3af0570cd555abee485f9597fe8aa7440a5e27bcc6f4d39b13ec9a8d55d56c97")
                        .index(0)
                .build());

        List<TransactionOutput> outputs = new ArrayList<>();
        List<Amount> amountOutputs = new ArrayList<>();

        amountOutputs.add(Amount.builder()
                        .unit("lovelace")
                        .assetName("lovelace")
                        .quantity(new BigInteger("9995285122"))
                .build());

        outputs.add(TransactionOutput.builder()
                        .address("addr_test1vq85t2h3k22emdh9l72dhv0cywlj2a5qc0rj8tpdf8uh23st77ahh")
                        .amounts(amountOutputs)
                .build());


        List<Amount> mints = new ArrayList<>();
        mints.add(Amount.builder()
                        .unit("1cf569e1ec3e0fee92f1f5002bfd4213b796c151c708db46e6e2d3a4.")
                        .policyId("1cf569e1ec3e0fee92f1f5002bfd4213b796c151c708db46e6e2d3a4")
                        .assetName("")
                        .quantity(new BigInteger("1"))
                .build());
        Set<TransactionInput> collateralInputs = new HashSet<>();
        collateralInputs.add(TransactionInput.builder()
                        .transactionId("3af0570cd555abee485f9597fe8aa7440a5e27bcc6f4d39b13ec9a8d55d56c97")
                        .index(0)
                .build());
        List<Amount> amountCollateralReturns = new ArrayList<>();
        amountCollateralReturns.add(Amount.builder()
                        .unit("lovelace")
                        .assetName("lovelace")
                        .quantity(new BigInteger("9995000000"))
                .build());
        return TransactionBody.builder()
                .txHash("d5fed4dca96c7efda3d1b0897c91c0eee8f8c6a18e5931dfbd56b6ec7a5951a1")
                .inputs(inputs)
                .outputs(outputs)
                .fee(new BigInteger("779848"))
                .ttl(0)
                .certificates(new ArrayList<>())
                .validityIntervalStart(0)
                .mint(mints)
                .scriptDataHash("03a3ee5cc4b1ac8863731642101888bb7622f7f61d61976f985ff5f4bace4748")
                .collateralInputs(collateralInputs)
                .netowrkId(0)
                .collateralReturn(TransactionOutput.builder()
                        .address("addr_test1vq85t2h3k22emdh9l72dhv0cywlj2a5qc0rj8tpdf8uh23st77ahh")
                        .amounts(amountCollateralReturns)
                        .build())
                .totalCollateral(new BigInteger("5000000"))
                .build();
    }

    @Test
    void givenBatchBlocksProcessedEvent_shouldPublishTxScriptEvent() {
        List<BatchBlock> batchBlocks = mockBlockCaches();
        BatchBlocksProcessedEvent batchBlocksProcessedEvent = BatchBlocksProcessedEvent.builder()
                .metadata(EventMetadata.builder()
                        .mainnet(false)
                        .protocolMagic(1)
                        .era(Era.Shelley)
                        .slotLeader("de7ca985023cf892f4de7f5f1d0a7181668884752d9ebb9e96c95059")
                        .epochNumber(4)
                        .block(246)
                        .blockHash("12b9a858098229f6e30faaa81391299d378385fb31ed0349d3027fda7c1c2e9d")
                        .blockTime(1655773600)
                        .prevBlockHash("d8f82e121b5309195f9e177827ff1cf1157e736dd2ff4c85d1c8247141d2126c")
                        .slot(90400)
                        .epochSlot(4000)
                        .noOfTxs(1)
                        .syncMode(false)
                        .remotePublish(true)
                        .parallelMode(false)
                        .build())
                .blockCaches(batchBlocks)
                .build();

        Field blockBatchPartitionSize = Objects.requireNonNull(ReflectionUtils.findField(
                ScriptRedeemerDatumProcessor.class, "blockBatchPartitionSize"));

        blockBatchPartitionSize.setAccessible(true);
        ReflectionUtils.setField(blockBatchPartitionSize, scriptRedeemerDatumProcessor, 10);

        Field executor = Objects.requireNonNull(ReflectionUtils.findField(
                ScriptRedeemerDatumProcessor.class, "executor"));

        executor.setAccessible(true);
        ReflectionUtils.setField(executor, scriptRedeemerDatumProcessor, Executors.newVirtualThreadPerTaskExecutor());

        Map<String, PlutusScript> scriptMap = new HashMap<>();
        Transaction transaction = batchBlocks.get(0).getTransactions().get(0);

        scriptMap.put(ScriptUtil.getPlutusScriptHash(transaction.getWitnesses().getPlutusV2Scripts().get(0)),
                transaction.getWitnesses().getPlutusV2Scripts().get(0));

        List<ScriptContext> scriptContexts = new ArrayList<>();
        ScriptContext scriptContext = new ScriptContext();
        scriptContext.setPlutusScript(scriptMap.get(transaction.getBody().getMint().get(0).getPolicyId()));
        scriptContext.setRedeemer(transaction.getWitnesses().getRedeemers().get(0));
        scriptContexts.add(scriptContext);

        Mockito.when(txScriptFinder.getScripts(transaction)).thenReturn(scriptMap);
        Mockito.when(redeemerMatcher.findScriptsForRedeemers(transaction, scriptMap)).thenReturn(scriptContexts);


        scriptRedeemerDatumProcessor.handleScriptTransactionEventForBlockCacheList(batchBlocksProcessedEvent);

        Mockito.verify(datumStorage, Mockito.times(1)).saveAll(datumArgCaptor.capture());
        List<com.bloxbean.cardano.yaci.store.script.domain.Datum> datumList = datumArgCaptor.getValue();

        assertThat(datumList.get(0).getCreatedAtTx()).isEqualTo("b75ec46c406113372efeb1e57d9880856c240c9b531e3c680c1c4d8bf2253625");

        Mockito.verify(scriptStorage, Mockito.times(2)).saveScripts(scriptArgCaptor.capture());
        List<Script> scripts = scriptArgCaptor.getValue();

        assertThat(scripts.get(0).getScriptHash()).isEqualTo("ebe691a343665487dbca5d1853cfb798b68e5ab3e1f699240e2cf999");
        assertThat(scripts.get(0).getScriptType()).isEqualTo(ScriptType.NATIVE_SCRIPT);
        assertThat(scripts.get(0).getContent()).isEqualTo(JsonUtil.getJson(NativeScript.builder()
                .content("{\n" +
                        "  \"type\" : \"sig\",\n" +
                        "  \"keyHash\" : \"998cb92c7751cddd467e485865fb9374c03f734b2d374938e10436ce\"\n" +
                        "}")
                .type(0)
                .build()));

        Mockito.verify(txScriptStorage, Mockito.times(1)).saveAll(txScriptArgCaptor.capture());
        List<TxScript> txScriptList = txScriptArgCaptor.getValue();

        assertThat(txScriptList.get(0).getTxHash()).isEqualTo("b75ec46c406113372efeb1e57d9880856c240c9b531e3c680c1c4d8bf2253625");
        assertThat(txScriptList.get(0).getSlot()).isEqualTo(86420);
        assertThat(txScriptList.get(0).getBlockHash()).isEqualTo("664b6ec8a708b9cf90b87c904e688477887b55cbf4ee6c36877166a2ef216665");
        assertThat(txScriptList.get(0).getRedeemerCbor()).isEqualTo("840100d87980821a000d726e1a1018f056");
        assertThat(txScriptList.get(0).getUnitMem()).isEqualTo(new BigInteger("881262"));
        assertThat(txScriptList.get(0).getUnitSteps()).isEqualTo(new BigInteger("270069846"));
        assertThat(txScriptList.get(0).getPurpose()).isEqualTo(RedeemerTag.Mint);
        assertThat(txScriptList.get(0).getRedeemerIndex()).isEqualTo(0);
        assertThat(txScriptList.get(0).getBlockNumber()).isEqualTo(47);
        assertThat(txScriptList.get(0).getBlockTime()).isEqualTo(1655769620);

        Mockito.verify(publisher, Mockito.times(1)).publishEvent(txScriptPublisherArgCaptor.capture());
        TxScriptEvent txScriptEvent = txScriptPublisherArgCaptor.getValue();

        assertThat(txScriptEvent.getEventMetadata().isMainnet()).isEqualTo(false);
        assertThat(txScriptEvent.getEventMetadata().getProtocolMagic()).isEqualTo(1);
        assertThat(txScriptEvent.getEventMetadata().getEra()).isEqualTo(Era.Shelley);
        assertThat(txScriptEvent.getEventMetadata().getSlotLeader()).isEqualTo("d15422b2e8b60e500a82a8f4ceaa98b04e55a0171d1125f6c58f8758");
        assertThat(txScriptEvent.getEventMetadata().getEpochNumber()).isEqualTo(4);
        assertThat(txScriptEvent.getEventMetadata().getBlock()).isEqualTo(47);
        assertThat(txScriptEvent.getEventMetadata().getBlockHash()).isEqualTo("664b6ec8a708b9cf90b87c904e688477887b55cbf4ee6c36877166a2ef216665");
        assertThat(txScriptEvent.getEventMetadata().getBlockTime()).isEqualTo(1655769620);
        assertThat(txScriptEvent.getEventMetadata().getPrevBlockHash()).isEqualTo("c971bfb21d2732457f9febf79d9b02b20b9a3bef12c561a78b818bcb8b35a574");
        assertThat(txScriptEvent.getEventMetadata().getSlot()).isEqualTo(86420);
        assertThat(txScriptEvent.getEventMetadata().getEpochSlot()).isEqualTo(20);
        assertThat(txScriptEvent.getEventMetadata().getNoOfTxs()).isEqualTo(1);
        assertThat(txScriptEvent.getEventMetadata().isSyncMode()).isEqualTo(false);
        assertThat(txScriptEvent.getEventMetadata().isParallelMode()).isEqualTo(false);
        assertThat(txScriptEvent.getEventMetadata().isRemotePublish()).isEqualTo(true);
        assertThat(txScriptEvent.getTxScriptList().get(0).getTxHash()).isEqualTo("b75ec46c406113372efeb1e57d9880856c240c9b531e3c680c1c4d8bf2253625");
        assertThat(txScriptEvent.getTxScriptList().get(0).getSlot()).isEqualTo(86420);
        assertThat(txScriptEvent.getTxScriptList().get(0).getBlockHash()).isEqualTo("664b6ec8a708b9cf90b87c904e688477887b55cbf4ee6c36877166a2ef216665");
        assertThat(txScriptEvent.getTxScriptList().get(0).getRedeemerCbor()).isEqualTo("840100d87980821a000d726e1a1018f056");
        assertThat(txScriptEvent.getTxScriptList().get(0).getUnitMem()).isEqualTo(new BigInteger("881262"));
        assertThat(txScriptEvent.getTxScriptList().get(0).getUnitSteps()).isEqualTo(new BigInteger("270069846"));
        assertThat(txScriptEvent.getTxScriptList().get(0).getPurpose()).isEqualTo(RedeemerTag.Mint);
        assertThat(txScriptEvent.getTxScriptList().get(0).getRedeemerIndex()).isEqualTo(0);
        assertThat(txScriptEvent.getTxScriptList().get(0).getBlockNumber()).isEqualTo(47);
        assertThat(txScriptEvent.getTxScriptList().get(0).getBlockTime()).isEqualTo(1655769620);
    }

    private List<BatchBlock> mockBlockCaches() {
        EventMetadata metadata = EventMetadata.builder()
                .mainnet(false)
                .protocolMagic(1)
                .era(Era.Shelley)
                .slotLeader("d15422b2e8b60e500a82a8f4ceaa98b04e55a0171d1125f6c58f8758")
                .epochNumber(4)
                .block(47)
                .blockHash("664b6ec8a708b9cf90b87c904e688477887b55cbf4ee6c36877166a2ef216665")
                .blockTime(1655769620)
                .prevBlockHash("c971bfb21d2732457f9febf79d9b02b20b9a3bef12c561a78b818bcb8b35a574")
                .slot(86420)
                .epochSlot(20)
                .noOfTxs(1)
                .syncMode(false)
                .remotePublish(true)
                .parallelMode(false)
                .build();

        List<TransactionBody> transactionBodies = new ArrayList<>();
        Set<TransactionInput> inputs = new HashSet<>();
        inputs.add(TransactionInput.builder()
                        .transactionId("5526b1373acfc774794a62122f95583ff17febb2ca8a0fe948d097e29cf99099")
                        .index(0)
                .build());

        List<Amount> amounts = new ArrayList<>();
        amounts.add(Amount.builder()
                        .unit("lovelace")
                        .assetName("lovelace")
                        .quantity(new BigInteger("29999999999800000"))
                .build());

        List<TransactionOutput> outputs = new ArrayList<>();
        outputs.add(TransactionOutput.builder()
                        .address("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket")
                        .amounts(amounts)
                .build());

        List<Amount> mints = new ArrayList<>();
        mints.add(Amount.builder()
                .unit("1cf569e1ec3e0fee92f1f5002bfd4213b796c151c708db46e6e2d3a4.")
                .policyId("1cf569e1ec3e0fee92f1f5002bfd4213b796c151c708db46e6e2d3a4")
                .assetName("")
                .quantity(new BigInteger("1"))
                .build());

        TransactionBody transactionBody = TransactionBody.builder()
                .txHash("b75ec46c406113372efeb1e57d9880856c240c9b531e3c680c1c4d8bf2253625")
                .inputs(inputs)
                .outputs(outputs)
                .fee(new BigInteger("200000"))
                .ttl(90000)
                .certificates(new ArrayList<>())
                .mint(mints)
                .netowrkId(0)
                .validityIntervalStart(0)
                .build();
        transactionBodies.add(transactionBody);

        List<BootstrapWitness> bootstrapWitnesses = new ArrayList<>();
        bootstrapWitnesses.add(BootstrapWitness.builder()
                        .publicKey("7229838c0d5c5baaf0f09138853a52c7e7088bda6aa497af5606e5de5120ef99")
                        .signature("a6ace79954aeec7630c4c9376110a1c710d11af30eb056394aee03968251845d6fa1406c23f753b359068082d47bcf985fd14bf21feb8067bcf2234726578d0f")
                        .chainCode("cd37d942b4eb28d5435df4d4e18bc7ed11388fe2ea43615f544f7c09c7b29f25")
                        .attributes("a1024101")
                .build());

        List<Redeemer> redeemers = new ArrayList<>();
        redeemers.add(Redeemer.builder()
                .tag(RedeemerTag.Mint)
                .index(0)
                .data(Datum.builder().build())
                .exUnits(new ExUnits(new BigInteger("881262"), new BigInteger("270069846")))
                .cbor("840100d87980821a000d726e1a1018f056")
                .build());

        List<PlutusScript> plutusV2Scripts = new ArrayList<>();
        plutusV2Scripts.add(PlutusScript.builder()
                .type("2")
                .content("590c7e590c7b01000033232323232323232323233223233223232323232323322323232323232323232323232323232323232323232323232323232323355501722232325335330053333573466e1cd55ce9baa0044800080c88c98c80c8cd5ce00c8190181999ab9a3370e6aae7540092000233221233001003002323232323232323232323232323333573466e1cd55cea8062400046666666666664444444444442466666666666600201a01801601401201000e00c00a00800600466a0320346ae854030cd4064068d5d0a80599a80c80d9aba1500a3335501d75ca0386ae854024ccd54075d7280e1aba1500833501902235742a00e666aa03a046eb4d5d0a8031919191999ab9a3370e6aae75400920002332212330010030023232323333573466e1cd55cea8012400046644246600200600466a05aeb4d5d0a80118171aba135744a004464c6409066ae700bc1201184d55cf280089baa00135742a0046464646666ae68cdc39aab9d5002480008cc8848cc00400c008cd40b5d69aba15002302e357426ae8940088c98c8120cd5ce01782402309aab9e5001137540026ae84d5d1280111931902219ab9c02b044042135573ca00226ea8004d5d0a80299a80cbae35742a008666aa03a03e40026ae85400cccd54075d710009aba150023021357426ae8940088c98c8100cd5ce01382001f09aba25001135744a00226ae8940044d5d1280089aba25001135744a00226ae8940044d5d1280089aba25001135744a00226aae7940044dd50009aba150023011357426ae8940088c98c80c8cd5ce00c819018081889a817a4810350543500135573ca00226ea8004cd55405c888c8c8c94cd4c008c8d4004888888888888031400454cd4cd540a0cd5406809cd401888004cd540a08004c025400454cd54cd4cd540a0cd5407009d400cc8004c0254004854cd4ccc88cd54008c8cd409088ccd400c88008008004d40048800448cc004894cd4008400440c40c0004c08048004cd5540788cc0a000520022350012200100113355301c12001235001220020011302e4984c0b5261302a4988854cd400454cd4cc0a4008c8d400488008c848cc0040f0008cdc5a41fc0666e2940d540d44cd540a894cd400440bc4cd5ce249256572726f7220276d6b44734b6579506f6c6963792720696c6c6567616c206f7574707574730002e5335323335530221200133502322533500221003100150262533533320015025300c001300e302200a13502800115027001323500122222222222200a500321335502b335501d02a5006335502b2001300a001102d1302c498884c0b926130294988854cd400440b8884c0b52613500322002320013550372253350011502f2322321533533320015025300c5003300e302200a15335335502c335502002b500732001300b500321533500113002498884d400888cd40dc008c02c01c4c00526130014988c0180084d4004880044d400488cccd40048c98c80c8cd5ce2481024c680003220012326320323357389201024c68000322326320323357389201024c6800032232323333573466e1cd55cea801240004664424660020060046eb8d5d0a8011bae357426ae8940088c98c80c0cd5ce00b81801709aab9e50011375400246a002444400646a002444400846a0024444444444440104660326038002a0342464460046eb0004c8004d540bc88cccd55cf80092814119a81398021aba1002300335744004054464646666ae68cdc39aab9d5002480008cc8848cc00400c008c028d5d0a80118029aba135744a004464c6405466ae700440a80a04d55cf280089baa0012323232323333573466e1cd55cea8022400046666444424666600200a0080060046464646666ae68cdc39aab9d5002480008cc07cc04cd5d0a80119a8068091aba135744a004464c6405e66ae700580bc0b44d55cf280089baa00135742a008666aa010eb9401cd5d0a8019919191999ab9a3370ea0029002119091118010021aba135573ca00646666ae68cdc3a80124004464244460020086eb8d5d09aab9e500423333573466e1d400d20002122200323263203133573803006205e05c05a26aae7540044dd50009aba1500233500975c6ae84d5d1280111931901599ab9c01202b029135744a00226ae8940044d55cf280089baa0011335500175ceb44488c88c008dd5800990009aa81611191999aab9f00225026233502533221233001003002300635573aa004600a6aae794008c010d5d100181409aba100112232323333573466e1d400520002350193005357426aae79400c8cccd5cd19b87500248008940648c98c80a0cd5ce00781401301289aab9d500113754002464646666ae68cdc3a800a400c46424444600800a600e6ae84d55cf280191999ab9a3370ea004900211909111180100298049aba135573ca00846666ae68cdc3a801a400446424444600200a600e6ae84d55cf280291999ab9a3370ea00890001190911118018029bae357426aae7940188c98c80a0cd5ce00781401301281201189aab9d500113754002464646666ae68cdc39aab9d5002480008cc8848cc00400c008c014d5d0a8011bad357426ae8940088c98c8090cd5ce00581201109aab9e5001137540024646666ae68cdc39aab9d5001480008dd71aba135573ca004464c6404466ae700240880804dd5000919191919191999ab9a3370ea002900610911111100191999ab9a3370ea004900510911111100211999ab9a3370ea00690041199109111111198008048041bae35742a00a6eb4d5d09aba2500523333573466e1d40112006233221222222233002009008375c6ae85401cdd71aba135744a00e46666ae68cdc3a802a400846644244444446600c01201060186ae854024dd71aba135744a01246666ae68cdc3a8032400446424444444600e010601a6ae84d55cf280591999ab9a3370ea00e900011909111111180280418071aba135573ca018464c6405666ae700480ac0a40a009c09809409008c4d55cea80209aab9e5003135573ca00426aae7940044dd50009191919191999ab9a3370ea002900111999110911998008028020019bad35742a0086eb4d5d0a8019bad357426ae89400c8cccd5cd19b875002480008c8488c00800cc020d5d09aab9e500623263202433573801604804404226aae75400c4d5d1280089aab9e500113754002464646666ae68cdc3a800a4004460266eb8d5d09aab9e500323333573466e1d400920002321223002003375c6ae84d55cf280211931901099ab9c00802101f01e135573aa00226ea8004488c8c8cccd5cd19b87500148010848880048cccd5cd19b875002480088c84888c00c010c018d5d09aab9e500423333573466e1d400d20002122200223263202233573801204404003e03c26aae7540044dd50009191999ab9a3370ea0029001100b91999ab9a3370ea0049000100b91931900f19ab9c00501e01c01b135573a6ea800524010350543100112232230020013200135502122533500110152213500222533533008002007101a130060033200135501e22112253350011501822133501930040023355300612001004001112232230020013200135501f2253350011500b22135002225335330080020071350100011300600311122230033002001235001220023200135501a221122253350011350032200122133350052200230040023335530071200100500400112212330010030021223500222350032232335005233500425335333573466e3c00800405004c5400c404c804c8cd4010804c94cd4ccd5cd19b8f002001014013150031013153350032153350022133500223350022335002233500223301200200120162335002201623301200200122201622233500420162225335333573466e1c01800c06406054cd4ccd5cd19b870050020190181330130040011018101810111533500121011101122123300100300212122300200311220012122300100322333573466e1c00800402001c88ccd5cd19b8f0020010070061122300200123500849012f6572726f7220276d6b44734b6579506f6c696379272062616420696e7075747320696e207472616e73616374696f6e002350074901266572726f7220276d6b44734b6579506f6c696379272062616420696e697469616c206d696e74001220021220012350044901286572726f7220276d6b44734b6579506f6c696379273a20626164206d696e74656420746f6b656e730011220021221223300100400312326320033357380020069309000899b8a50015001133714a002a002266e29400540044cdc52800a800899b8b483f80c005220100112323001001223300330020020014c140d8799f581c4aa93779bf0953b3d43adfa530fa090f928bc06541b8be5b039221d9581c1cf569e1ec3e0fee92f1f5002bfd4213b796c151c708db46e6e2d3a4ff0001")
                .build());

        List<NativeScript> nativeScripts = new ArrayList<>();
        nativeScripts.add(NativeScript.builder()
                .content("{\n" +
                        "  \"type\" : \"sig\",\n" +
                        "  \"keyHash\" : \"998cb92c7751cddd467e485865fb9374c03f734b2d374938e10436ce\"\n" +
                        "}")
                .type(0)
                .build());

        Witnesses witnesses = Witnesses.builder()
                .vkeyWitnesses(new ArrayList<>())
                .nativeScripts(nativeScripts)
                .bootstrapWitnesses(bootstrapWitnesses)
                .plutusV1Scripts(new ArrayList<>())
                .datums(new ArrayList<>())
                .redeemers(redeemers)
                .plutusV2Scripts(plutusV2Scripts)
                .plutusV3Scripts(new ArrayList<>())
                .build();

        List<Witnesses> transactionWitness = new ArrayList<>();
        transactionWitness.add(witnesses);

        Block block = Block.builder()
                .era(Era.Shelley)
                .header(BlockHeader.builder()
                        .headerBody(HeaderBody.builder()
                                .blockNumber(47)
                                .slot(86420)
                                .prevHash("c971bfb21d2732457f9febf79d9b02b20b9a3bef12c561a78b818bcb8b35a574")
                                .issuerVkey("942bb3aaab0f6442b906b65ba6ddbf7969caa662d90968926211a3d56532f11d")
                                .vrfVkey("85504e79a04c8243a5d29b798cbc32a1548f9bace061fad7ca3f4c83ae1809b5")
                                .nonceVrf(VrfCert.builder()
                                        ._1("5c75c5d031837d66ccb85e80f92bffcc4d246407fa6d9c83fc7111fd83e7db3065dd66ec33d14357650437623f2c41202ce3c59b6ec04a85b0f7f9753492cd15")
                                        ._2("b70e7213f08ed8f1faa900bf12527ee6f2bf7aba106f118f79bda114af3b4da1d3ea9a344954ed548865d76b9218ff5461acbb456287e8044cd0994bba1a24e6829153ffcfe8e513908f2aca2437b909")
                                        .build())
                                .leaderVrf(VrfCert.builder()
                                        ._1("f7f58358379ec7edee57632e1e4464de1d10d3c01ae3ba9c55a11b86e381183cc5e1168a8a47645f4a06d9c26f4d937761c84a9eb692ccaa2ab58ff0dd9318de")
                                        ._2("8759a87665b4ec77c6bf248ebf62e5f2b0a68d0ce9d8f1130f3329dbf8623440443d39931ad906438ab1bdb1b1e7bf64ae9544cef9f60aacfda73275cd19e2916e9e93d0cba52368b5d9dce2ec7e2109")
                                        .build())
                                .blockBodySize(240)
                                .blockBodyHash("f6e96054b901cf33da8be221747bad47ffae75702b58a69d0329473189e0289b")
                                .operationalCert(OperationalCert.builder()
                                        .hotVKey("54914d6210ca26a038509e3f5b01eeec60719766ec2f839e2c8b2edcb39e4847")
                                        .sequenceNumber(0)
                                        .kesPeriod(0)
                                        .sigma("2afe39cf023b5f25961b2e4b9148d533c6677c9c48b81e5d5a2cf52e32040f77683e81f0722baa5dea2ba0a425c5b43e4bb912c34a2f826ef4cc639cb3778a0f")
                                        .build())
                                .protocolVersion(ProtocolVersion.builder()
                                        ._1(3)
                                        ._2(0)
                                        .build())
                                .blockHash("664b6ec8a708b9cf90b87c904e688477887b55cbf4ee6c36877166a2ef216665")
                                .build())
                        .bodySignature("7c9c1084b7671c733285687d885e726463297d04d91552e84828681850fa4c4bd2ff9238901b927935f3ae583de9b590f291d8a96a2d298baf7b1a06403a89011392f7abbdba2e3da2655912c652ab86b7a8cee88c35154d850202b300e8b02966e9a216299622fcb209d0ec5189dd6aa2b11abbc64393be59c7301e633d2df9df83ed37ad9d8ed510ee48292b88974a51e3cb822d8f655f867fd7a6d7b5cf0c58bd1a3e4ef7d9ea8b3f621ce06213c0fb4a8869e4f3ef854c4a9b8721d67bc92e11dca957f9514e87cd1a5e7948810c297d29ced787b0b213b4c259c45ddecce0d25f46028ee9fbda26c47525729420d8bb73511514bc8f8194368686991da9ba35cc7e2d4d02bed059ae157af907a250104e8c7dea2a990ba8ff2f9e3562e97214faa0ab37de93456bb6181fdc71adc9686ed0d377c280a974d634e5334c85115446563e4b0f8afac494d4ab53a7e1ce8e7ded2bdc7e8a92ba9b17eeaabc5c68c67ab091bb0961c81bfe464c400900f317a1015cc7150d2c0a4c0854f15555a8f3bdf04371901a4761caf47837b789df274ffe4d00150e8272b7eb6582d70c2058d490334522c129791172ebecfce136db66cec20a0e332f4df8c9964122e6")
                        .build())
                .transactionBodies(transactionBodies)
                .transactionWitness(transactionWitness)
                .auxiliaryDataMap(new HashMap<>())
                .build();

        List<Utxo> utxos = new ArrayList<>();
        utxos.add(Utxo.builder()
                        .txHash("b75ec46c406113372efeb1e57d9880856c240c9b531e3c680c1c4d8bf2253625")
                        .index(0)
                        .address("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket")
                        .amounts(amounts)
                .build());

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(Transaction.builder()
                        .blockNumber(47)
                        .slot(86420)
                        .txHash("b75ec46c406113372efeb1e57d9880856c240c9b531e3c680c1c4d8bf2253625")
                        .body(transactionBody)
                        .utxos(utxos)
                        .witnesses(witnesses)
                        .invalid(false)
                .build());

        List<BatchBlock> batchBlocks = new ArrayList<>();
        batchBlocks.add(new BatchBlock(
                metadata, block, transactions));

        return batchBlocks;
    }
}
