package com.bloxbean.cardano.yaci.indexer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@SpringBootApplication
public class YaciIndexerApplication {

    public static void main(String[] args) {
        SpringApplication.run(YaciIndexerApplication.class, args);
    }

    @PreDestroy
    public void onDestroy() throws Exception {
        System.out.println("Spring Container is destroyed!");
    }

    @PostConstruct
    public void postConstruct() {

    }
}
