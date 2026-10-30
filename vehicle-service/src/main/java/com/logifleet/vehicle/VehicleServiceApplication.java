package com.logifleet.vehicle;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;

@SpringBootApplication
public class VehicleServiceApplication {

    public static void main(String[] a) {
        SpringApplication.run(
                VehicleServiceApplication.class,
                a
        );
    }
}