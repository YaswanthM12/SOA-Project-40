package com.logifleet.trip;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trips")
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
public class TripController {

    final TripRepository r;
    final VehicleClient vehicles;
    final MaintenanceClient maintenance;

    TripController(TripRepository r, VehicleClient v, MaintenanceClient m) {
        this.r = r;
        vehicles = v;
        maintenance = m;
    }

    @GetMapping
    public List<Trip> all(@RequestParam(name = "vehicleId", required = false) Long vehicleId) {
        return vehicleId == null ? r.findAll() : r.findByVehicleId(vehicleId);
    }

    @GetMapping("/{id}")
    Trip one(@PathVariable("id") Long id) {
        return r.findById(id).orElseThrow(() -> new NoSuchElementException("Trip not found"));
    }

    @PostMapping
    public ResponseEntity<?> assign(@RequestBody Trip t) {
        if (t.vehicleId == null || t.driverId == null) {
            return ResponseEntity.badRequest().body("vehicleId and driverId are required");
        }

        Map<String, Object> vehicle = vehicles.get(t.vehicleId);
        if (!"AVAILABLE".equalsIgnoreCase(String.valueOf(vehicle.get("status")))) {
            return ResponseEntity.status(409).body("Vehicle is not available");
        }

        if (!Boolean.TRUE.equals(maintenance.clear(t.vehicleId).get("clear"))) {
            return ResponseEntity.status(409).body("Vehicle has open maintenance work orders");
        }

        t.id = null;
        t.tripStatus = "ASSIGNED";
        t.assignedAt = LocalDateTime.now();

        Trip saved = r.save(t);
        vehicles.status(t.vehicleId, "ON_TRIP");

        return ResponseEntity.status(201).body(saved);
    }

    @PatchMapping("/{id}/status")
    public Trip status(@PathVariable("id") Long id, @RequestParam("status") String status) {
        Trip t = one(id);
        String next = status.toUpperCase();

        if (!Set.of("IN_PROGRESS", "COMPLETED", "CANCELLED").contains(next)) {
            throw new IllegalArgumentException("status must be IN_PROGRESS, COMPLETED or CANCELLED");
        }

        if ("IN_PROGRESS".equals(next) && !"ASSIGNED".equals(t.tripStatus)) {
            throw new IllegalStateException("Only assigned trips can start");
        }

        if ("COMPLETED".equals(next) && !"IN_PROGRESS".equals(t.tripStatus)) {
            throw new IllegalStateException("Only in-progress trips can complete");
        }

        t.tripStatus = next;

        if ("IN_PROGRESS".equals(next)) {
            t.startedAt = LocalDateTime.now();
        }

        if ("COMPLETED".equals(next) || "CANCELLED".equals(next)) {
            t.completedAt = LocalDateTime.now();
            vehicles.status(t.vehicleId, "AVAILABLE");
        }

        return r.save(t);
    }
}
