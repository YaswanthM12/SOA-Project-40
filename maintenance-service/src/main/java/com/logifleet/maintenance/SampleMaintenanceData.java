package com.logifleet.maintenance;

import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleMaintenanceData {
    @Bean
    CommandLineRunner seedMaintenance(MaintenanceRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                Maintenance record = new Maintenance();
                record.vehicleId = 2L;
                record.issue = "Scheduled engine oil and filter replacement";
                record.status = "COMPLETED";
                record.notes = "Completed during scheduled preventive maintenance.";
                record.openedOn = LocalDate.now().minusDays(7);
                record.completedOn = LocalDate.now().minusDays(7);
                repository.save(record);
            }
        };
    }
}
