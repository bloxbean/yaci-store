package com.bloxbean.cardano.yaci.store.admin.cli;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
@Slf4j
public class YaciStoreAdminCliApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(YaciStoreAdminCliApplication.class)
                .logStartupInfo(true)
                .run(args);
    }


}
