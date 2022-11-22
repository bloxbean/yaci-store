package com.bloxbean.cardano.yaci.indexer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@SpringBootApplication
@Slf4j
public class YaciIndexerApplication {

    @Autowired
    private ApplicationContext context;

    public static void main(String[] args) {
        SpringApplication.run(YaciIndexerApplication.class, args);
    }

    @PreDestroy
    public void onDestroy() throws Exception {
        log.info("Spring Container is destroyed!");
    }

    @PostConstruct
    public void postConstruct() {

    }

}
