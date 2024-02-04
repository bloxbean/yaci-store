package com.bloxbean.cardano.yaci.store.adapot.processor;

import com.bloxbean.cardano.yaci.core.model.certs.*;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.adapot.domain.Deposit;
import com.bloxbean.cardano.yaci.store.adapot.domain.DepositType;
import com.bloxbean.cardano.yaci.store.adapot.service.ProtocolParamService;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class DepositRules {

    private ProtocolParamService protocolParamService;

    public List<Deposit> findDepositAndRefund(EventMetadata metadata, Transaction transaction) {
        var certificates = transaction.getBody().getCertificates();
        if (certificates == null || certificates.isEmpty())
            return null;

        List<Deposit> depositRefundList = null;
        int index = 0;
        for (Certificate certificate : certificates) {
            var deposit = getDeposit(transaction.getTxHash(), index, certificate, metadata);
            var refund = getRefund(transaction.getTxHash(), index, certificate, metadata);
            index++;

            if (deposit.isPresent()) {
                if (depositRefundList == null)
                    depositRefundList = new ArrayList<>();
                depositRefundList.add(deposit.get());
            }

            if (refund.isPresent()) {
                if (depositRefundList == null)
                    depositRefundList = new ArrayList<>();
                depositRefundList.add(refund.get());
            }
        }

        return depositRefundList;
    }

    private Optional<Deposit> getDeposit(String txHash, int certIndex, Certificate certificate, EventMetadata metadata) {
        CertificateType certType = certificate.getType();

        switch (certType) {
            case STAKE_REGISTRATION, REG_CERT, STAKE_REG_DELEG_CERT, VOTE_REG_DELEG_CERT:
                StakeRegistration stakeRegistration;
                if (certType == CertificateType.STAKE_REGISTRATION) {
                    stakeRegistration = (StakeRegistration) certificate;
                } else if (certType == CertificateType.REG_CERT) {
                    stakeRegistration = StakeRegistration.builder()
                            .stakeCredential(((RegCert) certificate).getStakeCredential()).build();
                } else if (certType == CertificateType.STAKE_REG_DELEG_CERT) {
                    stakeRegistration = StakeRegistration.builder()
                            .stakeCredential(((StakeRegDelegCert) certificate).getStakeCredential())
                            .build();
                } else {
                    stakeRegistration = StakeRegistration.builder()
                            .stakeCredential(((VoteRegDelegCert) certificate).getStakeCredential())
                            .build();
                }

                return Optional.of(buildStakeKeyDeposit(txHash, certIndex, stakeRegistration, metadata));
            case POOL_REGISTRATION:
                PoolRegistration poolRegistration = (PoolRegistration) certificate;
                var poolDeposit = Deposit.builder()
                        .txHash(txHash)
                        .certIndex(certIndex)
                        .poolId(poolRegistration.getPoolParams().getOperator())
                        .depositType(DepositType.POOL_DEPOSIT)
                        .amount(protocolParamService.getPoolDeposit()) //TODO
                        .epoch(metadata.getEpochNumber())
                        .slot(metadata.getSlot())
                        .blockNumber(metadata.getBlock())
                        .blockHash(metadata.getBlockHash())
                        .blockTime(metadata.getBlockTime())
                        .build();

                return Optional.of(poolDeposit);
//            case REG_DREP_CERT:
//                var drepRegCert = (RegDRepCert) certificate;
//
//                Address address =
//                        AddressProvider.getRewardAddress(drepRegCert.getDrepCredential(),
//                                metadata.isMainnet()? Networks.mainnet(): Networks.testnet());
//
//                var drepDeposit = Deposit.builder()
//                        .address(address.toBech32())
//                        .depositType(DepositType.DREP_DEPOSIT)
//                        .txHash(txHash)
//                        .epoch(metadata.getEpochNumber())
//                        .slot(metadata.getSlot())
//                        .block(metadata.getBlock())
//                        .build();
//                return Optional.of(drepDeposit);
            default:
                return Optional.empty();
        }
    }

    public DepositRules(ProtocolParamService protocolParamService) {
        this.protocolParamService = protocolParamService;
    }

    private Optional<Deposit> getRefund(String txHash, int certIndex, Certificate certificate, EventMetadata metadata) {
        CertificateType certType = certificate.getType();

        switch (certType) {
            case STAKE_DEREGISTRATION:
                StakeDeregistration stakeDeregistration = (StakeDeregistration) certificate;

                var regRefund = Deposit.builder()
                        .txHash(txHash)
                        .certIndex(certIndex)
                        .credential(stakeDeregistration.getStakeCredential().getHash())
                        .credType(getCredType(stakeDeregistration.getStakeCredential()))
                        .depositType(DepositType.STAKE_KEY_REG_DEPOSIT)
                        .amount(protocolParamService.getKeyDeposit().negate()) //TODO
                        .epoch(metadata.getEpochNumber())
                        .slot(metadata.getSlot())
                        .blockNumber(metadata.getBlock())
                        .blockHash(metadata.getBlockHash())
                        .blockTime(metadata.getBlockTime())
                        .build();

                return Optional.of(regRefund);

            case POOL_RETIREMENT:
                PoolRetirement poolRetirement = (PoolRetirement) certificate;
                var poolRefund = Deposit.builder()
                        .txHash(txHash)
                        .certIndex(certIndex)
                        .poolId(poolRetirement.getPoolKeyHash())
                        .depositType(DepositType.POOL_DEPOSIT)
                        .amount(protocolParamService.getPoolDeposit().negate()) //TODO
                        .txHash(txHash)
                        .epoch(metadata.getEpochNumber())
                        .slot(metadata.getSlot())
                        .blockNumber(metadata.getBlock())
                        .blockHash(metadata.getBlockHash())
                        .blockTime(metadata.getBlockTime())
                        .build();

                return Optional.of(poolRefund);

//            case UNREG_DREP_CERT:
//                UnregDrepCert unregDrepCert = (UnregDrepCert) certificate;
//                Credential credential = getCclCredential(unregDrepCert.getDrepCredential());

            default:
                return Optional.empty();
        }
    }

    private Deposit buildStakeKeyDeposit(String txHash, int certIndex, StakeRegistration stakeRegistration,
                                         EventMetadata metadata) {
        return Deposit.builder()
                .txHash(txHash)
                .certIndex(certIndex)
                .credential(stakeRegistration.getStakeCredential().getHash())
                .credType(getCredType(stakeRegistration.getStakeCredential()))
                .depositType(DepositType.STAKE_KEY_REG_DEPOSIT)
                .amount(protocolParamService.getKeyDeposit()) //TODO
                .epoch(metadata.getEpochNumber())
                .slot(metadata.getSlot())
                .blockNumber(metadata.getBlock())
                .blockHash(metadata.getBlockHash())
                .blockTime(metadata.getBlockTime())
                .build();
    }


    private com.bloxbean.cardano.yaci.core.model.CredentialType getCredType(StakeCredential stakeCredential) {
        if (stakeCredential.getType() == StakeCredType.ADDR_KEYHASH)
            return com.bloxbean.cardano.yaci.core.model.CredentialType.ADDR_KEYHASH;
        else if (stakeCredential.getType() == StakeCredType.SCRIPTHASH)
            return com.bloxbean.cardano.yaci.core.model.CredentialType.SCRIPTHASH;
        else
            return null;
    }

}
