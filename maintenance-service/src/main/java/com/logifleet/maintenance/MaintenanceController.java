package com.logifleet.maintenance;

import java.time.*;
import java.util.*;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/maintenance")
public class MaintenanceController {

    final MaintenanceRepository r;

    MaintenanceController(MaintenanceRepository r) {
        this.r = r;
    }

    @GetMapping
    public List<Maintenance> all(
            @RequestParam(required = false) Long vehicleId) {

        return vehicleId == null
                ? r.findAll()
                : r.findByVehicleId(vehicleId);
    }

    @GetMapping("/{id}")
    Maintenance one(@PathVariable Long id) {

        return r.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Maintenance order not found"
                        )
                );
    }

    @GetMapping("/vehicle/{vehicleId}/clear")
    Map<String, Boolean> clear(@PathVariable Long vehicleId) {

        return Map.of(
                "clear",
                r.findByVehicleIdAndStatusNot(
                        vehicleId,
                        "COMPLETED"
                ).isEmpty()
        );
    }

    @PostMapping
    public ResponseEntity<Maintenance> create(
            @RequestBody Maintenance m) {

        m.id = null;

        if (m.status == null) {
            m.status = "OPEN";
        }

        m.openedOn = LocalDate.now();

        return ResponseEntity
                .status(201)
                .body(r.save(m));
    }

    @PatchMapping("/{id}/status")
    Maintenance status(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String notes) {

        Maintenance m = one(id);

        m.status = status.toUpperCase();

        if (notes != null) {
            m.notes = notes;
        }

        if ("COMPLETED".equals(m.status)) {
            m.completedOn = LocalDate.now();
        }

        return r.save(m);
    }
}