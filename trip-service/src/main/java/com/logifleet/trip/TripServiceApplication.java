package com.logifleet.trip;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;

import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class TripServiceApplication {

    public static void main(String[] a) {
        SpringApplication.run(
                TripServiceApplication.class,
                a
        );
    }
}