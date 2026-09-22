package com.logifleet.vehicle;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleVehicleData {
    @Bean
    CommandLineRunner seedVehicles(VehicleRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                Vehicle truck = new Vehicle();
                truck.registrationNumber = "KA01AB1234";
                truck.type = "TRUCK";
                truck.status = "AVAILABLE";
                truck.location = "Bengaluru Depot";
                repository.save(truck);

                Vehicle van = new Vehicle();
                van.registrationNumber = "KA02CD5678";
                van.type = "DELIVERY_VAN";
                van.status = "AVAILABLE";
                van.location = "Mysuru Depot";
                repository.save(van);
            }
        };
    }
}
