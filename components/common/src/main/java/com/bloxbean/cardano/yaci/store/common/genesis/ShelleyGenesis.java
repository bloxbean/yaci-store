package com.bloxbean.cardano.yaci.store.common.genesis;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.events.GenesisBalance;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.ToString;

import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Data
@ToString
public class ShelleyGenesis extends GenesisFile {
    public static final String ATTR_SYSTEM_START = "systemStart";
    public static final String ATTR_SLOT_LENGTH = "slotLength";
    public static final String ATTR_ACTIVE_SLOTS_COEFF = "activeSlotsCoeff";
    public static final String ATTR_MAX_LOVELACE_SUPPLY = "maxLovelaceSupply";
    public static final String ATTR_EPOCH_LENGTH = "epochLength";
    public static final String ATTR_NETWORK_MAGIC = "networkMagic";
    public static final String MIN_FEE_A = "minFeeA";
    public static final String MIN_FEE_B = "minFeeB";
    public static final String MAX_BLOCK_BODY_SIZE = "maxBlockBodySize";
    public static final String MAX_TX_SIZE = "maxTxSize";
    public static final String MAX_BLOCK_HEADER_SIZE = "maxBlockHeaderSize";
    public static final String KEY_DEPOSIT = "keyDeposit";
    public static final String POOL_DEPOSIT = "poolDeposit";
    public static final String E_MAX = "eMax";
    public static final String N_OPT = "nOpt";
    public static final String A_0 = "a0";
    public static final String RHO = "rho";
    public static final String TAU = "tau";
    public static final String DECENTRALISATION_PARAM = "decentralisationParam";
    public static final String EXTRA_ENTROPY = "extraEntropy";
    public static final String TAG = "tag";
    public static final String PROTOCOL_VERSION = "protocolVersion";
    public static final String MAJOR = "major";
    public static final String MINOR = "minor";
    public static final String MIN_U_TX_O_VALUE = "minUTxOValue";
    public static final String MIN_POOL_COST = "minPoolCost";

    private static ObjectMapper objectMapper = new ObjectMapper();

    private String systemStart;
    private double slotLength;
    private double activeSlotsCoeff;
    private BigInteger maxLovelaceSupply;
    private long epochLength;
    private long networkMagic;

    private List<GenesisBalance> initialFunds;
    private ProtocolParams protocolParams;

    public ShelleyGenesis(File shelleyGenesisFile) {
        super(shelleyGenesisFile);
    }

    public ShelleyGenesis(InputStream in) {
        super(in);
    }

    public ShelleyGenesis(long protocolMagic) {
        super(protocolMagic);
    }

    @Override
    protected void readGenesisData(JsonNode genesisJson) {
        systemStart = genesisJson.get(ATTR_SYSTEM_START).asText();
        slotLength = genesisJson.get(ATTR_SLOT_LENGTH).asDouble();
        activeSlotsCoeff = genesisJson.get(ATTR_ACTIVE_SLOTS_COEFF).asDouble();
        maxLovelaceSupply = new BigInteger(genesisJson.get(ATTR_MAX_LOVELACE_SUPPLY).asText());
        epochLength = genesisJson.get(ATTR_EPOCH_LENGTH).asLong();
        networkMagic = genesisJson.get(ATTR_NETWORK_MAGIC).asLong();

        JsonNode initialFundJson = genesisJson.get("initialFunds");
        initialFunds = new ArrayList<>();
        if (initialFundJson != null && initialFundJson.fields().hasNext()) {
            initialFundJson.fields().forEachRemaining(entry -> {
                String address = entry.getKey();
                BigInteger amount = new BigInteger(entry.getValue().asText());

                String shelleyAddr;
                if (Networks.mainnet().getProtocolMagic() == networkMagic) { //mainnet address
                    shelleyAddr = new Address("addr", HexUtil.decodeHexString(address)).toBech32();
                } else {
                    shelleyAddr = new Address("addr_test", HexUtil.decodeHexString(address)).toBech32();
                }

                String txHash = HexUtil.encodeHexString(Blake2bUtil.blake2bHash256(HexUtil.decodeHexString(address)));
                initialFunds.add(new GenesisBalance(shelleyAddr, txHash, amount));
            });
        }

        readProtocolParameters(genesisJson.get("protocolParams"));
    }

    @Override
    protected String getFileName() {
        return "shelley-genesis.json";
    }

    public void readProtocolParameters(JsonNode protocolParamsNode) {
        if (protocolParamsNode == null)
            return;

        var minFeeA = protocolParamsNode.get(MIN_FEE_A).asInt();
        var minFeeB = protocolParamsNode.get(MIN_FEE_B).asInt();
        var maxBlockSize = protocolParamsNode.get(MAX_BLOCK_BODY_SIZE).asInt();
        var maxTxSize = protocolParamsNode.get(MAX_TX_SIZE).asInt();
        var maxBlockHeaderSize = protocolParamsNode.get(MAX_BLOCK_HEADER_SIZE).asInt();

        var keyDeposit = protocolParamsNode.get(KEY_DEPOSIT).bigIntegerValue();
        var poolDeposit = protocolParamsNode.get(POOL_DEPOSIT).bigIntegerValue();
        var maxEpoch = protocolParamsNode.get(E_MAX).asInt();
        var nOpt = protocolParamsNode.get(N_OPT).asInt();

        var poolPledgeInfluence = protocolParamsNode.get(A_0).decimalValue();
        var monetaryExpansionRate = protocolParamsNode.get(RHO).decimalValue();
        var treasuryGrowthRate = protocolParamsNode.get(TAU).decimalValue();

        var decentralisationParam = protocolParamsNode.get(DECENTRALISATION_PARAM).decimalValue();

        var extraEntropyNode = protocolParamsNode.get(EXTRA_ENTROPY);

        var extraEntropy = extraEntropyNode != null? extraEntropyNode.get(TAG).asText(): null;

        var protocolVersionNode = protocolParamsNode.get(PROTOCOL_VERSION);
        var majorVersion = protocolVersionNode.get(MAJOR).asInt();
        var minorVersion = protocolVersionNode.get(MINOR).asInt();

        var minUTxOValue = protocolParamsNode.get(MIN_U_TX_O_VALUE).bigIntegerValue();
        var minPoolCost = protocolParamsNode.get(MIN_POOL_COST).bigIntegerValue();

        protocolParams = ProtocolParams.builder()
                .minFeeA(minFeeA)
                .minFeeB(minFeeB)
                .maxBlockSize(maxBlockSize)
                .maxTxSize(maxTxSize)
                .maxBlockHeaderSize(maxBlockHeaderSize)
                .keyDeposit(keyDeposit)
                .poolDeposit(poolDeposit)
                .maxEpoch(maxEpoch)
                .nOpt(nOpt)
                .poolPledgeInfluence(poolPledgeInfluence)
                .expansionRate(monetaryExpansionRate)
                .treasuryGrowthRate(treasuryGrowthRate)
                .decentralisationParam(decentralisationParam)
                //.extraEntropy(extraEntropy)
                .protocolMajorVer(majorVersion)
                .protocolMinorVer(minorVersion)
                .minUtxo(minUTxOValue)
                .minPoolCost(minPoolCost)
                .build();
    }

}
