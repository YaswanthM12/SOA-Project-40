package com.logifleet.maintenance;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

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
@RequestMapping("/maintenance")
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
public class MaintenanceController {
    final MaintenanceRepository r;

    MaintenanceController(MaintenanceRepository r) {
        this.r = r;
    }

    @GetMapping
    public List<Maintenance> all(@RequestParam(name = "vehicleId", required = false) Long vehicleId) {
        return vehicleId == null ? r.findAll() : r.findByVehicleId(vehicleId);
    }

    @GetMapping("/{id}")
    Maintenance one(@PathVariable("id") Long id) {
        return r.findById(id).orElseThrow(() -> new NoSuchElementException("Maintenance order not found"));
    }

    @GetMapping("/vehicle/{vehicleId}/clear")
    Map<String, Boolean> clear(@PathVariable("vehicleId") Long vehicleId) {
        return Map.of("clear", r.findByVehicleIdAndStatusNot(vehicleId, "COMPLETED").isEmpty());
    }

    @PostMapping
    public ResponseEntity<Maintenance> create(@RequestBody Maintenance m) {
        m.id = null;

        if (m.status == null) {
            m.status = "OPEN";
        }

        m.openedOn = LocalDate.now();
        return ResponseEntity.status(201).body(r.save(m));
    }

    @PatchMapping("/{id}/status")
    Maintenance status(@PathVariable("id") Long id, @RequestParam("status") String status,
            @RequestParam(name = "notes", required = false) String notes) {
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
