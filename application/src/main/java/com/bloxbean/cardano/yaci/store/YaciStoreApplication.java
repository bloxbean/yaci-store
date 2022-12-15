package com.bloxbean.cardano.yaci.store;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class YaciStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(YaciStoreApplication.class, args);
    }

    @PreDestroy
    public void onDestroy() throws Exception {
        log.info("Spring Container is destroyed!");
    }

    @PostConstruct
    public void postConstruct() {

    }

}
