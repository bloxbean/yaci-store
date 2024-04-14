package com.bloxbean.cardano.yaci.store.api.blocks.dto;

import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import org.springframework.stereotype.Component;

import static com.bloxbean.cardano.yaci.store.common.util.Bech32Prefixes.VRF_VK_PREFIX;

@Component
public class BlockDtoMapper {

    public BlockDto toBlockDto(Block block) {
        if (block == null)
            return null;

        String vrfVkey = block.getVrfVkey() != null? Bech32.encode(HexUtil.decodeHexString(block.getVrfVkey()), VRF_VK_PREFIX)
                : null;

        return BlockDto.builder()
                .time(block.getBlockTime())
                .height(block.getNumber())
                .number(block.getNumber())
                .hash(block.getHash())
                .slot(block.getSlot())
                .epoch(block.getEpochNumber())
                .era(block.getEra())
                .epochSlot(block.getEpochSlot())
                .slotLeader(block.getSlotLeader())
                .size(block.getBlockBodySize())
                .txCount(block.getNoOfTxs())
                .output(block.getTotalOutput())
                .fees(block.getTotalFees())
                .blockVrf(vrfVkey)
                .opCert(block.getOpCertHotVKey())
                .opCertCounter(block.getOpCertSeqNumber())
                .opCertKesPeriod(block.getOpcertKesPeriod())
                .opCertSigma(block.getOpCertSigma())
                .previousBlock(block.getPrevHash())
                //.nextBlock(block.getNextBlock()) TODO -- next block
                //.confirmations(block.getConfirmations()) TODO -- confirmations
                .issuerVkey(block.getIssuerVkey())
                .nonceVrf(block.getNonceVrf())
                .leaderVrf(block.getLeaderVrf())
                .vrfResult(block.getVrfResult())
                .blockBodyHash(block.getBlockBodyHash())
                .protocolVersion(block.getProtocolVersion())
                .build();
    }
}
