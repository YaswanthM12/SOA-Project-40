package com.logifleet.maintenance;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;

@SpringBootApplication
public class MaintenanceServiceApplication {

    public static void main(String[] a) {
        SpringApplication.run(
                MaintenanceServiceApplication.class,
                a
        );
    }
}