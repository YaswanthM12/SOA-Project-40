package com.logifleet.maintenance;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRepository
        extends JpaRepository<Maintenance, Long> {

    List<Maintenance> findByVehicleId(Long vehicleId);

    List<Maintenance> findByVehicleIdAndStatusNot(
            Long vehicleId,
            String status
    );
}