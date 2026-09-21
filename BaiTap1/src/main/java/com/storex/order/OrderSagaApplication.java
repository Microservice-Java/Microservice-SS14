package com.storex.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OrderSagaApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderSagaApplication.class, args);
    }
}
