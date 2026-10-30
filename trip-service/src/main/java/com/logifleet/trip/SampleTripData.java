package com.logifleet.trip;

import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleTripData {
    @Bean
    CommandLineRunner seedTrips(TripRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                Trip trip = new Trip();
                trip.vehicleId = 1L;
                trip.driverId = "DRV-100";
                trip.origin = "Bengaluru Depot";
                trip.destination = "Chennai Warehouse";
                trip.tripStatus = "COMPLETED";
                trip.assignedAt = LocalDateTime.now().minusDays(2);
                trip.startedAt = LocalDateTime.now().minusDays(2).plusHours(1);
                trip.completedAt = LocalDateTime.now().minusDays(1);
                repository.save(trip);
            }
        };
    }
}
