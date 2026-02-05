package com.thecookiezen.archiledger.infrastructure.config;

import com.ladybugdb.Connection;
import com.ladybugdb.Database;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LadybugDBConfig {

    @Bean
    public Database database() {
        return new Database();
    }

    // @Bean
    // public Connection connection(Database database) {
    // // return database.connect();
    // }
}
