package com.bloxbean.cardano.yaci.store.blocks.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.model.BlockEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:16+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class BlockMapperImpl extends BlockMapper {

    @Override
    public Block toBlock(BlockEntity blockEntity) {
        if ( blockEntity == null ) {
            return null;
        }

        Block.BlockBuilder block = Block.builder();

        block.blockBodyHash( blockEntity.getBlockBodyHash() );
        if ( blockEntity.getBlockBodySize() != null ) {
            block.blockBodySize( blockEntity.getBlockBodySize() );
        }
        block.blockTime( blockEntity.getBlockTime() );
        block.epochNumber( blockEntity.getEpochNumber() );
        block.epochSlot( blockEntity.getEpochSlot() );
        block.era( blockEntity.getEra() );
        block.hash( blockEntity.getHash() );
        block.issuerVkey( blockEntity.getIssuerVkey() );
        block.leaderVrf( blockEntity.getLeaderVrf() );
        if ( blockEntity.getNoOfTxs() != null ) {
            block.noOfTxs( blockEntity.getNoOfTxs() );
        }
        block.nonceVrf( blockEntity.getNonceVrf() );
        block.number( blockEntity.getNumber() );
        block.opCertHotVKey( blockEntity.getOpCertHotVKey() );
        block.opCertSeqNumber( blockEntity.getOpCertSeqNumber() );
        block.opCertSigma( blockEntity.getOpCertSigma() );
        block.opcertKesPeriod( blockEntity.getOpcertKesPeriod() );
        block.prevHash( blockEntity.getPrevHash() );
        block.protocolVersion( blockEntity.getProtocolVersion() );
        block.slot( blockEntity.getSlot() );
        block.slotLeader( blockEntity.getSlotLeader() );
        block.totalFees( blockEntity.getTotalFees() );
        block.totalOutput( blockEntity.getTotalOutput() );
        block.vrfResult( blockEntity.getVrfResult() );
        block.vrfVkey( blockEntity.getVrfVkey() );

        return block.build();
    }

    @Override
    public BlockEntity toBlockEntity(Block blockDetails) {
        if ( blockDetails == null ) {
            return null;
        }

        BlockEntity.BlockEntityBuilder blockEntity = BlockEntity.builder();

        blockEntity.blockBodyHash( blockDetails.getBlockBodyHash() );
        blockEntity.blockBodySize( blockDetails.getBlockBodySize() );
        blockEntity.blockTime( blockDetails.getBlockTime() );
        blockEntity.epochNumber( blockDetails.getEpochNumber() );
        blockEntity.epochSlot( blockDetails.getEpochSlot() );
        blockEntity.era( blockDetails.getEra() );
        blockEntity.hash( blockDetails.getHash() );
        blockEntity.issuerVkey( blockDetails.getIssuerVkey() );
        blockEntity.leaderVrf( blockDetails.getLeaderVrf() );
        blockEntity.noOfTxs( blockDetails.getNoOfTxs() );
        blockEntity.nonceVrf( blockDetails.getNonceVrf() );
        blockEntity.number( blockDetails.getNumber() );
        blockEntity.opCertHotVKey( blockDetails.getOpCertHotVKey() );
        blockEntity.opCertSeqNumber( blockDetails.getOpCertSeqNumber() );
        blockEntity.opCertSigma( blockDetails.getOpCertSigma() );
        blockEntity.opcertKesPeriod( blockDetails.getOpcertKesPeriod() );
        blockEntity.prevHash( blockDetails.getPrevHash() );
        blockEntity.protocolVersion( blockDetails.getProtocolVersion() );
        blockEntity.slot( blockDetails.getSlot() );
        blockEntity.slotLeader( blockDetails.getSlotLeader() );
        blockEntity.totalFees( blockDetails.getTotalFees() );
        blockEntity.totalOutput( blockDetails.getTotalOutput() );
        blockEntity.vrfResult( blockDetails.getVrfResult() );
        blockEntity.vrfVkey( blockDetails.getVrfVkey() );

        return blockEntity.build();
    }
}
