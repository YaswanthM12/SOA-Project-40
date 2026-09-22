package com.logifleet.trip;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByVehicleId(Long vehicleId);
}