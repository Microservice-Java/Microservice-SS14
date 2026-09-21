package com.storex.saga;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChoreographySagaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChoreographySagaApplication.class, args);
    }
}
