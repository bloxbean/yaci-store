package com.bloxbean.cardano.yaci.store.staking;

import com.bloxbean.cardano.yaci.store.epoch.service.ProtocolParamService;
import com.bloxbean.cardano.yaci.store.staking.service.DepositParamService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringBootTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootTestApplication.class, args);
    }

    @Bean
    public DepositParamService depositParamService() {
        ProtocolParamService protocolParamService = new ProtocolParamService(null);
        return new DepositParamService(protocolParamService);
    }
}
