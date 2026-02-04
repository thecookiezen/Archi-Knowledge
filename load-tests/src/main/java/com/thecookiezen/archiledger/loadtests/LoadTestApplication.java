package com.thecookiezen.archiledger.loadtests;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@SpringBootApplication(scanBasePackages = "com.thecookiezen.archiledger")
@EnableNeo4jRepositories(basePackages = "com.thecookiezen.archiledger")
public class LoadTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoadTestApplication.class, args);
    }
}
