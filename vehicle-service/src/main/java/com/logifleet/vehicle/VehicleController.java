package com.logifleet.vehicle;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vehicles")
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
public class VehicleController {

    final VehicleRepository r;

    VehicleController(VehicleRepository r) {
        this.r = r;
    }

    @GetMapping
    public List<Vehicle> all(
            @RequestParam(name = "status", required = false) String status
    ) {
        return status == null
                ? r.findAll()
                : r.findAll()
                        .stream()
                        .filter(v -> status.equalsIgnoreCase(v.status))
                        .toList();
    }

    @GetMapping("/{id}")
    public Vehicle one(@PathVariable("id") Long id) {
        return r.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Vehicle not found"));
    }

    @PostMapping
    public ResponseEntity<Vehicle> create(@RequestBody Vehicle v) {
        v.id = null;

        if (v.status == null) {
            v.status = "AVAILABLE";
        }

        return ResponseEntity.status(201).body(r.save(v));
    }

    @PutMapping("/{id}")
    public Vehicle update(@PathVariable("id") Long id, @RequestBody Vehicle x) {
        Vehicle v = one(id);

        v.registrationNumber = x.registrationNumber;
        v.type = x.type;
        v.status = x.status;
        v.location = x.location;

        return r.save(v);
    }

    @PatchMapping("/{id}/status")
    public Vehicle status(@PathVariable("id") Long id, @RequestParam("status") String status) {
        Vehicle v = one(id);
        v.status = status.toUpperCase();
        return r.save(v);
    }

    @PatchMapping("/{id}/location")
    public Vehicle location(@PathVariable("id") Long id, @RequestParam("location") String location) {
        Vehicle v = one(id);
        v.location = location;
        return r.save(v);
    }
}
